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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.existsByOrganizationIdAndUsername(
                request.getOrganizationId(),
                request.getUsername()
        )) {
            throw new DuplicateResourceException(
                    "Username already exists in this organization: "
                            + request.getUsername()
            );
        }

        if (request.getEmail() != null
                && !request.getEmail().isBlank()
                && userRepository.existsByOrganizationIdAndEmail(
                request.getOrganizationId(),
                request.getEmail()
        )) {
            throw new DuplicateResourceException(
                    "Email already exists in this organization: "
                            + request.getEmail()
            );
        }

        User user = userMapper.toEntity(request);
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        user.setPasswordHash(encodedPassword);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long organizationId, Long userId) {
        User user = userRepository
                .findByOrganizationIdAndId(organizationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                                + " in organization: " + organizationId
                ));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsersByOrganization(
            Long organizationId,
            Pageable pageable
    ) {
        Page<UserResponse> userResponsePage = userRepository
                .findAllByOrganizationId(organizationId, pageable)
                .map(userMapper::toResponse);

        return PageResponse.from(userResponsePage);
    }

    @Override
    public UserResponse updateUserProfile(
            Long organizationId,
            Long userId,
            UpdateUserProfileRequest request
    ) {
        User user = userRepository
                .findByOrganizationIdAndId(organizationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                                + " in organization: " + organizationId
                ));

        if (request.getEmail() != null
                && !request.getEmail().isBlank()
                && !request.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByOrganizationIdAndEmail(
                organizationId,
                request.getEmail()
        )) {
            throw new DuplicateResourceException(
                    "Email already exists in this organization: "
                            + request.getEmail()
            );
        }

        userMapper.updateProfile(request, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public UserResponse updateUserRoles(
            Long organizationId,
            Long userId,
            UpdateUserRolesRequest request
    ) {
        User user = userRepository
                .findByOrganizationIdAndId(organizationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                                + " in organization: " + organizationId
                ));

        user.setRoles(request.getRoles());
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    public UserResponse updateUserStatus(
            Long organizationId,
            Long userId,
            UpdateUserStatusRequest request
    ) {
        User user = userRepository
                .findByOrganizationIdAndId(organizationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                                + " in organization: " + organizationId
                ));

        user.setStatus(request.getStatus());
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }
}
