INSERT INTO permissions (
    code,
    description,
    owning_service,
    sensitive,
    active
)
VALUES
    /*
     * Own profile and account
     */
    (
        'PROFILE_VIEW_SELF',
        'View the authenticated user''s own profile',
        'identity-service',
        FALSE,
        TRUE
    ),
    (
        'PROFILE_UPDATE_SELF',
        'Update the authenticated user''s own profile',
        'identity-service',
        FALSE,
        TRUE
    ),
    (
        'PASSWORD_CHANGE_SELF',
        'Change the authenticated user''s own password',
        'identity-service',
        FALSE,
        TRUE
    ),
    (
        'LOGIN_HISTORY_VIEW_SELF',
        'View the authenticated user''s own login history',
        'identity-service',
        FALSE,
        TRUE
    ),
    (
        'SESSION_VIEW_SELF',
        'View the authenticated user''s own active sessions',
        'identity-service',
        FALSE,
        TRUE
    ),
    (
        'SESSION_REVOKE_SELF',
        'Revoke the authenticated user''s own sessions',
        'identity-service',
        FALSE,
        TRUE
    ),

    /*
     * User account administration
     */
    (
        'USER_CREATE',
        'Create user accounts within the authorized organization',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'USER_VIEW',
        'View user accounts within the authorized organization',
        'identity-service',
        FALSE,
        TRUE
    ),
    (
        'USER_PROFILE_UPDATE',
        'Update another user''s profile within the authorized organization',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'USER_ACTIVATE',
        'Activate an eligible user account',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'USER_ACTIVATION_RESEND',
        'Resend a user account activation invitation',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'USER_DEACTIVATE',
        'Deactivate a user account',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'USER_SUSPEND',
        'Suspend a user account',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'USER_REACTIVATE',
        'Reactivate a suspended or inactive user account',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'USER_UNLOCK',
        'Unlock a user account that was locked by the security system',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'USER_PASSWORD_RESET_INITIATE',
        'Initiate a password reset for another user',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'USER_LOGIN_HISTORY_VIEW',
        'View another user''s login history',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'USER_SESSION_REVOKE',
        'Revoke another user''s active sessions',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'USER_EXPORT',
        'Export user account information',
        'identity-service',
        TRUE,
        TRUE
    ),

    /*
     * Role and permission visibility
     */
    (
        'ROLE_VIEW',
        'View available system roles',
        'identity-service',
        FALSE,
        TRUE
    ),
    (
        'PERMISSION_VIEW',
        'View available permissions',
        'identity-service',
        FALSE,
        TRUE
    ),
    (
        'ROLE_PERMISSION_VIEW',
        'View permissions assigned to system roles',
        'identity-service',
        FALSE,
        TRUE
    ),

    /*
     * Routine role management
     */
    (
        'ROLE_ASSIGN_ROUTINE',
        'Assign a routine role to an eligible user',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'ROLE_REMOVE_ROUTINE',
        'Remove a routine role from a user',
        'identity-service',
        TRUE,
        TRUE
    ),

    /*
     * Sensitive role-assignment workflow
     */
    (
        'ROLE_ASSIGNMENT_REQUEST_CREATE',
        'Create a request to assign a sensitive role',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'ROLE_ASSIGNMENT_REQUEST_VIEW',
        'View sensitive role-assignment requests',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'ROLE_ASSIGNMENT_REQUEST_CANCEL',
        'Cancel an eligible sensitive role-assignment request',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'ROLE_ASSIGNMENT_APPROVE',
        'Approve or reject a sensitive role-assignment request',
        'identity-service',
        TRUE,
        TRUE
    ),

    /*
     * Sensitive role-removal workflow
     */
    (
        'ROLE_REMOVAL_REQUEST_CREATE',
        'Create a request to remove a sensitive role',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'ROLE_REMOVAL_REQUEST_VIEW',
        'View sensitive role-removal requests',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'ROLE_REMOVAL_REQUEST_CANCEL',
        'Cancel an eligible sensitive role-removal request',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'ROLE_REMOVAL_APPROVE',
        'Approve or reject a sensitive role-removal request',
        'identity-service',
        TRUE,
        TRUE
    ),

    /*
     * Role-permission configuration
     */
    (
        'ROLE_PERMISSION_UPDATE',
        'Modify the permissions assigned to system roles',
        'identity-service',
        TRUE,
        TRUE
    ),

    /*
     * Security monitoring
     */
    (
        'SECURITY_EVENT_VIEW',
        'View security events within the authorized organization',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'SECURITY_AUDIT_VIEW',
        'View security audit records within the authorized organization',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'SECURITY_AUDIT_EXPORT',
        'Export security audit records',
        'identity-service',
        TRUE,
        TRUE
    ),

    /*
     * Security policy configuration
     */
    (
        'SECURITY_POLICY_VIEW',
        'View identity and access security policies',
        'identity-service',
        TRUE,
        TRUE
    ),
    (
        'SECURITY_POLICY_UPDATE',
        'Modify identity and access security policies',
        'identity-service',
        TRUE,
        TRUE
    );