import type { AccessRequestInput, AccessRequestType, UserAccess } from '@/api/nbadmin';

export interface AccessRequestForm {
  type: AccessRequestType;
  username: string;
  fullName: string;
  email: string;
  roleCodes: string[];
  homeBranchId: string;
  justification: string;
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
};

export const REQUEST_TYPE_LABELS: Record<AccessRequestType, string> = {
  CREATE_USER: 'Create user',
  MODIFY_ROLES: 'Change roles',
  DISABLE_USER: 'Disable user',
  ENABLE_USER: 'Enable user',
};

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

/** Request body (fields that do not apply to the type are left out). */
export function toAccessRequest(f: AccessRequestForm): AccessRequestInput {
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

/** Roles added and removed by a role change. */
export function roleChanges(
  current: readonly string[],
  requested: readonly string[],
): { added: string[]; removed: string[] } {
  return {
    added: requested.filter((r) => !current.includes(r)),
    removed: current.filter((r) => !requested.includes(r)),
  };
}
