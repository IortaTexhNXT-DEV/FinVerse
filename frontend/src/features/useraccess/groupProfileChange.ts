import type {
  AccessRequest,
  AccessRequestInput,
  AccessRequestStatus,
  AccessRequestType,
  PrivilegeLevel,
  RoleInfo,
} from '@/api/nbadmin';

/** The changes of a group profile the maker proposes (BRD 3.002.1-4). */
export type ChangeType = Extract<
  AccessRequestType,
  'CREATE_ROLE' | 'MODIFY_ROLE_PERMISSIONS' | 'DEACTIVATE_ROLE' | 'REACTIVATE_ROLE'
>;

export const CHANGE_LABELS: Record<AccessRequestType, string> = {
  CREATE_ROLE: 'New group profile',
  MODIFY_ROLE_PERMISSIONS: 'Change of definition or permissions',
  DEACTIVATE_ROLE: 'Deactivation',
  REACTIVATE_ROLE: 'Reactivation',
  // Requests about users raised before UIDM-ISC maintained the users (history only).
  CREATE_USER: 'User enrolment (before UIDM-ISC)',
  MODIFY_USER: 'User change (before UIDM-ISC)',
  MODIFY_ROLES: 'User group profiles (before UIDM-ISC)',
  DISABLE_USER: 'User deactivation',
  ENABLE_USER: 'User reactivation (before UIDM-ISC)',
};

export const PRIVILEGE_LEVELS: PrivilegeLevel[] = ['LOW', 'STANDARD', 'HIGH', 'ADMIN'];

/** Statuses of a change still open (waiting for the maker, a checker or the implementation). */
export const OPEN_STATUSES: readonly AccessRequestStatus[] = [
  'DRAFT',
  'PENDING',
  'PENDING_SECOND',
  'RETURNED',
  'SCHEDULED',
  'FOR_IMPLEMENTATION',
];

export function isOpen(status: AccessRequestStatus): boolean {
  return OPEN_STATUSES.includes(status);
}

/** A proposal as edited. */
export interface ChangeForm {
  type: ChangeType;
  roleCode: string;
  roleName: string;
  roleDescription: string;
  privilegeLevel: PrivilegeLevel | '';
  /** Permissions the group profile holds now (empty for a new one). */
  current: string[];
  /** Permissions it should hold after the change. */
  permissions: string[];
  justification: string;
}

export type ChangeErrors = Partial<Record<keyof ChangeForm, string>>;

/**
 * A new proposal of a type for a group profile (none for a new profile). The name, description and
 * level of a change start blank: blank keeps the current value.
 */
export function newChange(type: ChangeType, role?: RoleInfo): ChangeForm {
  return {
    type,
    roleCode: role?.code ?? '',
    roleName: '',
    roleDescription: '',
    privilegeLevel: '',
    current: role?.permissions ?? [],
    permissions: role?.permissions ?? [],
    justification: '',
  };
}

/** A saved draft or a returned change, to correct. */
export function fromRequest(r: AccessRequest, role?: RoleInfo): ChangeForm {
  const current = role?.permissions ?? [];
  const kept = current.filter((p) => !r.permissionsRemoved.includes(p));
  return {
    type: r.type as ChangeType,
    roleCode: r.roleCode ?? '',
    roleName: r.details.roleName ?? '',
    roleDescription: r.details.roleDescription ?? '',
    privilegeLevel: r.details.privilegeLevel ?? '',
    current,
    permissions: [...new Set([...kept, ...r.permissionsAdded])].sort((a, b) => a.localeCompare(b)),
    justification: r.justification ?? '',
  };
}

const ROLE_CODE = /^[A-Z0-9_]{1,40}$/;

/** Permissions added and removed by a change. */
export function permissionChanges(f: ChangeForm): { added: string[]; removed: string[] } {
  return {
    added: f.permissions.filter((p) => !f.current.includes(p)),
    removed: f.current.filter((p) => !f.permissions.includes(p)),
  };
}

function createErrors(f: ChangeForm, submit: boolean): ChangeErrors {
  return {
    roleCode: ROLE_CODE.test(f.roleCode.trim())
      ? undefined
      : 'Enter the code: capital letters, digits and _ (up to 40)',
    roleName:
      submit && f.roleName.trim() === '' ? 'Enter the name of the group profile' : undefined,
    permissions:
      submit && f.permissions.length === 0 ? 'Select at least one permission' : undefined,
  };
}

function modifyErrors(f: ChangeForm, submit: boolean): ChangeErrors {
  if (f.roleCode === '') {
    return { roleCode: 'Choose the group profile' };
  }
  const { added, removed } = permissionChanges(f);
  const unchanged = added.length + removed.length === 0 && !definitionChanged(f);
  return submit && f.type === 'MODIFY_ROLE_PERMISSIONS' && unchanged
    ? { permissions: 'Change the permissions, the name, the description or the level' }
    : {};
}

/** What is missing before a proposal is submitted (a draft needs the type and profile only). */
export function validateChange(f: ChangeForm, submit: boolean): ChangeErrors {
  return {
    ...(f.type === 'CREATE_ROLE' ? createErrors(f, submit) : modifyErrors(f, submit)),
    justification:
      submit && f.justification.trim() === '' ? 'Enter the reason for the change' : undefined,
  };
}

/** Whether any field has a problem. */
export function hasErrors(errors: ChangeErrors): boolean {
  return Object.values(errors).some(Boolean);
}

function definitionChanged(f: ChangeForm): boolean {
  return f.roleName.trim() !== '' || f.roleDescription.trim() !== '' || f.privilegeLevel !== '';
}

const text = (v: string) => (v.trim() === '' ? undefined : v.trim());

/** The body sent to the server. */
export function toInput(f: ChangeForm): AccessRequestInput {
  const base: AccessRequestInput = {
    type: f.type,
    roleCode: f.roleCode.trim(),
    justification: text(f.justification),
  };
  if (f.type === 'DEACTIVATE_ROLE' || f.type === 'REACTIVATE_ROLE') {
    return base;
  }
  const data = {
    ...base,
    roleName: text(f.roleName),
    roleDescription: text(f.roleDescription),
    privilegeLevel: f.privilegeLevel === '' ? undefined : f.privilegeLevel,
  };
  if (f.type === 'CREATE_ROLE') {
    return { ...data, permissionsAdded: f.permissions };
  }
  const { added, removed } = permissionChanges(f);
  return { ...data, permissionsAdded: added, permissionsRemoved: removed };
}
