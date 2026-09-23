import type { Branch } from '@/api/types';
import { defaultBranchId } from './workspaceContext';

function branch(id: number, code: string, extra: Partial<Branch> = {}): Branch {
  return {
    id,
    companyId: 1,
    code,
    name: code,
    openingDate: '2020-01-01',
    headOffice: false,
    forexAuthorized: false,
    recordStatus: 'ACTIVE',
    createdBy: 'admin',
    ...extra,
  };
}

// Listed alphabetically, as the API returns them: Cebu first, head office second.
const BRANCHES = [branch(2, 'CEB'), branch(1, 'HO', { headOffice: true }), branch(3, 'DVO')];

describe('default branch of a new document', () => {
  it('uses the branch selected in the header first', () => {
    expect(defaultBranchId(BRANCHES, 3, 2)).toBe(3);
  });

  it('then the user home branch, then the head office, never simply the first branch', () => {
    expect(defaultBranchId(BRANCHES, undefined, 2)).toBe(2);
    expect(defaultBranchId(BRANCHES)).toBe(1);
    expect(defaultBranchId(BRANCHES, undefined, 99)).toBe(1);
  });

  it('skips inactive branches and falls back to the first branch or 0', () => {
    const closedHo = [
      branch(2, 'CEB'),
      branch(1, 'HO', { headOffice: true, recordStatus: 'INACTIVE' }),
    ];
    expect(defaultBranchId(closedHo, 1)).toBe(2);
    expect(defaultBranchId([branch(4, 'MNL')])).toBe(4);
    expect(defaultBranchId([])).toBe(0);
  });
});
