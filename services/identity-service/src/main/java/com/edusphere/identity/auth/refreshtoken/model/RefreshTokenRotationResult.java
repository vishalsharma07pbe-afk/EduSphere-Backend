package com.edusphere.identity.auth.refreshtoken.model;

public record RefreshTokenRotationResult(
        Long userId,
        String rawRefreshToken
) {
}