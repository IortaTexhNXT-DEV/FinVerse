import type { PmDrillRow } from '@/api/pmWorkspace';
import { dashboardExportParams, matrixExportParams } from '@/api/pmWorkspace';
import { KPI_COUNTS, agingText, drillLink, packagedFlag } from './pmDashboard';

const row = (patch: Partial<PmDrillRow>): PmDrillRow => ({
  kind: 'PACKAGE',
  id: 7,
  requestNo: 'PKR-2026-000007',
  requestType: 'New Package',
  lineCode: 'MOTOR',
  productLine: 'Motor',
  requestedBy: 'ao',
  assignee: 'tsu',
  status: 'For TSU Review',
  submittedAt: '2026-10-01T02:00:00Z',
  agingDays: 3,
  expiryDate: null,
  ...patch,
});

describe('Product Maintenance dashboard', () => {
  it('maps every KPI of BDOI to its count', () => {
    expect(Object.keys(KPI_COUNTS)).toEqual([
      'INCOMING',
      'IN_PROGRESS',
      'FOR_APPROVAL',
      'EXPIRING',
      'ISSUED',
      'DEACTIVATION',
    ]);
  });

  it('opens the record behind a drill-down row', () => {
    expect(drillLink(row({}))).toBe('/product-maintenance/requests/7');
    expect(drillLink(row({ kind: 'QUOTATION', id: 3 }))).toBe('/proposals/3');
    expect(drillLink(row({ kind: 'DEACTIVATION', id: 9 }))).toBe(
      '/product-maintenance/deactivations?open=9',
    );
    expect(drillLink(row({ kind: 'EXPIRY', id: null, requestNo: 'PAR25' }))).toBe(
      '/catalog/products/PAR25',
    );
  });

  it('shows the aging in days', () => {
    expect(agingText(row({ agingDays: 1 }))).toBe('1 day');
    expect(agingText(row({ agingDays: 12 }))).toBe('12 days');
    expect(agingText(row({ agingDays: null }))).toBe('');
  });

  it('turns the package type into the filter flag', () => {
    expect(packagedFlag('PACKAGE')).toBe(true);
    expect(packagedFlag('NON_PACKAGE')).toBe(false);
    expect(packagedFlag('')).toBeUndefined();
  });

  it('exports with the filters on screen', () => {
    expect(
      dashboardExportParams(3, { from: '2026-10-01', to: '2026-10-09', packaged: false }, 'ISSUED'),
    ).toEqual({
      companyId: '3',
      kpi: 'ISSUED',
      from: '2026-10-01',
      to: '2026-10-09',
      packageType: 'NON_PACKAGE',
    });
    expect(matrixExportParams(3, { tab: 'EXPIRING', text: 'motor' })).toEqual({
      companyId: '3',
      tab: 'EXPIRING',
      text: 'motor',
      packageType: 'ALL',
    });
  });
});
