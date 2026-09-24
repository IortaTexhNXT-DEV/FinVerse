import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { accountsApi } from '@/api/accounts';
import type { AccountCriteria, AccountSummary } from '@/api/accounts';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Pager } from '@/components/ui/Pager';
import { useCompanyId } from '@/context/workspaceContext';
import { ACCOUNT_COLUMNS } from './accountColumns';

interface Props {
  criteria: AccountCriteria;
  page: number;
  onPage: (page: number) => void;
  columns?: Column<AccountSummary>[];
  emptyMessage?: string;
}

/** A paged list of accounts matching criteria; a row opens the account. */
export function AccountTable({
  criteria,
  page,
  onPage,
  columns = ACCOUNT_COLUMNS,
  emptyMessage = 'No items to display',
}: Readonly<Props>) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const result = useQuery({
    queryKey: ['accounts', companyId, criteria, page],
    queryFn: () => accountsApi.search(companyId, criteria, page),
  });
  return (
    <>
      <ErrorAlert error={result.error} />
      <DataTable<AccountSummary>
        loading={result.isLoading}
        rows={result.data?.content ?? []}
        rowKey={(a) => a.id}
        onRowClick={(a) => void navigate(`/accounts/${a.id}`)}
        emptyMessage={emptyMessage}
        columns={columns}
      />
      <Pager
        page={page}
        totalPages={result.data?.totalPages ?? 0}
        total={result.data?.totalElements ?? 0}
        size={result.data?.size}
        onPage={onPage}
      />
    </>
  );
}
