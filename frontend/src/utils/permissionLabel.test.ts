import { permissionLabel, permissionLabels } from './permissionLabel';

describe('permission names', () => {
  it('reads the module prefix and the acronyms', () => {
    expect(permissionLabel('UAM_ENROLL')).toBe('User access enroll');
    expect(permissionLabel('PKG_TSU_APPROVE')).toBe('Package TSU approve');
    expect(permissionLabel('AUDIT_VIEW')).toBe('Audit view');
    expect(permissionLabel('UAM_SOD_AUTHORIZE')).toBe('User access SOD authorize');
    expect(permissionLabel('')).toBe('');
  });

  it('lists several names in the order of the codes', () => {
    expect(permissionLabels(['REPORT_VIEW', 'AUDIT_VIEW'])).toBe('Audit view, Report view');
  });
});
