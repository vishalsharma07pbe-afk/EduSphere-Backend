package com.edusphere.identity.user.controller;

import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.user.dto.CreateUserRequest;
import com.edusphere.identity.user.dto.UpdateUserProfileRequest;
import com.edusphere.identity.user.dto.UpdateUserRolesRequest;
import com.edusphere.identity.user.dto.UpdateUserStatusRequest;
import com.edusphere.identity.user.dto.UserResponse;
import com.edusphere.identity.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/v1/organizations/{organizationId}/users"
)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateUserRequest request
    ) {
        if (!organizationId.equals(request.getOrganizationId())) {
            throw new IllegalArgumentException(
                    "Organization ID in the URL must match "
                            + "the organization ID in the request body"
            );
        }
        UserResponse response = userService.createUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long organizationId,
            @PathVariable Long userId
    ) {
        UserResponse response = userService.getUserById(organizationId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> getAllUsers(
            @PathVariable Long organizationId,
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "firstName"
            )
            Pageable pageable
    ) {
        PageResponse<UserResponse> response =
                userService.getAllUsersByOrganization(
                        organizationId,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody
            UpdateUserProfileRequest request
    ) {
        UserResponse response =
                userService.updateUserProfile(
                        organizationId,
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<UserResponse> updateUserRoles(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody
            UpdateUserRolesRequest request
    ) {
        UserResponse response =
                userService.updateUserRoles(
                        organizationId,
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @Valid @RequestBody
            UpdateUserStatusRequest request
    ) {
        UserResponse response =
                userService.updateUserStatus(
                        organizationId,
                        userId,
                        request
                );

        return ResponseEntity.ok(response);
    }
}