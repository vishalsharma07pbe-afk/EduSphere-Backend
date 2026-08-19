package com.edusphere.identity.auth.security;

import com.edusphere.identity.permission.enums.PermissionCode;
import com.edusphere.identity.user.entity.User;

import java.util.Set;

public interface JwtService {

    String generateAccessToken(
            User user,
            Set<PermissionCode> permissions
    );

    long getAccessTokenExpirationSeconds();
}