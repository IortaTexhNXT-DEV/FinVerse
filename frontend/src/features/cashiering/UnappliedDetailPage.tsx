import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Banknote, CalendarDays, FileText, Scale, User, Users } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import type { WorkAction } from '@/api/workflow';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { cashieringApi } from './cashieringApi';
import type { Disposition, DispositionBody, UnappliedItem } from './cashieringApi';
import { DispositionForm } from './DispositionForm';

const ENTITY = 'Unapplied';

const HISTORY: Column<Disposition>[] = [
  { key: 'type', header: 'Disposition', render: (d) => humanize(d.dispositionType) },
  { key: 'amount', header: 'Amount', numeric: true, render: (d) => <Amount value={d.amount} /> },
  {
    key: 'target',
    header: 'Target',
    render: (d) => d.targetInvoiceNo ?? d.targetClientCode ?? d.targetUnit ?? d.payeeName ?? '',
  },
  {
    key: 'req',
    header: 'Requested',
    render: (d) => `${d.requestedBy} · ${formatDateTime(d.requestedAt)}`,
  },
  {
    key: 'appr',
    header: 'Approved',
    render: (d) => (d.approvedBy ? `${d.approvedBy} · ${formatDateTime(d.approvedAt)}` : ''),
  },
  {
    key: 'doc',
    header: 'Document',
    render: (d) => d.disbursementRequestNo ?? d.journalBatchNo ?? '',
  },
  { key: 'status', header: 'Status', render: (d) => <StatusBadge status={d.status} /> },
];

/** Business actions of the workflow run through the Cashiering endpoints. */
const HANDLED = ['submit', 'approve', 'withdraw', 'mark_reversal', 'approve_reversal'];

function facts(u: UnappliedItem) {
  return [
    { icon: Banknote, label: 'Amount', value: <Amount value={u.amount} /> },
    { icon: Scale, label: 'Balance', value: <Amount value={u.balance} /> },
    { icon: User, label: 'Payor', value: u.payorName ?? '' },
    {
      icon: Users,
      label: 'Client / Unit',
      value: [u.clientCode, u.salesUnit].filter(Boolean).join(' · '),
    },
    {
      icon: FileText,
      label: 'Source',
      value: u.sourceRef ? `${humanize(u.sourceModule ?? '')} ${u.sourceRef}` : humanize(u.origin),
    },
    { icon: CalendarDays, label: 'Received', value: formatDate(u.createdAt) },
  ];
}

/**
 * Unapplied item page (CSHID.024/025): the payment, its disposition (assigned, updated,
 * submitted, approved, reversed) under workflow OPS_DISPOSITION, and the disposition history.
 */
export default function UnappliedDetailPage() {
  const id = Number(useParams().id);
  const toast = useToast();
  const queryClient = useQueryClient();
  const [reversing, setReversing] = useState(false);
  const item = useQuery({
    queryKey: ['cashiering', 'unapplied-item', id],
    queryFn: () => cashieringApi.unappliedItem(id),
    enabled: id > 0,
  });
  const history = useQuery({
    queryKey: ['cashiering', 'dispositions', id],
    queryFn: () => cashieringApi.dispositions(id),
    enabled: id > 0,
  });
  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
    await queryClient.invalidateQueries({ queryKey: ['workflow'] });
  };
  const act = useMutation({
    mutationFn: (fn: () => Promise<unknown>) => fn(),
    onSuccess: async () => {
      setReversing(false);
      toast.success('Disposition updated');
      await refresh();
    },
  });
  const u = item.data;
  if (u === undefined) {
    return <ErrorAlert error={item.error} />;
  }
  const save = (body: DispositionBody) =>
    act.mutate(() =>
      u.stage === 'UNAPPLIED'
        ? cashieringApi.assign(id, body)
        : cashieringApi.updateDisposition(id, body),
    );
  const run = (a: WorkAction) => {
    const calls: Record<string, () => Promise<unknown>> = {
      submit: () => cashieringApi.submit(id),
      approve: () => cashieringApi.approve(id),
      withdraw: () => cashieringApi.withdraw(id),
      approve_reversal: () => cashieringApi.approveReversal(id),
    };
    const call = calls[a.action];
    if (a.action === 'mark_reversal') {
      setReversing(true);
    } else if (call !== undefined) {
      act.mutate(call);
    }
  };
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title={`Unapplied Payment ${u.reference}`}
        description="An unapplied payment and its disposition."
        backTo="/cashiering/unapplied"
        actions={
          u.receiptId !== undefined && (
            <Link className="btn btn-secondary" to={`/cashiering/receipts/${u.receiptId}`}>
              Open Receipt
            </Link>
          )
        }
      />
      <ErrorAlert error={act.error} />
      <RecordSummary
        title={u.payorName ?? u.reference}
        chips={
          <>
            <ReferenceChip value={u.reference} />
            <StatusBadge status={u.stage} />
          </>
        }
        flags={<span className="tag">{humanize(u.origin)}</span>}
        facts={facts(u)}
      />
      <WorkflowPanel
        entityType={ENTITY}
        entityId={u.id}
        onChanged={() => void refresh()}
        renderBusinessActions={(actions) =>
          actions
            .filter((a) => HANDLED.includes(a.action))
            .map((a) => (
              <Button
                key={a.action}
                size="sm"
                variant={a.action.startsWith('approve') ? 'accent' : 'secondary'}
                busy={act.isPending}
                onClick={() => run(a)}
              >
                {a.label}
              </Button>
            ))
        }
      />
      {(u.stage === 'UNAPPLIED' || u.stage === 'MONITORING') && (
        <DispositionForm key={u.current?.id ?? 0} item={u} busy={act.isPending} onSave={save} />
      )}
      <Card title="Disposition History" flush>
        <DataTable
          caption="Disposition history"
          columns={HISTORY}
          rows={history.data ?? []}
          rowKey={(d) => d.id}
          loading={history.isLoading}
          emptyMessage="No disposition yet"
        />
      </Card>
      {reversing && (
        <ActionDialog
          title={`Mark ${u.reference} for Reversal`}
          reasonLov="RETURN_REASON"
          confirmLabel="Mark for Reversal"
          busy={act.isPending}
          error={act.error}
          onConfirm={(note) =>
            act.mutate(() =>
              cashieringApi.markReversal(
                id,
                [humanize(note.reasonCode ?? 'REVERSAL'), note.comment?.trim()]
                  .filter(Boolean)
                  .join(': '),
              ),
            )
          }
          onClose={() => setReversing(false)}
        />
      )}
    </div>
  );
}
