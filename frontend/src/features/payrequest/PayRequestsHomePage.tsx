import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Banknote, FilePlus2, FileX2 } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { ItemResultsDialog } from '@/components/broking/ItemResultsDialog';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { payRequestApi } from './api';
import type { PayRequest, RequestKind, RequestSummary, StageCounts } from './api';
import { KIND_LABELS, STAGE_TABS, tabOf } from './requestForm';
import type { StageTab } from './requestForm';

const keyOf = (r: RequestSummary) => String(r.id);

const COLUMNS: Column<RequestSummary>[] = [
  {
    key: 'no',
    header: 'Request No.',
    render: (r) => (
      <>
        <strong>{r.requestNo}</strong>
        <span className="cell-sub">{formatDate(r.requestDate)}</span>
      </>
    ),
  },
  { key: 'kind', header: 'Kind', render: (r) => KIND_LABELS[r.kind] },
  {
    key: 'payee',
    header: 'Payee',
    render: (r) => (
      <>
        {r.payeeName}
        <span className="cell-sub">{r.payeeCode}</span>
      </>
    ),
  },
  { key: 'amount', header: 'Amount', numeric: true, render: (r) => <Amount value={r.amount} /> },
  {
    key: 'dv',
    header: 'Disbursement',
    render: (r) => (
      <>
        {r.dvNo ?? '—'}
        <span className="cell-sub">{r.instrumentStatus ?? r.disbursementStatus ?? ''}</span>
      </>
    ),
  },
  { key: 'by', header: 'Requested By', render: (r) => r.createdBy },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.stage} /> },
  {
    key: 'flags',
    header: 'Flags',
    render: (r) =>
      r.validationRequired ? (
        <span className="tag-list">
          <span className="tag">Cancelled Policy</span>
        </span>
      ) : null,
  },
];

function tabsWithCounts(counts: StageCounts | undefined) {
  return STAGE_TABS.map((t) => {
    const count = t.id === 'ALL' ? undefined : counts?.[t.id];
    return { id: t.id, label: count ? `${t.label} (${String(count)})` : t.label };
  });
}

/** Bulk decision of the tab: endorse in For Review, approve in For Approval / HR Approval. */
const BULK: Partial<
  Record<StageTab, { permission: string; label: string; run: (id: number) => Promise<PayRequest> }>
> = {
  FOR_REVIEW: {
    permission: 'PRQ_REVIEW',
    label: 'Endorse Selected',
    run: (id) => payRequestApi.endorse(id),
  },
  FOR_APPROVAL: {
    permission: 'PRQ_APPROVE',
    label: 'Approve Selected',
    run: (id) => payRequestApi.approve(id),
  },
  HR_APPROVAL: {
    permission: 'PRQ_HR_APPROVE',
    label: 'Approve Selected (HR)',
    run: (id) => payRequestApi.approve(id),
  },
};

interface Outcome {
  reference: string;
  ok: boolean;
  message: string;
}

async function runAll(
  ids: string[],
  rows: RequestSummary[],
  run: (id: number) => Promise<PayRequest>,
): Promise<Outcome[]> {
  const outcomes: Outcome[] = [];
  for (const id of ids) {
    const reference = rows.find((r) => keyOf(r) === id)?.requestNo ?? id;
    try {
      const r = await run(Number(id));
      outcomes.push({ reference, ok: true, message: `Now ${r.stage}` });
    } catch (e) {
      outcomes.push({ reference, ok: false, message: e instanceof Error ? e.message : 'Failed' });
    }
  }
  return outcomes;
}

function Filters({
  kind,
  from,
  to,
  onChange,
}: Readonly<{
  kind: string;
  from: string;
  to: string;
  onChange: (next: { kind: string; from: string; to: string }) => void;
}>) {
  return (
    <div className="worklist-filters form-grid">
      <Field label="Kind">
        {(id) => (
          <select
            id={id}
            className="select"
            value={kind}
            onChange={(e) => onChange({ kind: e.target.value, from, to })}
          >
            <option value="">All kinds</option>
            {(Object.keys(KIND_LABELS) as RequestKind[]).map((k) => (
              <option key={k} value={k}>
                {KIND_LABELS[k]}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Request Date From">
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            value={from}
            onChange={(e) => onChange({ kind, from: e.target.value, to })}
          />
        )}
      </Field>
      <Field label="Request Date To">
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            value={to}
            onChange={(e) => onChange({ kind, from, to: e.target.value })}
          />
        )}
      </Field>
    </div>
  );
}

/**
 * Refund & Cash Advance Requests (MKT 1.3.0-1.18.0): the requests by stage with their payment in
 * Disbursement, searchable by request, payee, reference or DV and filtered by kind and request
 * date; reviewers endorse and approvers approve the selected requests in bulk (MKT 1.4.0).
 */
export default function PayRequestsHomePage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { can } = useAuth();
  const selection = useRowSelection();
  const [params, setParams] = useSearchParams();
  const tab = tabOf(params.get('stage'));
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [showFilters, setShowFilters] = useState(false);
  const [filter, setFilter] = useState({ kind: '', from: '', to: '' });
  const [results, setResults] = useState<Outcome[]>();
  const stage = tab === 'ALL' ? undefined : tab;
  const list = useQuery({
    queryKey: ['payrequest', 'requests', companyId, stage, q, page, filter],
    queryFn: () =>
      payRequestApi.search(companyId, {
        stage,
        q,
        page,
        kind: filter.kind === '' ? undefined : (filter.kind as RequestKind),
        from: filter.from,
        to: filter.to,
      }),
    enabled: companyId > 0,
  });
  const counts = useQuery({
    queryKey: ['payrequest', 'counts', companyId],
    queryFn: () => payRequestApi.counts(companyId),
    enabled: companyId > 0,
  });
  const rows = list.data?.content ?? [];
  const bulk = BULK[tab];
  const mayBulk = bulk !== undefined && can(bulk.permission);
  const decide = useMutation({
    mutationFn: () => runAll(selection.keys, rows, bulk?.run ?? payRequestApi.approve),
    onSuccess: async (outcomes) => {
      selection.clear();
      setResults(outcomes);
      await queryClient.invalidateQueries({ queryKey: ['payrequest'] });
    },
  });
  const columns = mayBulk
    ? [selectionColumn(rows, keyOf, selection, (r) => r.requestNo), ...COLUMNS]
    : COLUMNS;
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Refund & Cash Advance Requests"
        title="Refund & Cash Advance Requests"
        description="Client refunds, employee cash advances and disbursed-check cancellations, from preparation to payment."
        actions={
          can('PRQ_CREATE') && (
            <>
              <Link className="btn btn-secondary" to="/payment-requests/check-cancellation">
                <FileX2 size={16} aria-hidden="true" /> Cancel a Check
              </Link>
              <Link className="btn btn-secondary" to="/payment-requests/new-cash-advance">
                <Banknote size={16} aria-hidden="true" /> New Cash Advance
              </Link>
              <Link className="btn btn-primary" to="/payment-requests/new-refund">
                <FilePlus2 size={16} aria-hidden="true" /> New Refund Request
              </Link>
            </>
          )
        }
      />
      <ErrorAlert error={list.error ?? decide.error} />
      <Card flush>
        <div className="work-tabs">
          <Tabs<StageTab>
            tabs={tabsWithCounts(counts.data)}
            active={tab}
            onChange={(next) => {
              setPage(0);
              selection.clear();
              setParams(next === 'ALL' ? {} : { stage: next });
            }}
          />
        </div>
        <WorklistToolbar
          placeholder="Search Request No."
          onSearch={(text) => {
            setQ(text);
            setPage(0);
          }}
          filters={{ open: showFilters, onToggle: () => setShowFilters((v) => !v) }}
        >
          {mayBulk && (
            <Button
              variant="accent"
              disabled={selection.keys.length === 0}
              busy={decide.isPending}
              onClick={() => decide.mutate()}
            >
              {bulk.label}
            </Button>
          )}
        </WorklistToolbar>
        {showFilters && (
          <Filters
            {...filter}
            onChange={(next) => {
              setFilter(next);
              setPage(0);
            }}
          />
        )}
        <DataTable
          caption="Refund and cash-advance requests"
          columns={columns}
          rows={rows}
          rowKey={(r) => r.id}
          loading={list.isLoading}
          emptyMessage="No items to display"
          onRowClick={(r) => void navigate(`/payment-requests/requests/${String(r.id)}`)}
        />
        <PageFooter data={list.data} noun="requests" onPage={setPage} />
      </Card>
      {results && (
        <ItemResultsDialog
          title="Bulk Decision Results"
          results={results}
          onClose={() => setResults(undefined)}
        />
      )}
    </div>
  );
}
