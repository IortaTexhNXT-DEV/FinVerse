import { api } from './client';

/** One branch of a company that a data scope can grant. */
export interface BranchUnit {
  id: number;
  code: string;
  name: string;
}

/** One company with its branches that a data scope can grant. */
export interface CompanyUnit {
  id: number;
  code: string;
  name: string;
  branches: BranchUnit[];
}

/** One granted company: all its branches, or some. */
export interface CompanyGrant {
  companyId: number;
  allBranches: boolean;
  branchIds: number[];
}

/** The companies and branches a user may act for ("All companies" by default). */
export interface DataScopeValue {
  allCompanies: boolean;
  companies: CompanyGrant[];
}

/** The data scope of a user as the server returns it, with names. */
export interface DataScopeView {
  allCompanies: boolean;
  companies: {
    companyId: number;
    code?: string;
    name?: string;
    allBranches: boolean;
    branches: { branchId: number; code?: string; name?: string }[];
  }[];
  description: string;
}

export const ALL_COMPANIES: DataScopeValue = { allCompanies: true, companies: [] };

export const dataScopeApi = {
  /** Companies and branches the signed-in administrator may grant (his own data access). */
  units: () => api.get<CompanyUnit[]>('/admin/data-scope/units'),
  ofUser: (id: number) => api.get<DataScopeView>(`/admin/users/${id}/data-scope`),
  replace: (id: number, scope: DataScopeValue) =>
    api.put<DataScopeView>(`/admin/users/${id}/data-scope`, scope),
};

/** The editable value of a scope returned by the server. */
export function scopeOfView(view: DataScopeView): DataScopeValue {
  return {
    allCompanies: view.allCompanies,
    companies: view.companies.map((c) => ({
      companyId: c.companyId,
      allBranches: c.allBranches,
      branchIds: c.branches.map((b) => b.branchId),
    })),
  };
}

/**
 * The text form stored on an access request: "ALL", or "12:*;14:3,5" (company 12 with all its
 * branches, company 14 with branches 3 and 5).
 */
export function scopeText(scope: DataScopeValue): string {
  if (scope.allCompanies) {
    return 'ALL';
  }
  return [...scope.companies]
    .sort((a, b) => a.companyId - b.companyId)
    .map(
      (c) =>
        `${String(c.companyId)}:${
          c.allBranches
            ? '*'
            : [...c.branchIds]
                .sort((a, b) => a - b)
                .map(String)
                .join(',')
        }`,
    )
    .join(';');
}

/** One "company:branches" entry of the text form; undefined when malformed. */
function parseCompany(part: string): CompanyGrant | undefined {
  const [company = '', branches = ''] = part.split(':');
  const companyId = Number(company);
  if (company.trim() === '' || !Number.isInteger(companyId)) {
    return undefined;
  }
  const all = branches.trim() === '*';
  const branchIds = all || branches.trim() === '' ? [] : branches.split(',').map(Number);
  return branchIds.every((b) => Number.isInteger(b))
    ? { companyId, allBranches: all, branchIds }
    : undefined;
}

/** Reads the text form; undefined for an absent or malformed text. */
export function parseScopeText(text: string | undefined | null): DataScopeValue | undefined {
  if (text === undefined || text === null) {
    return undefined;
  }
  const trimmed = text.trim();
  if (trimmed === 'ALL') {
    return ALL_COMPANIES;
  }
  const parsed = trimmed === '' ? [] : trimmed.split(';').map(parseCompany);
  const companies = parsed.filter((c): c is CompanyGrant => c !== undefined);
  return companies.length === parsed.length ? { allCompanies: false, companies } : undefined;
}
