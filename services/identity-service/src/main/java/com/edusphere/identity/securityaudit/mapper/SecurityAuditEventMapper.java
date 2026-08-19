package com.edusphere.identity.securityaudit.mapper;

import com.edusphere.identity.securityaudit.dto.SecurityAuditEventResponse;
import com.edusphere.identity.securityaudit.entity.SecurityAuditEvent;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditEventMapper {

    public SecurityAuditEventResponse toResponse(
            SecurityAuditEvent event
    ) {
        return new SecurityAuditEventResponse(
                event.getId(),
                event.getOrganizationId(),
                event.getActorUserId(),
                event.getAction(),
                event.getOutcome(),
                event.getTargetType(),
                event.getTargetId(),
                event.getRequestId(),
                event.getIpAddress(),
                event.getDetails(),
                event.getOccurredAt()
        );
    }
}
