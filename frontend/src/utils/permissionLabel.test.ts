import { permissionDescription, permissionLabel, permissionLabels } from './permissionLabel';

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

  it('describes what a permission allows in plain words, without the code', () => {
    expect(permissionDescription('CLIENT_VIEW')).toBe('Open and search client records');
    expect(permissionDescription('PKG_TSU_APPROVE')).toBe(
      'Approve package TSU items submitted by others',
    );
    expect(permissionDescription('ACCOUNT_MAINTAIN')).toBe('Create and change account records');
    expect(permissionDescription('UAM_ENROLL')).toBe('Request access for new users');
    expect(permissionDescription('REPORT_FINANCIAL')).toBe('Use the report financial function');
    expect(permissionDescription('')).toBe('');
  });
});
