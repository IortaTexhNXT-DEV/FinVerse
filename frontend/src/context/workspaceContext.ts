import { createContext, useContext } from 'react';
import type { Branch, Company } from '@/api/types';

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
