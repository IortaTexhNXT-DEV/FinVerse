import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Banknote, Building2, FileDown, FileText, Landmark, UserRound } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import type { WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { SentMessages } from '@/components/broking/SentMessages';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, humanize } from '@/utils/format';
import { disbursementApi } from './api';
import type { Voucher } from './api';
import { InstrumentTab } from './InstrumentTab';
import { MODE_LABELS } from './labels';
import { ProformaTab } from './ProformaTab';
import { TagsTab } from './TagsTab';
import { VoucherDetailsTab } from './VoucherDetailsTab';
import { CommentDialog } from './VoucherDialogs';
import './disbursement.css';

const ENTITY = 'DisbursementVoucher';
const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'entry', label: 'Entry' },
  { id: 'instrument', label: 'Instrument' },
  { id: 'tags', label: 'OR / AR and CWT' },
  { id: 'documents', label: 'Documents' },
  { id: 'emails', label: 'E-mails' },
] as const;
type TabId = (typeof TABS)[number]['id'];

/** Business actions of workflow DISB_VOUCHER with their button label. */
const LABELS: Record<string, string> = {
  submit: 'Submit for Review',
  route_to_approver: 'Route to Approver',
  submit_for_approval: 'Submit for Approval',
  approve: 'Approve and Post',
  reject: 'Reject',
  cancel: 'Cancel Voucher',
};
const API_ACTION: Record<string, string> = {
  submit: 'submit',
  route_to_approver: 'route',
  submit_for_approval: 'submit-for-approval',
  approve: 'approve',
};

function Summary({ voucher }: Readonly<{ voucher: Voucher }>) {
  const s = voucher.summary;
  return (
    <RecordSummary
      title={s.payeeName}
      chips={
        <>
          <ReferenceChip label="DV" value={s.dvNo} />
          <StatusBadge status={s.stage} />
        </>
      }
      flags={
        <>
          {s.autoCreated && <span className="tag">Automatic</span>}
          {s.proformaEdited && <span className="tag">Entry Edited</span>}
          {s.postingStatus !== 'NOT_POSTED' && (
            <span className="tag">{humanize(s.postingStatus)}</span>
          )}
        </>
      }
      facts={[
        { icon: UserRound, label: 'Payee', value: s.payeeCode },
        { icon: FileText, label: 'Type', value: humanize(s.disbursementType) },
        { icon: Landmark, label: 'Mode', value: s.mode ? MODE_LABELS[s.mode] : '—' },
        { icon: Banknote, label: 'Net Amount', value: `${s.currency} ${formatAmount(s.net)}` },
        { icon: Building2, label: 'Paying Account', value: voucher.bankAccount ?? '—' },
      ]}
    />
  );
}

interface Dialog {
  action: string;
  reason: boolean;
}

/** The confirmation of a business action, with the mandatory reason of reject and cancel. */
function VoucherActionDialog({
  dialog,
  voucher,
  onClose,
  onDone,
}: Readonly<{
  dialog: Dialog;
  voucher: Voucher;
  onClose: () => void;
  onDone: (v: Voucher) => void;
}>) {
  const toast = useToast();
  const s = voucher.summary;
  const label = LABELS[dialog.action] ?? dialog.action;
  const act = useMutation({
    mutationFn: ({ reason, comment }: { reason?: string; comment?: string }) =>
      reason === undefined
        ? disbursementApi.act(s.id, API_ACTION[dialog.action] ?? dialog.action, comment)
        : disbursementApi.withReason(s.id, dialog.action as 'reject' | 'cancel', reason, comment),
    onSuccess: (v) => {
      toast.success(`${v.summary.dvNo}: ${label} done`);
      onDone(v);
    },
  });
  if (dialog.reason) {
    return (
      <ActionDialog
        title={`${label} ${s.dvNo}`}
        reasonLov={dialog.action === 'cancel' ? 'DISB_CANCEL_REASON' : 'DISB_RETURN_REASON'}
        confirmLabel={label}
        busy={act.isPending}
        error={act.error}
        onClose={onClose}
        onConfirm={(note) => act.mutate({ reason: note.reasonCode ?? '', comment: note.comment })}
      />
    );
  }
  return (
    <CommentDialog
      title={`${label} ${s.dvNo}`}
      label={label}
      intro={
        dialog.action === 'approve'
          ? `The entry of ${formatAmount(s.gross)} ${s.currency} is posted and the instrument issued.`
          : undefined
      }
      busy={act.isPending}
      error={act.error}
      onClose={onClose}
      onConfirm={(comment) => act.mutate({ comment })}
    />
  );
}

/** The content of the selected tab of the voucher. */
function VoucherTab({
  tab,
  voucher,
  onSaved,
}: Readonly<{ tab: TabId; voucher: Voucher; onSaved: (v?: Voucher) => void }>) {
  const { can } = useAuth();
  const s = voucher.summary;
  const editable = s.stage === 'IN_PROCESS' && can('DISB_PROCESS');
  switch (tab) {
    case 'details':
      return (
        <VoucherDetailsTab
          key={`d-${s.stage}`}
          voucher={voucher}
          editable={editable}
          onSaved={onSaved}
        />
      );
    case 'entry':
      return (
        <ProformaTab
          key={`e-${s.stage}-${voucher.lines.length}`}
          voucher={voucher}
          editable={editable}
          onSaved={onSaved}
        />
      );
    case 'instrument':
      return <InstrumentTab voucher={voucher} onChanged={onSaved} />;
    case 'tags':
      return <TagsTab voucher={voucher} onSaved={onSaved} />;
    case 'documents':
      return (
        <Card title="Supporting Documents">
          <Attachments entityType={ENTITY} entityId={s.id} reference={s.dvNo} />
        </Card>
      );
    default:
      return (
        <Card title="E-mails Sent">
          <SentMessages entityType={ENTITY} entityId={s.id} />
        </Card>
      );
  }
}

function Notices({ voucher }: Readonly<{ voucher: Voucher }>) {
  return (
    <>
      {voucher.missing.length > 0 && (
        <p className="dsb-missing" role="status">
          To complete before submitting: {voucher.missing.join(', ')}.
        </p>
      )}
      {voucher.postingError !== undefined && <ErrorAlert error={new Error(voucher.postingError)} />}
    </>
  );
}

/**
 * Disbursement voucher (DIS 2.7.3-2.7.12, 2.8-2.21): the payee, type, mode and amounts, the
 * DISB_VOUCHER workflow with its business actions (submit, route, submit for approval, approve and
 * post, reject, cancel), and tabs for the details, the proforma entry, the instrument, the OR / AR
 * and CWT tags, the supporting documents and the e-mails sent.
 */
export default function VoucherPage() {
  const id = Number(useParams().id);
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [tab, setTab] = useState<TabId>('details');
  const [dialog, setDialog] = useState<Dialog>();
  const key = ['disbursement', 'voucher', id];
  const voucher = useQuery({ queryKey: key, queryFn: () => disbursementApi.voucher(id) });
  const refresh = async (v?: Voucher) => {
    if (v === undefined) {
      await queryClient.invalidateQueries({ queryKey: key });
    } else {
      queryClient.setQueryData(key, v);
    }
    await queryClient.invalidateQueries({ queryKey: workflowKey(ENTITY, id) });
    await queryClient.invalidateQueries({ queryKey: ['disbursement', 'summary'] });
  };
  const v = voucher.data;
  if (v === undefined) {
    return voucher.error ? (
      <ErrorAlert error={voucher.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const s = v.summary;
  const businessActions = (available: WorkAction[]) =>
    available
      .filter((a) => LABELS[a.action] !== undefined)
      .map((a) => (
        <Button
          key={a.action}
          size="sm"
          variant={a.action === 'cancel' || a.action === 'reject' ? 'secondary' : 'primary'}
          onClick={() => setDialog({ action: a.action, reason: a.reasonLov !== undefined })}
        >
          {LABELS[a.action]}
        </Button>
      ));
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        backTo="/disbursement"
        title={s.dvNo}
        description={`${humanize(s.disbursementType)} to ${s.payeeName} · request ${v.request.requestNo}`}
        actions={
          <Button
            variant="secondary"
            icon={<FileDown size={16} />}
            busy={download.isPending}
            onClick={() => download.mutate(() => disbursementApi.voucherDocument(id))}
          >
            Download Voucher
          </Button>
        }
      />
      <Summary voucher={v} />
      <WorkflowPanel
        entityType={ENTITY}
        entityId={id}
        renderBusinessActions={businessActions}
        onChanged={() => void refresh()}
      />
      <ErrorAlert error={download.error} />
      <Notices voucher={v} />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <VoucherTab tab={tab} voucher={v} onSaved={(n) => void refresh(n)} />
      {dialog !== undefined && (
        <VoucherActionDialog
          dialog={dialog}
          voucher={v}
          onClose={() => setDialog(undefined)}
          onDone={(n) => {
            setDialog(undefined);
            void refresh(n);
          }}
        />
      )}
    </div>
  );
}
