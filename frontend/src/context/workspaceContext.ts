import { createContext, useContext } from 'react';
import type { Branch, Company } from '@/api/types';
import { useAuth } from '@/auth/authContext';

export interface Workspace {
  companies: Company[];
  company: Company | undefined;
  branches: Branch[];
  branchId: number | undefined;
  setCompanyId: (id: number) => void;
  setBranchId: (id: number | undefined) => void;
}

export const WorkspaceContext = createContext<Workspace | null>(null);

/** Selected company and branch (see WorkspaceProvider). */
export function useWorkspace(): Workspace {
  const ctx = useContext(WorkspaceContext);
  if (ctx === null) {
    throw new Error('useWorkspace must be used inside WorkspaceProvider');
  }
  return ctx;
}

/** Returns the selected company id (0 until the company list has loaded). */
export function useCompanyId(): number {
  const { company } = useWorkspace();
  return company?.id ?? 0;
}

/**
 * Branch a new document is booked to unless the user picks another: the branch selected in the
 * header, else the user's home branch, else the head office, else the first branch (0 while the
 * branch list is loading). Only active branches of the selected company qualify; this is the one
 * rule for every create form, never simply the first branch in the list.
 */
export function defaultBranchId(
  branches: readonly Branch[],
  selectedBranchId?: number,
  homeBranchId?: number,
): number {
  const active = branches.filter((b) => b.recordStatus === 'ACTIVE');
  const usable = active.length > 0 ? active : branches;
  const byId = (id: number | undefined) =>
    id === undefined ? undefined : usable.find((b) => b.id === id);
  const branch =
    byId(selectedBranchId) ?? byId(homeBranchId) ?? usable.find((b) => b.headOffice) ?? usable[0];
  return branch?.id ?? 0;
}

/** {@link defaultBranchId} for the current workspace and user. */
export function useDefaultBranchId(): number {
  const { branches, branchId } = useWorkspace();
  const { user } = useAuth();
  return defaultBranchId(branches, branchId, user?.homeBranchId);
}
