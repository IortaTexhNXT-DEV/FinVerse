import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { TriangleAlert } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { ItemResultsDialog } from '@/components/broking/ItemResultsDialog';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, humanize } from '@/utils/format';
import type { ItemResult } from '../plans/api';
import type { EscalateInput, Escalation } from './api';
import { escalationsApi } from './api';
import { EscalateDialog } from './EscalationDialogs';
import type { EscalationTab } from './labels';
import { ESCALATION_TABS, LEVEL_LABELS, stagesOf } from './labels';

const COLUMNS: Column<Escalation>[] = [
  {
    key: 'no',
    header: 'Escalation No.',
    render: (e) => (
      <>
        <strong>{e.escalationNo}</strong>
        <div className="muted">{e.kind === 'AUTO' ? `Rule ${e.ruleCode ?? ''}` : e.raisedBy}</div>
      </>
    ),
  },
  { key: 'arn', header: 'ARN', render: (e) => e.arn },
  { key: 'assured', header: 'Name of Assured', render: (e) => e.assuredName },
  { key: 'reason', header: 'Reason', render: (e) => humanize(e.reasonCode) },
  {
    key: 'target',
    header: 'Escalated To',
    render: (e) => e.targetUsername ?? LEVEL_LABELS[e.targetLevel],
  },
  {
    key: 'balance',
    header: 'Outstanding',
    numeric: true,
    render: (e) => <Amount value={e.totalBalance} />,
  },
  {
    key: 'since',
    header: 'In Stage Since',
    render: (e) => (
      <>
        {formatDateTime(e.stageSince)}
        {e.overdue && <span className="tag">Past SLA</span>}
      </>
    ),
  },
  { key: 'status', header: 'Status', render: (e) => <StatusBadge status={e.status} /> },
];

/**
 * Escalations inbox (BRCLXN.049/050): escalations by stage, raised by the rules (aging,
 * commitments, broken promises, overdue installments) or by users, and the manual escalation of
 * one or several invoices.
 */
export default function EscalationsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<EscalationTab>('TL');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [escalating, setEscalating] = useState(false);
  const [results, setResults] = useState<ItemResult[]>();
  const rows = useQuery({
    queryKey: ['collections', 'escalations', companyId, tab, query, page],
    queryFn: () => escalationsApi.escalations(companyId, stagesOf(tab), query, page),
    enabled: companyId > 0,
  });
  const escalate = useMutation({
    mutationFn: (input: EscalateInput) => escalationsApi.escalate(input),
    onSuccess: async (r) => {
      setEscalating(false);
      setResults(r);
      await queryClient.invalidateQueries({ queryKey: ['collections', 'escalations'] });
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        title="Escalations"
        description="Accounts escalated to the team lead or the unit / section head, by rule or by hand, until they are resolved or collected."
        actions={
          can('CLX_ESCALATE') ? (
            <Button
              variant="accent"
              icon={<TriangleAlert size={16} />}
              onClick={() => setEscalating(true)}
            >
              Escalate Accounts
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={rows.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={ESCALATION_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <WorklistToolbar
            placeholder="Search Escalation, ARN or Invoice No."
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
          />
          <DataTable
            caption="Escalations"
            columns={COLUMNS}
            rows={rows.data?.content ?? []}
            rowKey={(e) => e.id}
            loading={rows.isLoading}
            emptyMessage="No escalations to display"
            onRowClick={(e) => void navigate(`/collections/escalations/${e.id}`)}
          />
          <PageFooter data={rows.data} noun="escalations" onPage={setPage} />
        </div>
      </Card>
      {escalating && (
        <EscalateDialog
          companyId={companyId}
          busy={escalate.isPending}
          error={escalate.error}
          onClose={() => setEscalating(false)}
          onEscalate={(input) => escalate.mutate(input)}
        />
      )}
      {results !== undefined && (
        <ItemResultsDialog
          title="Escalation Results"
          results={results}
          onClose={() => setResults(undefined)}
        />
      )}
    </div>
  );
}
