package com.edusphere.identity.user.service;

import com.edusphere.identity.auth.activation.event.UserActivationRequestedEvent;
import com.edusphere.identity.common.dto.PageResponse;
import com.edusphere.identity.common.exception.DuplicateResourceException;
import com.edusphere.identity.common.exception.ResourceNotFoundException;
import com.edusphere.identity.roleapproval.dto.CreateRoleAssignmentRequest;
import com.edusphere.identity.roleapproval.entity.RoleAssignmentRequest;
import com.edusphere.identity.roleapproval.enums.ApprovalStatus;
import com.edusphere.identity.roleapproval.exception.InvalidRoleRequestException;
import com.edusphere.identity.roleapproval.mapper.RoleAssignmentRequestMapper;
import com.edusphere.identity.roleapproval.policy.RoleApprovalPolicy;
import com.edusphere.identity.roleapproval.repository.RoleAssignmentRequestRepository;
import com.edusphere.identity.user.dto.CreateUserRequest;
import com.edusphere.identity.user.dto.UpdateUserProfileRequest;
import com.edusphere.identity.user.dto.UpdateUserRolesRequest;
import com.edusphere.identity.user.dto.UpdateUserStatusRequest;
import com.edusphere.identity.user.dto.UserResponse;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.user.enums.UserRole;
import com.edusphere.identity.user.enums.UserStatus;
import com.edusphere.identity.user.mapper.UserMapper;
import com.edusphere.identity.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edusphere.identity.roleapproval.exception.ApprovalNotAllowedException;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Set<UserRole> ACTIVATION_RESEND_ROLES =
            Set.of(
                    UserRole.ADMIN,
                    UserRole.PRINCIPAL,
                    UserRole.VICE_PRINCIPAL_HEADMASTER,
                    UserRole.HR,
                    UserRole.ADMISSIONS
            );

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RoleApprovalPolicy roleApprovalPolicy;
    private final RoleAssignmentRequestRepository roleRequestRepository;
    private final RoleAssignmentRequestMapper roleRequestMapper;
    private final ApplicationEventPublisher eventPublisher;

    public UserServiceImpl(
            UserRepository userRepository,
            UserMapper userMapper,
            RoleApprovalPolicy roleApprovalPolicy,
            RoleAssignmentRequestRepository roleRequestRepository,
            RoleAssignmentRequestMapper roleRequestMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.roleApprovalPolicy = roleApprovalPolicy;
        this.roleRequestRepository = roleRequestRepository;
        this.roleRequestMapper = roleRequestMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UserResponse createUser(
            Long organizationId,
            Long createdByUserId,
            CreateUserRequest request
    ) {
        // Load the authenticated creator from the secured organization.
        User creator = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        createdByUserId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Creating user not found"
                ));

        // Only active users can create another account.
        if (creator.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidRoleRequestException(
                    "Only an active user can create another user"
            );
        }

        // Check username uniqueness within the organization.
        if (userRepository.existsByOrganizationIdAndUsername(
                organizationId,
                request.getUsername()
        )) {
            throw new DuplicateResourceException(
                    "Username already exists in this organization: "
                            + request.getUsername()
            );
        }

        // Check email uniqueness within the organization.
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

        // Separate immediately assignable roles from approval-protected roles.
        Set<UserRole> routineRoles =
                roleApprovalPolicy.getRoutineRoles(request.getRoles());

        Set<UserRole> sensitiveRoles =
                roleApprovalPolicy.getSensitiveRoles(request.getRoles());

        // Validate permission to request every sensitive role.
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

        // Map the administrative request without creating a password.
        User user = userMapper.toEntity(request);

        // Assign only routine roles and trust the organization ID from the URL.
        user.setRoles(routineRoles);
        user.setOrganizationId(organizationId);

        // Sensitive-role accounts wait for approval before activation.
        if (sensitiveRoles.isEmpty()) {
            user.setStatus(UserStatus.PENDING_ACTIVATION);
        } else {
            user.setStatus(UserStatus.PENDING_APPROVAL);
        }

        User savedUser = userRepository.save(user);

        // Create one independent approval request for every sensitive role.
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

        // Send activation only after a routine-role user is committed successfully.
        if (savedUser.getStatus() == UserStatus.PENDING_ACTIVATION) {
            eventPublisher.publishEvent(
                    new UserActivationRequestedEvent(
                            savedUser.getId()
                    )
            );
        }

        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(
            Long organizationId,
            Long userId
    ) {
        User user = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        userId
                )
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
                .findAllByOrganizationId(
                        organizationId,
                        pageable
                )
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
                .findByOrganizationIdAndId(
                        organizationId,
                        userId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                                + " in organization: " + organizationId
                ));

        // Prevent duplicate email addresses within the organization.
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

        // Only active users can update another user's roles.
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

        // Prevent users from assigning roles to themselves.
        if (updater.getId().equals(targetUser.getId())) {
            throw new InvalidRoleRequestException(
                    "You cannot update your own roles"
            );
        }

        // Only active existing accounts can receive role updates.
        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidRoleRequestException(
                    "Roles can only be updated for an active user"
            );
        }

        // Treat submitted roles as additions instead of a full replacement.
        Set<UserRole> newRoles =
                new HashSet<>(request.getRoles());

        newRoles.removeAll(targetUser.getRoles());

        if (newRoles.isEmpty()) {
            return userMapper.toResponse(targetUser);
        }

        // Separate routine roles from approval-protected roles.
        Set<UserRole> routineRoles =
                roleApprovalPolicy.getRoutineRoles(newRoles);

        Set<UserRole> sensitiveRoles =
                roleApprovalPolicy.getSensitiveRoles(newRoles);

        // Validate permission and prevent duplicate pending requests.
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

        // Assign routine roles immediately.
        for (UserRole routineRole : routineRoles) {
            targetUser.addRole(routineRole);
        }

        // Convert every sensitive role into an approval request.
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
                .findByOrganizationIdAndId(
                        organizationId,
                        userId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with ID: " + userId
                                + " in organization: " + organizationId
                ));

        user.setStatus(request.getStatus());

        User updatedUser = userRepository.save(user);

        return userMapper.toResponse(updatedUser);
    }

    @Override
    public void resendActivationLink(
            Long organizationId,
            Long userId,
            Long requestedByUserId
    ) {
        User requester = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        requestedByUserId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Requesting user not found"
                ));

        if (requester.getStatus() != UserStatus.ACTIVE) {
            throw new ApprovalNotAllowedException(
                    "Only an active user can resend an activation link"
            );
        }

        boolean authorized = requester.getRoles()
                .stream()
                .anyMatch(ACTIVATION_RESEND_ROLES::contains);

        if (!authorized) {
            throw new ApprovalNotAllowedException(
                    "You are not authorized to resend activation links"
            );
        }

        User targetUser = userRepository
                .findByOrganizationIdAndId(
                        organizationId,
                        userId
                )
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Target user not found"
                ));

        if (targetUser.getStatus()
                != UserStatus.PENDING_ACTIVATION) {
            throw new InvalidRoleRequestException(
                    "Only a user pending activation can receive an activation link"
            );
        }

        if (targetUser.getEmail() == null
                || targetUser.getEmail().isBlank()) {
            throw new InvalidRoleRequestException(
                    "The user does not have an email address"
            );
        }

        eventPublisher.publishEvent(
                new UserActivationRequestedEvent(
                        targetUser.getId()
                )
        );
    }
}