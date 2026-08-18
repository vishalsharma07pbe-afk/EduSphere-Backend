package com.edusphere.identity.auth.service;

import com.edusphere.identity.auth.dto.LoginRequest;
import com.edusphere.identity.auth.dto.LoginResponse;
import com.edusphere.identity.auth.exception.AccountNotActiveException;
import com.edusphere.identity.auth.exception.InvalidCredentialsException;
import com.edusphere.identity.auth.model.AuthenticationResult;
import com.edusphere.identity.auth.refreshtoken.model.RefreshTokenRotationResult;
import com.edusphere.identity.auth.refreshtoken.service.RefreshTokenService;
import com.edusphere.identity.auth.security.JwtService;
import com.edusphere.identity.common.exception.ResourceNotFoundException;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.user.enums.UserRole;
import com.edusphere.identity.user.enums.UserStatus;
import com.edusphere.identity.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public AuthenticationResult login(
            LoginRequest request
    ) {
        User user = userRepository
                .findByOrganizationIdAndUsername(
                        request.getOrganizationId(),
                        request.getUsername()
                )
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid username or password"
                ));

        boolean passwordMatches =
                user.getPasswordHash() != null
                        && passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new InvalidCredentialsException(
                    "Invalid username or password"
            );
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException(
                    "Account is not active"
            );
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(OffsetDateTime.now());

        String rawRefreshToken =
                refreshTokenService.createRefreshToken(
                        user.getId()
                );

        return createAuthenticationResult(
                user,
                rawRefreshToken
        );
    }

    @Override
    @Transactional
    public AuthenticationResult refresh(
            String rawRefreshToken
    ) {
        RefreshTokenRotationResult rotationResult =
                refreshTokenService.rotateRefreshToken(
                        rawRefreshToken
                );

        User user = userRepository
                .findById(rotationResult.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            refreshTokenService.revokeAllForUser(
                    user.getId()
            );

            throw new AccountNotActiveException(
                    "Account is not active"
            );
        }

        return createAuthenticationResult(
                user,
                rotationResult.rawRefreshToken()
        );
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeRefreshToken(
                rawRefreshToken
        );
    }

    private AuthenticationResult createAuthenticationResult(
            User user,
            String rawRefreshToken
    ) {
        String accessToken =
                jwtService.generateAccessToken(user);

        Set<String> roles = user.getRoles()
                .stream()
                .map(UserRole::name)
                .collect(Collectors.toSet());

        LoginResponse response = new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                user.getId(),
                user.getOrganizationId(),
                user.getUsername(),
                roles
        );

        return new AuthenticationResult(
                response,
                rawRefreshToken
        );
    }
}