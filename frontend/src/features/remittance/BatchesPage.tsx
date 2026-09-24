import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCheck, Send } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
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
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { remittanceApi } from './api';
import type { BatchSummary } from './api';
import { TypeChip } from './RemittanceParts';
import { BATCH_TABS, batchTabOf, stagesOf, TYPE_LABELS } from './remittanceLabels';
import type { BatchTab } from './remittanceLabels';
import './remittance.css';

const COLUMNS: Column<BatchSummary>[] = [
  {
    key: 'no',
    header: 'Batch No.',
    render: (b) => (
      <>
        <strong>{b.batchNo}</strong>
        <div className="remit-muted">{b.specialRequestNo ?? formatDate(b.createdAt)}</div>
      </>
    ),
  },
  { key: 'ins', header: 'Insurer', render: (b) => b.insurerCode },
  { key: 'type', header: 'Type', render: (b) => <TypeChip type={b.type} /> },
  { key: 'n', header: 'Accounts', numeric: true, render: (b) => b.lineCount },
  {
    key: 'paid',
    header: 'Paid AR',
    numeric: true,
    render: (b) => <Amount value={b.totals.paidAr} />,
  },
  {
    key: 'pay',
    header: 'Payable',
    numeric: true,
    render: (b) => <Amount value={b.totals.payable} />,
  },
  { key: 'cur', header: 'Currency', render: (b) => b.currency },
  { key: 'proc', header: 'Processor', render: (b) => b.processor ?? 'Unassigned' },
  { key: 'dv', header: 'DV No.', render: (b) => b.dvNo ?? '' },
  { key: 'stage', header: 'Status', render: (b) => <StatusBadge status={b.stage} /> },
];

function Filters({
  insurer,
  type,
  onChange,
}: Readonly<{
  insurer: string;
  type: string;
  onChange: (insurer: string, type: string) => void;
}>) {
  return (
    <div className="worklist-filters remit-form">
      <Field label="Insurer Code">
        {(id) => (
          <input
            id={id}
            className="input"
            value={insurer}
            onChange={(e) => onChange(e.target.value, type)}
          />
        )}
      </Field>
      <Field label="Remittance Type">
        {(id) => (
          <select
            id={id}
            className="select"
            value={type}
            onChange={(e) => onChange(insurer, e.target.value)}
          >
            <option value="">All types</option>
            {Object.entries(TYPE_LABELS).map(([code, label]) => (
              <option key={code} value={code}>
                {label}
              </option>
            ))}
          </select>
        )}
      </Field>
    </div>
  );
}

/** The bulk action of a tab for the user: submit in review, approve for approval. */
function bulkActionOf(
  tab: BatchTab,
  can: (permission: string) => boolean,
): 'submit' | 'approve' | undefined {
  if (tab === 'REVIEW' && can('REMIT_PROCESS')) {
    return 'submit';
  }
  return tab === 'APPROVAL' && can('REMIT_APPROVE') ? 'approve' : undefined;
}

/** Submits or approves the selected batches one by one (RMTID.010). */
function useBulk(reset: () => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async ({ ids, approve }: { ids: number[]; approve: boolean }) => {
      for (const id of ids) {
        await (approve ? remittanceApi.approve(id) : remittanceApi.submit(id));
      }
      return { count: ids.length, approve };
    },
    onSuccess: async ({ count, approve }) => {
      reset();
      await queryClient.invalidateQueries({ queryKey: ['remittance'] });
      toast.success(`${count} batch(es) ${approve ? 'approved' : 'submitted for approval'}`);
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['remittance', 'batches'] }),
  });
}

/**
 * Remittance batches (RMTID.007-011/024/027): the Process Remittance queues by stage, from review
 * to the insurer OR. Processors submit the batches they reviewed; team leaders approve those for
 * approval and push them to Disbursement. Open a batch to exclude accounts and see its documents.
 */
export default function BatchesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [tab, setTab] = useState<BatchTab>(() => batchTabOf(params.get('tab')));
  const [query, setQuery] = useState('');
  const [insurer, setInsurer] = useState('');
  const [type, setType] = useState('');
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [page, setPage] = useState(0);
  const selection = useRowSelection();
  const bulk = useBulk(selection.clear);
  const rows = useQuery({
    queryKey: ['remittance', 'batches', companyId, tab, query, insurer, type, page],
    queryFn: () =>
      remittanceApi.batches(
        companyId,
        stagesOf(BATCH_TABS, tab),
        { q: query, insurer, type },
        page,
      ),
    enabled: companyId > 0,
  });
  const content = rows.data?.content ?? [];
  const bulkAction = bulkActionOf(tab, can);
  const ids = content.filter((b) => selection.has(String(b.id))).map((b) => b.id);
  const columns =
    bulkAction !== undefined
      ? [
          selectionColumn(
            content,
            (b) => String(b.id),
            selection,
            (b) => b.batchNo,
          ),
          ...COLUMNS,
        ]
      : COLUMNS;
  return (
    <div className="stack">
      <PageHeader
        section="Remittance"
        title="Remittance Batches"
        description="Process Remittance: review, submit, approve and follow the batches to Disbursement and the insurer OR."
      />
      <ErrorAlert error={rows.error ?? bulk.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={BATCH_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
              selection.clear();
            }}
          />
          <WorklistToolbar
            placeholder="Search Batch or Invoice No."
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
            filters={{ open: filtersOpen, onToggle: () => setFiltersOpen(!filtersOpen) }}
          >
            {bulkAction !== undefined && (
              <Button
                icon={bulkAction === 'approve' ? <CheckCheck size={16} /> : <Send size={16} />}
                disabled={ids.length === 0}
                busy={bulk.isPending}
                onClick={() => bulk.mutate({ ids, approve: bulkAction === 'approve' })}
              >
                {bulkAction === 'approve' ? 'Approve Selected' : 'Submit Selected'}
              </Button>
            )}
          </WorklistToolbar>
          {filtersOpen && (
            <Filters
              insurer={insurer}
              type={type}
              onChange={(i, t) => {
                setInsurer(i);
                setType(t);
                setPage(0);
              }}
            />
          )}
          <DataTable
            caption="Remittance batches"
            columns={columns}
            rows={content}
            rowKey={(b) => b.id}
            loading={rows.isLoading}
            onRowClick={(b) => void navigate(`/remittance/batches/${b.id}`)}
          />
          <PageFooter data={rows.data} noun="batches" onPage={setPage} />
        </div>
      </Card>
    </div>
  );
}
