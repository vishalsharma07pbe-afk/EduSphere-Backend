package com.edusphere.identity.securityaudit.service;

import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.securityaudit.dto.SecurityAuditEventResponse;
import com.edusphere.identity.securityaudit.enums.SecurityAuditAction;
import com.edusphere.identity.securityaudit.enums.SecurityAuditOutcome;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface SecurityAuditService {

    void record(
            Long organizationId,
            Long actorUserId,
            SecurityAuditAction action,
            SecurityAuditOutcome outcome,
            String targetType,
            Long targetId,
            Map<String, ?> details
    );

    PageResponse<SecurityAuditEventResponse> getEvents(
            Long organizationId,
            Pageable pageable
    );
}
