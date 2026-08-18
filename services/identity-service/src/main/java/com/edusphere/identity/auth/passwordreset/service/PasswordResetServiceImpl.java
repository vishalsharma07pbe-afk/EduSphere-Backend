package com.edusphere.identity.auth.passwordreset.service;

import com.edusphere.identity.auth.activation.exception.PasswordMismatchException;
import com.edusphere.identity.auth.activation.security.ActivationTokenCodec;
import com.edusphere.identity.auth.passwordreset.config.PasswordResetTokenProperties;
import com.edusphere.identity.auth.passwordreset.dto.CompletePasswordResetRequest;
import com.edusphere.identity.auth.passwordreset.dto.PasswordResetRequest;
import com.edusphere.identity.auth.passwordreset.entity.UserPasswordResetToken;
import com.edusphere.identity.auth.passwordreset.event.UserPasswordResetRequestedEvent;
import com.edusphere.identity.auth.passwordreset.exception.InvalidPasswordResetTokenException;
import com.edusphere.identity.auth.passwordreset.repository.UserPasswordResetTokenRepository;
import com.edusphere.identity.common.exception.ResourceNotFoundException;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.user.enums.UserStatus;
import com.edusphere.identity.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String INVALID_TOKEN_MESSAGE =
            "The password reset token is invalid or expired";
    private static final String RESET_LIMIT_MESSAGE =
            "Password can be reset only once in 24 hours. Please contact an admin for approval.";

    private final UserPasswordResetTokenRepository resetTokenRepository;
    private final UserRepository userRepository;
    private final ActivationTokenCodec tokenCodec;
    private final PasswordResetTokenProperties tokenProperties;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public PasswordResetServiceImpl(
            UserPasswordResetTokenRepository resetTokenRepository,
            UserRepository userRepository,
            ActivationTokenCodec tokenCodec,
            PasswordResetTokenProperties tokenProperties,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher
    ) {
        this.resetTokenRepository = resetTokenRepository;
        this.userRepository = userRepository;
        this.tokenCodec = tokenCodec;
        this.tokenProperties = tokenProperties;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void requestPasswordReset(PasswordResetRequest request) {
        userRepository
                .findByOrganizationIdAndEmail(
                        request.getOrganizationId(),
                        request.getEmail()
                )
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> eventPublisher.publishEvent(
                        new UserPasswordResetRequestedEvent(user.getId())
                ));
    }

    @Override
    @Transactional
    public String generatePasswordResetToken(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw invalidToken();
        }

        OffsetDateTime currentTime = OffsetDateTime.now();
        OffsetDateTime requestWindowStart =
                currentTime.minus(tokenProperties.getRequestWindow());

        long emailsGenerated =
                resetTokenRepository.countByUserIdAndCreatedAtAfter(
                        userId,
                        requestWindowStart
                );

        if (emailsGenerated
                >= tokenProperties.getMaxEmailsPerWindow()) {
            throw new IllegalStateException(
                    "Password reset email rate limit exceeded"
            );
        }

        List<UserPasswordResetToken> previousTokens =
                resetTokenRepository
                        .findAllByUserIdAndUsedAtIsNullAndRevokedAtIsNull(
                                userId
                        );

        previousTokens.forEach(token -> token.revoke(currentTime));

        String rawToken = tokenCodec.generateRawToken();
        String tokenHash = tokenCodec.hash(rawToken);

        UserPasswordResetToken resetToken =
                new UserPasswordResetToken(
                        userId,
                        tokenHash,
                        currentTime.plus(tokenProperties.getExpiration())
                );

        resetTokenRepository.save(resetToken);

        return rawToken;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPasswordResetTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }

        String tokenHash;

        try {
            tokenHash = tokenCodec.hash(rawToken);
        } catch (IllegalArgumentException exception) {
            return false;
        }

        return resetTokenRepository
                .findByTokenHash(tokenHash)
                .filter(token -> token.isValidAt(OffsetDateTime.now()))
                .flatMap(token -> userRepository.findById(token.getUserId()))
                .map(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(false);
    }

    @Override
    @Transactional
    public void completePasswordReset(
            CompletePasswordResetRequest request
    ) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException(
                    "Password and confirmation do not match"
            );
        }

        String tokenHash = tokenCodec.hash(request.getToken());

        UserPasswordResetToken resetToken =
                resetTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(this::invalidToken);

        OffsetDateTime currentTime = OffsetDateTime.now();

        if (!resetToken.isValidAt(currentTime)) {
            throw invalidToken();
        }

        User user = userRepository
                .findById(resetToken.getUserId())
                .orElseThrow(this::invalidToken);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw invalidToken();
        }

        OffsetDateTime resetWindowStart =
                currentTime.minus(tokenProperties.getResetWindow());

        long resetsCompleted =
                resetTokenRepository.countByUserIdAndUsedAtAfter(
                        user.getId(),
                        resetWindowStart
                );

        if (resetsCompleted
                >= tokenProperties.getMaxResetsPerWindow()) {
            throw new IllegalStateException(RESET_LIMIT_MESSAGE);
        }

        user.resetPassword(passwordEncoder.encode(request.getPassword()));
        resetToken.markUsed(currentTime);
    }

    private InvalidPasswordResetTokenException invalidToken() {
        return new InvalidPasswordResetTokenException(
                INVALID_TOKEN_MESSAGE
        );
    }
}
