import { describe, expect, it } from 'vitest';
import type { CatalogueDataset, ConfigImport, ImportDataset } from '@/api/configPromotion';
import {
  byGroup,
  changeCount,
  changeLabel,
  defaultSelection,
  driftLabel,
  driftTotals,
  fileSize,
  findings,
  importActions,
  importStatus,
  itemRows,
  packageKindLabel,
  reconciliationLabel,
  shortHash,
} from './promotion';

const base: ConfigImport = {
  id: 1,
  importNo: 'CFI-2026-000001',
  packageId: 2,
  packageNo: 'CFP-2026-000002',
  sourceEnvironment: 'SIT',
  status: 'CHECKED',
  production: false,
  pipeline: false,
  compatible: true,
  blockerCount: 0,
  warningCount: 0,
  added: 1,
  changed: 2,
  unchanged: 3,
  onlyInTarget: 4,
  preparedBy: 'admin',
  preparedAt: '2026-10-08T01:00:00Z',
  options: { datasets: [], deactivate: [], includeUsers: false },
  messages: { refusals: [], notes: [], blockers: [], warnings: [] },
};

const preparer = { username: 'Admin', mayPrepare: true, mayApprove: false };
const approver = { username: 'cfgapprover', mayPrepare: false, mayApprove: true };

function dataset(code: string, extra: Partial<CatalogueDataset> = {}): CatalogueDataset {
  return {
    code,
    name: code,
    group: 'G',
    module: 'm',
    key: [],
    dependsOn: [],
    collection: false,
    optional: false,
    users: false,
    environmentFields: [],
    environmentRows: false,
    items: 0,
    ...extra,
  };
}

describe('import actions', () => {
  it('lets the preparer submit a clean dry run, check again and withdraw', () => {
    expect(importActions(base, preparer)).toEqual(['submit', 'check', 'withdraw']);
  });

  it('does not offer the submission while the dry run reports blockers', () => {
    expect(importActions({ ...base, blockerCount: 1 }, preparer)).toEqual(['check', 'withdraw']);
    expect(importActions({ ...base, compatible: false }, preparer)).not.toContain('submit');
  });

  it('lets another user approve or reject, never the preparer', () => {
    const submitted = { ...base, status: 'SUBMITTED' as const };
    expect(importActions(submitted, approver)).toEqual(['approve', 'reject']);
    expect(importActions(submitted, { ...preparer, mayApprove: true })).toEqual(['withdraw']);
  });

  it('offers the rollback of an applied import with a snapshot', () => {
    const applied = { ...base, status: 'APPLIED' as const, snapshotPackageId: 9 };
    expect(importActions(applied, preparer)).toEqual(['rollback']);
    expect(importActions({ ...applied, snapshotPackageId: null }, preparer)).toEqual([]);
  });
});

describe('wording', () => {
  it('names statuses, kinds and differences in business words', () => {
    expect(importStatus('SUBMITTED').label).toBe('Waiting for Approval');
    expect(importStatus('CANCELLED').label).toBe('Withdrawn');
    expect(packageKindLabel('SNAPSHOT')).toBe('Snapshot before an import');
    expect(changeLabel('ONLY_IN_TARGET')).toBe('Only in this environment');
    expect(driftLabel('ADDED')).toBe('New since the baseline');
    expect(driftLabel('ONLY_IN_TARGET')).toBe('No longer present');
    expect(driftLabel('CHANGED')).toBe('Changed');
  });

  it('shortens checksums and sizes', () => {
    expect(shortHash('0123456789abcdef0123')).toBe('0123456789ab');
    expect(shortHash(null)).toBe('—');
    expect(fileSize(512)).toBe('512 bytes');
    expect(fileSize(2048)).toBe('2.0 KB');
    expect(fileSize(3 * 1024 * 1024)).toBe('3.0 MB');
  });

  it('tells whether a dataset reconciled', () => {
    const line = { reconciled: true } as ImportDataset;
    expect(reconciliationLabel(line).label).toBe('Reconciled');
    expect(reconciliationLabel({ ...line, reconciled: false }).tone).toBe('danger');
    expect(reconciliationLabel({ ...line, reconciled: null }).label).toBe('Not applied');
    expect(changeCount(base)).toBe(7);
  });
});

describe('catalogue selection', () => {
  const all = [
    dataset('A'),
    dataset('B', { optional: true }),
    dataset('C', { users: true }),
    dataset('D', { collection: true }),
  ];

  it('selects every dataset but the optional ones, users only when asked', () => {
    expect(defaultSelection(all, false)).toEqual(['A']);
    expect(defaultSelection(all, true)).toEqual(['A', 'C']);
  });

  it('groups the datasets in catalogue order and leaves empty groups out', () => {
    const groups = byGroup(all, [
      { code: 'X', name: 'Empty' },
      { code: 'G', name: 'Group' },
    ]);
    expect(groups).toHaveLength(1);
    expect(groups[0]?.datasets).toHaveLength(4);
  });
});

describe('difference lines', () => {
  it('writes one line per field and names the item on its first line', () => {
    const rows = itemRows([
      {
        type: 'CHANGED',
        key: 'FVI / HO',
        fields: [
          { column: 'name', label: 'Name', from: 'Main', to: 'Head Office' },
          { column: 'region', label: 'Region', from: null, to: 'NCR' },
        ],
      },
      { type: 'ONLY_IN_TARGET', key: 'FVI / BR2', fields: [] },
    ]);
    expect(rows).toHaveLength(3);
    expect(rows[0]).toMatchObject({
      first: true,
      key: 'FVI / HO',
      field: 'Name',
      to: 'Head Office',
    });
    expect(rows[1]).toMatchObject({ first: false, from: '—', to: 'NCR' });
    expect(rows[2]).toMatchObject({ first: true, field: '—' });
  });
});

describe('findings', () => {
  it('lists refusals and blockers before warnings and notes', () => {
    const rows = findings({
      refusals: ['Other fields'],
      notes: ['Other platform version'],
      blockers: [
        { dataset: 'CAT_PRODUCT', datasetName: 'Products', key: 'MTR30', message: 'Used' },
      ],
      warnings: [{ message: 'Kept' }],
    });
    expect(rows.map((r) => r.blocking)).toEqual([true, true, false, false]);
    expect(rows[1]).toMatchObject({ dataset: 'Products', item: 'MTR30' });
    expect(rows[2]).toMatchObject({ dataset: 'Package', item: '—' });
  });
});

describe('drift totals', () => {
  it('counts the drifted datasets and their items', () => {
    const d = { code: 'A', name: 'A', group: 'G', comparable: true, items: [] };
    expect(
      driftTotals([
        { ...d, added: 1, changed: 2, removed: 0 },
        { ...d, code: 'B', added: 0, changed: 0, removed: 0 },
        { ...d, code: 'C', added: 0, changed: 0, removed: 3 },
      ]),
    ).toEqual({ datasets: 2, added: 1, changed: 2, removed: 3 });
  });
});
