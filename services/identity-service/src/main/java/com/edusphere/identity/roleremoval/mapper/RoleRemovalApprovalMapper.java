package com.edusphere.identity.roleremoval.mapper;

import com.edusphere.identity.roleapproval.dto.RoleApprovalDecisionRequest;
import com.edusphere.identity.roleremoval.dto.RoleRemovalApprovalResponse;
import com.edusphere.identity.roleremoval.entity.RoleRemovalApproval;
import com.edusphere.identity.user.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class RoleRemovalApprovalMapper {

    public RoleRemovalApproval toEntity(
            Long requestId,
            Long approverUserId,
            UserRole approverRole,
            RoleApprovalDecisionRequest decisionRequest
    ) {
        return new RoleRemovalApproval(
                requestId,
                approverUserId,
                approverRole,
                decisionRequest.getDecision(),
                decisionRequest.getRemarks()
        );
    }

    public RoleRemovalApprovalResponse toResponse(
            RoleRemovalApproval approval
    ) {
        return new RoleRemovalApprovalResponse(
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
