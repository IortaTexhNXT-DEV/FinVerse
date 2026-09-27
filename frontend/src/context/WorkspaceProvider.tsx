import { useQuery } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { organizationApi } from '@/api/organization';
import { WorkspaceContext } from './workspaceContext';

/**
 * The company and branch the user is working in. Every screen reads the selected company from
 * here, so switching company in the header re-scopes the whole application.
 */
export function WorkspaceProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [companyId, setCompanyId] = useState<number | undefined>();
  const [branchId, setBranchId] = useState<number | undefined>();
  const companies = useQuery({ queryKey: ['companies'], queryFn: organizationApi.companies });
  const company =
    companies.data?.find((c) => c.id === companyId) ??
    companies.data?.find((c) => c.recordStatus === 'ACTIVE');
  const branches = useQuery({
    queryKey: ['branches', company?.id],
    queryFn: () => organizationApi.branches(company?.id ?? 0),
    enabled: company !== undefined,
  });

  const value = useMemo(
    () => ({
      companies: companies.data ?? [],
      company,
      branches: branches.data ?? [],
      branchId,
      setCompanyId,
      setBranchId,
    }),
    [companies.data, company, branches.data, branchId],
  );
  return <WorkspaceContext.Provider value={value}>{children}</WorkspaceContext.Provider>;
}
