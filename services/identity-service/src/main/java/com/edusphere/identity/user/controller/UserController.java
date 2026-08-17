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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/users")
@PreAuthorize("@tenantSecurity.canAccessOrganization(authentication, #organizationId)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("""
        @tenantSecurity.canAccessOrganization(authentication, #organizationId)
        and hasAnyRole('ADMIN', 'HR')
        and @userAuthorization.canCreateUser(authentication, #request.roles)
        """)
    public ResponseEntity<UserResponse> createUser(
            @PathVariable Long organizationId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateUserRequest request
    ) {
        if (!organizationId.equals(request.getOrganizationId())) {
            throw new IllegalArgumentException(
                    "Organization ID in the URL must match the request body"
            );
        }

        // The JWT subject contains the authenticated creator's database user ID.
        Long createdByUserId = Long.valueOf(jwt.getSubject());

        UserResponse response = userService.createUser(
                organizationId,
                createdByUserId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PreAuthorize(
            "@tenantSecurity.isCurrentUser(authentication, #userId)"
                    + " or hasAnyRole('ADMIN', 'PRINCIPAL', 'HR')"
    )
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
    @PreAuthorize("""
        @tenantSecurity.canAccessOrganization(authentication, #organizationId)
        and hasAnyRole('ADMIN', 'HR')
        """)
    public ResponseEntity<UserResponse> updateUserRoles(
            @PathVariable Long organizationId,
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateUserRolesRequest request
    ) {
        // Identify the authenticated person performing the role update.
        Long updatedByUserId = Long.valueOf(jwt.getSubject());

        UserResponse response = userService.updateUserRoles(
                organizationId,
                updatedByUserId,
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