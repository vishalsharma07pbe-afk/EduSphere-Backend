package com.edusphere.identity.roleremoval.service;

import com.edusphere.identity.auth.security.AuthorizationContext;
import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.common.exception.DuplicateResourceException;
import com.edusphere.identity.common.exception.ResourceNotFoundException;
import com.edusphere.identity.permission.enums.PermissionCode;
import com.edusphere.identity.roleapproval.enums.ApprovalStatus;
import com.edusphere.identity.roleapproval.exception.ApprovalNotAllowedException;
import com.edusphere.identity.roleapproval.exception.InvalidApprovalStateException;
import com.edusphere.identity.roleapproval.exception.InvalidRoleRequestException;
import com.edusphere.identity.roleremoval.dto.CreateRoleRemovalRequest;
import com.edusphere.identity.roleremoval.dto.RoleRemovalApprovalResponse;
import com.edusphere.identity.roleremoval.dto.RoleRemovalRequestDetailsResponse;
import com.edusphere.identity.roleremoval.dto.RoleRemovalRequestResponse;
import com.edusphere.identity.roleremoval.entity.RoleRemovalRequest;
import com.edusphere.identity.roleremoval.mapper.RoleRemovalApprovalMapper;
import com.edusphere.identity.roleremoval.mapper.RoleRemovalRequestMapper;
import com.edusphere.identity.roleremoval.policy.ProtectedRoleRemovalPolicy;
import com.edusphere.identity.roleremoval.policy.RoleRemovalApprovalPolicy;
import com.edusphere.identity.roleremoval.repository.RoleRemovalApprovalRepository;
import com.edusphere.identity.roleremoval.repository.RoleRemovalRequestRepository;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.user.enums.UserRole;
import com.edusphere.identity.user.enums.UserStatus;
import com.edusphere.identity.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class RoleRemovalRequestServiceImpl
        implements RoleRemovalRequestService {

    private final RoleRemovalRequestRepository requestRepository;
    private final RoleRemovalApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final RoleRemovalApprovalPolicy approvalPolicy;
    private final ProtectedRoleRemovalPolicy protectedRoleRemovalPolicy;
    private final RoleRemovalRequestMapper requestMapper;
    private final RoleRemovalApprovalMapper approvalMapper;

    public RoleRemovalRequestServiceImpl(
            RoleRemovalRequestRepository requestRepository,
            RoleRemovalApprovalRepository approvalRepository,
            UserRepository userRepository,
            RoleRemovalApprovalPolicy approvalPolicy,
            ProtectedRoleRemovalPolicy protectedRoleRemovalPolicy,
            RoleRemovalRequestMapper requestMapper,
            RoleRemovalApprovalMapper approvalMapper
    ) {
        this.requestRepository = requestRepository;
        this.approvalRepository = approvalRepository;
        this.userRepository = userRepository;
        this.approvalPolicy = approvalPolicy;
        this.protectedRoleRemovalPolicy = protectedRoleRemovalPolicy;
        this.requestMapper = requestMapper;
        this.approvalMapper = approvalMapper;
    }

    @Override
    @Transactional
    public RoleRemovalRequestResponse createRequest(
            Long organizationId,
            AuthorizationContext authorizationContext,
            CreateRoleRemovalRequest request
    ) {
        requirePermission(
                authorizationContext,
                PermissionCode.ROLE_REMOVAL_REQUEST_CREATE
        );

        User requester = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        authorizationContext.getUserId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requesting user not found"
                ));

        if (requester.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidRoleRequestException(
                    "Only an active user can submit a role removal request"
            );
        }

        if (!approvalPolicy.requiresApproval(request.getRequestedRole())) {
            throw new InvalidRoleRequestException(
                    "Routine role removal must use the routine role update endpoint"
            );
        }

        if (!approvalPolicy.canRequestRemoval(
                requester.getRoles(),
                request.getRequestedRole()
        )) {
            throw new InvalidRoleRequestException(
                    "You are not allowed to request removal of this role"
            );
        }

        User targetUser = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        request.getUserId()
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Target user not found"
                ));

        if (!targetUser.hasRole(request.getRequestedRole())) {
            throw new InvalidRoleRequestException(
                    "The target user does not currently have the requested role"
            );
        }

        protectedRoleRemovalPolicy.assertRemovalAllowed(
                organizationId,
                targetUser,
                request.getRequestedRole()
        );

        boolean pendingRequestExists = requestRepository
                .existsByOrganizationIdAndUserIdAndRequestedRoleAndStatus(
                        organizationId,
                        targetUser.getId(),
                        request.getRequestedRole(),
                        ApprovalStatus.PENDING
                );

        if (pendingRequestExists) {
            throw new DuplicateResourceException(
                    "A pending removal request already exists for this user and role"
            );
        }

        RoleRemovalRequest roleRequest =
                requestMapper.toEntity(
                        organizationId,
                        requester.getId(),
                        request
                );

        return requestMapper.toResponse(
                requestRepository.save(roleRequest)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public RoleRemovalRequestDetailsResponse getRequestDetails(
            Long organizationId,
            Long requestId,
            Long viewerUserId
    ) {
        RoleRemovalRequest roleRequest = requestRepository
                .findByIdAndOrganizationId(requestId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role removal request not found"
                ));

        User viewer = userRepository
                .findByOrganizationIdAndId(organizationId, viewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Viewing user not found"
                ));

        Set<UserRole> reviewableRoles =
                approvalPolicy.getReviewableRoles(viewer.getRoles());

        boolean isRequester =
                viewer.getId().equals(roleRequest.getRequestedByUserId());
        boolean isTargetUser =
                viewer.getId().equals(roleRequest.getUserId());
        boolean isEligibleApprover =
                reviewableRoles.contains(roleRequest.getRequestedRole());

        if (!isRequester && !isTargetUser && !isEligibleApprover) {
            throw new ApprovalNotAllowedException(
                    "You are not allowed to view this role removal request"
            );
        }

        List<RoleRemovalApprovalResponse> approvalHistory =
                approvalRepository
                        .findAllByRequestIdOrderByDecidedAtAsc(requestId)
                        .stream()
                        .map(approvalMapper::toResponse)
                        .toList();

        return new RoleRemovalRequestDetailsResponse(
                requestMapper.toResponse(roleRequest),
                approvalHistory
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleRemovalRequestResponse>
    getActionableRequestsForApprover(
            Long organizationId,
            Long approverUserId,
            Pageable pageable
    ) {
        User approver = userRepository
                .findByOrganizationIdAndId(organizationId, approverUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Approver user not found"
                ));

        if (approver.getStatus() != UserStatus.ACTIVE) {
            throw new ApprovalNotAllowedException(
                    "Only an active user can view removal requests"
            );
        }

        Set<UserRole> reviewableRoles =
                approvalPolicy.getReviewableRoles(approver.getRoles());

        if (reviewableRoles.isEmpty()) {
            return PageResponse.from(Page.empty(pageable));
        }

        return PageResponse.from(
                requestRepository
                        .findActionableRequestsForApprover(
                                organizationId,
                                ApprovalStatus.PENDING,
                                reviewableRoles,
                                approverUserId,
                                pageable
                        )
                        .map(requestMapper::toResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleRemovalRequestResponse> getRequesterHistory(
            Long organizationId,
            Long requesterUserId,
            Pageable pageable
    ) {
        return PageResponse.from(
                requestRepository
                        .findAllByOrganizationIdAndRequestedByUserId(
                                organizationId,
                                requesterUserId,
                                pageable
                        )
                        .map(requestMapper::toResponse)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RoleRemovalRequestResponse> getTargetUserHistory(
            Long organizationId,
            Long targetUserId,
            Pageable pageable
    ) {
        return PageResponse.from(
                requestRepository
                        .findAllByOrganizationIdAndUserId(
                                organizationId,
                                targetUserId,
                                pageable
                        )
                        .map(requestMapper::toResponse)
        );
    }

    @Override
    @Transactional
    public RoleRemovalRequestResponse cancelRequest(
            Long organizationId,
            Long requestId,
            AuthorizationContext authorizationContext
    ) {
        requirePermission(
                authorizationContext,
                PermissionCode.ROLE_REMOVAL_REQUEST_CANCEL
        );

        RoleRemovalRequest roleRequest = requestRepository
                .findByIdAndOrganizationId(requestId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role removal request not found"
                ));

        if (!roleRequest.getRequestedByUserId().equals(
                authorizationContext.getUserId()
        )) {
            throw new ApprovalNotAllowedException(
                    "Only the original requester can cancel this role removal request"
            );
        }

        if (!roleRequest.isPending()) {
            throw new InvalidApprovalStateException(
                    "Only a pending role removal request can be cancelled"
            );
        }

        roleRequest.cancel();
        return requestMapper.toResponse(roleRequest);
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
