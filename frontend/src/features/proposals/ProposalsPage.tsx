import { useQuery } from '@tanstack/react-query';
import { ClipboardList, Plus } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { proposalsApi } from '@/api/proposals';
import type { ProposalListItem } from '@/api/proposals';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { PROPOSAL_COLUMNS } from './proposalColumns';
import { PROPOSAL_TABS, proposalCriteria } from './proposalList';
import type { ProposalTab } from './proposalList';
import '@/styles/quotation.css';

/**
 * Proposal Requests (BRNB.005-017): the PRF work list by stage, from the Account Officer's draft to
 * the client's acceptance, with the Proposal No. (PRF reference), ARN and slips.
 */
export default function ProposalsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [tab, setTab] = useState<ProposalTab>('drafts');
  const [applied, setApplied] = useState('');
  const [mine, setMine] = useState(false);
  const [page, setPage] = useState(0);
  const criteria = proposalCriteria(tab, applied, mine);
  const list = useQuery({
    queryKey: ['proposals', companyId, criteria, page],
    queryFn: () => proposalsApi.search(companyId, criteria, page),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Non-Package Management"
        title="Proposal Requests"
        description="Non-package risks priced by TSU with the insurers: PRF, quotation slip, insurer terms, comparative table and proposal slip."
        actions={
          <>
            {(can('TSU_PROCESS') || can('TSU_APPROVE')) && (
              <Link className="btn btn-secondary" to="/proposals/tsu">
                <ClipboardList size={16} aria-hidden="true" /> TSU Workbench
              </Link>
            )}
            {can('PROPOSAL_REQUEST') && (
              <Link className="btn btn-accent" to="/proposals/new">
                <Plus size={16} aria-hidden="true" /> New PRF
              </Link>
            )}
          </>
        }
      />
      <Card flush>
        <Tabs
          tabs={PROPOSAL_TABS}
          active={tab}
          onChange={(t) => {
            setTab(t);
            setPage(0);
          }}
        />
        <WorklistToolbar
          onSearch={(text) => {
            setApplied(text);
            setPage(0);
          }}
          extra={
            <label className="checkbox">
              <input type="checkbox" checked={mine} onChange={(e) => setMine(e.target.checked)} />
              Only My PRFs
            </label>
          }
        />
        <ErrorAlert error={list.error} />
        <DataTable<ProposalListItem>
          loading={list.isLoading}
          rows={list.data?.content ?? []}
          rowKey={(p) => p.id}
          onRowClick={(p) => void navigate(`/proposals/${p.id}`)}
          emptyMessage="No items to display"
          columns={PROPOSAL_COLUMNS}
        />
        <PageFooter data={list.data} noun="proposal requests" onPage={setPage} />
      </Card>
    </div>
  );
}
