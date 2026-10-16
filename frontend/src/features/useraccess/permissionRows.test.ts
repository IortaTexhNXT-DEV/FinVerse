import { describe, expect, it } from 'vitest';
import { areaOf, matchesPermission, sortPermissions } from './permissionRows';

describe('permission lists', () => {
  const rows = [
    { permission: 'REPORT_VIEW', area: 'REPORTS', groupProfiles: ['Account Officer'] },
    { permission: 'CLIENT_VIEW', area: 'CLIENTS', groupProfiles: ['Account Officer'] },
    { permission: 'CLIENT_CREATE', area: 'CLIENTS', groupProfiles: ['Sales Head'] },
    { permission: 'AUDIT_VIEW', area: null, groupProfiles: ['Auditor'] },
  ];

  it('sorts by area, then by permission name; an unclassified permission is General', () => {
    expect(sortPermissions(rows).map((r) => r.permission)).toEqual([
      'CLIENT_CREATE',
      'CLIENT_VIEW',
      'AUDIT_VIEW',
      'REPORT_VIEW',
    ]);
    expect(areaOf(rows[3] ?? { permission: '' })).toBe('General');
  });

  it('searches the name, the area and the granting group profile', () => {
    expect(rows.filter((r) => matchesPermission(r, 'sales head')).map((r) => r.permission)).toEqual(
      ['CLIENT_CREATE'],
    );
    expect(rows.filter((r) => matchesPermission(r, 'clients'))).toHaveLength(2);
    expect(rows.filter((r) => matchesPermission(r, ''))).toHaveLength(4);
  });
});
