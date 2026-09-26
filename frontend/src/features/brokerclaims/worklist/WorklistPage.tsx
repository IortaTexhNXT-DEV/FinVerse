import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { UserRoundCog, X } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { CLAIMS_SECTION } from '../ClaimsPlaceholder';
import { claimsHomeApi } from '../home/api';
import type { WorklistQuery, WorklistRow, WorklistTab } from '../home/api';
import { ReassignDialog } from './ReassignDialog';
import { WORKLIST_TABS, filterText, queryFromSearch } from './worklistLogic';

const COLUMNS: Column<WorklistRow>[] = [
  {
    key: 'claim',
    header: 'Claim No.',
    render: (r) => (
      <>
        <strong>{r.claimNo}</strong>
        <div className="muted">{r.cover.insurerClaimNos ?? r.cover.policyNo ?? r.cover.arn}</div>
      </>
    ),
  },
  {
    key: 'assured',
    header: 'Assured / Claimant',
    render: (r) => (
      <>
        {r.cover.assuredName}
        <div className="muted">{r.cover.claimantName}</div>
      </>
    ),
  },
  { key: 'loss', header: 'Date of Loss', render: (r) => formatDate(r.dates.lossDate) },
  {
    key: 'age',
    header: 'Age (Stage / Overall)',
    render: (r) => `${r.dates.ageThisStage} d / ${r.dates.ageOverall} d`,
  },
  {
    key: 'follow',
    header: 'Next Follow-up',
    render: (r) => (
      <>
        {formatDate(r.dates.nextFollowUpDate)}
        {r.dates.followUpOverdue && <span className="tag">Overdue</span>}
      </>
    ),
  },
  { key: 'plan', header: 'Next Action', render: (r) => r.nextActionPlan ?? '' },
  { key: 'handler', header: 'Handler', render: (r) => r.handling.handler },
  {
    key: 'status',
    header: 'Status',
    render: (r) => <StatusBadge status={r.handling.statusLabel ?? r.handling.phase} />,
  },
];

/**
 * Claims worklist (BRCLM.034/043, FR-CM-055): the tabs My Claims, Open, Temporarily Closed, Closed
 * and Follow-ups Due, search by claim number, insurer claim number, ARN, policy number or assured,
 * the home tile filters, and the bulk reassignment of the selected claims (WORK_ASSIGN).
 */
export default function WorklistPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { can } = useAuth();
  const [search] = useSearchParams();
  const [query, setQuery] = useState<WorklistQuery>(() => queryFromSearch(search));
  const [page, setPage] = useState(0);
  const [reassigning, setReassigning] = useState(false);
  const selection = useRowSelection();
  const rows = useQuery({
    queryKey: ['broker-claims', 'worklist', companyId, query, page],
    queryFn: () => claimsHomeApi.worklist(companyId, query, page),
    enabled: companyId > 0,
  });
  const reassign = useMutation({
    mutationFn: ({ handler, comment }: { handler: string; comment: string }) =>
      claimsHomeApi.reassign(
        companyId,
        selection.keys.map(Number),
        handler,
        comment === '' ? undefined : comment,
      ),
    onSuccess: async (r) => {
      setReassigning(false);
      selection.clear();
      await queryClient.invalidateQueries({ queryKey: ['broker-claims'] });
      toast.success(`${r.moved} claim(s) reassigned`);
    },
  });
  const change = (next: WorklistQuery) => {
    setQuery(next);
    setPage(0);
    selection.clear();
  };
  const items = rows.data?.content ?? [];
  const filter = filterText(query);
  return (
    <div className="stack">
      <PageHeader
        section={CLAIMS_SECTION}
        backTo="/claims-handling"
        title="Claims Worklist"
        description="Every claim you may see, by tab; search by claim number, insurer claim number, ARN, policy number or assured."
      />
      <ErrorAlert error={rows.error} />
      <Card flush>
        <Tabs
          tabs={WORKLIST_TABS}
          active={query.tab}
          onChange={(t: WorklistTab) => change({ ...query, tab: t })}
        />
        <WorklistToolbar
          placeholder="Search Claim No., Insurer Claim No., ARN, Policy No. or Assured"
          onSearch={(text) => change({ ...query, q: text === '' ? undefined : text })}
          extra={
            filter !== undefined && (
              <Button
                size="sm"
                variant="ghost"
                icon={<X size={14} />}
                onClick={() => change({ tab: query.tab, q: query.q })}
              >
                {filter}
              </Button>
            )
          }
        >
          {can('WORK_ASSIGN') && (
            <Button
              variant="secondary"
              icon={<UserRoundCog size={16} />}
              disabled={selection.keys.length === 0}
              onClick={() => setReassigning(true)}
            >
              Reassign
            </Button>
          )}
        </WorklistToolbar>
        <DataTable
          caption="Claims"
          columns={
            can('WORK_ASSIGN')
              ? [
                  selectionColumn(
                    items,
                    (r) => String(r.id),
                    selection,
                    (r) => r.claimNo,
                  ),
                  ...COLUMNS,
                ]
              : COLUMNS
          }
          rows={items}
          rowKey={(r) => r.id}
          loading={rows.isLoading}
          emptyMessage="No claims match"
          onRowClick={(r) => void navigate(`/claims-handling/${r.id}`)}
        />
        <PageFooter data={rows.data} noun="claims" onPage={setPage} />
      </Card>
      {reassigning && (
        <ReassignDialog
          count={selection.keys.length}
          busy={reassign.isPending}
          error={reassign.error}
          onClose={() => setReassigning(false)}
          onSave={(handler, comment) => reassign.mutate({ handler, comment })}
        />
      )}
    </div>
  );
}
