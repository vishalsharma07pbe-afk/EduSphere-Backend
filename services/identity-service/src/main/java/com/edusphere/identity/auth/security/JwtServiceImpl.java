package com.edusphere.identity.auth.security;

import com.edusphere.identity.permission.enums.PermissionCode;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.user.enums.UserRole;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class JwtServiceImpl implements JwtService {

    private static final String ISSUER =
            "edusphere-identity-service";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtServiceImpl(
            JwtEncoder jwtEncoder,
            JwtProperties jwtProperties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public String generateAccessToken(
            User user,
            Set<PermissionCode> permissions
    ) {
        Instant issuedAt = Instant.now();

        Instant expiresAt = issuedAt.plusSeconds(
                jwtProperties.getAccessTokenExpiration()
        );

        List<String> roles = user.getRoles()
                .stream()
                .map(UserRole::name)
                .sorted()
                .toList();

        List<String> permissionCodes = permissions
                .stream()
                .map(PermissionCode::name)
                .sorted()
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(
                        "organizationId",
                        user.getOrganizationId()
                )
                .claim("username", user.getUsername())
                .claim("roles", roles)
                .claim("permissions", permissionCodes)
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    @Override
    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpiration();
    }
}