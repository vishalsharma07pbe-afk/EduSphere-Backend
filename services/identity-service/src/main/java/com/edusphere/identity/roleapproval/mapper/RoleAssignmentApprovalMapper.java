package com.edusphere.identity.roleapproval.mapper;

import com.edusphere.identity.roleapproval.dto.RoleApprovalDecisionRequest;
import com.edusphere.identity.roleapproval.dto.RoleAssignmentApprovalResponse;
import com.edusphere.identity.roleapproval.entity.RoleAssignmentApproval;
import com.edusphere.identity.user.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class RoleAssignmentApprovalMapper {

    public RoleAssignmentApproval toEntity(
            Long requestId,
            Long approverUserId,
            UserRole approverRole,
            RoleApprovalDecisionRequest decisionRequest
    ) {
        return new RoleAssignmentApproval(
                requestId,
                approverUserId,
                approverRole,
                decisionRequest.getDecision(),
                decisionRequest.getRemarks()
        );
    }

    public RoleAssignmentApprovalResponse toResponse(
            RoleAssignmentApproval approval
    ) {
        return new RoleAssignmentApprovalResponse(
                approval.getId(),
                approval.getRequestId(),
                approval.getApproverUserId(),
                approval.getApproverRole(),
                approval.getDecision(),
                approval.getRemarks(),
                approval.getDecidedAt()
        );
    }
}