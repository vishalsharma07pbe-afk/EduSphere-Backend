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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtService
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
        verify(jwtService, never()).generateAccessToken(any(User.class));
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
    }

    @Test
    void login_whenValid_returnsTokenAndUpdatesLoginState() {
        LoginRequest request = request();
        User user = user(UserStatus.ACTIVE);
        user.setFailedLoginAttempts(3);

        when(userRepository.findByOrganizationIdAndUsername(1L, "teacher01"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Teacher@123", "encoded-password"))
                .thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(request);

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        assertEquals(10L, response.getUserId());
        assertEquals(1L, response.getOrganizationId());
        assertEquals("teacher01", response.getUsername());
        assertEquals(Set.of("TEACHER", "HR"), response.getRoles());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNotNull(user.getLastLoginAt());
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
                "encoded-password",
                "Rahul",
                Set.of(UserRole.TEACHER, UserRole.HR)
        );
        ReflectionTestUtils.setField(user, "id", 10L);
        user.setStatus(status);
        return user;
    }
}
