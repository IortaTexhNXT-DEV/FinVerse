import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
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
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import type { ItemResult, PaymentPromise } from './api';
import { plansApi } from './api';
import type { PromiseTab } from './labels';
import { PROMISE_TABS, statusesOf } from './labels';
import { PromiseDialog } from './PlanDialogs';
import { ReasonDialog } from './Parts';
import { usePromiseSave } from './usePromiseSave';

const COLUMNS: Column<PaymentPromise>[] = [
  {
    key: 'inv',
    header: 'Invoice No.',
    render: (p) => (
      <>
        <strong>{p.invoiceNo}</strong>
        <div className="muted">{p.arn}</div>
      </>
    ),
  },
  { key: 'assured', header: 'Name of Assured', render: (p) => p.assuredName },
  { key: 'on', header: 'Promised On', render: (p) => formatDate(p.promisedOn) },
  { key: 'date', header: 'Promised Date', render: (p) => formatDate(p.promisedDate) },
  {
    key: 'amount',
    header: 'Promised',
    numeric: true,
    render: (p) => <Amount value={p.promisedAmount} />,
  },
  {
    key: 'paid',
    header: 'Paid in Time',
    numeric: true,
    render: (p) => <Amount value={p.actualPaid} />,
  },
  {
    key: 'by',
    header: 'Recorded By',
    render: (p) => (
      <>
        {p.recordedBy}
        {p.bulkRef !== undefined && <div className="muted">{p.bulkRef}</div>}
      </>
    ),
  },
  { key: 'status', header: 'Status', render: (p) => <StatusBadge status={p.status} /> },
];

/**
 * Promises to pay (BRCLXN.055): open promises by promised date, broken, kept and withdrawn ones;
 * record a promise on one invoice, or the same promise on several (bulk update), and withdraw
 * the selected open promises. The nightly promise check marks them kept or broken; a broken
 * promise escalates the account.
 */
export default function PromisesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [params] = useSearchParams();
  const [tab, setTab] = useState<PromiseTab>('OPEN');
  const [query, setQuery] = useState(params.get('q') ?? '');
  const [page, setPage] = useState(0);
  const [recording, setRecording] = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);
  const [results, setResults] = useState<ItemResult[]>();
  const selection = useRowSelection();
  const rows = useQuery({
    queryKey: ['collections', 'promises', companyId, tab, query, page],
    queryFn: () => plansApi.promises(companyId, statusesOf(PROMISE_TABS, tab), query, page),
    enabled: companyId > 0,
  });
  const save = usePromiseSave(companyId, (r) => {
    setRecording(false);
    setResults(r);
  });
  const withdraw = useMutation({
    mutationFn: async (reason: string) => {
      for (const id of selection.keys) {
        await plansApi.cancelPromise(Number(id), reason);
      }
      return selection.keys.length;
    },
    onSuccess: async (n) => {
      setWithdrawing(false);
      selection.clear();
      await queryClient.invalidateQueries({ queryKey: ['collections', 'promises'] });
      toast.success(`${n} promise(s) withdrawn`);
    },
  });
  const list = rows.data?.content ?? [];
  const selectable = tab === 'OPEN' && can('CLX_WORK');
  const columns = selectable
    ? [
        selectionColumn(
          list,
          (p) => String(p.id),
          selection,
          (p) => p.invoiceNo,
        ),
        ...COLUMNS,
      ]
    : COLUMNS;
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        title="Promises to Pay"
        description="Promises made by clients, checked every night against the payments applied in the invoice ledger."
        actions={
          can('CLX_WORK') ? (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setRecording(true)}>
              Record Promise
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={rows.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={PROMISE_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
              selection.clear();
            }}
          />
          <WorklistToolbar
            placeholder="Search Invoice No. or ARN"
            initial={query}
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
          >
            {selectable && (
              <Button
                variant="secondary"
                disabled={selection.keys.length === 0}
                onClick={() => setWithdrawing(true)}
              >
                Withdraw Promise
              </Button>
            )}
          </WorklistToolbar>
          <DataTable
            caption="Promises to pay"
            columns={columns}
            rows={list}
            rowKey={(p) => p.id}
            loading={rows.isLoading}
            emptyMessage="No promises to pay to display"
          />
          <PageFooter data={rows.data} noun="promises" onPage={setPage} />
        </div>
      </Card>
      {recording && (
        <PromiseDialog
          allowMany={can('CLX_BULK_UPDATE')}
          busy={save.isPending}
          error={save.error}
          onClose={() => setRecording(false)}
          onSave={(draft) => save.mutate(draft)}
        />
      )}
      {withdrawing && (
        <ReasonDialog
          title="Withdraw Promise to Pay"
          confirmLabel="Withdraw Promise"
          busy={withdraw.isPending}
          error={withdraw.error}
          onClose={() => setWithdrawing(false)}
          onConfirm={(reason) => withdraw.mutate(reason)}
        />
      )}
      {results !== undefined && (
        <ItemResultsDialog
          title="Promises Recorded"
          results={results}
          onClose={() => setResults(undefined)}
        />
      )}
    </div>
  );
}
