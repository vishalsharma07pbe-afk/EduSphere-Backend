package com.edusphere.identity.roleapproval.service;

import com.edusphere.identity.auth.security.AuthorizationContext;
import com.edusphere.identity.permission.enums.PermissionCode;
import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.common.exception.DuplicateResourceException;
import com.edusphere.identity.roleapproval.dto.RoleApprovalDecisionRequest;
import com.edusphere.identity.roleapproval.dto.RoleAssignmentApprovalResponse;
import com.edusphere.identity.roleapproval.entity.RoleAssignmentApproval;
import com.edusphere.identity.roleapproval.entity.RoleAssignmentRequest;
import com.edusphere.identity.roleapproval.enums.ApprovalDecision;
import com.edusphere.identity.roleapproval.enums.ApprovalStatus;
import com.edusphere.identity.roleapproval.exception.ApprovalNotAllowedException;
import com.edusphere.identity.roleapproval.exception.InvalidApprovalStateException;
import com.edusphere.identity.roleapproval.mapper.RoleAssignmentApprovalMapper;
import com.edusphere.identity.roleapproval.policy.RoleApprovalPolicy;
import com.edusphere.identity.roleapproval.repository.RoleAssignmentApprovalRepository;
import com.edusphere.identity.roleapproval.repository.RoleAssignmentRequestRepository;
import com.edusphere.identity.auth.activation.event.UserActivationRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.user.enums.UserRole;
import com.edusphere.identity.user.enums.UserStatus;
import com.edusphere.identity.common.exception.ResourceNotFoundException;
import com.edusphere.identity.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoleAssignmentApprovalServiceImpl
        implements RoleAssignmentApprovalService {

    private final RoleAssignmentRequestRepository requestRepository;
    private final RoleAssignmentApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final RoleApprovalPolicy approvalPolicy;
    private final RoleAssignmentApprovalMapper approvalMapper;
    private final ApplicationEventPublisher eventPublisher;

    public RoleAssignmentApprovalServiceImpl(
            RoleAssignmentRequestRepository requestRepository,
            RoleAssignmentApprovalRepository approvalRepository,
            UserRepository userRepository,
            RoleApprovalPolicy approvalPolicy,
            RoleAssignmentApprovalMapper approvalMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.requestRepository = requestRepository;
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.approvalPolicy = approvalPolicy;
        this.approvalMapper = approvalMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public RoleAssignmentApprovalResponse recordDecision(
            Long organizationId,
            Long requestId,
            AuthorizationContext authorizationContext,
            RoleApprovalDecisionRequest decisionRequest
    ) {
        requirePermission(
                authorizationContext,
                PermissionCode.ROLE_ASSIGNMENT_APPROVE
        );

        RoleAssignmentRequest roleRequest = requestRepository
                .findByIdAndOrganizationId(
                        requestId,
                        organizationId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role assignment request not found"
                ));

        if (!roleRequest.isPending()) {
            throw new InvalidApprovalStateException(
                    "Only a pending role request can be reviewed"
            );
        }

        User approver = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        authorizationContext.getUserId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Approver user not found"
                ));

        if (approver.getStatus() != UserStatus.ACTIVE) {
            throw new ApprovalNotAllowedException(
                    "Only an active user can review a role request"
            );
        }

        if (roleRequest.getRequestedByUserId()
                .equals(approver.getId())) {
            throw new ApprovalNotAllowedException(
                    "The requester cannot approve their own request"
            );
        }

        if (roleRequest.getUserId().equals(approver.getId())) {
            throw new ApprovalNotAllowedException(
                    "A user cannot approve a role requested for themselves"
            );
        }

        boolean alreadyDecided = approvalRepository
                .existsByRequestIdAndApproverUserId(
                        requestId,
                        approver.getId()
                );

        if (alreadyDecided) {
            throw new DuplicateResourceException(
                    "You have already decided on this role request"
            );
        }

        List<RoleAssignmentApproval> existingApprovals =
                approvalRepository.findAllByRequestId(requestId);

        Set<UserRole> satisfiedApproverRoles = existingApprovals
                .stream()
                .filter(approval ->
                        approval.getDecision()
                                == ApprovalDecision.APPROVED
                )
                .map(RoleAssignmentApproval::getApproverRole)
                .collect(
                        HashSet::new,
                        HashSet::add,
                        HashSet::addAll
                );

        UserRole actingApproverRole = approvalPolicy
                .findAvailableApproverRole(
                        roleRequest.getRequestedRole(),
                        approver.getRoles(),
                        satisfiedApproverRoles
                )
                .orElseThrow(() -> new ApprovalNotAllowedException(
                        "You are not authorized to review this role request"
                ));

        // Store the approver role that satisfied this approval requirement.
        RoleAssignmentApproval approval =
                approvalMapper.toEntity(
                        roleRequest.getId(),
                        approver.getId(),
                        actingApproverRole,
                        decisionRequest
                );

        RoleAssignmentApproval savedApproval =
                approvalRepository.save(approval);

        /*
         * Any rejection completes this individual role request.
         * The requested role is not assigned.
         */
        if (decisionRequest.getDecision()
                == ApprovalDecision.REJECTED) {
            roleRequest.reject();

            updateOnboardingStatusIfComplete(
                    organizationId,
                    roleRequest
            );

            return approvalMapper.toResponse(savedApproval);
        }

        satisfiedApproverRoles.add(actingApproverRole);

        Set<UserRole> requiredApproverRoles =
                approvalPolicy.getRequiredApproverRoles(
                        roleRequest.getRequestedRole()
                );

        boolean allRequiredApprovalsReceived =
                satisfiedApproverRoles.containsAll(
                        requiredApproverRoles
                );

        /*
         * Assign the role only after every required approver role
         * has submitted an approval.
         */
        if (allRequiredApprovalsReceived) {
            User targetUser = userRepository
                    .findByOrganizationIdAndId(
                            organizationId,
                            roleRequest.getUserId()
                    )
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Target user not found"
                    ));

            targetUser.addRole(
                    roleRequest.getRequestedRole()
            );

            roleRequest.approve();

            // New sensitive-role users can activate only after approvals finish.
            updateOnboardingStatusIfComplete(
                    organizationId,
                    roleRequest
            );
        }

        return approvalMapper.toResponse(savedApproval);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleAssignmentApprovalResponse>
    getDecisionHistoryForApprover(
            Long organizationId,
            Long approverUserId,
            Pageable pageable
    ) {
        User approver = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        approverUserId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Approver user not found"
                ));

        if (approver.getStatus() != UserStatus.ACTIVE) {
            throw new ApprovalNotAllowedException(
                    "Only an active user can view decision history"
            );
        }

        Page<RoleAssignmentApprovalResponse> responsePage =
                approvalRepository
                        .findDecisionHistoryForApprover(
                                organizationId,
                                approver.getId(),
                                pageable
                        )
                        .map(approvalMapper::toResponse);

        return PageResponse.from(responsePage);
    }

    /*
     * This method only changes accounts that are in the onboarding
     * PENDING_APPROVAL state. Existing ACTIVE users remain ACTIVE
     * when an additional role request is approved or rejected.
     */
    private void updateOnboardingStatusIfComplete(
            Long organizationId,
            RoleAssignmentRequest completedRequest
    ) {
        User targetUser = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        completedRequest.getUserId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Target user not found"
                ));

        if (targetUser.getStatus()
                != UserStatus.PENDING_APPROVAL) {
            return;
        }

        /*
         * Exclude the request just completed. This prevents the result
         * from depending on whether Hibernate has flushed its new status.
         */
        boolean anotherPendingRequestExists =
                requestRepository
                        .existsByOrganizationIdAndUserIdAndStatusAndIdNot(
                                organizationId,
                                targetUser.getId(),
                                ApprovalStatus.PENDING,
                                completedRequest.getId()
                        );

        if (anotherPendingRequestExists) {
            return;
        }

        /*
         * At least one assigned role means the account may continue
         * to password activation. This includes routine roles and any
         * sensitive roles that were approved.
         */
        if (!targetUser.getRoles().isEmpty()) {
            targetUser.setStatus(
                    UserStatus.PENDING_ACTIVATION
            );

            // Send the activation link only after the approval transaction commits.
            eventPublisher.publishEvent(
                    new UserActivationRequestedEvent(
                            targetUser.getId()
                    )
            );
        } else {
            // Keep accounts without any approved or routine role unusable.
            targetUser.setStatus(UserStatus.INACTIVE);
        }
    }

    private void requirePermission(
            AuthorizationContext authorizationContext,
            PermissionCode permissionCode
    ) {
        if (authorizationContext == null
                || !authorizationContext.hasPermission(permissionCode)) {
            throw new ApprovalNotAllowedException(
                    "Missing required permission: " + permissionCode
            );
        }
    }
}
