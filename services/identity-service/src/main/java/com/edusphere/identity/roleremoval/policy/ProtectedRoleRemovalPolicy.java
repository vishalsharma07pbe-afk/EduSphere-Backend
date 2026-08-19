package com.edusphere.identity.roleremoval.policy;

import com.edusphere.identity.roleremoval.exception.ProtectedRoleRemovalException;
import com.edusphere.identity.user.entity.User;
import com.edusphere.identity.user.enums.UserRole;
import com.edusphere.identity.user.enums.UserStatus;
import com.edusphere.identity.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ProtectedRoleRemovalPolicy {

    private static final Set<UserRole> PROTECTED_LAST_ACTIVE_ROLES =
            Set.of(UserRole.ADMIN, UserRole.GOVERNING_AUTHORITY);

    private final UserRepository userRepository;

    public ProtectedRoleRemovalPolicy(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void assertRemovalAllowed(
            Long organizationId,
            User targetUser,
            UserRole requestedRole
    ) {
        if (!PROTECTED_LAST_ACTIVE_ROLES.contains(requestedRole)) {
            return;
        }

        if (targetUser.getStatus() != UserStatus.ACTIVE) {
            return;
        }

        long activeRoleHolders =
                userRepository.countByOrganizationIdAndStatusAndRole(
                        organizationId,
                        UserStatus.ACTIVE,
                        requestedRole
                );

        if (activeRoleHolders <= 1) {
            throw new ProtectedRoleRemovalException(
                    "Cannot remove the last active "
                            + requestedRole
                            + " in the organization"
            );
        }
    }
}
