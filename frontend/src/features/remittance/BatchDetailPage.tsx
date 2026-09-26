import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Building2,
  FileSpreadsheet,
  FileText,
  ListChecks,
  Mail,
  ReceiptText,
  UserRound,
  Wallet,
} from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import type { WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { workflowKey } from '@/components/broking/workflowKey';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDateTime, humanize } from '@/utils/format';
import { remittanceApi } from './api';
import type { Batch, DocumentKind } from './api';
import { ApproveDialog, PreviewDialog, SendScheduleDialog } from './BatchDialogs';
import { BatchLinesTab } from './BatchLinesTab';
import { BatchSettlementTab } from './BatchSettlementTab';
import { TotalsStrip, TypeChip } from './RemittanceParts';
import { joinParts } from './remittanceLabels';
import './remittance.css';

const ENTITY = 'RemittanceBatch';
const TABS = [
  { id: 'lines', label: 'Accounts' },
  { id: 'settlement', label: 'Settlement' },
  { id: 'documents', label: 'Documents and Receipts' },
] as const;
type TabId = (typeof TABS)[number]['id'];
type Dialog = 'submit' | 'approve' | 'return' | 'send';
const SENDABLE = ['APPROVED', 'PARTIALLY_REMITTED', 'FULLY_REMITTED', 'OR_RECEIVED'];

function disbursementText(batch: Batch): string {
  const d = batch.disbursement;
  if (d.requestNo === undefined) {
    return d.status === 'NOT_REQUIRED' ? 'Settled by deductions' : 'Not sent';
  }
  const dv = batch.summary.dvNo === undefined ? undefined : `DV ${batch.summary.dvNo}`;
  return joinParts([d.requestNo, humanize(d.status ?? ''), dv]);
}

/** Which batch actions the user may take now. */
function permissionsOf(batch: Batch, can: (permission: string) => boolean) {
  const stage = batch.summary.stage;
  return {
    editable: (stage === 'REVIEW_IN_PROCESS' || stage === 'ON_HOLD') && can('REMIT_EXCLUDE'),
    canSend: SENDABLE.includes(stage) && batch.scheduleSentAt === undefined && can('REMIT_PROCESS'),
  };
}

function Summary({ batch }: Readonly<{ batch: Batch }>) {
  const s = batch.summary;
  const r = batch.receipts;
  return (
    <RecordSummary
      title={batch.insurerName}
      chips={
        <>
          <ReferenceChip label="Batch" value={s.batchNo} />
          <StatusBadge status={s.stage} />
        </>
      }
      flags={
        <>
          <TypeChip type={s.type} />
          {s.specialRequestNo !== undefined && <span className="tag">{s.specialRequestNo}</span>}
        </>
      }
      facts={[
        { icon: Building2, label: 'Insurer', value: `${s.insurerCode} · ${s.currency}` },
        { icon: UserRound, label: 'Processor', value: s.processor ?? 'Unassigned' },
        { icon: ListChecks, label: 'Accounts', value: `${s.lineCount} of ${batch.lines.length}` },
        {
          icon: Wallet,
          label: 'Disbursement',
          value: disbursementText(batch),
        },
        {
          icon: ReceiptText,
          label: 'Commission / Incentive OR',
          value: `${r.commissionOrNo ?? humanize(r.commissionOrStatus ?? 'NONE')} / ${r.incentiveOrNo ?? humanize(r.incentiveOrStatus ?? 'NONE')}`,
        },
      ]}
    />
  );
}

const DOCUMENTS: { kind: DocumentKind; label: string; icon: typeof FileText }[] = [
  { kind: 'SCHEDULE_PDF', label: 'Remittance Schedule (PDF)', icon: FileText },
  { kind: 'SCHEDULE_XLSX', label: 'Remittance Schedule (Excel)', icon: FileSpreadsheet },
  { kind: 'PAYMENT_REQUEST_PDF', label: 'Payment Request (PDF)', icon: FileText },
];

function DocumentsTab({ batch }: Readonly<{ batch: Batch }>) {
  const download = useFileDownload();
  const r = batch.receipts;
  return (
    <Card title="Documents and Receipts">
      <div className="stack">
        <ErrorAlert error={download.error} />
        <div className="row">
          {DOCUMENTS.map((d) => (
            <Button
              key={d.kind}
              variant="secondary"
              icon={<d.icon size={16} />}
              busy={download.isPending}
              onClick={() =>
                download.mutate(() => remittanceApi.document(batch.summary.id, d.kind))
              }
            >
              {d.label}
            </Button>
          ))}
        </div>
        <dl className="detail-list">
          <dt>Submitted by</dt>
          <dd>{batch.submittedBy ?? '—'}</dd>
          <dt>Approved by</dt>
          <dd>{batch.approvedBy ?? '—'}</dd>
          <dt>Payment request</dt>
          <dd>
            {batch.disbursement.requestNo ?? '—'} {formatAmount(batch.disbursement.amount)}
          </dd>
          <dt>ORs</dt>
          <dd>{r.message ?? '—'}</dd>
          <dt>Schedule sent to insurer</dt>
          <dd>{batch.scheduleSentAt ? formatDateTime(batch.scheduleSentAt) : 'Not sent'}</dd>
          <dt>Return reason</dt>
          <dd>{batch.returnReason ?? '—'}</dd>
        </dl>
      </div>
    </Card>
  );
}

const LABELS: Record<string, string> = {
  submit: 'Preview and Submit',
  approve: 'Approve and Push',
  return: 'Return Batch',
};

function BusinessActions({
  actions,
  onOpen,
}: Readonly<{ actions: WorkAction[]; onOpen: (dialog: Dialog) => void }>) {
  return (
    <>
      {actions
        .filter((a) => LABELS[a.action] !== undefined)
        .map((a) => (
          <Button
            key={a.action}
            size="sm"
            variant={a.action === 'return' ? 'secondary' : 'primary'}
            onClick={() => onOpen(a.action as Dialog)}
          >
            {LABELS[a.action]}
          </Button>
        ))}
    </>
  );
}

/** The batch actions, each refreshing the batch, its workflow and the lists. */
function useBatchActions(id: number, done: () => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const after = async (b: Batch, message: string) => {
    done();
    queryClient.setQueryData(['remittance', 'batch', id], b);
    await queryClient.invalidateQueries({ queryKey: workflowKey(ENTITY, id) });
    await queryClient.invalidateQueries({ queryKey: ['remittance'] });
    toast.success(message);
  };
  return {
    submit: useMutation({
      mutationFn: (comment?: string) => remittanceApi.submit(id, comment),
      onSuccess: (b) => after(b, `${b.summary.batchNo} submitted for approval`),
    }),
    approve: useMutation({
      mutationFn: (comment?: string) => remittanceApi.approve(id, comment),
      onSuccess: (b) => after(b, `${b.summary.batchNo} approved and sent to Disbursement`),
    }),
    returnBatch: useMutation({
      mutationFn: (v: { reason: string; comment?: string }) =>
        remittanceApi.returnBatch(id, v.reason, v.comment),
      onSuccess: (b) => after(b, `${b.summary.batchNo} returned`),
    }),
    send: useMutation({
      mutationFn: (mail: { to: string[]; subject: string; body: string }) =>
        remittanceApi.sendSchedule(id, mail),
      onSuccess: async () => {
        done();
        await queryClient.invalidateQueries({ queryKey: ['remittance', 'batch', id] });
        toast.success('Schedule sent to the insurer');
      },
    }),
  };
}

/**
 * Remittance batch (RMTID.002/009-011/019/029/036): the insurer, type, processor and Disbursement
 * facts, the Process Remittance workflow with its history, the totals strip, the accounts with
 * exclusions and restore, and the schedule and payment request documents.
 */
export default function BatchDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('lines');
  const [dialog, setDialog] = useState<Dialog>();
  const batch = useQuery({
    queryKey: ['remittance', 'batch', id],
    queryFn: () => remittanceApi.batch(id),
  });
  const actions = useBatchActions(id, () => setDialog(undefined));
  if (batch.data === undefined) {
    return batch.error ? (
      <ErrorAlert error={batch.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const b = batch.data;
  const s = b.summary;
  const { editable, canSend } = permissionsOf(b, can);
  return (
    <div className="stack">
      <PageHeader
        section="Remittance · Batches"
        backTo="/remittance/batches"
        title={s.batchNo}
        description={`${b.insurerName} · ${s.lineCount} account(s) · extracted ${formatDateTime(s.createdAt)}`}
        actions={
          canSend ? (
            <Button variant="secondary" icon={<Mail size={16} />} onClick={() => setDialog('send')}>
              Send Schedule to Insurer
            </Button>
          ) : undefined
        }
      />
      <Summary batch={b} />
      <WorkflowPanel
        entityType={ENTITY}
        entityId={id}
        renderBusinessActions={(available) => (
          <BusinessActions actions={available} onOpen={setDialog} />
        )}
        onChanged={() =>
          void queryClient.invalidateQueries({ queryKey: ['remittance', 'batch', id] })
        }
      />
      <TotalsStrip totals={s.totals} currency={s.currency} settlement={b.settlement} />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'lines' && (
        <BatchLinesTab
          batch={b}
          editable={editable}
          onChanged={(next) => queryClient.setQueryData(['remittance', 'batch', id], next)}
        />
      )}
      {tab === 'settlement' && <BatchSettlementTab batch={b} />}
      {tab === 'documents' && <DocumentsTab batch={b} />}
      {dialog === 'submit' && (
        <PreviewDialog
          batch={b}
          busy={actions.submit.isPending}
          error={actions.submit.error}
          onClose={() => setDialog(undefined)}
          onSubmit={(comment) => actions.submit.mutate(comment)}
        />
      )}
      {dialog === 'approve' && (
        <ApproveDialog
          batch={b}
          busy={actions.approve.isPending}
          error={actions.approve.error}
          onClose={() => setDialog(undefined)}
          onApprove={(comment) => actions.approve.mutate(comment)}
        />
      )}
      {dialog === 'return' && (
        <ActionDialog
          title={`Return ${s.batchNo}`}
          reasonLov="REMIT_RETURN_REASON"
          confirmLabel="Return Batch"
          busy={actions.returnBatch.isPending}
          error={actions.returnBatch.error}
          onClose={() => setDialog(undefined)}
          onConfirm={(note) =>
            actions.returnBatch.mutate({ reason: note.reasonCode ?? '', comment: note.comment })
          }
        />
      )}
      {dialog === 'send' && (
        <SendScheduleDialog
          batch={b}
          busy={actions.send.isPending}
          error={actions.send.error}
          onClose={() => setDialog(undefined)}
          onSend={(mail) => actions.send.mutate(mail)}
        />
      )}
    </div>
  );
}
