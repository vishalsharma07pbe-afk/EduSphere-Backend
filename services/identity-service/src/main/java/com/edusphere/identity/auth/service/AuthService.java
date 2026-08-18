package com.edusphere.identity.auth.service;

import com.edusphere.identity.auth.dto.LoginRequest;
import com.edusphere.identity.auth.model.AuthenticationResult;

public interface AuthService {

    AuthenticationResult login(
            LoginRequest request
    );

    AuthenticationResult refresh(
            String rawRefreshToken
    );

    void logout(
            String rawRefreshToken
    );
}