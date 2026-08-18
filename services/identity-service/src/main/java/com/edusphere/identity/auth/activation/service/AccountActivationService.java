package com.edusphere.identity.auth.activation.service;

import com.edusphere.identity.auth.activation.dto.CompleteAccountActivationRequest;
import com.edusphere.identity.auth.activation.dto.ResendActivationRequest;

public interface AccountActivationService {

    String generateActivationToken(
            Long userId
    );

    boolean isActivationTokenValid(
            String rawToken
    );

    void completeActivation(
            CompleteAccountActivationRequest request
    );

    void requestActivationResend(
            ResendActivationRequest request
    );
}