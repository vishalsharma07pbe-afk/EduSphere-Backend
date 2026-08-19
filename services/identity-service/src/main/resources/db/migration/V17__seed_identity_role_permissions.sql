/*
 * Every authenticated role can manage its own account.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('PRINCIPAL'),
             ('VICE_PRINCIPAL'),
             ('TEACHER'),
             ('STUDENT'),
             ('PARENT'),
             ('ADMISSIONS_OFFICER'),
             ('EXAMINATION_CONTROLLER'),
             ('LIBRARIAN'),
             ('HR'),
             ('ACCOUNTANT'),
             ('TRANSPORT_MANAGER'),
             ('HOSTEL_STAFF'),
             ('INVENTORY_MANAGER'),
             ('SPORTS_STAFF'),
             ('SCIENCE_LAB_STAFF'),
             ('COMPUTER_LAB_STAFF'),
             ('MUSIC_STAFF'),
             ('MAINTENANCE_STAFF'),
             ('HOUSEKEEPING_STAFF'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code IN (
                           'PROFILE_VIEW_SELF',
                           'PROFILE_UPDATE_SELF',
                           'PASSWORD_CHANGE_SELF',
                           'LOGIN_HISTORY_VIEW_SELF',
                           'SESSION_VIEW_SELF',
                           'SESSION_REVOKE_SELF'
    );


/*
 * User account visibility.
 *
 * Scope checks will still restrict users to their organization
 * and permitted population.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('PRINCIPAL'),
             ('VICE_PRINCIPAL'),
             ('HR'),
             ('ADMISSIONS_OFFICER'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'USER_VIEW';


/*
 * User creation responsibilities.
 *
 * HR creates staff accounts.
 * Admissions creates students and parents.
 * Admin retains organization-level administrative capability.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('HR'),
             ('ADMISSIONS_OFFICER')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'USER_CREATE';


/*
 * User profile administration.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('HR'),
             ('ADMISSIONS_OFFICER')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'USER_PROFILE_UPDATE';


/*
 * Account activation operations.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('HR'),
             ('ADMISSIONS_OFFICER')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code IN (
                           'USER_ACTIVATE',
                           'USER_ACTIVATION_RESEND'
    );


/*
 * High-impact account status operations.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('HR')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code IN (
                           'USER_DEACTIVATE',
                           'USER_SUSPEND',
                           'USER_REACTIVATE'
    );


/*
 * Account security support.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('HR')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code IN (
                           'USER_UNLOCK',
                           'USER_PASSWORD_RESET_INITIATE',
                           'USER_LOGIN_HISTORY_VIEW',
                           'USER_SESSION_REVOKE'
    );


/*
 * User data export.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('HR'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'USER_EXPORT';


/*
 * Role and permission catalogue visibility.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('PRINCIPAL'),
             ('VICE_PRINCIPAL'),
             ('HR'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code IN (
                           'ROLE_VIEW',
                           'PERMISSION_VIEW',
                           'ROLE_PERMISSION_VIEW'
    );


/*
 * Routine role management.
 *
 * Sensitive roles cannot be granted directly with these permissions.
 * They must pass through the approval workflow.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('HR')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code IN (
                           'ROLE_ASSIGN_ROUTINE',
                           'ROLE_REMOVE_ROUTINE'
    );


/*
 * Creation and management of sensitive role-assignment requests.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('HR')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code IN (
                           'ROLE_ASSIGNMENT_REQUEST_CREATE',
                           'ROLE_ASSIGNMENT_REQUEST_VIEW',
                           'ROLE_ASSIGNMENT_REQUEST_CANCEL'
    );


/*
 * Leadership can view sensitive role-assignment requests.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('PRINCIPAL'),
             ('VICE_PRINCIPAL'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'ROLE_ASSIGNMENT_REQUEST_VIEW';


/*
 * Approval capability.
 *
 * RoleApprovalPolicy must still decide whether a particular approver
 * is valid for the requested role.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('PRINCIPAL'),
             ('VICE_PRINCIPAL'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'ROLE_ASSIGNMENT_APPROVE';


/*
 * Sensitive role-removal request management.
 *
 * The permissions are reserved now even though the workflow will be
 * implemented later.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('HR')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code IN (
                           'ROLE_REMOVAL_REQUEST_CREATE',
                           'ROLE_REMOVAL_REQUEST_VIEW',
                           'ROLE_REMOVAL_REQUEST_CANCEL'
    );


/*
 * Leadership can view sensitive role-removal requests.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('PRINCIPAL'),
             ('VICE_PRINCIPAL'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'ROLE_REMOVAL_REQUEST_VIEW';


/*
 * Sensitive role-removal approval.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('PRINCIPAL'),
             ('VICE_PRINCIPAL'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'ROLE_REMOVAL_APPROVE';


/*
 * Changing role-permission definitions is restricted to the
 * governing authority.
 *
 * Initially, mappings are still managed through Flyway rather than
 * through an administration endpoint.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT 'GOVERNING_AUTHORITY', permissions.id
FROM permissions
WHERE permissions.code = 'ROLE_PERMISSION_UPDATE';


/*
 * Security-event monitoring.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('PRINCIPAL'),
             ('VICE_PRINCIPAL'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'SECURITY_EVENT_VIEW';


/*
 * Security-audit visibility.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('PRINCIPAL'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'SECURITY_AUDIT_VIEW';


/*
 * Exporting audit records is more restricted than viewing them.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('PRINCIPAL'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'SECURITY_AUDIT_EXPORT';


/*
 * Security-policy visibility.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT roles.role, permissions.id
FROM (
         VALUES
             ('ADMIN'),
             ('PRINCIPAL'),
             ('GOVERNING_AUTHORITY')
     ) AS roles(role)
         CROSS JOIN permissions
WHERE permissions.code = 'SECURITY_POLICY_VIEW';


/*
 * Only the governing authority receives the capability to modify
 * security policy.
 */
INSERT INTO role_permissions (role, permission_id)
SELECT 'GOVERNING_AUTHORITY', permissions.id
FROM permissions
WHERE permissions.code = 'SECURITY_POLICY_UPDATE';