package com.edusphere.identity.roleapproval.service;

import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.auth.security.AuthorizationContext;
import com.edusphere.identity.roleapproval.dto.RoleApprovalDecisionRequest;
import com.edusphere.identity.roleapproval.dto.RoleAssignmentApprovalResponse;
import org.springframework.data.domain.Pageable;

public interface RoleAssignmentApprovalService {

    RoleAssignmentApprovalResponse recordDecision(
            Long organizationId,
            Long requestId,
            AuthorizationContext authorizationContext,
            RoleApprovalDecisionRequest decisionRequest
    );

    PageResponse<RoleAssignmentApprovalResponse> getDecisionHistoryForApprover(
            Long organizationId,
            Long approverUserId,
            Pageable pageable
    );
}
