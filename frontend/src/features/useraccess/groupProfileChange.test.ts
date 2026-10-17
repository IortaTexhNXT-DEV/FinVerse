import { describe, expect, it } from 'vitest';
import type { RoleInfo } from '@/api/nbadmin';
import { hasErrors, isOpen, newChange, toInput, validateChange } from './groupProfileChange';

const role: RoleInfo = {
  id: 1,
  code: 'MKT_AO',
  name: 'Account Officer',
  permissions: ['CLIENT_VIEW', 'QUOTE_VIEW'],
  active: true,
  privilegeLevel: 'STANDARD',
};

describe('group-profile change', () => {
  it('asks for code, name, permissions and reason of a new profile', () => {
    const f = newChange('CREATE_ROLE');
    expect(validateChange(f, false).roleCode).toBeDefined();
    const errors = validateChange({ ...f, roleCode: 'NEW_ONE' }, true);
    expect(errors.roleCode).toBeUndefined();
    expect(errors.roleName).toBeDefined();
    expect(errors.permissions).toBeDefined();
    expect(errors.justification).toBeDefined();
    expect(hasErrors(errors)).toBe(true);
  });

  it('sends the permissions added and removed of a change', () => {
    const f = { ...newChange('MODIFY_ROLE_PERMISSIONS', role), justification: 'Scope' };
    const changed = { ...f, permissions: ['CLIENT_VIEW', 'REPORT_VIEW'] };
    expect(hasErrors(validateChange(changed, true))).toBe(false);
    expect(toInput(changed)).toMatchObject({
      type: 'MODIFY_ROLE_PERMISSIONS',
      roleCode: 'MKT_AO',
      permissionsAdded: ['REPORT_VIEW'],
      permissionsRemoved: ['QUOTE_VIEW'],
    });
  });

  it('sends only the profile and the reason of a deactivation', () => {
    const f = { ...newChange('DEACTIVATE_ROLE', role), justification: 'Merged' };
    expect(toInput(f)).toEqual({
      type: 'DEACTIVATE_ROLE',
      roleCode: 'MKT_AO',
      justification: 'Merged',
    });
  });

  it('knows the open statuses', () => {
    expect(isOpen('PENDING')).toBe(true);
    expect(isOpen('IMPLEMENTED')).toBe(false);
  });
});
