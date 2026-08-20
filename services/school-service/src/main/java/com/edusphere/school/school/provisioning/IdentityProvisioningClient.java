package com.edusphere.school.school.provisioning;

public interface IdentityProvisioningClient {

    IdentityProvisioningResponse provisionInitialAuthority(
            IdentityProvisioningRequest request,
            String idempotencyKey
    );
}