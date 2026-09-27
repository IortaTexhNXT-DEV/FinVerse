import { actionText, areaLabel, groupByArea, UNCLASSIFIED } from './accessMatrix';

describe('access matrix helpers (PMADD05)', () => {
  it('labels areas', () => {
    expect(areaLabel('PACKAGE_REQUEST')).toBe('Package Requests');
    expect(areaLabel('BULK_PROCESSING')).toBe('Bulk Processing');
    expect(areaLabel(UNCLASSIFIED)).toBe('Other (not classified)');
  });

  it('groups permissions by area with unclassified ones last', () => {
    expect(
      groupByArea([
        { permission: 'JOURNAL_CREATE', roles: [], actions: [] },
        { permission: 'PKG_REQUEST', roles: [], area: 'PACKAGE_REQUEST', actions: ['CREATE'] },
        { permission: 'CLIENT_VIEW', roles: [], area: 'CLIENTS', actions: ['VIEW'] },
        { permission: 'PKG_QS_APPROVE', roles: [], area: 'PACKAGE_REQUEST', actions: ['APPROVE'] },
      ]),
    ).toEqual([
      { area: 'PACKAGE_REQUEST', permissions: ['PKG_REQUEST', 'PKG_QS_APPROVE'] },
      { area: 'CLIENTS', permissions: ['CLIENT_VIEW'] },
      { area: UNCLASSIFIED, permissions: ['JOURNAL_CREATE'] },
    ]);
  });

  it('describes action classes', () => {
    expect(actionText(['CREATE', 'AMEND'])).toBe('Create, Amend');
    expect(actionText([])).toBe('');
  });
});
