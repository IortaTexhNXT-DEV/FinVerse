import { ALL_COMPANIES } from '@/api/dataScope';
import type { BranchUnit, CompanyGrant, CompanyUnit, DataScopeValue } from '@/api/dataScope';
import type { TreeNode } from '@/components/ui/treeRows';

/** One row of the Data access tree: a company, or a branch under its company. */
export type AccessRow =
  | { kind: 'company'; company: CompanyUnit }
  | { kind: 'branch'; company: CompanyUnit; branch: BranchUnit };

/** A company of the scope that the viewer cannot see by name (outside his own data access). */
function unknownCompany(grant: CompanyGrant): CompanyUnit {
  return {
    id: grant.companyId,
    code: '',
    name: `Company ${String(grant.companyId)}`,
    branches: grant.branchIds.map((id) => ({ id, code: '', name: `Branch ${String(id)}` })),
  };
}

/** The companies shown: those the viewer may grant, then any other company of the scope. */
export function shownCompanies(
  units: readonly CompanyUnit[],
  scope: DataScopeValue,
): CompanyUnit[] {
  const known = new Set(units.map((u) => u.id));
  return [...units, ...scope.companies.filter((c) => !known.has(c.companyId)).map(unknownCompany)];
}

/** The tree of companies and their branches. */
export function accessNodes(
  units: readonly CompanyUnit[],
  scope: DataScopeValue,
): TreeNode<AccessRow>[] {
  return shownCompanies(units, scope).map((company) => ({
    key: `c${String(company.id)}`,
    row: { kind: 'company', company },
    children: company.branches.map((branch) => ({
      key: `b${String(branch.id)}`,
      row: { kind: 'branch', company, branch },
    })),
  }));
}

/** Keys of the companies granted with a branch list (opened so the branches show). */
export function openedCompanies(scope: DataScopeValue): Set<string> {
  return new Set(
    scope.companies.filter((c) => !c.allBranches).map((c) => `c${String(c.companyId)}`),
  );
}

function grantOf(scope: DataScopeValue, companyId: number): CompanyGrant | undefined {
  return scope.companies.find((c) => c.companyId === companyId);
}

/** Whether the company is in the scope. */
export function companyGranted(scope: DataScopeValue, companyId: number): boolean {
  return scope.allCompanies || grantOf(scope, companyId) !== undefined;
}

/** Whether all branches of the company are in the scope. */
export function allBranchesGranted(scope: DataScopeValue, companyId: number): boolean {
  return scope.allCompanies || grantOf(scope, companyId)?.allBranches === true;
}

/** Whether the branch is in the scope. */
export function branchGranted(scope: DataScopeValue, companyId: number, branchId: number): boolean {
  const grant = grantOf(scope, companyId);
  return (
    scope.allCompanies ||
    grant?.allBranches === true ||
    grant?.branchIds.includes(branchId) === true
  );
}

/** "All companies" on or off (off starts with no company: the administrator picks them). */
export function setAllCompanies(all: boolean): DataScopeValue {
  return all ? ALL_COMPANIES : { allCompanies: false, companies: [] };
}

function replaceGrant(
  scope: DataScopeValue,
  companyId: number,
  grant: CompanyGrant | undefined,
): DataScopeValue {
  const others = scope.companies.filter((c) => c.companyId !== companyId);
  return { allCompanies: false, companies: grant === undefined ? others : [...others, grant] };
}

/** Adds the company with all its branches, or removes it. */
export function toggleCompany(scope: DataScopeValue, companyId: number): DataScopeValue {
  return replaceGrant(
    scope,
    companyId,
    grantOf(scope, companyId) === undefined
      ? { companyId, allBranches: true, branchIds: [] }
      : undefined,
  );
}

/** "All branches" of a granted company on or off (off keeps the branches to be picked). */
export function setAllBranches(
  scope: DataScopeValue,
  companyId: number,
  all: boolean,
): DataScopeValue {
  return replaceGrant(scope, companyId, { companyId, allBranches: all, branchIds: [] });
}

/** Adds or removes one branch; a company not yet granted is added with that branch. */
export function toggleBranch(
  scope: DataScopeValue,
  companyId: number,
  branchId: number,
): DataScopeValue {
  const grant = grantOf(scope, companyId);
  if (grant?.allBranches) {
    return scope;
  }
  const current = grant?.branchIds ?? [];
  const branchIds = current.includes(branchId)
    ? current.filter((b) => b !== branchId)
    : [...current, branchId];
  return replaceGrant(scope, companyId, { companyId, allBranches: false, branchIds });
}

/** What is missing before the scope can be saved, as a business message; undefined when none. */
export function scopeError(
  scope: DataScopeValue,
  units: readonly CompanyUnit[],
): string | undefined {
  if (scope.allCompanies) {
    return undefined;
  }
  if (scope.companies.length === 0) {
    return 'Select at least one company, or give access to all companies.';
  }
  const empty = scope.companies.find((c) => !c.allBranches && c.branchIds.length === 0);
  if (empty !== undefined) {
    const name = units.find((u) => u.id === empty.companyId)?.name ?? 'a company';
    return `Select at least one branch of ${name}, or give access to all its branches.`;
  }
  return undefined;
}

function labelOf(unit: BranchUnit | CompanyUnit | undefined, id: number): string {
  if (unit === undefined) {
    return String(id);
  }
  return unit.code === '' ? unit.name : unit.code;
}

/** The scope in words: "All companies", or each company with "all branches" or its branches. */
export function describeScope(scope: DataScopeValue, units: readonly CompanyUnit[]): string {
  if (scope.allCompanies) {
    return 'All companies';
  }
  if (scope.companies.length === 0) {
    return 'No company';
  }
  const shown = shownCompanies(units, scope);
  return scope.companies
    .map((c) => {
      const company = shown.find((u) => u.id === c.companyId);
      const name = labelOf(company, c.companyId);
      if (c.allBranches) {
        return `${name} (all branches)`;
      }
      const branches = c.branchIds.map((b) =>
        labelOf(
          company?.branches.find((x) => x.id === b),
          b,
        ),
      );
      return `${name}: ${branches.join(', ')}`;
    })
    .join('; ');
}
