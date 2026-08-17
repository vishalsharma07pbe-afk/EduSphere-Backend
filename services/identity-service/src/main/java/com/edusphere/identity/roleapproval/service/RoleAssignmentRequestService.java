package com.edusphere.identity.roleapproval.service;

import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.roleapproval.dto.CreateRoleAssignmentRequest;
import com.edusphere.identity.roleapproval.dto.RoleAssignmentRequestResponse;
import com.edusphere.identity.roleapproval.dto.RoleAssignmentRequestDetailsResponse;
import org.springframework.data.domain.Pageable;

public interface RoleAssignmentRequestService {

    RoleAssignmentRequestResponse createRequest(
            Long organizationId,
            Long requesterUserId,
            CreateRoleAssignmentRequest request
    );

    PageResponse<RoleAssignmentRequestResponse>
    getActionableRequestsForApprover(
            Long organizationId,
            Long approverUserId,
            Pageable pageable
    );

    RoleAssignmentRequestDetailsResponse getRequestDetails(
            Long organizationId,
            Long requestId,
            Long viewerUserId
    );

    RoleAssignmentRequestResponse cancelRequest(
            Long organizationId,
            Long requestId,
            Long requesterUserId
    );
}