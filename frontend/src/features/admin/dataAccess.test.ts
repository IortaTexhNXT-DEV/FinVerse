import { ALL_COMPANIES, parseScopeText, scopeOfView, scopeText } from '@/api/dataScope';
import type { CompanyUnit, DataScopeValue } from '@/api/dataScope';
import {
  accessNodes,
  allBranchesGranted,
  branchGranted,
  companyGranted,
  describeScope,
  openedCompanies,
  scopeError,
  setAllBranches,
  setAllCompanies,
  toggleBranch,
  toggleCompany,
} from './dataAccess';

const UNITS: CompanyUnit[] = [
  {
    id: 1,
    code: 'HO',
    name: 'Head Company',
    branches: [
      { id: 10, code: 'MKT', name: 'Makati' },
      { id: 11, code: 'CEB', name: 'Cebu' },
    ],
  },
  { id: 2, code: 'SUB', name: 'Subsidiary', branches: [{ id: 20, code: 'HO2', name: 'Main' }] },
];

const SOME: DataScopeValue = {
  allCompanies: false,
  companies: [
    { companyId: 2, allBranches: true, branchIds: [] },
    { companyId: 1, allBranches: false, branchIds: [11, 10] },
  ],
};

describe('data scope text form', () => {
  it('writes and reads the text stored on an access request', () => {
    expect(scopeText(ALL_COMPANIES)).toBe('ALL');
    expect(scopeText(SOME)).toBe('1:10,11;2:*');
    expect(parseScopeText('ALL')).toEqual(ALL_COMPANIES);
    expect(parseScopeText('1:10,11;2:*')).toEqual({
      allCompanies: false,
      companies: [
        { companyId: 1, allBranches: false, branchIds: [10, 11] },
        { companyId: 2, allBranches: true, branchIds: [] },
      ],
    });
    expect(parseScopeText('')).toEqual({ allCompanies: false, companies: [] });
    expect(parseScopeText('3:')).toEqual({
      allCompanies: false,
      companies: [{ companyId: 3, allBranches: false, branchIds: [] }],
    });
  });

  it('refuses a malformed text and an absent one', () => {
    expect(parseScopeText(undefined)).toBeUndefined();
    expect(parseScopeText(null)).toBeUndefined();
    expect(parseScopeText('x:*')).toBeUndefined();
    expect(parseScopeText(':*')).toBeUndefined();
    expect(parseScopeText('1:a')).toBeUndefined();
  });

  it('turns the server view into an editable value', () => {
    expect(
      scopeOfView({
        allCompanies: false,
        description: 'HO: MKT',
        companies: [
          {
            companyId: 1,
            code: 'HO',
            allBranches: false,
            branches: [{ branchId: 10, code: 'MKT' }],
          },
        ],
      }),
    ).toEqual({
      allCompanies: false,
      companies: [{ companyId: 1, allBranches: false, branchIds: [10] }],
    });
  });
});

describe('data access tree', () => {
  it('lists every company with its branches and opens the companies with a branch list', () => {
    const nodes = accessNodes(UNITS, SOME);
    expect(nodes.map((n) => n.key)).toEqual(['c1', 'c2']);
    expect(nodes[0]?.children?.map((n) => n.key)).toEqual(['b10', 'b11']);
    expect([...openedCompanies(SOME)]).toEqual(['c1']);
  });

  it('shows a granted company the viewer cannot see by name', () => {
    const scope: DataScopeValue = {
      allCompanies: false,
      companies: [{ companyId: 9, allBranches: false, branchIds: [90] }],
    };
    const nodes = accessNodes(UNITS, scope);
    expect(nodes.at(-1)?.row).toMatchObject({ kind: 'company', company: { name: 'Company 9' } });
    expect(describeScope(scope, UNITS)).toBe('Company 9: Branch 90');
  });

  it('answers what is granted', () => {
    expect(companyGranted(SOME, 1)).toBe(true);
    expect(companyGranted(SOME, 3)).toBe(false);
    expect(allBranchesGranted(SOME, 2)).toBe(true);
    expect(allBranchesGranted(SOME, 1)).toBe(false);
    expect(branchGranted(SOME, 1, 10)).toBe(true);
    expect(branchGranted(SOME, 2, 20)).toBe(true);
    expect(branchGranted(SOME, 3, 30)).toBe(false);
    expect(branchGranted(ALL_COMPANIES, 3, 30)).toBe(true);
  });
});

describe('data access changes', () => {
  it('switches all companies on and off', () => {
    expect(setAllCompanies(true)).toEqual(ALL_COMPANIES);
    expect(setAllCompanies(false)).toEqual({ allCompanies: false, companies: [] });
  });

  it('adds a company with all its branches and removes it', () => {
    const added = toggleCompany(setAllCompanies(false), 1);
    expect(added.companies).toEqual([{ companyId: 1, allBranches: true, branchIds: [] }]);
    expect(toggleCompany(added, 1).companies).toEqual([]);
  });

  it('narrows a company to some branches', () => {
    const company = toggleCompany(setAllCompanies(false), 1);
    const listed = setAllBranches(company, 1, false);
    expect(scopeError(listed, UNITS)).toBe(
      'Select at least one branch of Head Company, or give access to all its branches.',
    );
    const one = toggleBranch(listed, 1, 11);
    expect(one.companies[0]?.branchIds).toEqual([11]);
    expect(toggleBranch(one, 1, 11).companies[0]?.branchIds).toEqual([]);
    expect(toggleBranch(company, 1, 11)).toBe(company);
    expect(toggleBranch(setAllCompanies(false), 2, 20).companies).toEqual([
      { companyId: 2, allBranches: false, branchIds: [20] },
    ]);
  });

  it('says what is missing before a save', () => {
    expect(scopeError(ALL_COMPANIES, UNITS)).toBeUndefined();
    expect(scopeError(setAllCompanies(false), UNITS)).toBe(
      'Select at least one company, or give access to all companies.',
    );
    expect(scopeError(SOME, UNITS)).toBeUndefined();
    expect(
      scopeError(
        { allCompanies: false, companies: [{ companyId: 7, allBranches: false, branchIds: [] }] },
        [],
      ),
    ).toBe('Select at least one branch of a company, or give access to all its branches.');
  });

  it('describes the scope in business words', () => {
    expect(describeScope(ALL_COMPANIES, UNITS)).toBe('All companies');
    expect(describeScope(setAllCompanies(false), UNITS)).toBe('No company');
    expect(describeScope(SOME, UNITS)).toBe('SUB (all branches); HO: CEB, MKT');
  });
});
