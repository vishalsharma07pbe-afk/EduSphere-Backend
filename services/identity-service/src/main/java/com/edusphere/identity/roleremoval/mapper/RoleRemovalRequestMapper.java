package com.edusphere.identity.roleremoval.mapper;

import com.edusphere.identity.roleapproval.enums.ApprovalDecision;
import com.edusphere.identity.roleapproval.policy.RoleApprovalPolicy;
import com.edusphere.identity.roleremoval.dto.CreateRoleRemovalRequest;
import com.edusphere.identity.roleremoval.dto.RoleRemovalRequestResponse;
import com.edusphere.identity.roleremoval.entity.RoleRemovalApproval;
import com.edusphere.identity.roleremoval.entity.RoleRemovalRequest;
import com.edusphere.identity.roleremoval.repository.RoleRemovalApprovalRepository;
import com.edusphere.identity.user.enums.UserRole;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class RoleRemovalRequestMapper {

    private final RoleApprovalPolicy roleApprovalPolicy;
    private final RoleRemovalApprovalRepository approvalRepository;

    public RoleRemovalRequestMapper(
            RoleApprovalPolicy roleApprovalPolicy,
            RoleRemovalApprovalRepository approvalRepository
    ) {
        this.roleApprovalPolicy = roleApprovalPolicy;
        this.approvalRepository = approvalRepository;
    }

    public RoleRemovalRequest toEntity(
            Long organizationId,
            Long requestedByUserId,
            CreateRoleRemovalRequest request
    ) {
        return new RoleRemovalRequest(
                organizationId,
                request.getUserId(),
                request.getRequestedRole(),
                requestedByUserId,
                request.getReason()
        );
    }

    public RoleRemovalRequestResponse toResponse(
            RoleRemovalRequest request
    ) {
        Set<UserRole> collected = approvalRepository
                .findAllByRequestId(request.getId())
                .stream()
                .filter(approval ->
                        approval.getDecision() == ApprovalDecision.APPROVED
                )
                .map(RoleRemovalApproval::getApproverRole)
                .collect(
                        HashSet::new,
                        HashSet::add,
                        HashSet::addAll
                );

        Set<UserRole> stillRequired = new HashSet<>(
                roleApprovalPolicy.getRequiredApproverRoles(
                        request.getRequestedRole()
                )
        );
        stillRequired.removeAll(collected);

        return new RoleRemovalRequestResponse(
                request.getId(),
                request.getOrganizationId(),
                request.getUserId(),
                request.getRequestedRole(),
                request.getRequestedByUserId(),
                request.getReason(),
                request.getStatus(),
                collected,
                stillRequired,
                request.getCompletedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
