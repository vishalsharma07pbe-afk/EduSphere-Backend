package com.edusphere.identity.permission.service;

import com.edusphere.identity.permission.enums.PermissionCode;
import com.edusphere.identity.user.enums.UserRole;

import java.util.Set;

public interface PermissionService {

    Set<PermissionCode> getActivePermissionsForRoles(
            Set<UserRole> roles
    );
}