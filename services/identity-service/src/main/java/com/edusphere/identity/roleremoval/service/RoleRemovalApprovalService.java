package com.edusphere.identity.roleremoval.service;

import com.edusphere.identity.auth.security.AuthorizationContext;
import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.roleapproval.dto.RoleApprovalDecisionRequest;
import com.edusphere.identity.roleremoval.dto.RoleRemovalApprovalResponse;
import org.springframework.data.domain.Pageable;

public interface RoleRemovalApprovalService {

    RoleRemovalApprovalResponse recordDecision(
            Long organizationId,
            Long requestId,
            AuthorizationContext authorizationContext,
            RoleApprovalDecisionRequest decisionRequest
    );

    PageResponse<RoleRemovalApprovalResponse> getDecisionHistoryForApprover(
            Long organizationId,
            Long approverUserId,
            Pageable pageable
    );
}
