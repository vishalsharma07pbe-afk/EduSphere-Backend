package com.edusphere.identity.user.service;

import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.user.dto.*;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.common.exception.DuplicateResourceException;
import com.edusphere.identity.common.exception.ResourceNotFoundException;
import com.edusphere.identity.user.mapper.UserMapper;
import com.edusphere.identity.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edusphere.identity.roleapproval.dto.CreateRoleAssignmentRequest;
import com.edusphere.identity.roleapproval.entity.RoleAssignmentRequest;
import com.edusphere.identity.roleapproval.exception.InvalidRoleRequestException;
import com.edusphere.identity.roleapproval.mapper.RoleAssignmentRequestMapper;
import com.edusphere.identity.roleapproval.policy.RoleApprovalPolicy;
import com.edusphere.identity.roleapproval.repository.RoleAssignmentRequestRepository;
import com.edusphere.identity.user.enums.UserRole;
import com.edusphere.identity.user.enums.UserStatus;
import com.edusphere.identity.roleapproval.enums.ApprovalStatus;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RoleApprovalPolicy roleApprovalPolicy;
    private final RoleAssignmentRequestRepository roleRequestRepository;
    private final RoleAssignmentRequestMapper roleRequestMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            RoleApprovalPolicy roleApprovalPolicy,
            RoleAssignmentRequestRepository roleRequestRepository,
            RoleAssignmentRequestMapper roleRequestMapper
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.roleApprovalPolicy = roleApprovalPolicy;
        this.roleRequestRepository = roleRequestRepository;
        this.roleRequestMapper = roleRequestMapper;
    }

    @Override
    @Transactional
    public UserResponse createUser(
            Long organizationId,
            Long createdByUserId,
            CreateUserRequest request
    ) {
        /*
         * Load the authenticated creator from the same organization.
         * This prevents one organization from creating users for another.
         */
        User creator = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        createdByUserId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creating user not found"
                ));

        if (creator.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidRoleRequestException(
                    "Only an active user can create another user"
            );
        }

        /*
         * Always use the organization ID from the secured URL.
         * Do not use the request-body value for database queries.
         */
        if (userRepository.existsByOrganizationIdAndUsername(
                organizationId,
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
                organizationId,
                request.getEmail()
        )) {
            throw new DuplicateResourceException(
                    "Email already exists in this organization: "
                            + request.getEmail()
            );
        }

        /*
         * Routine roles can be assigned immediately.
         * Sensitive roles must pass through the approval workflow.
         */
        Set<UserRole> routineRoles =
                roleApprovalPolicy.getRoutineRoles(request.getRoles());

        Set<UserRole> sensitiveRoles =
                roleApprovalPolicy.getSensitiveRoles(request.getRoles());

        /*
         * Verify that the creator is permitted to request every sensitive role.
         * Throwing here rolls back the entire transaction.
         */
        for (UserRole sensitiveRole : sensitiveRoles) {
            if (!roleApprovalPolicy.canRequestApproval(
                    creator.getRoles(),
                    sensitiveRole
            )) {
                throw new InvalidRoleRequestException(
                        "You are not allowed to request role: "
                                + sensitiveRole
                );
            }
        }

        User user = userMapper.toEntity(request);

        /*
         * UserMapper may have copied all submitted roles.
         * Replace them with routine roles so sensitive roles are not assigned.
         */
        user.setRoles(routineRoles);
        user.setOrganizationId(organizationId);

        /*
         * Temporary password handling remains until activation links are added.
         */
        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        user.setPasswordHash(encodedPassword);

        /*
         * Sensitive roles keep the account waiting for approval.
         * Otherwise it can move directly to activation.
         */
        if (sensitiveRoles.isEmpty()) {
            user.setStatus(UserStatus.PENDING_ACTIVATION);
        } else {
            user.setStatus(UserStatus.PENDING_APPROVAL);
        }

        User savedUser = userRepository.save(user);

        /*
         * Create one independent approval request for each sensitive role.
         */
        for (UserRole sensitiveRole : sensitiveRoles) {
            CreateRoleAssignmentRequest approvalRequest =
                    new CreateRoleAssignmentRequest(
                            savedUser.getId(),
                            sensitiveRole,
                            "Role requested during account creation"
                    );

            RoleAssignmentRequest roleRequest =
                    roleRequestMapper.toEntity(
                            organizationId,
                            createdByUserId,
                            approvalRequest
                    );

            roleRequestRepository.save(roleRequest);
        }

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
    @Transactional
    public UserResponse updateUserRoles(
            Long organizationId,
            Long updatedByUserId,
            Long userId,
            UpdateUserRolesRequest request
    ) {
        User updater = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        updatedByUserId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Role-updating user not found"
                ));

        if (updater.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidRoleRequestException(
                    "Only an active user can update roles"
            );
        }

        User targetUser = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        userId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                                + " in organization: " + organizationId
                ));

        /*
         * Users cannot assign or request roles for themselves.
         * This blocks self-privilege escalation.
         */
        if (updater.getId().equals(targetUser.getId())) {
            throw new InvalidRoleRequestException(
                    "You cannot update your own roles"
            );
        }

        /*
         * Account-creation roles are handled by createUser().
         * This endpoint handles role changes for existing active accounts.
         */
        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidRoleRequestException(
                    "Roles can only be updated for an active user"
            );
        }

        /*
         * Treat request.roles as roles to ADD, not a complete replacement.
         * Existing roles are removed from the collection of new roles.
         */
        Set<UserRole> newRoles = new HashSet<>(request.getRoles());
        newRoles.removeAll(targetUser.getRoles());

        if (newRoles.isEmpty()) {
            return userMapper.toResponse(targetUser);
        }

        Set<UserRole> routineRoles =
                roleApprovalPolicy.getRoutineRoles(newRoles);

        Set<UserRole> sensitiveRoles =
                roleApprovalPolicy.getSensitiveRoles(newRoles);

        /*
         * Validate all sensitive roles before changing the user.
         * A failure rolls back the complete transaction.
         */
        for (UserRole sensitiveRole : sensitiveRoles) {
            if (!roleApprovalPolicy.canRequestApproval(
                    updater.getRoles(),
                    sensitiveRole
            )) {
                throw new InvalidRoleRequestException(
                        "You are not allowed to request role: "
                                + sensitiveRole
                );
            }

            boolean pendingRequestExists =
                    roleRequestRepository
                            .existsByOrganizationIdAndUserIdAndRequestedRoleAndStatus(
                                    organizationId,
                                    targetUser.getId(),
                                    sensitiveRole,
                                    ApprovalStatus.PENDING
                            );

            if (pendingRequestExists) {
                throw new DuplicateResourceException(
                        "A pending request already exists for user "
                                + targetUser.getId()
                                + " and role "
                                + sensitiveRole
                );
            }
        }

        /*
         * Routine roles are safe to assign immediately.
         */
        for (UserRole routineRole : routineRoles) {
            targetUser.addRole(routineRole);
        }

        /*
         * Sensitive roles are not added to the user.
         * Each one becomes a separate pending approval request.
         */
        for (UserRole sensitiveRole : sensitiveRoles) {
            CreateRoleAssignmentRequest approvalRequest =
                    new CreateRoleAssignmentRequest(
                            targetUser.getId(),
                            sensitiveRole,
                            "Role change requested by an authorized user"
                    );

            RoleAssignmentRequest roleRequest =
                    roleRequestMapper.toEntity(
                            organizationId,
                            updatedByUserId,
                            approvalRequest
                    );

            roleRequestRepository.save(roleRequest);
        }

        User updatedUser = userRepository.save(targetUser);
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
