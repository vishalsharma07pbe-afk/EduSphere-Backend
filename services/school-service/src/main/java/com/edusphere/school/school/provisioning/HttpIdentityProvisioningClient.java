package com.edusphere.school.school.provisioning;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpIdentityProvisioningClient
        implements IdentityProvisioningClient {

    private static final String PROVISIONING_PATH =
            "/internal/v1/school-provisioning/initial-authority";

    private final RestClient identityProvisioningRestClient;
    private final IdentityProvisioningProperties properties;

    public HttpIdentityProvisioningClient(
            RestClient identityProvisioningRestClient,
            IdentityProvisioningProperties properties
    ) {
        this.identityProvisioningRestClient =
                identityProvisioningRestClient;
        this.properties = properties;
    }

    @Override
    public IdentityProvisioningResponse provisionInitialAuthority(
            IdentityProvisioningRequest request,
            String idempotencyKey
    ) {
        validateConfiguration();

        IdentityProvisioningResponse response =
                identityProvisioningRestClient
                        .post()
                        .uri(PROVISIONING_PATH)
                        .header(
                                "Idempotency-Key",
                                idempotencyKey
                        )
                        .header(
                                "X-Service-Name",
                                properties.getServiceName()
                        )
                        .header(
                                "X-Internal-Api-Key",
                                properties.getApiKey()
                        )
                        .body(request)
                        .retrieve()
                        .body(IdentityProvisioningResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Identity-service returned an empty "
                            + "provisioning response"
            );
        }

        return response;
    }

    private void validateConfiguration() {
        if (properties.getApiKey() == null
                || properties.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "Identity-service internal API key "
                            + "is not configured"
            );
        }

        if (properties.getServiceName() == null
                || properties.getServiceName().isBlank()) {
            throw new IllegalStateException(
                    "Identity-service caller name is not configured"
            );
        }
    }
}