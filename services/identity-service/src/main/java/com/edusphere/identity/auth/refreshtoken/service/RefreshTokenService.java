package com.edusphere.identity.auth.refreshtoken.service;

import com.edusphere.identity.auth.refreshtoken.model.RefreshTokenRotationResult;

public interface RefreshTokenService {

    String createRefreshToken(
            Long userId
    );

    RefreshTokenRotationResult rotateRefreshToken(
            String rawRefreshToken
    );

    void revokeRefreshToken(
            String rawRefreshToken
    );

    void revokeAllForUser(
            Long userId
    );
}