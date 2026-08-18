package com.edusphere.identity.auth.service;

import com.edusphere.identity.auth.dto.LoginRequest;
import com.edusphere.identity.auth.dto.LoginResponse;
import com.edusphere.identity.auth.exception.AccountLockedException;
import com.edusphere.identity.auth.exception.AccountNotActiveException;
import com.edusphere.identity.auth.exception.InvalidCredentialsException;
import com.edusphere.identity.auth.lockout.LoginLockoutProperties;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        LoginLockoutProperties lockoutProperties =
                new LoginLockoutProperties();
        lockoutProperties.setFirstFailureThreshold(5);
        lockoutProperties.setEscalatedFailureThreshold(3);
        lockoutProperties.setFirstLockDuration(Duration.ofMinutes(30));
        lockoutProperties.setSecondLockDuration(Duration.ofDays(30));
        lockoutProperties.setFinalLockDuration(Duration.ofDays(365));

        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                new LoginLockoutService(lockoutProperties)
        );
    }

    @Test
    void login_whenUserMissing_throwsInvalidCredentialsException() {
        LoginRequest request = request();

        when(userRepository.findByOrganizationIdAndUsername(1L, "teacher01"))
                .thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateAccessToken(any(User.class));
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void login_whenPasswordDoesNotMatch_throwsInvalidCredentialsException() {
        LoginRequest request = request();
        User user = user(UserStatus.ACTIVE);

        when(userRepository.findByOrganizationIdAndUsername(1L, "teacher01"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Teacher@123", "encoded-password"))
                .thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid username or password", exception.getMessage());
        assertEquals(1, user.getFailedLoginAttempts());
        verify(jwtService, never()).generateAccessToken(any(User.class));
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void login_whenFifthBadPassword_locksAccountForThirtyMinutes() {
        LoginRequest request = request();
        User user = user(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(4);

        when(userRepository.findByOrganizationIdAndUsername(1L, "teacher01"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Teacher@123", "encoded-password"))
                .thenReturn(false);

        AccountLockedException exception = assertThrows(
                AccountLockedException.class,
                () -> authService.login(request)
        );

        assertTrue(exception.getMessage().startsWith(
                "Account is locked until "
        ));
        assertEquals(0, user.getFailedLoginAttempts());
        assertEquals(1, user.getLoginLockLevel());
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(
                OffsetDateTime.now().plusMinutes(29)
        ));
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void login_whenAccountLocked_blocksLoginWithoutCheckingPassword() {
        LoginRequest request = request();
        User user = user(UserStatus.ACTIVE);
        user.setLockedUntil(OffsetDateTime.now().plusMinutes(10));

        when(userRepository.findByOrganizationIdAndUsername(1L, "teacher01"))
                .thenReturn(Optional.of(user));

        assertThrows(
                AccountLockedException.class,
                () -> authService.login(request)
        );

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void login_whenLockExpired_allowsSuccessfulLoginAndClearsFailures() {
        LoginRequest request = request();
        User user = user(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(2);
        user.setLockedUntil(OffsetDateTime.now().minusMinutes(1));

        when(userRepository.findByOrganizationIdAndUsername(1L, "teacher01"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Teacher@123", "encoded-password"))
                .thenReturn(true);
        when(refreshTokenService.createRefreshToken(10L))
                .thenReturn("refresh-token");
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        AuthenticationResult result = authService.login(request);

        assertEquals("jwt-token", result.response().getAccessToken());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }

    @Test
    void login_afterFirstLock_whenThirdBadPassword_locksAccountForOneMonth() {
        LoginRequest request = request();
        User user = user(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "loginLockLevel", 1);
        user.setFailedLoginAttempts(2);

        when(userRepository.findByOrganizationIdAndUsername(1L, "teacher01"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Teacher@123", "encoded-password"))
                .thenReturn(false);

        assertThrows(
                AccountLockedException.class,
                () -> authService.login(request)
        );

        assertEquals(0, user.getFailedLoginAttempts());
        assertEquals(2, user.getLoginLockLevel());
        assertTrue(user.getLockedUntil().isAfter(
                OffsetDateTime.now().plusDays(27)
        ));
    }

    @Test
    void login_afterSecondLock_whenThirdBadPassword_locksAccountForOneYear() {
        LoginRequest request = request();
        User user = user(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "loginLockLevel", 2);
        user.setFailedLoginAttempts(2);

        when(userRepository.findByOrganizationIdAndUsername(1L, "teacher01"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Teacher@123", "encoded-password"))
                .thenReturn(false);

        assertThrows(
                AccountLockedException.class,
                () -> authService.login(request)
        );

        assertEquals(0, user.getFailedLoginAttempts());
        assertEquals(3, user.getLoginLockLevel());
        assertTrue(user.getLockedUntil().isAfter(
                OffsetDateTime.now().plusDays(360)
        ));
    }

    @Test
    void login_whenAccountNotActive_throwsAccountNotActiveException() {
        LoginRequest request = request();
        User user = user(UserStatus.PENDING_ACTIVATION);

        when(userRepository.findByOrganizationIdAndUsername(1L, "teacher01"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Teacher@123", "encoded-password"))
                .thenReturn(true);

        AccountNotActiveException exception = assertThrows(
                AccountNotActiveException.class,
                () -> authService.login(request)
        );

        assertEquals("Account is not active", exception.getMessage());
        verify(jwtService, never()).generateAccessToken(any(User.class));
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void login_whenValid_returnsTokensAndUpdatesLoginState() {
        LoginRequest request = request();
        User user = user(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(3);

        when(userRepository.findByOrganizationIdAndUsername(1L, "teacher01"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Teacher@123", "encoded-password"))
                .thenReturn(true);
        when(refreshTokenService.createRefreshToken(10L))
                .thenReturn("refresh-token");
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        AuthenticationResult result = authService.login(request);
        LoginResponse response = result.response();

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("refresh-token", result.rawRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertEquals(10L, response.getUserId());
        assertEquals(1L, response.getOrganizationId());
        assertEquals("teacher01", response.getUsername());
        assertEquals(Set.of("TEACHER", "HR"), response.getRoles());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNotNull(user.getLastLoginAt());
        verify(refreshTokenService).createRefreshToken(10L);
    }

    @Test
    void refresh_whenUserMissing_throwsResourceNotFoundException() {
        when(refreshTokenService.rotateRefreshToken("refresh-token"))
                .thenReturn(new RefreshTokenRotationResult(
                        10L,
                        "rotated-refresh-token"
                ));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> authService.refresh("refresh-token")
        );
    }

    @Test
    void refresh_whenUserInactive_revokesAllAndThrowsAccountNotActiveException() {
        User user = user(UserStatus.SUSPENDED);

        when(refreshTokenService.rotateRefreshToken("refresh-token"))
                .thenReturn(new RefreshTokenRotationResult(
                        10L,
                        "rotated-refresh-token"
                ));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        AccountNotActiveException exception = assertThrows(
                AccountNotActiveException.class,
                () -> authService.refresh("refresh-token")
        );

        assertEquals("Account is not active", exception.getMessage());
        verify(refreshTokenService).revokeAllForUser(10L);
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void refresh_whenValid_returnsNewAccessTokenWithRotatedRefreshToken() {
        User user = user(UserStatus.ACTIVE);

        when(refreshTokenService.rotateRefreshToken("refresh-token"))
                .thenReturn(new RefreshTokenRotationResult(
                        10L,
                        "rotated-refresh-token"
                ));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        AuthenticationResult result = authService.refresh("refresh-token");

        assertEquals("jwt-token", result.response().getAccessToken());
        assertEquals("rotated-refresh-token", result.rawRefreshToken());
    }

    @Test
    void logout_revokesRefreshToken() {
        authService.logout("refresh-token");

        verify(refreshTokenService).revokeRefreshToken("refresh-token");
    }

    private static LoginRequest request() {
        LoginRequest request = new LoginRequest();
        request.setOrganizationId(1L);
        request.setUsername("teacher01");
        request.setPassword("Teacher@123");
        return request;
    }

    private static User user(UserStatus status) {
        User user = new User(
                1L,
                "teacher01",
                "Rahul",
                Set.of(UserRole.TEACHER, UserRole.HR)
        );
        ReflectionTestUtils.setField(user, "id", 10L);
        ReflectionTestUtils.setField(user, "passwordHash", "encoded-password");
        user.setStatus(status);
        return user;
    }
}
