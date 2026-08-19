package com.edusphere.identity.roleremoval.service;

import com.edusphere.identity.auth.security.AuthorizationContext;
import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.roleremoval.dto.CreateRoleRemovalRequest;
import com.edusphere.identity.roleremoval.dto.RoleRemovalRequestDetailsResponse;
import com.edusphere.identity.roleremoval.dto.RoleRemovalRequestResponse;
import org.springframework.data.domain.Pageable;

public interface RoleRemovalRequestService {

    RoleRemovalRequestResponse createRequest(
            Long organizationId,
            AuthorizationContext authorizationContext,
            CreateRoleRemovalRequest request
    );

    RoleRemovalRequestDetailsResponse getRequestDetails(
            Long organizationId,
            Long requestId,
            Long viewerUserId
    );

    PageResponse<RoleRemovalRequestResponse> getActionableRequestsForApprover(
            Long organizationId,
            Long approverUserId,
            Pageable pageable
    );

    PageResponse<RoleRemovalRequestResponse> getRequesterHistory(
            Long organizationId,
            Long requesterUserId,
            Pageable pageable
    );

    PageResponse<RoleRemovalRequestResponse> getTargetUserHistory(
            Long organizationId,
            Long targetUserId,
            Pageable pageable
    );

    RoleRemovalRequestResponse cancelRequest(
            Long organizationId,
            Long requestId,
            AuthorizationContext authorizationContext
    );
}
