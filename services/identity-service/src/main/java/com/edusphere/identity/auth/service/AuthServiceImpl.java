package com.edusphere.identity.auth.service;

import com.edusphere.identity.auth.dto.LoginRequest;
import com.edusphere.identity.auth.dto.LoginResponse;
import com.edusphere.identity.auth.exception.AccountNotActiveException;
import com.edusphere.identity.auth.exception.InvalidCredentialsException;
import com.edusphere.identity.auth.security.JwtService;
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

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository
                .findByOrganizationIdAndUsername(
                        request.getOrganizationId(),
                        request.getUsername()
                )
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid username or password"
                ));

        boolean passwordMatches = passwordEncoder.matches(
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

        String accessToken = jwtService.generateAccessToken(user);

        Set<String> roles = user.getRoles()
                .stream()
                .map(UserRole::name)
                .collect(Collectors.toSet());

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(OffsetDateTime.now());

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getAccessTokenExpirationSeconds(),
                user.getId(),
                user.getOrganizationId(),
                user.getUsername(),
                roles
        );
    }
}