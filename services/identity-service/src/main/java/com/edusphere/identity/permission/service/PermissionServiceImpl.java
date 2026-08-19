package com.edusphere.identity.permission.service;

import com.edusphere.identity.permission.enums.PermissionCode;
import com.edusphere.identity.permission.repository.RolePermissionRepository;
import com.edusphere.identity.user.enums.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class PermissionServiceImpl implements PermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    public PermissionServiceImpl(
            RolePermissionRepository rolePermissionRepository
    ) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<PermissionCode> getActivePermissionsForRoles(
            Set<UserRole> roles
    ) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }

        return rolePermissionRepository
                .findActivePermissionCodesByRoles(roles);
    }
}