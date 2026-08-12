package com.edusphere.identity.user.service;

import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.user.dto.CreateUserRequest;
import com.edusphere.identity.user.dto.UpdateUserProfileRequest;
import com.edusphere.identity.user.dto.UpdateUserRolesRequest;
import com.edusphere.identity.user.dto.UpdateUserStatusRequest;
import com.edusphere.identity.user.dto.UserResponse;
import com.edusphere.identity.user.enums.UserRole;
import com.edusphere.identity.user.enums.UserStatus;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(
            Long organizationId,
            Long userId
    );

    PageResponse<UserResponse> getAllUsersByOrganization(
            Long organizationId,
            Pageable pageable
    );

    UserResponse updateUserProfile(
            Long organizationId,
            Long userId,
            UpdateUserProfileRequest request
    );

    UserResponse updateUserRoles(
            Long organizationId,
            Long userId,
            UpdateUserRolesRequest request
    );

    UserResponse updateUserStatus(
            Long organizationId,
            Long userId,
            UpdateUserStatusRequest request
    );
}