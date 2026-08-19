package com.edusphere.identity.auth.security;

import com.edusphere.identity.permission.enums.PermissionCode;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class JwtServiceImplTest {

    @Test
    void generateAccessToken_writesSortedRolesAndPermissionsClaims() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        JwtProperties properties = new JwtProperties();
        properties.setAccessTokenExpiration(900);

        when(encoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(Jwt.withTokenValue("jwt-token")
                        .header("alg", "RS256")
                        .issuer("edusphere-identity-service")
                        .subject("10")
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(900))
                        .build());

        JwtServiceImpl service =
                new JwtServiceImpl(encoder, properties);

        User user = new User(
                1L,
                "teacher01",
                "Rahul",
                Set.of(UserRole.HR, UserRole.TEACHER)
        );
        ReflectionTestUtils.setField(user, "id", 10L);

        String token = service.generateAccessToken(
                user,
                Set.of(
                        PermissionCode.USER_VIEW,
                        PermissionCode.PROFILE_VIEW_SELF
                )
        );

        assertEquals("jwt-token", token);

        ArgumentCaptor<JwtEncoderParameters> captor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);

        verify(encoder).encode(captor.capture());

        assertEquals(
                List.of("HR", "TEACHER"),
                captor.getValue().getClaims().getClaim("roles")
        );
        assertEquals(
                List.of("PROFILE_VIEW_SELF", "USER_VIEW"),
                captor.getValue().getClaims().getClaim("permissions")
        );
    }
}
