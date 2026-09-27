import type {
  AccessRequest,
  AccessRequestInput,
  AccessRequestStatus,
  AccessRequestType,
  AccessUserType,
  PrivilegeLevel,
  RoleInfo,
  UserAccess,
} from '@/api/nbadmin';

/** An access request as edited on the New / Edit Request page (BRD 1.002-1.005, 3.002). */
export interface AccessRequestForm {
  type: AccessRequestType;
  userType: AccessUserType;
  username: string;
  fullName: string;
  email: string;
  roleCodes: string[];
  homeBranchId: string;
  justification: string;
  windowsId: string;
  businessUnitCode: string;
  userLevel: string;
  reasonCode: string;
  unlock: boolean;
  /** Date the change applies (yyyy-MM-dd); blank = on approval (UAM-NFR-14). */
  effectiveFrom: string;
  /** Approvers in order (a user request has one). */
  approvers: string[];
  /** Role of a group-profile request. */
  roleCode: string;
  /** Permissions the role holds today (loaded when the role is chosen). */
  currentPermissions: string[];
  /** Permissions the role should hold after the change (all permissions of a new profile). */
  permissions: string[];
  roleName: string;
  roleDescription: string;
  privilegeLevel: PrivilegeLevel | '';
  partyKind: '' | 'INSURER' | 'CLIENT';
  partyCode: string;
  portalRole: string;
}

export type AccessRequestErrors = Partial<Record<keyof AccessRequestForm, string>>;

export const EMPTY_ACCESS_REQUEST: AccessRequestForm = {
  type: 'CREATE_USER',
  userType: 'INTERNAL',
  username: '',
  fullName: '',
  email: '',
  roleCodes: [],
  homeBranchId: '',
  justification: '',
  windowsId: '',
  businessUnitCode: '',
  userLevel: '',
  reasonCode: '',
  unlock: false,
  effectiveFrom: '',
  approvers: [],
  roleCode: '',
  currentPermissions: [],
  permissions: [],
  roleName: '',
  roleDescription: '',
  privilegeLevel: '',
  partyKind: '',
  partyCode: '',
  portalRole: '',
};

export const REQUEST_TYPE_LABELS: Record<AccessRequestType, string> = {
  CREATE_USER: 'Enrol new user',
  MODIFY_USER: 'Modify user',
  DISABLE_USER: 'Deactivate user',
  ENABLE_USER: 'Reactivate user',
  MODIFY_ROLES: 'Change roles',
  CREATE_ROLE: 'New group profile',
  MODIFY_ROLE_PERMISSIONS: 'Modify group profile',
  DEACTIVATE_ROLE: 'Deactivate group profile',
  REACTIVATE_ROLE: 'Reactivate group profile',
};

/** Types offered on a new user request (FR-UA-011 to FR-UA-014). */
export const USER_TYPES: AccessRequestType[] = [
  'CREATE_USER',
  'MODIFY_USER',
  'DISABLE_USER',
  'ENABLE_USER',
];

/** Types offered on a new group-profile request (FR-UA-040 to FR-UA-043). */
export const GROUP_TYPES: AccessRequestType[] = [
  'CREATE_ROLE',
  'MODIFY_ROLE_PERMISSIONS',
  'DEACTIVATE_ROLE',
  'REACTIVATE_ROLE',
];

export const PRIVILEGE_LEVELS: PrivilegeLevel[] = ['LOW', 'STANDARD', 'HIGH', 'ADMIN'];

/** Types an external (portal) user request may have (decision D7). */
const EXTERNAL_TYPES: AccessRequestType[] = ['CREATE_USER', 'DISABLE_USER', 'ENABLE_USER'];

export function isGroupProfile(type: AccessRequestType): boolean {
  return GROUP_TYPES.includes(type);
}

/** Types whose request carries the group profiles of a user. */
export function carriesUserRoles(type: AccessRequestType): boolean {
  return type === 'CREATE_USER' || type === 'MODIFY_ROLES' || type === 'MODIFY_USER';
}

export function allowsExternal(type: AccessRequestType): boolean {
  return EXTERNAL_TYPES.includes(type);
}

/** Statuses in which the creator edits the request (draft, correction of a return). */
export function isEditable(status: AccessRequestStatus): boolean {
  return status === 'DRAFT' || status === 'RETURNED';
}

/** Statuses from which a request can be cancelled (BRD 1.007; UQ06). */
export function isCancellable(status: AccessRequestStatus): boolean {
  return ['DRAFT', 'PENDING', 'PENDING_SECOND', 'RETURNED', 'SCHEDULED'].includes(status);
}

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
const ROLE_CODE = /^[A-Z0-9_]{1,40}$/;

/** What the validation needs besides the form. */
export interface ValidationContext {
  users: UserAccess[];
  /** Full checks of a submission; false for a draft (formats only). */
  submit: boolean;
  /** USER_ID_PATTERN of a new user ID, blank for none. */
  userIdPattern?: string;
  /** Today (yyyy-MM-dd): an effective date cannot be earlier. */
  today?: string;
}

function patternError(pattern: string | undefined, username: string): string | undefined {
  if (!pattern) {
    return undefined;
  }
  try {
    return new RegExp(pattern).test(username)
      ? undefined
      : `The user ID must follow the format ${pattern}`;
  } catch {
    return undefined;
  }
}

function existingUserError(f: AccessRequestForm, user: UserAccess | undefined): string | undefined {
  if (user === undefined) {
    return 'Select an existing user';
  }
  if (f.type === 'DISABLE_USER' && !user.enabled) {
    return 'This user is already deactivated';
  }
  if (f.type === 'ENABLE_USER' && user.enabled && !user.locked) {
    return 'This user is already active';
  }
  return undefined;
}

function usernameError(f: AccessRequestForm, ctx: ValidationContext): string | undefined {
  const name = f.username.trim();
  if (!USERNAME.test(name)) {
    return 'Use 3 to 50 letters, digits, dots, dashes or underscores';
  }
  if (!ctx.submit || f.userType === 'EXTERNAL') {
    return undefined;
  }
  const user = ctx.users.find((u) => u.username.toLowerCase() === name.toLowerCase());
  if (f.type !== 'CREATE_USER') {
    return existingUserError(f, user);
  }
  return user === undefined ? patternError(ctx.userIdPattern, name) : 'This user already exists';
}

function userErrors(f: AccessRequestForm, ctx: ValidationContext): AccessRequestErrors {
  const errors: AccessRequestErrors = {};
  errors.username = usernameError(f, ctx);
  if (!ctx.submit) {
    return errors;
  }
  if (f.type === 'CREATE_USER' && f.fullName.trim() === '') {
    errors.fullName = 'Enter the full name of the new user';
  }
  const rolesNeeded = f.type === 'CREATE_USER' || f.type === 'MODIFY_ROLES';
  if (f.userType === 'INTERNAL' && rolesNeeded && f.roleCodes.length === 0) {
    errors.roleCodes = 'Select at least one role';
  }
  if (f.userType === 'EXTERNAL') {
    errors.partyKind = f.partyKind === '' ? 'Select the insurer or client' : undefined;
    errors.partyCode = f.partyCode.trim() === '' ? 'Enter the party code' : undefined;
  }
  return errors;
}

function newProfileErrors(f: AccessRequestForm): AccessRequestErrors {
  return {
    roleCode: ROLE_CODE.test(f.roleCode.trim())
      ? undefined
      : 'Use up to 40 capital letters, digits or underscores',
    roleName: f.roleName.trim() === '' ? 'Enter the name of the group profile' : undefined,
    permissions: f.permissions.length === 0 ? 'Select at least one permission' : undefined,
  };
}

function profileChangeError(f: AccessRequestForm): string | undefined {
  const { added, removed } = roleChanges(f.currentPermissions, f.permissions);
  const dataChange =
    f.roleName.trim() !== '' || f.roleDescription.trim() !== '' || f.privilegeLevel !== '';
  return added.length === 0 && removed.length === 0 && !dataChange
    ? 'Grant or withdraw at least one permission'
    : undefined;
}

function groupErrors(f: AccessRequestForm, ctx: ValidationContext): AccessRequestErrors {
  if (f.type === 'CREATE_ROLE') {
    return ctx.submit
      ? newProfileErrors(f)
      : { roleCode: f.roleCode.trim() === '' ? 'Enter the profile code' : undefined };
  }
  if (f.roleCode === '') {
    return { roleCode: 'Select the role to change' };
  }
  return ctx.submit && f.type === 'MODIFY_ROLE_PERMISSIONS'
    ? { permissions: profileChangeError(f) }
    : {};
}

function commonErrors(f: AccessRequestForm, ctx: ValidationContext): AccessRequestErrors {
  const errors: AccessRequestErrors = {};
  if (f.effectiveFrom !== '' && ctx.today !== undefined && f.effectiveFrom < ctx.today) {
    errors.effectiveFrom = 'The effective date cannot be before today';
  }
  if (!ctx.submit) {
    return errors;
  }
  if (f.justification.trim() === '') {
    errors.justification = 'Enter the justification';
  }
  if (f.approvers.length === 0) {
    errors.approvers = isGroupProfile(f.type) ? 'Add at least one approver' : 'Select the approver';
  }
  return errors;
}

function withoutBlanks(errors: AccessRequestErrors): AccessRequestErrors {
  const out: AccessRequestErrors = {};
  (Object.keys(errors) as (keyof AccessRequestForm)[]).forEach((key) => {
    const message = errors[key];
    if (message !== undefined) {
      out[key] = message;
    }
  });
  return out;
}

/**
 * Field errors of an access request (FR-UA-010 to FR-UA-016, FR-UA-040 to FR-UA-044): formats only
 * for a draft; for a submission the user state, the new user's data, the group profiles, the
 * remarks and the approvers.
 */
export function validateAccessRequest(
  f: AccessRequestForm,
  ctx: ValidationContext,
): AccessRequestErrors {
  const specific = isGroupProfile(f.type) ? groupErrors(f, ctx) : userErrors(f, ctx);
  return withoutBlanks({ ...specific, ...commonErrors(f, ctx) });
}

function text(value: string): string | undefined {
  const t = value.trim();
  return t === '' ? undefined : t;
}

function groupInput(f: AccessRequestForm): AccessRequestInput {
  const base: AccessRequestInput = {
    type: f.type,
    roleCode: f.roleCode.trim(),
    privilegeLevel: f.privilegeLevel === '' ? undefined : f.privilegeLevel,
    justification: text(f.justification),
    approvers: f.approvers,
  };
  const data = { roleName: text(f.roleName), roleDescription: text(f.roleDescription) };
  if (f.type === 'CREATE_ROLE') {
    return { ...base, ...data, permissionsAdded: f.permissions };
  }
  if (f.type === 'MODIFY_ROLE_PERMISSIONS') {
    const { added, removed } = roleChanges(f.currentPermissions, f.permissions);
    return { ...base, ...data, permissionsAdded: added, permissionsRemoved: removed };
  }
  return base;
}

function userDataInput(f: AccessRequestForm): Partial<AccessRequestInput> {
  if (f.type !== 'CREATE_USER' && f.type !== 'MODIFY_USER') {
    return {};
  }
  return {
    fullName: text(f.fullName),
    email: text(f.email),
    homeBranchId: f.homeBranchId === '' ? undefined : Number(f.homeBranchId),
    windowsId: text(f.windowsId),
    businessUnitCode: text(f.businessUnitCode),
    userLevel: text(f.userLevel),
  };
}

function externalInput(f: AccessRequestForm): Partial<AccessRequestInput> {
  if (f.userType !== 'EXTERNAL') {
    return {};
  }
  return {
    partyKind: f.partyKind === '' ? undefined : f.partyKind,
    partyCode: text(f.partyCode),
    portalRole: text(f.portalRole),
  };
}

function userInput(f: AccessRequestForm): AccessRequestInput {
  return {
    type: f.type,
    username: f.username.trim(),
    ...userDataInput(f),
    ...externalInput(f),
    roleCodes: carriesUserRoles(f.type) && f.userType === 'INTERNAL' ? f.roleCodes : undefined,
    reasonCode: f.type === 'DISABLE_USER' ? text(f.reasonCode) : undefined,
    unlock: f.type === 'ENABLE_USER' ? f.unlock : undefined,
    effectiveFrom: text(f.effectiveFrom),
    justification: text(f.justification),
    approvers: f.approvers,
  };
}

/** Request body (fields that do not apply to the type are left out). */
export function toAccessRequest(f: AccessRequestForm): AccessRequestInput {
  return isGroupProfile(f.type) ? groupInput(f) : userInput(f);
}

function blank(value: string | undefined): string {
  return value ?? '';
}

function savedPermissions(r: AccessRequest, current: string[]): string[] {
  if (r.type === 'CREATE_ROLE') {
    return r.permissionsAdded;
  }
  const kept = current.filter((p) => !r.permissionsRemoved.includes(p));
  return [...new Set([...kept, ...r.permissionsAdded])];
}

function savedUser(r: AccessRequest): Partial<AccessRequestForm> {
  const d = r.details;
  return {
    username: blank(r.username),
    fullName: blank(r.fullName),
    email: blank(r.email),
    roleCodes: r.roleCodes,
    homeBranchId: r.homeBranchId === undefined ? '' : String(r.homeBranchId),
    windowsId: blank(d.windowsId),
    businessUnitCode: blank(d.businessUnitCode),
    userLevel: blank(d.userLevel),
    reasonCode: blank(d.reasonCode),
    unlock: d.unlock,
    effectiveFrom: blank(d.effectiveFrom),
    partyKind: d.partyKind ?? '',
    partyCode: blank(d.partyCode),
    portalRole: blank(d.portalRole),
  };
}

function savedProfile(r: AccessRequest, roles: RoleInfo[]): Partial<AccessRequestForm> {
  const d = r.details;
  const current = roles.find((x) => x.code === r.roleCode)?.permissions ?? [];
  return {
    roleCode: blank(r.roleCode),
    currentPermissions: current,
    permissions: savedPermissions(r, current),
    roleName: blank(d.roleName),
    roleDescription: blank(d.roleDescription),
    privilegeLevel: d.privilegeLevel ?? '',
  };
}

/** The form of a saved request (edit of a draft or correction of a returned request). */
export function fromAccessRequest(r: AccessRequest, roles: RoleInfo[]): AccessRequestForm {
  return {
    ...EMPTY_ACCESS_REQUEST,
    ...savedUser(r),
    ...savedProfile(r, roles),
    type: r.type,
    userType: r.userType,
    justification: blank(r.justification),
    approvers: r.lifecycle.approvers.map((a) => a.approver),
  };
}

/** Status shown for a user (FR-UA-052): Active, Disabled or Locked. */
export function userStatus(user: Pick<UserAccess, 'enabled' | 'locked'>): string {
  if (user.locked) {
    return 'LOCKED';
  }
  return user.enabled ? 'ACTIVE' : 'DISABLED';
}

/** Users offered for a request type: enabled users to deactivate, disabled or locked to reactivate. */
export function usersFor(form: AccessRequestForm, users: UserAccess[]): UserAccess[] {
  if (form.type === 'DISABLE_USER') {
    return users.filter((u) => u.enabled);
  }
  if (form.type === 'ENABLE_USER') {
    return users.filter((u) => !u.enabled || u.locked);
  }
  return users;
}

/** A new request, preset from the link (Users screen: ?type=MODIFY_USER&user=...). */
export function initialForm(search: URLSearchParams, users: UserAccess[]): AccessRequestForm {
  const preset = search.get('type') as AccessRequestType | null;
  const fallback: AccessRequestType =
    search.get('kind') === 'group' ? 'CREATE_ROLE' : 'CREATE_USER';
  const type = preset !== null && preset in REQUEST_TYPE_LABELS ? preset : fallback;
  const username = search.get('user') ?? '';
  const user = users.find((u) => u.username === username);
  return { ...EMPTY_ACCESS_REQUEST, type, username, roleCodes: user?.roleCodes ?? [] };
}
