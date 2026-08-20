package com.edusphere.identity.organization.provisioning.service;

import com.edusphere.identity.organization.provisioning.dto.ProvisionOrganizationRequest;
import com.edusphere.identity.organization.provisioning.dto.ProvisionOrganizationResponse;

public interface OrganizationProvisioningService {

    ProvisionOrganizationResponse provision(
            String idempotencyKey,
            ProvisionOrganizationRequest request
    );
}