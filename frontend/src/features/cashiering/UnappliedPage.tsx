import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ItemResultsDialog } from '@/components/broking/ItemResultsDialog';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import { cashieringApi } from './cashieringApi';
import type { BulkResult, UnappliedItem, UnappliedTab } from './cashieringApi';

const TABS: readonly { id: UnappliedTab; label: string }[] = [
  { id: 'UNAPPLIED', label: 'Unapplied' },
  { id: 'MONITORING', label: 'Monitoring' },
  { id: 'FOR_APPROVAL', label: 'For Approval' },
  { id: 'FOR_REVERSAL', label: 'For Reversal' },
  { id: 'DONE', label: 'Done' },
];

const keyOf = (u: UnappliedItem) => String(u.id);

const COLUMNS: Column<UnappliedItem>[] = [
  {
    key: 'ref',
    header: 'Reference',
    render: (u) => (
      <>
        <strong>{u.reference}</strong>
        <span className="cell-sub">{humanize(u.origin)}</span>
      </>
    ),
  },
  {
    key: 'payor',
    header: 'Payor / Client',
    render: (u) => (
      <>
        {u.payorName ?? ''}
        <span className="cell-sub">{u.clientCode ?? u.invoiceNo ?? ''}</span>
      </>
    ),
  },
  { key: 'unit', header: 'Unit', render: (u) => u.salesUnit ?? '' },
  { key: 'amount', header: 'Amount', numeric: true, render: (u) => <Amount value={u.amount} /> },
  { key: 'balance', header: 'Balance', numeric: true, render: (u) => <Amount value={u.balance} /> },
  {
    key: 'disp',
    header: 'Disposition',
    render: (u) =>
      u.current
        ? `${humanize(u.current.dispositionType)} ${u.current.amount.toFixed(2)}`
        : (u.dispositionHint ?? ''),
  },
  { key: 'date', header: 'Received', render: (u) => formatDate(u.createdAt) },
  { key: 'stage', header: 'Stage', render: (u) => <StatusBadge status={u.stage} /> },
];

function toResults(r: BulkResult) {
  return [
    ...r.done.map((reference) => ({ reference, ok: true, message: 'Done' })),
    ...r.failures.map((f) => ({ reference: f.split(':')[0] ?? f, ok: false, message: f })),
  ];
}

const BULK: Partial<
  Record<
    UnappliedTab,
    { permission: string; label: string; run: (ids: number[]) => Promise<BulkResult> }
  >
> = {
  MONITORING: {
    permission: 'CASH_DISPOSITION',
    label: 'Submit Selected',
    run: cashieringApi.bulkSubmit,
  },
  FOR_APPROVAL: {
    permission: 'CASH_DISPOSITION_APPROVE',
    label: 'Approve Selected',
    run: cashieringApi.bulkApprove,
  },
};

/** The bulk action of the tab (submit in Monitoring, approve in For Approval), if the user may. */
function BulkButton({
  tab,
  count,
  busy,
  onRun,
}: Readonly<{
  tab: UnappliedTab;
  count: number;
  busy: boolean;
  onRun: (fn: (ids: number[]) => Promise<BulkResult>) => void;
}>) {
  const { can } = useAuth();
  const action = BULK[tab];
  if (action === undefined || !can(action.permission)) {
    return null;
  }
  return (
    <Button variant="accent" disabled={count === 0} busy={busy} onClick={() => onRun(action.run)}>
      {action.label}
    </Button>
  );
}

/**
 * Unapplied Payments workbench (CSHID.024/025): payments that could not be applied, by stage.
 * The cashier assigns a disposition (apply, refund, reclass, transfer), submits it, and the
 * approver processes it; selected items are submitted or approved in bulk.
 */
export default function UnappliedPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const [tab, setTab] = useState<UnappliedTab>('UNAPPLIED');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [results, setResults] = useState<BulkResult>();
  const list = useQuery({
    queryKey: ['cashiering', 'unapplied', companyId, tab, q, page],
    queryFn: () => cashieringApi.unapplied(companyId, tab, q, page),
    enabled: companyId > 0,
  });
  const bulk = useMutation({
    mutationFn: (fn: (ids: number[]) => Promise<BulkResult>) => fn(selection.keys.map(Number)),
    onSuccess: async (r) => {
      selection.clear();
      setResults(r);
      await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
    },
  });
  const rows = list.data?.content ?? [];
  const bulkAction = BULK[tab] !== undefined;
  const columns = bulkAction
    ? [selectionColumn(rows, keyOf, selection, (u) => u.reference), ...COLUMNS]
    : COLUMNS;
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Unapplied Payments"
        description="Payments not applied to an invoice: assign a disposition, submit it and have it approved."
      />
      <Card flush>
        <Tabs
          tabs={TABS}
          active={tab}
          onChange={(t) => {
            setTab(t);
            setPage(0);
            selection.clear();
          }}
        />
        <WorklistToolbar
          placeholder="Search Reference or Payor"
          onSearch={(text) => {
            setQ(text);
            setPage(0);
          }}
        >
          <BulkButton
            tab={tab}
            count={selection.keys.length}
            busy={bulk.isPending}
            onRun={(fn) => bulk.mutate(fn)}
          />
        </WorklistToolbar>
        <ErrorAlert error={list.error ?? bulk.error} />
        {!list.isLoading && rows.length === 0 ? (
          <EmptyState />
        ) : (
          <DataTable
            caption="Unapplied payments"
            columns={columns}
            rows={rows}
            rowKey={(u) => u.id}
            loading={list.isLoading}
            onRowClick={(u) => void navigate(`/cashiering/unapplied/${u.id}`)}
          />
        )}
        <PageFooter data={list.data} noun="items" onPage={setPage} />
      </Card>
      {results && (
        <ItemResultsDialog
          title="Bulk Action Results"
          results={toResults(results)}
          onClose={() => setResults(undefined)}
        />
      )}
    </div>
  );
}
