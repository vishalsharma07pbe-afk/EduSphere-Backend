package com.edusphere.identity.user.service;

import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.user.dto.*;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.user.enums.UserRole;
import com.edusphere.identity.user.enums.UserStatus;
import com.edusphere.identity.user.exception.DuplicateResourceException;
import com.edusphere.identity.user.exception.ResourceNotFoundException;
import com.edusphere.identity.user.mapper.UserMapper;
import com.edusphere.identity.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                userMapper,
                passwordEncoder
        );
    }

    @Test
    void createUser_whenUsernameExists_throwsDuplicateResourceException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setOrganizationId(1L);
        request.setUsername("teacher01");

        when(userRepository.existsByOrganizationIdAndUsername(
                1L,
                "teacher01"
        )).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(request)
        );

        assertEquals(
                "Username already exists in this organization: teacher01",
                exception.getMessage()
        );

        verify(userRepository).existsByOrganizationIdAndUsername(
                1L,
                "teacher01"
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void createUser_whenEmailExists_throwsDuplicateResourceException() {
        CreateUserRequest request = new CreateUserRequest();
        request.setOrganizationId(1L);
        request.setUsername("teacher01");
        request.setEmail("teacher@edusphere.com");

        when(userRepository.existsByOrganizationIdAndUsername(
                1L,
                "teacher01"
        )).thenReturn(false);

        when(userRepository.existsByOrganizationIdAndEmail(
                1L,
                "teacher@edusphere.com"
        )).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> userService.createUser(request)
        );

        assertEquals(
                "Email already exists in this organization: "
                        + "teacher@edusphere.com",
                exception.getMessage()
        );

        verify(userRepository).existsByOrganizationIdAndUsername(
                1L,
                "teacher01"
        );

        verify(userRepository).existsByOrganizationIdAndEmail(
                1L,
                "teacher@edusphere.com"
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void createUser_whenValid_savesAndReturnsResponse() {
        CreateUserRequest request = new CreateUserRequest();
        request.setOrganizationId(1L);
        request.setUsername("teacher01");
        request.setPassword("Teacher@123");
        request.setFirstName("Rahul");
        request.setEmail("rahul@edusphere.com");

        User user = new User();
        user.setOrganizationId(1L);
        user.setUsername("teacher01");
        user.setFirstName("Rahul");
        user.setEmail("rahul@edusphere.com");

        User savedUser = new User();
        savedUser.setOrganizationId(1L);
        savedUser.setUsername("teacher01");
        savedUser.setPasswordHash("encoded-password");
        savedUser.setFirstName("Rahul");
        savedUser.setEmail("rahul@edusphere.com");

        UserResponse expectedResponse = new UserResponse();
        expectedResponse.setId(10L);
        expectedResponse.setOrganizationId(1L);
        expectedResponse.setUsername("teacher01");
        expectedResponse.setFirstName("Rahul");
        expectedResponse.setEmail("rahul@edusphere.com");

        when(userRepository.existsByOrganizationIdAndUsername(
                1L,
                "teacher01"
        )).thenReturn(false);

        when(userRepository.existsByOrganizationIdAndEmail(
                1L,
                "rahul@edusphere.com"
        )).thenReturn(false);

        when(userMapper.toEntity(request))
                .thenReturn(user);

        when(passwordEncoder.encode("Teacher@123"))
                .thenReturn("encoded-password");

        when(userRepository.save(user))
                .thenReturn(savedUser);

        when(userMapper.toResponse(savedUser))
                .thenReturn(expectedResponse);

        UserResponse actualResponse =
                userService.createUser(request);

        assertSame(expectedResponse, actualResponse);

        assertEquals(
                "encoded-password",
                user.getPasswordHash()
        );

        verify(userRepository)
                .existsByOrganizationIdAndUsername(
                        1L,
                        "teacher01");
        verify(userRepository)
                .existsByOrganizationIdAndEmail(
                        1L,
                        "rahul@edusphere.com");
        verify(userMapper).toEntity(request);
        verify(passwordEncoder).encode("Teacher@123");
        verify(userRepository).save(user);
        verify(userMapper).toResponse(savedUser);
    }

    @Test
    void createUser_whenEmailIsBlank_doesNotCheckEmailUniqueness() {
        CreateUserRequest request = new CreateUserRequest();
        request.setOrganizationId(1L);
        request.setUsername("teacher01");
        request.setPassword("Teacher@123");
        request.setEmail(" ");

        User user = user(1L, "teacher01", "old@edusphere.com");
        UserResponse expectedResponse = response(10L, 1L, "teacher01");

        when(userRepository.existsByOrganizationIdAndUsername(
                1L,
                "teacher01"
        )).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode("Teacher@123"))
                .thenReturn("encoded-password");
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse actualResponse = userService.createUser(request);

        assertSame(expectedResponse, actualResponse);
        assertEquals("encoded-password", user.getPasswordHash());

        verify(userRepository, never())
                .existsByOrganizationIdAndEmail(anyLong(), anyString());
    }

    @Test
    void getUserById_whenFound_returnsResponse() {
        User user = user(1L, "teacher01", "teacher@edusphere.com");
        UserResponse expectedResponse = response(10L, 1L, "teacher01");

        when(userRepository.findByOrganizationIdAndId(1L, 10L))
                .thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse actualResponse = userService.getUserById(1L, 10L);

        assertSame(expectedResponse, actualResponse);
        verify(userRepository).findByOrganizationIdAndId(1L, 10L);
        verify(userMapper).toResponse(user);
    }

    @Test
    void getUserById_whenMissing_throwsResourceNotFoundException() {
        when(userRepository.findByOrganizationIdAndId(1L, 10L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(1L, 10L)
        );

        assertEquals(
                "User not found with ID: 10 in organization: 1",
                exception.getMessage()
        );
        verify(userMapper, never()).toResponse(any(User.class));
    }

    @Test
    void getAllUsersByOrganization_whenUsersExist_returnsPageResponse() {
        Pageable pageable = PageRequest.of(1, 2);
        User firstUser = user(1L, "teacher01", "teacher@edusphere.com");
        User secondUser = user(1L, "student01", "student@edusphere.com");
        UserResponse firstResponse = response(10L, 1L, "teacher01");
        UserResponse secondResponse = response(11L, 1L, "student01");

        when(userRepository.findAllByOrganizationId(1L, pageable))
                .thenReturn(new PageImpl<>(
                        List.of(firstUser, secondUser),
                        pageable,
                        5
                ));
        when(userMapper.toResponse(firstUser)).thenReturn(firstResponse);
        when(userMapper.toResponse(secondUser)).thenReturn(secondResponse);

        PageResponse<UserResponse> actualResponse =
                userService.getAllUsersByOrganization(1L, pageable);

        assertEquals(List.of(firstResponse, secondResponse),
                actualResponse.content());
        assertEquals(1, actualResponse.pageNumber());
        assertEquals(2, actualResponse.pageSize());
        assertEquals(5, actualResponse.totalElements());
        assertEquals(3, actualResponse.totalPages());
        assertFalse(actualResponse.first());
        assertFalse(actualResponse.last());
        assertFalse(actualResponse.empty());
    }

    @Test
    void updateUserProfile_whenUserMissing_throwsResourceNotFoundException() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();

        when(userRepository.findByOrganizationIdAndId(1L, 10L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateUserProfile(1L, 10L, request)
        );

        assertEquals(
                "User not found with ID: 10 in organization: 1",
                exception.getMessage()
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfile_whenNewEmailExists_throwsDuplicateResourceException() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setEmail("new@edusphere.com");
        User user = user(1L, "teacher01", "old@edusphere.com");

        when(userRepository.findByOrganizationIdAndId(1L, 10L))
                .thenReturn(Optional.of(user));
        when(userRepository.existsByOrganizationIdAndEmail(
                1L,
                "new@edusphere.com"
        )).thenReturn(true);

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> userService.updateUserProfile(1L, 10L, request)
        );

        assertEquals(
                "Email already exists in this organization: new@edusphere.com",
                exception.getMessage()
        );
        verify(userMapper, never())
                .updateProfile(any(UpdateUserProfileRequest.class),
                        any(User.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserProfile_whenEmailUnchangedIgnoresCase_updatesAndReturnsResponse() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFirstName("Rahul");
        request.setEmail("TEACHER@EDUSPHERE.COM");
        User user = user(1L, "teacher01", "teacher@edusphere.com");
        UserResponse expectedResponse = response(10L, 1L, "teacher01");

        when(userRepository.findByOrganizationIdAndId(1L, 10L))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse actualResponse =
                userService.updateUserProfile(1L, 10L, request);

        assertSame(expectedResponse, actualResponse);
        verify(userRepository, never())
                .existsByOrganizationIdAndEmail(anyLong(), anyString());
        verify(userMapper).updateProfile(request, user);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUserProfile_whenValidNewEmail_updatesAndReturnsResponse() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setFirstName("Rahul");
        request.setEmail("new@edusphere.com");
        User user = user(1L, "teacher01", "old@edusphere.com");
        UserResponse expectedResponse = response(10L, 1L, "teacher01");

        when(userRepository.findByOrganizationIdAndId(1L, 10L))
                .thenReturn(Optional.of(user));
        when(userRepository.existsByOrganizationIdAndEmail(
                1L,
                "new@edusphere.com"
        )).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse actualResponse =
                userService.updateUserProfile(1L, 10L, request);

        assertSame(expectedResponse, actualResponse);
        verify(userMapper).updateProfile(request, user);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUserRoles_whenUserMissing_throwsResourceNotFoundException() {
        UpdateUserRolesRequest request =
                new UpdateUserRolesRequest(Set.of(UserRole.ADMIN));

        when(userRepository.findByOrganizationIdAndId(1L, 10L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateUserRoles(1L, 10L, request)
        );

        assertEquals(
                "User not found with ID: 10 in organization: 1",
                exception.getMessage()
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRoles_whenValid_updatesRolesAndReturnsResponse() {
        UpdateUserRolesRequest request =
                new UpdateUserRolesRequest(Set.of(UserRole.ADMIN,
                        UserRole.TEACHER));
        User user = user(1L, "teacher01", "teacher@edusphere.com");
        UserResponse expectedResponse = response(10L, 1L, "teacher01");

        when(userRepository.findByOrganizationIdAndId(1L, 10L))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse actualResponse =
                userService.updateUserRoles(1L, 10L, request);

        assertSame(expectedResponse, actualResponse);
        assertEquals(Set.of(UserRole.ADMIN, UserRole.TEACHER),
                user.getRoles());
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUserStatus_whenUserMissing_throwsResourceNotFoundException() {
        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(UserStatus.ACTIVE);

        when(userRepository.findByOrganizationIdAndId(1L, 10L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateUserStatus(1L, 10L, request)
        );

        assertEquals(
                "User not found with ID: 10 in organization: 1",
                exception.getMessage()
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserStatus_whenValid_updatesStatusAndReturnsResponse() {
        UpdateUserStatusRequest request =
                new UpdateUserStatusRequest(UserStatus.ACTIVE);
        User user = user(1L, "teacher01", "teacher@edusphere.com");
        UserResponse expectedResponse = response(10L, 1L, "teacher01");

        when(userRepository.findByOrganizationIdAndId(1L, 10L))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse actualResponse =
                userService.updateUserStatus(1L, 10L, request);

        assertSame(expectedResponse, actualResponse);
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    private static User user(
            Long organizationId,
            String username,
            String email
    ) {
        User user = new User();
        user.setOrganizationId(organizationId);
        user.setUsername(username);
        user.setFirstName("Rahul");
        user.setEmail(email);
        user.setRoles(Set.of(UserRole.TEACHER));
        user.setStatus(UserStatus.PENDING_ACTIVATION);
        return user;
    }

    private static UserResponse response(
            Long id,
            Long organizationId,
            String username
    ) {
        UserResponse response = new UserResponse();
        response.setId(id);
        response.setOrganizationId(organizationId);
        response.setUsername(username);
        return response;
    }
}
