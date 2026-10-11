import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { proposalsApi } from '@/api/proposals';
import type { ProposalListItem, ProposalStatus } from '@/api/proposals';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { useCompanyId } from '@/context/workspaceContext';
import { PROPOSAL_COLUMNS } from './proposalColumns';

const CONFIRMED: ProposalStatus[] = ['ACCEPTED', 'CONVERTED'];

/** Columns of the client page list: the client is known, so it is not repeated. */
const COLUMNS = PROPOSAL_COLUMNS.filter((c) => c.key !== 'client');

/** The proposals a client accepted (BDOI Client Record Details, tab Confirmed Proposals). */
export function ClientProposalsTab({ clientId }: Readonly<{ clientId: number }>) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const list = useQuery({
    queryKey: ['proposals', companyId, { clientId, status: CONFIRMED }, page],
    queryFn: () => proposalsApi.search(companyId, { clientId, status: CONFIRMED }, page, 10),
    enabled: companyId > 0,
  });
  return (
    <Card title="Confirmed proposals" flush>
      <ErrorAlert error={list.error} />
      <DataTable<ProposalListItem>
        loading={list.isLoading}
        rows={list.data?.content ?? []}
        rowKey={(p) => p.id}
        onRowClick={(p) => void navigate(`/proposals/${p.id}`)}
        emptyMessage="No proposal accepted by this client yet."
        columns={COLUMNS}
      />
      <PageFooter data={list.data} noun="proposals" onPage={setPage} />
    </Card>
  );
}
