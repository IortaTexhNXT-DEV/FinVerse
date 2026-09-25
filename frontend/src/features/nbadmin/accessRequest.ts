import type { AccessRequestInput, AccessRequestType, UserAccess } from '@/api/nbadmin';

export interface AccessRequestForm {
  type: AccessRequestType;
  username: string;
  fullName: string;
  email: string;
  roleCodes: string[];
  homeBranchId: string;
  justification: string;
  /** Role whose permissions change (MODIFY_ROLE_PERMISSIONS). */
  roleCode: string;
  /** Permissions the role holds today (loaded when the role is chosen). */
  currentPermissions: string[];
  /** Permissions the role should hold after the change. */
  permissions: string[];
}

export type AccessRequestErrors = Partial<Record<keyof AccessRequestForm, string>>;

export const EMPTY_ACCESS_REQUEST: AccessRequestForm = {
  type: 'CREATE_USER',
  username: '',
  fullName: '',
  email: '',
  roleCodes: [],
  homeBranchId: '',
  justification: '',
  roleCode: '',
  currentPermissions: [],
  permissions: [],
};

export const REQUEST_TYPE_LABELS: Record<AccessRequestType, string> = {
  CREATE_USER: 'Create user',
  MODIFY_ROLES: 'Change roles',
  DISABLE_USER: 'Disable user',
  ENABLE_USER: 'Enable user',
  MODIFY_ROLE_PERMISSIONS: 'Change role permissions',
};

/** Codes added and removed by a change (roles of a user, permissions of a role). */
export function roleChanges(
  current: readonly string[],
  requested: readonly string[],
): { added: string[]; removed: string[] } {
  return {
    added: requested.filter((r) => !current.includes(r)),
    removed: current.filter((r) => !requested.includes(r)),
  };
}

const USERNAME = /^[a-zA-Z0-9._-]{3,50}$/;

function userErrors(f: AccessRequestForm, users: UserAccess[]): AccessRequestErrors {
  const existing = users.find((u) => u.username.toLowerCase() === f.username.trim().toLowerCase());
  if (f.type === 'CREATE_USER') {
    return existing === undefined ? {} : { username: 'This user already exists' };
  }
  if (existing === undefined) {
    return { username: 'Select an existing user' };
  }
  if (f.type === 'DISABLE_USER' && !existing.enabled) {
    return { username: 'This user is already disabled' };
  }
  if (f.type === 'ENABLE_USER' && existing.enabled) {
    return { username: 'This user is already enabled' };
  }
  return {};
}

/**
 * Field errors of an access request (BRNB.085): a valid user name, new for a creation and existing
 * otherwise, a full name for a creation, roles for a creation or role change, and a justification.
 */
export function validateAccessRequest(
  f: AccessRequestForm,
  users: UserAccess[],
): AccessRequestErrors {
  if (f.type === 'MODIFY_ROLE_PERMISSIONS') {
    return validatePermissionChange(f);
  }
  const errors: AccessRequestErrors = USERNAME.test(f.username.trim())
    ? userErrors(f, users)
    : { username: 'Use 3 to 50 letters, digits, dots, dashes or underscores' };
  if (f.type === 'CREATE_USER' && f.fullName.trim() === '') {
    errors.fullName = 'Enter the full name';
  }
  if ((f.type === 'CREATE_USER' || f.type === 'MODIFY_ROLES') && f.roleCodes.length === 0) {
    errors.roleCodes = 'Select at least one role';
  }
  if (f.justification.trim() === '') {
    errors.justification = 'Explain why the access is needed';
  }
  return errors;
}

/** Field errors of a role-permission change: a role, at least one change and a justification. */
function validatePermissionChange(f: AccessRequestForm): AccessRequestErrors {
  const errors: AccessRequestErrors = {};
  if (f.roleCode === '') {
    errors.roleCode = 'Select the role to change';
  } else {
    const { added, removed } = roleChanges(f.currentPermissions, f.permissions);
    if (added.length === 0 && removed.length === 0) {
      errors.permissions = 'Grant or withdraw at least one permission';
    }
  }
  if (f.justification.trim() === '') {
    errors.justification = 'Explain why the change is needed';
  }
  return errors;
}

/** Request body (fields that do not apply to the type are left out). */
export function toAccessRequest(f: AccessRequestForm): AccessRequestInput {
  if (f.type === 'MODIFY_ROLE_PERMISSIONS') {
    const { added, removed } = roleChanges(f.currentPermissions, f.permissions);
    return {
      type: f.type,
      roleCode: f.roleCode,
      permissionsAdded: added,
      permissionsRemoved: removed,
      justification: f.justification.trim(),
    };
  }
  const create = f.type === 'CREATE_USER';
  const withRoles = create || f.type === 'MODIFY_ROLES';
  return {
    type: f.type,
    username: f.username.trim(),
    fullName: create ? f.fullName.trim() : undefined,
    email: create && f.email.trim() !== '' ? f.email.trim() : undefined,
    roleCodes: withRoles ? f.roleCodes : undefined,
    homeBranchId: create && f.homeBranchId !== '' ? Number(f.homeBranchId) : undefined,
    justification: f.justification.trim(),
  };
}
