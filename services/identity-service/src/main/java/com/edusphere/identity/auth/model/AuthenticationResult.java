package com.edusphere.identity.auth.model;

import com.edusphere.identity.auth.dto.LoginResponse;

public record AuthenticationResult(
        LoginResponse response,
        String rawRefreshToken
) {
}