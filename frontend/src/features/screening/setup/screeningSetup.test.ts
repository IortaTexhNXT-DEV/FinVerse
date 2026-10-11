import type { ConfigContent, ConfigVersion, WatchlistEntry } from './api';
import {
  ROW_KEY,
  cellText,
  defaultVersion,
  inForce,
  keyed,
  mapTables,
  parseCell,
  rowsOf,
  toCodes,
  unkeyed,
  versionRoles,
  withRows,
} from './configSpec';
import { CONFIG_TYPES, typeSpec } from './configTables';
import { aliasesText, compareValues, entryErrors, parseAliases, requestOf } from './watchlistLogic';

const empty: ConfigContent = {
  matchRules: [],
  riskCategories: [],
  riskRules: [],
  routes: [],
  assignmentRules: [],
  slaRules: [],
  validationRules: [],
};

function version(patch: Partial<ConfigVersion>): ConfigVersion {
  return {
    id: 1,
    companyId: 1,
    type: 'MATCH_CRITERIA',
    versionNo: 1,
    label: 'MATCH_CRITERIA v1',
    status: 'ACTIVE',
    effectiveFrom: '2026-01-01',
    createdBy: 'compoff',
    createdAt: '2026-01-01T00:00:00Z',
    ...patch,
  };
}

describe('configuration editor helpers', () => {
  it('reads and replaces the rows of every table', () => {
    const withMatch = withRows(empty, 'matchRules', [{ algorithm: 'FUZZY' }]);
    expect(rowsOf(withMatch, 'matchRules')).toHaveLength(1);
    const withFields = withRows(empty, 'template.fields', [{ code: 'F1' }]);
    expect(rowsOf(withFields, 'template.fields')).toEqual([{ code: 'F1' }]);
    const withColumns = withRows(empty, 'layout.columns', [{ order: 1 }]);
    expect(withColumns.layout?.format).toBe('CSV');
    expect(rowsOf(withColumns, 'layout.columns')).toHaveLength(1);
    expect(rowsOf(undefined, 'routes')).toEqual([]);
  });

  it('converts cells between text and values', () => {
    expect(toCodes(' name, alias ,,')).toEqual(['NAME', 'ALIAS']);
    expect(cellText(['NAME', 'ALIAS'])).toBe('NAME, ALIAS');
    expect(cellText(null)).toBe('');
    expect(cellText(0.85)).toBe('0.85');
    expect(cellText({ a: 1 })).toBe('{"a":1}');
    expect(parseCell('decimal', '0.92')).toBe(0.92);
    expect(parseCell('number', ' ')).toBeNull();
    expect(parseCell('codes', 'pep')).toEqual(['PEP']);
    expect(parseCell('text', 'x')).toBe('x');
  });

  it('keys rows for editing and strips the keys before saving', () => {
    const content = withRows(empty, 'slaRules', [{ stage: 'INVESTIGATION' }]);
    const k = mapTables(content, keyed);
    expect(rowsOf(k, 'slaRules')[0]?.[ROW_KEY]).toBeDefined();
    expect(rowsOf(mapTables(k, unkeyed), 'slaRules')).toEqual([{ stage: 'INVESTIGATION' }]);
  });

  it('finds the version in force and the version to open first', () => {
    const v1 = version({ id: 1, status: 'SUPERSEDED', effectiveFrom: '2026-01-01' });
    const v2 = version({ id: 2, versionNo: 2, status: 'ACTIVE', effectiveFrom: '2026-06-01' });
    const v3 = version({ id: 3, versionNo: 3, status: 'ACTIVE', effectiveFrom: '2026-12-01' });
    expect(inForce([v1, v2, v3], '2026-07-01')?.id).toBe(2);
    expect(inForce([v1, v2, v3], '2025-12-31')).toBeUndefined();
    const pending = version({ id: 4, versionNo: 4, status: 'PENDING' });
    expect(defaultVersion([v1, v2, pending], '2026-07-01')?.id).toBe(4);
    expect(defaultVersion([v1, v2], '2026-07-01')?.id).toBe(2);
    const kyc = version({ id: 5, type: 'TEMPLATE', scope: 'KYC_REVIEW' });
    expect(defaultVersion([kyc], '2026-07-01', 'EDD')).toBeUndefined();
    expect(defaultVersion([kyc], '2026-07-01', 'KYC_REVIEW')?.id).toBe(5);
  });

  it('never lets the maker decide the own version', () => {
    const all = () => true;
    const pending = version({ status: 'PENDING', submittedBy: 'compdual', createdBy: 'compdual' });
    expect(versionRoles(pending, 'compdual', all)).toEqual({
      editable: false,
      isMaker: true,
      mayDecide: false,
    });
    expect(versionRoles(pending, 'compchk', all).mayDecide).toBe(true);
    expect(versionRoles(version({ status: 'DRAFT' }), 'compoff', all).editable).toBe(true);
    expect(versionRoles(version({ status: 'DRAFT' }), 'compoff', () => false).editable).toBe(false);
  });

  it('describes every configuration type edited on Configuration Versions', () => {
    expect(CONFIG_TYPES.map((t) => t.type)).not.toContain('TEMPLATE');
    expect(typeSpec('RISK_RULES').tables).toHaveLength(2);
    expect(typeSpec(undefined).type).toBe('MATCH_CRITERIA');
    CONFIG_TYPES.forEach((t) =>
      t.tables.forEach((table) => expect(table.fields.length).toBeGreaterThan(0)),
    );
  });
});

describe('watchlist helpers', () => {
  const entry: WatchlistEntry = {
    id: 7,
    sourceCode: 'INTERNAL',
    externalRef: 'INT-000007',
    listType: 'INTERNAL',
    entityType: 'INDIVIDUAL',
    primaryName: 'Pedro Invented Lagman',
    status: 'ACTIVE',
    entryVersion: 1,
  };

  it('converts aliases', () => {
    const aliases = parseAliases(' A1 ;; A2 ');
    expect(aliases).toEqual([
      { name: 'A1', type: 'AKA' },
      { name: 'A2', type: 'AKA' },
    ]);
    expect(aliasesText(aliases)).toBe('A1; A2');
    expect(aliasesText(undefined)).toBe('');
  });

  it('compares before and after values for the checker', () => {
    const before = { ...requestOf(entry), aliases: parseAliases('Old') };
    const after = {
      ...before,
      primaryName: 'Pedro Invented Lagman Jr',
      aliases: parseAliases('Old'),
    };
    const lines = compareValues(before, after);
    expect(lines.find((l) => l.field === 'Primary Name')).toEqual({
      field: 'Primary Name',
      before: 'Pedro Invented Lagman',
      after: 'Pedro Invented Lagman Jr',
      changed: true,
    });
    expect(lines.find((l) => l.field === 'Aliases')?.changed).toBe(false);
    expect(compareValues(null, after).every((l) => l.before === '')).toBe(true);
  });

  it('checks a manual entry with the FRS messages', () => {
    const blank = requestOf();
    expect(blank.sourceCode).toBe('INTERNAL');
    expect(entryErrors(blank)).toEqual({
      primaryName: 'Enter the name of the listed person or entity',
      remarks: 'Enter the reason for the change',
    });
    expect(entryErrors({ ...blank, listType: '', primaryName: 'X', remarks: 'r' })).toEqual({
      listType: 'Select the list type',
    });
    expect(requestOf(entry).primaryName).toBe('Pedro Invented Lagman');
  });
});
