import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { LoadingPanel } from '@/components/ui/LoadingPanel';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { recordActions } from './recordLogic';
import { recordsApi } from './recordsApi';
import type { ReceiptRecord, RecordAccountLine } from './recordsApi';
import './cashiering.css';

interface HistoryEntry {
  at: string;
  user?: string;
  action: string;
  summary: string;
}

const ACCOUNT_COLUMNS: Column<RecordAccountLine>[] = [
  { key: 'ref', header: 'Account Number / Invoice Number', render: (a) => a.reference },
  { key: 'amount', header: 'Amount', numeric: true, render: (a) => <Amount value={a.amount} /> },
];

const HISTORY_COLUMNS: Column<HistoryEntry>[] = [
  { key: 'at', header: 'Date and Time', render: (h) => formatDateTime(h.at) },
  { key: 'user', header: 'User', render: (h) => h.user ?? '' },
  {
    key: 'action',
    header: 'Activity',
    render: (h) => h.action.charAt(0) + h.action.slice(1).toLowerCase(),
  },
  { key: 'summary', header: 'Details', render: (h) => h.summary },
];

const POST_TEXT: Record<string, string> = {
  CREATION:
    'AR/OR will be issued. Payments will be applied. Any excess payments will be reflected in the Unapplied List. Do you wish to continue or return to creator?',
  CANCELLATION:
    'AR/OR will be cancelled. Payments will be cancelled once cancellation is posted. Do you wish to continue or return to creator?',
  REINSTATEMENT:
    'AR/OR will be reinstated. Payments will be reinstated once posted. Do you wish to continue?',
};

const TENDER_LABELS: Record<string, string> = {
  CASH: 'Cash',
  CHECK: 'Check',
  DIRECT_CREDIT: 'Direct Credit',
};
const REINSTATEMENT_LABELS: Record<string, string> = {
  FULL: 'Full Reinstatement',
  PARTIAL: 'Partial Reinstatement',
};

function checkText(r: ReceiptRecord): string | undefined {
  const check = r.tender?.check;
  if (!check?.checkNo) {
    return undefined;
  }
  return [check.checkBank, check.checkNo, formatDate(check.checkDate)].filter(Boolean).join(' ');
}

function reasonText(r: ReceiptRecord): string | undefined {
  if (!r.reasonLabel) {
    return undefined;
  }
  const extra = r.reason?.reasonText;
  return extra ? `${r.reasonLabel}: ${extra}` : r.reasonLabel;
}

function byWhom(name: string | undefined, at: string | undefined): string | undefined {
  return name ? `${name} · ${formatDateTime(at)}` : undefined;
}

function details(r: ReceiptRecord) {
  const p = r.party ?? {};
  const t = r.tender ?? {};
  return [
    { label: 'Record Number', value: r.recordNo },
    {
      label: 'Receipt Type',
      value: r.receiptKind === 'AR' ? 'Acknowledgement Receipt' : 'Official Receipt',
    },
    { label: 'AR / OR Type', value: r.receiptTypeLabel ?? r.receiptType },
    { label: 'Receipt Number', value: r.receiptNo ?? 'To be generated once posted' },
    { label: 'Receipting Branch', value: r.branchName },
    { label: 'Payor Name', value: p.payorName },
    { label: 'Client', value: p.clientName },
    { label: 'Insurer', value: p.insurerName },
    { label: 'Payment Type', value: TENDER_LABELS[t.tenderType ?? 'CASH'] },
    { label: 'Post to Bank Account', value: r.bankAccountName },
    { label: 'Paid Amount', value: <Amount value={r.total} /> },
    { label: 'Currency', value: t.currency },
    { label: 'Check', value: checkText(r) },
    { label: 'Reason', value: reasonText(r) },
    {
      label: 'Type of Reinstatement',
      value: REINSTATEMENT_LABELS[r.reason?.reinstatementType ?? ''],
    },
    { label: 'Unit Head', value: r.reason?.unitHead },
    { label: 'Remarks', value: t.remarks, wide: true },
    { label: 'Returned', value: r.returnReason, wide: true },
    { label: 'Last posting attempt', value: r.postingResult, wide: true },
    { label: 'Created by', value: byWhom(r.createdBy, r.createdAt) },
    { label: 'Posted by', value: byWhom(r.postedBy, r.postedAt) },
  ];
}

function doneMessage(action: string, r: ReceiptRecord): string {
  if (action === 'submit') {
    return 'Record is now submitted for Posting';
  }
  const receipt = r.receiptNo ? ` (${r.receiptNo})` : '';
  return `${r.recordNo}: ${r.statusLabel}${receipt}`;
}

type Dialog = 'submit' | 'cancel' | 'post' | 'return';

function HeaderActions({
  record,
  allowed,
  onEdit,
  onDialog,
}: Readonly<{
  record: ReceiptRecord;
  allowed: ReturnType<typeof recordActions>;
  onEdit: () => void;
  onDialog: (d: Dialog) => void;
}>) {
  return (
    <>
      <StatusBadge status={record.stage} label={record.statusLabel} />
      {allowed.edit && record.recordKind === 'CREATION' && (
        <Button variant="secondary" onClick={onEdit}>
          Edit
        </Button>
      )}
      {allowed.cancel && (
        <Button variant="secondary" onClick={() => onDialog('cancel')}>
          Cancel Record
        </Button>
      )}
      {allowed.submit && (
        <Button variant="accent" onClick={() => onDialog('submit')}>
          Submit for Posting
        </Button>
      )}
      {allowed.post && (
        <Button variant="secondary" onClick={() => onDialog('return')}>
          Return
        </Button>
      )}
      {allowed.post && (
        <Button variant="accent" onClick={() => onDialog('post')}>
          Post
        </Button>
      )}
    </>
  );
}

/**
 * A creation, cancellation or reinstatement record (FRS.CSH.02.01.10 to 02.05.07): its details,
 * accounts and history, with the actions of the user: edit, submit for posting and cancel for the
 * creator; post or return for the Approver/Poster, who is never the creator.
 */
export default function RecordDetailPage() {
  const { id } = useParams();
  const recordId = Number(id);
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { user, can } = useAuth();
  const [dialog, setDialog] = useState<Dialog | null>(null);
  const record = useQuery({
    queryKey: ['cashiering', 'record', recordId],
    queryFn: () => recordsApi.get(recordId),
  });
  const history = useQuery({
    queryKey: ['cashiering', 'record', recordId, 'history'],
    queryFn: () => api.get<HistoryEntry[]>(`/cashiering/records/${recordId}/history`),
  });
  const done = async (message: string) => {
    toast.success(message);
    setDialog(null);
    await queryClient.invalidateQueries({ queryKey: ['cashiering'] });
  };
  const act = useMutation({
    mutationFn: async ({ action, reason }: { action: string; reason?: string }) => {
      if (action === 'submit') return recordsApi.submit(recordId);
      if (action === 'cancel') return recordsApi.cancelRecord(recordId);
      if (action === 'return') return recordsApi.returnToCreator(recordId, reason ?? '');
      const outcome = (await recordsApi.post([recordId]))[0];
      if (!outcome?.posted) throw new Error(outcome?.message ?? 'The record was not posted');
      return recordsApi.get(recordId);
    },
    onSuccess: (r, v) => done(doneMessage(v.action, r)),
  });
  if (!record.data) {
    return <>{record.isLoading ? <LoadingPanel /> : <ErrorAlert error={record.error} />}</>;
  }
  const r = record.data;
  const allowed = recordActions(r, user?.username, can);
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title={r.recordNo}
        description={`${r.receiptKind} ${r.recordKind.toLowerCase()} record`}
        backTo="/cashiering/posting"
        actions={
          <HeaderActions
            record={r}
            allowed={allowed}
            onEdit={() => void navigate(`/cashiering/records/${r.id}/edit`)}
            onDialog={setDialog}
          />
        }
      />
      <ErrorAlert error={act.error} />
      <Card title="Record">
        <DefinitionGrid items={details(r)} columns={2} />
      </Card>
      <Card title="Accounts">
        <DataTable
          caption="Accounts of the record"
          columns={ACCOUNT_COLUMNS}
          rows={r.accounts}
          rowKey={(a) => a.reference}
          emptyMessage="No account on the record"
        />
      </Card>
      <Card title="History">
        <DataTable
          caption="History of the record"
          columns={HISTORY_COLUMNS}
          rows={history.data ?? []}
          rowKey={(h) => `${h.at}-${h.action}`}
          emptyMessage="No activity yet"
        />
      </Card>
      {dialog === 'submit' && (
        <ConfirmDialog
          title="Submit for Posting"
          record={r.recordNo}
          effect="The record goes to the Approver/Poster for posting."
          confirmLabel="Submit"
          busy={act.isPending}
          onConfirm={() => act.mutate({ action: 'submit' })}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog === 'cancel' && (
        <ConfirmDialog
          title="Cancel Record"
          record={r.recordNo}
          effect="Record will be Cancelled. Do you wish to continue?"
          confirmLabel="Yes"
          destructive
          busy={act.isPending}
          onConfirm={() => act.mutate({ action: 'cancel' })}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog === 'post' && (
        <ConfirmDialog
          title="Post"
          record={r.recordNo}
          effect={POST_TEXT[r.recordKind]}
          confirmLabel="Yes"
          busy={act.isPending}
          onConfirm={() => act.mutate({ action: 'post' })}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog === 'return' && (
        <ConfirmDialog
          title="Return to Creator"
          record={r.recordNo}
          effect="The record goes back to its creator with the reason."
          confirmLabel="Return"
          reason="required"
          busy={act.isPending}
          onConfirm={(reason) => act.mutate({ action: 'return', reason })}
          onClose={() => setDialog(null)}
        />
      )}
    </div>
  );
}
