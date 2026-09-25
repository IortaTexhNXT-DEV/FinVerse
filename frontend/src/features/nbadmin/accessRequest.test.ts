import type { UserAccess } from '@/api/nbadmin';
import {
  EMPTY_ACCESS_REQUEST,
  roleChanges,
  toAccessRequest,
  validateAccessRequest,
} from './accessRequest';

const USERS: UserAccess[] = [
  { username: 'aileen', fullName: 'Aileen', enabled: true, roleCodes: ['MKT_AO'] },
  { username: 'old', fullName: 'Old User', enabled: false, roleCodes: [] },
];

describe('access request validation', () => {
  it('needs a new user name, full name, roles and justification to create a user', () => {
    expect(validateAccessRequest(EMPTY_ACCESS_REQUEST, USERS)).toEqual({
      username: 'Use 3 to 50 letters, digits, dots, dashes or underscores',
      fullName: 'Enter the full name',
      roleCodes: 'Select at least one role',
      justification: 'Explain why the access is needed',
    });
    expect(
      validateAccessRequest({ ...EMPTY_ACCESS_REQUEST, username: 'AILEEN', fullName: 'X' }, USERS)
        .username,
    ).toBe('This user already exists');
    expect(
      validateAccessRequest(
        {
          ...EMPTY_ACCESS_REQUEST,
          username: 'new.user',
          fullName: 'New User',
          roleCodes: ['TSU'],
          justification: 'Joined TSU',
        },
        USERS,
      ),
    ).toEqual({});
  });

  it('needs an existing user in the right state for other requests', () => {
    const base = { ...EMPTY_ACCESS_REQUEST, justification: 'x' };
    expect(
      validateAccessRequest({ ...base, type: 'MODIFY_ROLES', username: 'nobody' }, USERS),
    ).toEqual({
      username: 'Select an existing user',
      roleCodes: 'Select at least one role',
    });
    expect(
      validateAccessRequest({ ...base, type: 'DISABLE_USER', username: 'old' }, USERS).username,
    ).toBe('This user is already disabled');
    expect(
      validateAccessRequest({ ...base, type: 'ENABLE_USER', username: 'aileen' }, USERS).username,
    ).toBe('This user is already enabled');
    expect(validateAccessRequest({ ...base, type: 'ENABLE_USER', username: 'old' }, USERS)).toEqual(
      {},
    );
  });
});

describe('access request mapping', () => {
  it('sends only the fields of the request type', () => {
    const form = {
      ...EMPTY_ACCESS_REQUEST,
      type: 'DISABLE_USER' as const,
      username: ' aileen ',
      fullName: 'ignored',
      roleCodes: ['TSU'],
      justification: ' Left ',
    };
    expect(toAccessRequest(form)).toEqual({
      type: 'DISABLE_USER',
      username: 'aileen',
      fullName: undefined,
      email: undefined,
      roleCodes: undefined,
      homeBranchId: undefined,
      justification: 'Left',
    });
    const create = toAccessRequest({
      ...EMPTY_ACCESS_REQUEST,
      username: 'n',
      fullName: 'N',
      email: 'n@x.ph',
      roleCodes: ['TSU'],
      homeBranchId: '3',
      justification: 'j',
    });
    expect(create.homeBranchId).toBe(3);
    expect(create.email).toBe('n@x.ph');
  });

  it('lists added and removed roles', () => {
    expect(roleChanges(['A', 'B'], ['B', 'C'])).toEqual({ added: ['C'], removed: ['A'] });
  });
});

describe('role-permission change requests (PMADD05)', () => {
  const base = {
    ...EMPTY_ACCESS_REQUEST,
    type: 'MODIFY_ROLE_PERMISSIONS' as const,
    justification: ' Package requests ',
  };

  it('needs a role, a change and a justification', () => {
    expect(validateAccessRequest({ ...base, justification: '' }, USERS)).toEqual({
      roleCode: 'Select the role to change',
      justification: 'Explain why the change is needed',
    });
    expect(
      validateAccessRequest(
        { ...base, roleCode: 'TSU', currentPermissions: ['A'], permissions: ['A'] },
        USERS,
      ),
    ).toEqual({ permissions: 'Grant or withdraw at least one permission' });
    expect(
      validateAccessRequest(
        { ...base, roleCode: 'TSU', currentPermissions: ['A'], permissions: ['B'] },
        USERS,
      ),
    ).toEqual({});
  });

  it('sends the role with the permissions added and removed', () => {
    expect(
      toAccessRequest({
        ...base,
        username: 'ignored',
        roleCode: 'TSU',
        currentPermissions: ['A', 'B'],
        permissions: ['B', 'C'],
      }),
    ).toEqual({
      type: 'MODIFY_ROLE_PERMISSIONS',
      roleCode: 'TSU',
      permissionsAdded: ['C'],
      permissionsRemoved: ['A'],
      justification: 'Package requests',
    });
  });
});
