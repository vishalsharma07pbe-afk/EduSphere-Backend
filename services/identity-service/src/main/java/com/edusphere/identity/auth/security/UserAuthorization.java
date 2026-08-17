package com.edusphere.identity.auth.security;

import com.edusphere.identity.user.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component("userAuthorization")
public class UserAuthorization {

    private static final Set<UserRole> ADMISSIONS_ALLOWED_ROLES =
            Set.of(
                    UserRole.STUDENT,
                    UserRole.PARENT
            );

    private static final Set<UserRole> HR_ALLOWED_ROLES =
            Set.of(
                    UserRole.TEACHER,
                    UserRole.ACCOUNTANT,
                    UserRole.LIBRARIAN,
                    UserRole.EXAMINATION_CONTROLLER,
                    UserRole.TRANSPORT_MANAGER,
                    UserRole.INVENTORY_MANAGER,
                    UserRole.SPORTS_DEPARTMENT,
                    UserRole.SCIENCE_LAB,
                    UserRole.COMPUTER_LAB,
                    UserRole.MUSIC_DEPARTMENT,
                    UserRole.MAINTENANCE,
                    UserRole.HOUSEKEEPING,
                    UserRole.HOSTEL
            );

    public boolean canCreateUser(
            Authentication authentication,
            Set<UserRole> requestedRoles
    ) {
        if (authentication == null ||
                requestedRoles == null ||
                requestedRoles.isEmpty()) {
            return false;
        }

        if (hasAnyRole(
                authentication,
                "ROLE_ADMIN",
                "ROLE_PRINCIPAL"
        )) {
            return true;
        }

        Set<UserRole> allowedRoles = new HashSet<>();

        if (hasRole(authentication, "ROLE_ADMISSIONS")) {
            allowedRoles.addAll(ADMISSIONS_ALLOWED_ROLES);
        }

        if (hasRole(authentication, "ROLE_HR")) {
            allowedRoles.addAll(HR_ALLOWED_ROLES);
        }

        return allowedRoles.containsAll(requestedRoles);
    }

    private boolean hasAnyRole(
            Authentication authentication,
            String... roles
    ) {
        for (String role : roles) {
            if (hasRole(authentication, role)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasRole(
            Authentication authentication,
            String requiredRole
    ) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority()
                                .equals(requiredRole)
                );
    }
}