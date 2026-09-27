import type { AccessRequest, RoleInfo, UserAccess } from '@/api/nbadmin';
import {
  EMPTY_ACCESS_REQUEST,
  fromAccessRequest,
  isCancellable,
  isEditable,
  roleChanges,
  toAccessRequest,
  validateAccessRequest,
} from './accessRequest';
import type { AccessRequestForm, ValidationContext } from './accessRequest';

const USERS: UserAccess[] = [
  { username: 'aileen', fullName: 'Aileen', enabled: true, roleCodes: ['MKT_AO'], locked: false },
  { username: 'old', fullName: 'Old User', enabled: false, roleCodes: [], locked: false },
  { username: 'locked', fullName: 'Locked', enabled: true, roleCodes: [], locked: true },
];

const SUBMIT: ValidationContext = {
  users: USERS,
  submit: true,
  userIdPattern: '^[a-zA-Z][0-9]{9}$',
  today: '2026-09-25',
};
const DRAFT: ValidationContext = { users: USERS, submit: false };

const form = (patch: Partial<AccessRequestForm>): AccessRequestForm => ({
  ...EMPTY_ACCESS_REQUEST,
  ...patch,
});

describe('user access request validation', () => {
  it('checks only the formats of a draft', () => {
    expect(validateAccessRequest(form({ username: 'a013000196' }), DRAFT)).toEqual({});
    expect(validateAccessRequest(form({ username: 'ab' }), DRAFT)).toEqual({
      username: 'Use 3 to 50 letters, digits, dots, dashes or underscores',
    });
  });

  it('needs the new user data, a group profile, the remarks and the approver to submit', () => {
    expect(validateAccessRequest(form({ username: 'a013000196' }), SUBMIT)).toEqual({
      fullName: 'Enter the full name of the new user',
      roleCodes: 'Select at least one role',
      justification: 'Enter the justification',
      approvers: 'Select the approver',
    });
    expect(validateAccessRequest(form({ username: 'AILEEN' }), SUBMIT).username).toBe(
      'This user already exists',
    );
    expect(validateAccessRequest(form({ username: 'a01300019X' }), SUBMIT).username).toBe(
      'The user ID must follow the format ^[a-zA-Z][0-9]{9}$',
    );
    expect(
      validateAccessRequest(
        form({
          username: 'a013000196',
          fullName: 'UAT User One',
          roleCodes: ['MKT_AO'],
          justification: 'Joined',
          approvers: ['uamapprover'],
        }),
        SUBMIT,
      ),
    ).toEqual({});
  });

  it('needs an existing user in the right state and a date from today', () => {
    const base = { justification: 'x', approvers: ['uamapprover'] };
    expect(
      validateAccessRequest(form({ ...base, type: 'MODIFY_USER', username: 'nobody' }), SUBMIT)
        .username,
    ).toBe('Select an existing user');
    expect(
      validateAccessRequest(form({ ...base, type: 'DISABLE_USER', username: 'old' }), SUBMIT)
        .username,
    ).toBe('This user is already deactivated');
    expect(
      validateAccessRequest(form({ ...base, type: 'ENABLE_USER', username: 'aileen' }), SUBMIT)
        .username,
    ).toBe('This user is already active');
    expect(
      validateAccessRequest(form({ ...base, type: 'ENABLE_USER', username: 'locked' }), SUBMIT),
    ).toEqual({});
    expect(
      validateAccessRequest(
        form({ ...base, type: 'DISABLE_USER', username: 'aileen', effectiveFrom: '2026-09-24' }),
        SUBMIT,
      ).effectiveFrom,
    ).toBe('The effective date cannot be before today');
  });

  it('needs the party of an external user', () => {
    expect(
      validateAccessRequest(
        form({ userType: 'EXTERNAL', username: 'hr.user', fullName: 'HR', justification: 'x' }),
        SUBMIT,
      ),
    ).toEqual({
      partyKind: 'Select the insurer or client',
      partyCode: 'Enter the party code',
      approvers: 'Select the approver',
    });
  });
});

describe('group-profile request validation', () => {
  const base = { justification: 'x', approvers: ['uamapprover'] };

  it('needs a code, a name and permissions for a new profile', () => {
    expect(validateAccessRequest(form({ type: 'CREATE_ROLE' }), DRAFT)).toEqual({
      roleCode: 'Enter the profile code',
    });
    expect(
      validateAccessRequest(form({ ...base, type: 'CREATE_ROLE', roleCode: 'bad code' }), SUBMIT),
    ).toEqual({
      roleCode: 'Use up to 40 capital letters, digits or underscores',
      roleName: 'Enter the name of the group profile',
      permissions: 'Select at least one permission',
    });
    expect(
      validateAccessRequest(form({ type: 'CREATE_ROLE', roleCode: 'X' }), SUBMIT).approvers,
    ).toBe('Add at least one approver');
  });

  it('needs a role and a change for a modification', () => {
    const change = { ...base, type: 'MODIFY_ROLE_PERMISSIONS' as const };
    expect(validateAccessRequest(form(change), SUBMIT)).toEqual({
      roleCode: 'Select the role to change',
    });
    expect(
      validateAccessRequest(
        form({ ...change, roleCode: 'TSU', currentPermissions: ['A'], permissions: ['A'] }),
        SUBMIT,
      ),
    ).toEqual({ permissions: 'Grant or withdraw at least one permission' });
    expect(
      validateAccessRequest(
        form({ ...change, roleCode: 'TSU', currentPermissions: ['A'], privilegeLevel: 'HIGH' }),
        SUBMIT,
      ),
    ).toEqual({});
  });
});

describe('access request mapping', () => {
  it('sends only the fields of a user request type', () => {
    expect(
      toAccessRequest(
        form({
          type: 'DISABLE_USER',
          username: ' aileen ',
          fullName: 'ignored',
          roleCodes: ['TSU'],
          reasonCode: 'RESIGNED',
          effectiveFrom: '2026-09-30',
          justification: ' Left ',
          approvers: ['uamapprover'],
        }),
      ),
    ).toEqual({
      type: 'DISABLE_USER',
      username: 'aileen',
      roleCodes: undefined,
      reasonCode: 'RESIGNED',
      unlock: undefined,
      effectiveFrom: '2026-09-30',
      justification: 'Left',
      approvers: ['uamapprover'],
    });
    const create = toAccessRequest(
      form({
        username: 'n',
        fullName: 'N',
        homeBranchId: '3',
        windowsId: 'W1',
        roleCodes: ['TSU'],
      }),
    );
    expect(create.homeBranchId).toBe(3);
    expect(create.windowsId).toBe('W1');
    expect(create.roleCodes).toEqual(['TSU']);
    const external = toAccessRequest(
      form({ userType: 'EXTERNAL', username: 'hr', partyKind: 'CLIENT', partyCode: 'C1' }),
    );
    expect(external.partyKind).toBe('CLIENT');
    expect(external.roleCodes).toBeUndefined();
  });

  it('sends the permissions of a group-profile request', () => {
    expect(
      toAccessRequest(
        form({
          type: 'MODIFY_ROLE_PERMISSIONS',
          roleCode: 'TSU',
          currentPermissions: ['A', 'B'],
          permissions: ['B', 'C'],
          justification: ' Package requests ',
        }),
      ),
    ).toMatchObject({
      type: 'MODIFY_ROLE_PERMISSIONS',
      roleCode: 'TSU',
      permissionsAdded: ['C'],
      permissionsRemoved: ['A'],
      justification: 'Package requests',
    });
    expect(
      toAccessRequest(form({ type: 'CREATE_ROLE', roleCode: 'NEW', permissions: ['A'] }))
        .permissionsAdded,
    ).toEqual(['A']);
    expect(toAccessRequest(form({ type: 'DEACTIVATE_ROLE', roleCode: 'OLD' }))).toEqual({
      type: 'DEACTIVATE_ROLE',
      roleCode: 'OLD',
      privilegeLevel: undefined,
      justification: undefined,
      approvers: [],
    });
  });

  it('lists added and removed codes and reads a saved request back', () => {
    expect(roleChanges(['A', 'B'], ['B', 'C'])).toEqual({ added: ['C'], removed: ['A'] });
    const roles: RoleInfo[] = [
      {
        id: 1,
        code: 'TSU',
        name: 'TSU',
        permissions: ['A', 'B'],
        active: true,
        privilegeLevel: 'STANDARD',
      },
    ];
    const saved = {
      id: 7,
      requestNo: 'AR-1',
      type: 'MODIFY_ROLE_PERMISSIONS',
      userType: 'INTERNAL',
      summary: '',
      roleCodes: [],
      status: 'RETURNED',
      requestedBy: 'badmin',
      requestedAt: '2026-09-25T00:00:00Z',
      roleCode: 'TSU',
      permissionsAdded: ['C'],
      permissionsRemoved: ['A'],
      returnedCount: 1,
      details: { unlock: false },
      lifecycle: {
        approvers: [{ sequence: 1, approver: 'uamapprover', decision: 'RETURNED' }],
        riskFlags: [],
        secondApprovalRequired: false,
      },
    } satisfies AccessRequest;
    const back = fromAccessRequest(saved, roles);
    expect(back.permissions).toEqual(['B', 'C']);
    expect(back.approvers).toEqual(['uamapprover']);
    expect(isEditable('RETURNED')).toBe(true);
    expect(isCancellable('APPROVED')).toBe(false);
  });
});
