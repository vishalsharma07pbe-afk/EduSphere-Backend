package com.edusphere.identity.auth.security;

import com.edusphere.identity.user.entity.User;

public interface JwtService {
    String generateAccessToken(User user);
    long getAccessTokenExpirationSeconds();
}