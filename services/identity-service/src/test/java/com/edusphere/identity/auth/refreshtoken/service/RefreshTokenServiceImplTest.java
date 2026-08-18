package com.edusphere.identity.auth.refreshtoken.service;

import com.edusphere.identity.auth.refreshtoken.config.RefreshTokenProperties;
import com.edusphere.identity.auth.refreshtoken.entity.RefreshToken;
import com.edusphere.identity.auth.refreshtoken.exception.InvalidRefreshTokenException;
import com.edusphere.identity.auth.refreshtoken.model.RefreshTokenRotationResult;
import com.edusphere.identity.auth.refreshtoken.repository.RefreshTokenRepository;
import com.edusphere.identity.auth.refreshtoken.security.RefreshTokenCodec;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.user.enums.UserRole;
import com.edusphere.identity.user.enums.UserStatus;
import com.edusphere.identity.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private RefreshTokenCodec tokenCodec;
    @Mock
    private UserRepository userRepository;

    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        RefreshTokenProperties tokenProperties =
                new RefreshTokenProperties();
        tokenProperties.setExpiration(Duration.ofDays(30));

        service = new RefreshTokenServiceImpl(
                refreshTokenRepository,
                tokenCodec,
                tokenProperties,
                userRepository
        );
    }

    @Test
    void createRefreshToken_whenUserActive_savesHashedToken() {
        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        when(tokenCodec.generateRawToken()).thenReturn("raw-token");
        when(tokenCodec.hash("raw-token")).thenReturn("token-hash");

        String rawToken = service.createRefreshToken(10L);

        assertEquals("raw-token", rawToken);

        ArgumentCaptor<RefreshToken> tokenCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertEquals(10L, tokenCaptor.getValue().getUserId());
        assertEquals("token-hash", tokenCaptor.getValue().getTokenHash());
        assertTrue(tokenCaptor.getValue().getExpiresAt()
                .isAfter(OffsetDateTime.now()));
    }

    @Test
    void createRefreshToken_whenUserInactive_throwsInvalidTokenException() {
        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user(UserStatus.SUSPENDED)));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.createRefreshToken(10L)
        );
        verifyNoInteractions(tokenCodec);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotateRefreshToken_whenTokenMissing_throwsInvalidTokenException() {
        when(tokenCodec.hash("raw-token")).thenReturn("token-hash");
        when(refreshTokenRepository.findByTokenHash("token-hash"))
                .thenReturn(Optional.empty());

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.rotateRefreshToken("raw-token")
        );
    }

    @Test
    void rotateRefreshToken_whenExpired_revokesAndThrowsInvalidTokenException() {
        RefreshToken token = refreshToken(
                OffsetDateTime.now().minusSeconds(1)
        );

        when(tokenCodec.hash("raw-token")).thenReturn("token-hash");
        when(refreshTokenRepository.findByTokenHash("token-hash"))
                .thenReturn(Optional.of(token));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.rotateRefreshToken("raw-token")
        );
        assertTrue(token.isRevoked());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void rotateRefreshToken_whenReusedToken_revokesTokenFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshToken reusedToken =
                new RefreshToken(
                        10L,
                        familyId,
                        "token-hash",
                        OffsetDateTime.now().plusHours(1)
                );
        reusedToken.rotate(OffsetDateTime.now(), "replacement-hash");

        RefreshToken activeFamilyToken =
                new RefreshToken(
                        10L,
                        familyId,
                        "active-token-hash",
                        OffsetDateTime.now().plusHours(1)
                );

        when(tokenCodec.hash("raw-token")).thenReturn("token-hash");
        when(refreshTokenRepository.findByTokenHash("token-hash"))
                .thenReturn(Optional.of(reusedToken));
        when(refreshTokenRepository
                .findAllByTokenFamilyIdAndRevokedAtIsNull(familyId))
                .thenReturn(List.of(activeFamilyToken));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.rotateRefreshToken("raw-token")
        );
        assertTrue(activeFamilyToken.isRevoked());
    }

    @Test
    void rotateRefreshToken_whenUserInactive_revokesTokenFamilyAndThrows() {
        RefreshToken token =
                refreshToken(OffsetDateTime.now().plusHours(1));
        RefreshToken activeFamilyToken =
                new RefreshToken(
                        10L,
                        token.getTokenFamilyId(),
                        "active-token-hash",
                        OffsetDateTime.now().plusHours(1)
                );

        when(tokenCodec.hash("raw-token")).thenReturn("token-hash");
        when(refreshTokenRepository.findByTokenHash("token-hash"))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user(UserStatus.SUSPENDED)));
        when(refreshTokenRepository.findAllByTokenFamilyIdAndRevokedAtIsNull(
                token.getTokenFamilyId()
        )).thenReturn(List.of(activeFamilyToken));

        assertThrows(
                InvalidRefreshTokenException.class,
                () -> service.rotateRefreshToken("raw-token")
        );
        assertTrue(activeFamilyToken.isRevoked());
        verify(tokenCodec, never()).generateRawToken();
    }

    @Test
    void rotateRefreshToken_whenValid_rotatesAndSavesReplacement() {
        RefreshToken token =
                refreshToken(OffsetDateTime.now().plusHours(1));

        when(tokenCodec.hash("raw-token")).thenReturn("token-hash");
        when(refreshTokenRepository.findByTokenHash("token-hash"))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(10L))
                .thenReturn(Optional.of(user(UserStatus.ACTIVE)));
        when(tokenCodec.generateRawToken()).thenReturn("replacement-raw");
        when(tokenCodec.hash("replacement-raw"))
                .thenReturn("replacement-hash");

        RefreshTokenRotationResult result =
                service.rotateRefreshToken("raw-token");

        assertEquals(10L, result.userId());
        assertEquals("replacement-raw", result.rawRefreshToken());
        assertTrue(token.isRevoked());
        assertEquals("replacement-hash", token.getReplacedByTokenHash());

        ArgumentCaptor<RefreshToken> replacementCaptor =
                ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(replacementCaptor.capture());
        assertEquals(token.getTokenFamilyId(),
                replacementCaptor.getValue().getTokenFamilyId());
    }

    @Test
    void revokeRefreshToken_whenBlank_doesNothing() {
        service.revokeRefreshToken(" ");

        verifyNoInteractions(tokenCodec);
        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void revokeRefreshToken_whenTokenExists_revokesIt() {
        RefreshToken token =
                refreshToken(OffsetDateTime.now().plusHours(1));

        when(tokenCodec.hash("raw-token")).thenReturn("token-hash");
        when(refreshTokenRepository.findByTokenHash("token-hash"))
                .thenReturn(Optional.of(token));

        service.revokeRefreshToken("raw-token");

        assertTrue(token.isRevoked());
    }

    @Test
    void revokeAllForUser_revokesAllActiveTokens() {
        RefreshToken first =
                refreshToken(OffsetDateTime.now().plusHours(1));
        RefreshToken second =
                refreshToken(OffsetDateTime.now().plusHours(1));

        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(10L))
                .thenReturn(List.of(first, second));

        service.revokeAllForUser(10L);

        assertTrue(first.isRevoked());
        assertTrue(second.isRevoked());
    }

    private static RefreshToken refreshToken(OffsetDateTime expiresAt) {
        return new RefreshToken(
                10L,
                UUID.randomUUID(),
                "token-hash",
                expiresAt
        );
    }

    private static User user(UserStatus status) {
        User user = new User(
                1L,
                "teacher01",
                "Rahul",
                Set.of(UserRole.TEACHER)
        );
        ReflectionTestUtils.setField(user, "id", 10L);
        user.setStatus(status);
        return user;
    }
}
