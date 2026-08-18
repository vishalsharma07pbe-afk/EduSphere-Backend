package com.edusphere.identity.auth.refreshtoken.service;

import com.edusphere.identity.auth.refreshtoken.config.RefreshTokenProperties;
import com.edusphere.identity.auth.refreshtoken.entity.RefreshToken;
import com.edusphere.identity.auth.refreshtoken.exception.InvalidRefreshTokenException;
import com.edusphere.identity.auth.refreshtoken.model.RefreshTokenRotationResult;
import com.edusphere.identity.auth.refreshtoken.repository.RefreshTokenRepository;
import com.edusphere.identity.auth.refreshtoken.security.RefreshTokenCodec;
import com.edusphere.identity.common.exception.ResourceNotFoundException;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.user.enums.UserStatus;
import com.edusphere.identity.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl
        implements RefreshTokenService {

    private static final String INVALID_TOKEN_MESSAGE =
            "The refresh token is invalid or expired";

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenCodec tokenCodec;
    private final RefreshTokenProperties tokenProperties;
    private final UserRepository userRepository;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenCodec tokenCodec,
            RefreshTokenProperties tokenProperties,
            UserRepository userRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenCodec = tokenCodec;
        this.tokenProperties = tokenProperties;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public String createRefreshToken(Long userId) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw invalidToken();
        }

        String rawToken = tokenCodec.generateRawToken();
        String tokenHash = tokenCodec.hash(rawToken);
        OffsetDateTime currentTime = OffsetDateTime.now();
        OffsetDateTime familyExpiresAt = currentTime.plus(
                tokenProperties.getAbsoluteSessionLifetime()
        );

        RefreshToken refreshToken =
                new RefreshToken(
                        userId,
                        UUID.randomUUID(),
                        tokenHash,
                        currentTime.plus(
                                tokenProperties.getExpiration()
                        ),
                        familyExpiresAt
                );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Override
    @Transactional(
            noRollbackFor = InvalidRefreshTokenException.class
    )
    public RefreshTokenRotationResult rotateRefreshToken(
            String rawRefreshToken
    ) {
        String submittedTokenHash;

        try {
            submittedTokenHash =
                    tokenCodec.hash(rawRefreshToken);
        } catch (IllegalArgumentException exception) {
            throw invalidToken();
        }

        RefreshToken existingToken =
                refreshTokenRepository
                        .findByTokenHash(submittedTokenHash)
                        .orElseThrow(this::invalidToken);

        OffsetDateTime currentTime = OffsetDateTime.now();

        if (existingToken.isRevoked()) {
            if (existingToken.getReplacedByTokenHash()
                    != null) {
                revokeTokenFamily(
                        existingToken.getTokenFamilyId(),
                        currentTime
                );
            }

            throw invalidToken();
        }

        if (existingToken.isExpired(currentTime)) {
            existingToken.revoke(currentTime);
            throw invalidToken();
        }

        if (existingToken.isFamilyExpired(currentTime)) {
            revokeTokenFamily(
                    existingToken.getTokenFamilyId(),
                    currentTime
            );

            throw invalidToken();
        }

        User user = userRepository
                .findById(existingToken.getUserId())
                .orElseThrow(this::invalidToken);

        if (user.getStatus() != UserStatus.ACTIVE) {
            revokeTokenFamily(
                    existingToken.getTokenFamilyId(),
                    currentTime
            );

            throw invalidToken();
        }

        String replacementRawToken =
                tokenCodec.generateRawToken();

        String replacementTokenHash =
                tokenCodec.hash(replacementRawToken);

        OffsetDateTime familyExpiresAt =
                existingToken.getFamilyExpiresAt();

        OffsetDateTime rollingExpiry =
                currentTime.plus(tokenProperties.getExpiration());

        OffsetDateTime effectiveExpiry =
                rollingExpiry.isBefore(familyExpiresAt)
                        ? rollingExpiry
                        : familyExpiresAt;

        RefreshToken replacementToken =
                new RefreshToken(
                        user.getId(),
                        existingToken.getTokenFamilyId(),
                        replacementTokenHash,
                        effectiveExpiry,
                        familyExpiresAt
                );

        existingToken.rotate(
                currentTime,
                replacementTokenHash
        );

        refreshTokenRepository.save(replacementToken);

        return new RefreshTokenRotationResult(
                user.getId(),
                replacementRawToken
        );
    }

    @Override
    @Transactional
    public void revokeRefreshToken(
            String rawRefreshToken
    ) {
        if (rawRefreshToken == null
                || rawRefreshToken.isBlank()) {
            return;
        }

        String tokenHash;

        try {
            tokenHash = tokenCodec.hash(rawRefreshToken);
        } catch (IllegalArgumentException exception) {
            return;
        }

        refreshTokenRepository
                .findByTokenHash(tokenHash)
                .ifPresent(token ->
                        token.revoke(OffsetDateTime.now())
                );
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        List<RefreshToken> activeTokens =
                refreshTokenRepository
                        .findAllByUserIdAndRevokedAtIsNull(
                                userId
                        );

        OffsetDateTime currentTime = OffsetDateTime.now();

        activeTokens.forEach(token ->
                token.revoke(currentTime)
        );
    }

    private void revokeTokenFamily(
            UUID tokenFamilyId,
            OffsetDateTime revokedAt
    ) {
        List<RefreshToken> activeFamilyTokens =
                refreshTokenRepository
                        .findAllByTokenFamilyIdAndRevokedAtIsNull(
                                tokenFamilyId
                        );

        activeFamilyTokens.forEach(token ->
                token.revoke(revokedAt)
        );
    }

    private InvalidRefreshTokenException invalidToken() {
        return new InvalidRefreshTokenException(
                INVALID_TOKEN_MESSAGE
        );
    }
}
