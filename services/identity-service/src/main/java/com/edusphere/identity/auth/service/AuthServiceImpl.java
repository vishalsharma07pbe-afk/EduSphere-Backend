package com.edusphere.identity.auth.service;

import com.edusphere.identity.auth.dto.LoginRequest;
import com.edusphere.identity.auth.dto.LoginResponse;
import com.edusphere.identity.auth.exception.AccountNotActiveException;
import com.edusphere.identity.auth.exception.InvalidCredentialsException;
import com.edusphere.identity.auth.lockout.LoginLockoutService;
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

    private static final String INVALID_CREDENTIALS_MESSAGE =
            "Invalid username or password";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginLockoutService loginLockoutService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            LoginLockoutService loginLockoutService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginLockoutService = loginLockoutService;
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
                        INVALID_CREDENTIALS_MESSAGE
                ));

        OffsetDateTime currentTime = OffsetDateTime.now();

        loginLockoutService.checkLoginAllowed(user, currentTime);

        boolean passwordMatches =
                user.getPasswordHash() != null
                        && passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            loginLockoutService.recordFailedLogin(user, currentTime);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException(
                    "Account is not active"
            );
        }

        loginLockoutService.recordSuccessfulLogin(user);
        user.setLastLoginAt(currentTime);

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
