import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Banknote, CalendarDays, CheckCircle2, FileText, RefreshCcw, User } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import type { ActionNote, WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { workflowKey } from '@/components/broking/workflowKey';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { RUN_ENTITY, frbsApi } from './api';
import type { ServiceFeeInvoice, ServiceFeeLine, ServiceFeeRun } from './api';
import { LiquidateDialog, ReleaseDialog } from './LineDialogs';
import { lineActions, tagProgress, totalsByCurrency } from './serviceFee';
import './frbs.css';

type TabId = 'lines' | 'invoices';
type Pending = { kind: 'release' | 'liquidate'; line: ServiceFeeLine } | undefined;

const INVOICE_COLUMNS: Column<ServiceFeeInvoice>[] = [
  {
    key: 'invoice',
    header: 'Invoice',
    render: (i) => (
      <>
        <strong>{i.invoiceNo}</strong>
        <span className="cell-sub">{i.assuredName}</span>
      </>
    ),
  },
  { key: 'insurer', header: 'Insurer', render: (i) => i.insurerCode },
  { key: 'segment', header: 'Segment', render: (i) => i.marketSegment ?? '—' },
  { key: 'paid', header: 'Fully Paid', render: (i) => formatDate(i.paidOn) },
  {
    key: 'commission',
    header: 'Commission',
    numeric: true,
    render: (i) => <Amount value={i.commission} />,
  },
  {
    key: 'wtax',
    header: 'Withholding Tax',
    numeric: true,
    render: (i) => <Amount value={i.wtax} />,
  },
  { key: 'base', header: 'Base', numeric: true, render: (i) => <Amount value={i.base} /> },
  { key: 'fee', header: 'Service Fee', numeric: true, render: (i) => <Amount value={i.fee} /> },
];

function lineColumns(
  run: ServiceFeeRun,
  mayTag: boolean,
  mayManage: boolean,
  onAction: (kind: 'release' | 'liquidate' | 'resend', line: ServiceFeeLine) => void,
): Column<ServiceFeeLine>[] {
  return [
    {
      key: 'line',
      header: 'Recipient',
      render: (l) => (
        <>
          <strong>{l.payeeName}</strong>
          <span className="cell-sub">
            {l.segment} · {l.salesUnit} · {l.costCenter ?? 'cost centre by rule'}
          </span>
        </>
      ),
    },
    { key: 'count', header: 'Invoices', numeric: true, render: (l) => l.amounts.invoiceCount },
    {
      key: 'base',
      header: 'Base',
      numeric: true,
      render: (l) => <Amount value={l.amounts.base} />,
    },
    { key: 'rate', header: 'Rate', numeric: true, render: (l) => `${String(l.amounts.rate)}%` },
    {
      key: 'fee',
      header: 'Service Fee',
      numeric: true,
      render: (l) => `${l.amounts.currency} ${formatAmount(l.amounts.fee)}`,
    },
    {
      key: 'payout',
      header: 'Payout',
      render: (l) => (
        <>
          {l.payout.requestNo ?? '—'}
          <span className="cell-sub">
            {[l.payout.dvNo, l.payout.gatewayStatus, l.payout.message].filter(Boolean).join(' · ')}
          </span>
        </>
      ),
    },
    {
      key: 'tags',
      header: 'Released / Liquidated',
      render: (l) => (
        <>
          {l.tags.releasedOn ? formatDate(l.tags.releasedOn) : '—'}
          <span className="cell-sub">
            {l.tags.liquidatedOn ? formatDate(l.tags.liquidatedOn) : ''}
          </span>
        </>
      ),
    },
    { key: 'status', header: 'Status', render: (l) => <StatusBadge status={l.status} /> },
    {
      key: 'actions',
      header: '',
      render: (l) => (
        <span className="frbs-actions">
          {lineActions(l, run.stage)
            .filter((a) => (a === 'resend' ? mayManage : mayTag))
            .map((a) => (
              <Button key={a} size="sm" variant="secondary" onClick={() => onAction(a, l)}>
                {a === 'release' && 'Tag Released'}
                {a === 'liquidate' && 'Tag Liquidated'}
                {a === 'resend' && 'Send Again'}
              </Button>
            ))}
        </span>
      ),
    },
  ];
}

function RunActions({ run, actions }: Readonly<{ run: ServiceFeeRun; actions: WorkAction[] }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<'submit' | 'approve'>();
  const done = async (r: ServiceFeeRun, label: string) => {
    setPending(undefined);
    await queryClient.invalidateQueries({ queryKey: ['frbs'] });
    await queryClient.invalidateQueries({ queryKey: workflowKey(RUN_ENTITY, r.id) });
    toast.success(`${label}: ${r.runNo} is ${humanize(r.stage).toLowerCase()}`);
  };
  const act = useMutation({
    mutationFn: ({ action, note }: { action: 'submit' | 'approve'; note: ActionNote }) =>
      action === 'submit'
        ? frbsApi.submit(run.id, note.comment)
        : frbsApi.approve(run.id, note.comment),
    onSuccess: (r, { action }) => done(r, action === 'submit' ? 'Submitted' : 'Approved'),
  });
  const recompute = useMutation({
    mutationFn: () => frbsApi.recompute(run.id),
    onSuccess: (r) => done(r, 'Recomputed'),
  });
  const offered = actions.map((a) => a.action);
  return (
    <>
      {run.stage === 'COMPUTED' && can('SERVICE_FEE_MANAGE') && (
        <Button
          variant="secondary"
          icon={<RefreshCcw size={16} />}
          busy={recompute.isPending}
          onClick={() => recompute.mutate()}
        >
          Recompute
        </Button>
      )}
      {offered.includes('submit') && (
        <Button onClick={() => setPending('submit')}>Submit for Approval</Button>
      )}
      {offered.includes('approve') && (
        <Button
          variant="accent"
          icon={<CheckCircle2 size={16} />}
          onClick={() => setPending('approve')}
        >
          Approve and Send for Payment
        </Button>
      )}
      <ErrorAlert error={recompute.error} />
      {pending && (
        <ActionDialog
          title={`${pending === 'submit' ? 'Submit for Approval' : 'Approve and Send for Payment'} · ${run.runNo}`}
          confirmLabel={
            pending === 'submit' ? 'Submit for Approval' : 'Approve and Send for Payment'
          }
          busy={act.isPending}
          error={act.error}
          onConfirm={(note) => act.mutate({ action: pending, note })}
          onClose={() => setPending(undefined)}
        />
      )}
    </>
  );
}

function LinesCard({
  run,
  rows,
  loading,
  onAction,
}: Readonly<{
  run: ServiceFeeRun;
  rows: ServiceFeeLine[];
  loading: boolean;
  onAction: (kind: 'release' | 'liquidate' | 'resend', line: ServiceFeeLine) => void;
}>) {
  const { can } = useAuth();
  return (
    <Card flush>
      <DataTable
        caption="Service-fee lines"
        columns={lineColumns(run, can('SERVICE_FEE_TAG'), can('SERVICE_FEE_MANAGE'), onAction)}
        rows={rows}
        rowKey={(l) => l.id}
        loading={loading}
        emptyMessage="No lines"
      />
      <div className="frbs-totals">
        {totalsByCurrency(rows).map((t) => (
          <span key={t.currency}>
            {t.currency}: base <strong>{formatAmount(t.base)}</strong>, service fee{' '}
            <strong>{formatAmount(t.fee)}</strong> on {t.count} invoice(s)
          </span>
        ))}
      </div>
    </Card>
  );
}

function LineDialog({
  pending,
  onClose,
  onDone,
}: Readonly<{ pending: Pending; onClose: () => void; onDone: (message: string) => void }>) {
  if (pending?.kind === 'release') {
    return (
      <ReleaseDialog
        line={pending.line}
        onClose={onClose}
        onDone={(l) => onDone(`Line ${String(l.lineNo)} tagged released`)}
      />
    );
  }
  if (pending?.kind === 'liquidate') {
    return (
      <LiquidateDialog
        line={pending.line}
        onClose={onClose}
        onDone={(l) => onDone(`Line ${String(l.lineNo)} tagged liquidated`)}
      />
    );
  }
  return null;
}

/**
 * One service-fee run (FRBS 2.10.0-2.10.2): header with the run number and status, the summary,
 * the workflow panel (submit, approve, recompute; return and cancel from the panel), and tabs for
 * the lines - accrual, payout request, release and liquidation tags - and the invoices counted.
 */
export default function ServiceFeeRunPage() {
  const id = Number(useParams().id);
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('lines');
  const [pending, setPending] = useState<Pending>();
  const run = useQuery({ queryKey: ['frbs', 'run', id], queryFn: () => frbsApi.run(id) });
  const lines = useQuery({
    queryKey: ['frbs', 'run', id, 'lines'],
    queryFn: () => frbsApi.lines(id),
  });
  const invoices = useQuery({
    queryKey: ['frbs', 'run', id, 'invoices'],
    queryFn: () => frbsApi.invoices(id),
    enabled: tab === 'invoices',
  });
  const refresh = async (message: string) => {
    setPending(undefined);
    toast.success(message);
    await queryClient.invalidateQueries({ queryKey: ['frbs'] });
    await queryClient.invalidateQueries({ queryKey: workflowKey(RUN_ENTITY, id) });
  };
  const resend = useMutation({
    mutationFn: (line: ServiceFeeLine) => frbsApi.resend(line.id),
    onSuccess: (l) => refresh(`Line ${String(l.lineNo)} sent to Disbursement again`),
  });
  if (run.data === undefined) {
    return run.error ? (
      <ErrorAlert error={run.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const r = run.data;
  const rows = lines.data ?? [];
  const progress = tagProgress(rows);
  return (
    <div className="stack">
      <PageHeader
        backTo="/frbs/service-fee"
        section="Finance · Accounting Reports"
        title={r.runNo}
        description={`Service fee of the invoices fully paid from ${formatDate(r.periodFrom)} to ${formatDate(r.periodTo)}`}
      />
      <RecordSummary
        title={`Service Fee ${formatDate(r.periodFrom)} – ${formatDate(r.periodTo)}`}
        chips={
          <>
            <ReferenceChip label="Run" value={r.runNo} />
            <StatusBadge status={r.stage} />
          </>
        }
        facts={[
          { icon: FileText, label: 'Invoices', value: String(r.invoiceCount) },
          { icon: Banknote, label: 'Service Fee', value: formatAmount(r.feeTotal) },
          { icon: User, label: 'Computed By', value: r.createdBy },
          { icon: CheckCircle2, label: 'Approved By', value: r.approvedBy ?? '—' },
          {
            icon: CalendarDays,
            label: 'Released / Liquidated',
            value: `${String(progress.released)} / ${String(progress.liquidated)} of ${String(progress.paid)} lines`,
          },
        ]}
      />
      <WorkflowPanel
        entityType={RUN_ENTITY}
        entityId={r.id}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['frbs'] })}
        renderBusinessActions={(actions) => <RunActions run={r} actions={actions} />}
      />
      <ErrorAlert error={lines.error ?? resend.error} />
      <Tabs<TabId>
        tabs={[
          { id: 'lines', label: `Lines (${String(rows.length)})` },
          { id: 'invoices', label: `Invoices (${String(r.invoiceCount)})` },
        ]}
        active={tab}
        onChange={setTab}
      />
      {tab === 'lines' && (
        <LinesCard
          run={r}
          rows={rows}
          loading={lines.isLoading}
          onAction={(kind, line) =>
            kind === 'resend' ? resend.mutate(line) : setPending({ kind, line })
          }
        />
      )}
      {tab === 'invoices' && (
        <Card flush>
          <DataTable
            caption="Invoices of the run"
            columns={INVOICE_COLUMNS}
            rows={invoices.data ?? []}
            rowKey={(i) => i.invoiceNo}
            loading={invoices.isLoading}
          />
        </Card>
      )}
      <LineDialog
        pending={pending}
        onClose={() => setPending(undefined)}
        onDone={(message) => void refresh(message)}
      />
    </div>
  );
}
