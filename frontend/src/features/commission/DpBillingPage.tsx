import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, CalendarClock, Coins, ReceiptText, Send, UserRound } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
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
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { AnswersDialog, CollectDialog, SendBillingDialog } from './BillingDialogs';
import { BILLING_ENTITY, commissionApi } from './commissionApi';
import type { DpBilling, DpItem } from './commissionApi';
import { ReasonDialog } from './DpItemDialogs';

const TABS = [
  { id: 'accounts', label: 'Accounts' },
  { id: 'documents', label: 'Documents' },
] as const;

type TabId = (typeof TABS)[number]['id'];
type DialogKind = 'send' | 'cancel' | 'answers' | 'collect';

const ITEM_COLUMNS: Column<DpItem>[] = [
  {
    key: 'invoice',
    header: 'Invoice No.',
    render: (i) => (
      <>
        <strong>{i.invoiceNo}</strong>
        <div className="muted">{i.policyNo}</div>
      </>
    ),
  },
  { key: 'assured', header: 'Assured', render: (i) => i.assuredName ?? '' },
  {
    key: 'premium',
    header: 'Premium',
    numeric: true,
    render: (i) => <Amount value={i.amounts.premium} />,
  },
  {
    key: 'comm',
    header: 'Commission',
    numeric: true,
    render: (i) => <Amount value={i.amounts.commission} />,
  },
  {
    key: 'wtax',
    header: 'Withholding Tax',
    numeric: true,
    render: (i) => <Amount value={i.amounts.wtax} />,
  },
  { key: 'net', header: 'Net', numeric: true, render: (i) => <Amount value={i.amounts.net} /> },
  {
    key: 'answer',
    header: 'Insurer Answer',
    render: (i) =>
      i.feedback.feedbackReason
        ? `${humanize(i.feedback.feedbackReason)} ${i.feedback.feedbackComment ?? ''}`
        : '',
  },
  { key: 'tag', header: 'Status', render: (i) => <StatusBadge status={i.tag} /> },
];

function Summary({ billing: b }: Readonly<{ billing: DpBilling }>) {
  return (
    <RecordSummary
      title={b.insurerCode}
      chips={
        <>
          <ReferenceChip label="Billing" value={b.billingNo} />
          <StatusBadge status={b.stage} />
        </>
      }
      flags={b.overdue ? <span className="tag">Answer Overdue</span> : undefined}
      facts={[
        { icon: Building2, label: 'Accounts', value: b.itemCount },
        {
          icon: Coins,
          label: 'Net Commission',
          value: `${formatAmount(b.amounts.net)} (withholding tax ${formatAmount(b.amounts.wtax)})`,
        },
        { icon: Send, label: 'Sent', value: formatDateTime(b.sentAt) },
        { icon: CalendarClock, label: 'Answer Due', value: formatDate(b.slaDue) },
        {
          icon: ReceiptText,
          label: 'Official Receipt',
          value: b.orNo ?? humanize(b.orStatus ?? ''),
        },
        { icon: UserRound, label: 'Handler', value: b.handler },
      ]}
    />
  );
}

function Actions({
  billing,
  onDialog,
}: Readonly<{ billing: DpBilling; onDialog: (k: DialogKind) => void }>) {
  const download = useFileDownload();
  const { can } = useAuth();
  const mayProcess = can('COMMREC_PROCESS');
  const stage = billing.stage;
  return (
    <>
      {billing.fileId !== undefined && (
        <Button
          variant="secondary"
          busy={download.isPending}
          onClick={() => download.mutate(() => commissionApi.billingFile(billing.id))}
        >
          Download Billing
        </Button>
      )}
      {mayProcess && stage === 'DP_FOR_BILLING' && (
        <>
          <Button variant="secondary" onClick={() => onDialog('cancel')}>
            Cancel Billing
          </Button>
          <Button onClick={() => onDialog('send')}>Send to Insurer</Button>
        </>
      )}
      {mayProcess && stage === 'AWAITING_INSURER' && (
        <Button onClick={() => onDialog('answers')}>Record Answers</Button>
      )}
      {mayProcess && stage === 'APPROVED' && (
        <Button onClick={() => onDialog('collect')}>Record Collection</Button>
      )}
    </>
  );
}

/**
 * A direct payment billing (CMRID.008-011): the insurer, amounts and answer due date, where it
 * stands, its accounts with the insurer answers, and its documents.
 */
export default function DpBillingPage() {
  const id = Number(useParams().id);
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('accounts');
  const [dialog, setDialog] = useState<DialogKind>();
  const billing = useQuery({
    queryKey: ['commission', 'billing', id],
    queryFn: () => commissionApi.billing(id),
  });
  const items = useQuery({
    queryKey: ['commission', 'billing-items', id],
    queryFn: () => commissionApi.billingItems(id),
  });
  const settings = useQuery({
    queryKey: ['commission', 'settings'],
    queryFn: () => commissionApi.settings(),
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<DpBilling>) => fn(),
    onSuccess: async (b) => {
      setDialog(undefined);
      await queryClient.invalidateQueries({ queryKey: ['commission'] });
      toast.success(`${b.billingNo} is now ${humanize(b.stage).toLowerCase()}`);
    },
  });
  if (billing.data === undefined) {
    return billing.error ? (
      <ErrorAlert error={billing.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const b = billing.data;
  const close = () => setDialog(undefined);
  return (
    <div className="stack">
      <PageHeader
        section="Commission Receivables · DP Billings"
        backTo="/commission/dp/billings"
        title={b.billingNo}
        description={`${b.insurerCode} · ${String(b.itemCount)} account(s)`}
        actions={<Actions billing={b} onDialog={setDialog} />}
      />
      <Summary billing={b} />
      <WorkflowPanel
        entityType={BILLING_ENTITY}
        entityId={b.id}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['commission'] })}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'accounts' ? (
        <Card>
          <div className="stack">
            <ErrorAlert error={items.error} />
            <DataTable
              caption="Accounts of the billing"
              columns={ITEM_COLUMNS}
              rows={items.data ?? []}
              rowKey={(i) => i.id}
              loading={items.isLoading}
              emptyMessage="No items to display"
            />
          </div>
        </Card>
      ) : (
        <Attachments entityType={BILLING_ENTITY} entityId={b.id} reference={b.billingNo} />
      )}
      {dialog === 'send' && (
        <SendBillingDialog
          billing={b}
          busy={act.isPending}
          error={act.error}
          onClose={close}
          onSend={(to, cc) => act.mutate(() => commissionApi.send(id, to, cc))}
        />
      )}
      {dialog === 'cancel' && (
        <ReasonDialog
          title={`Cancel ${b.billingNo}`}
          action="Cancel Billing"
          label="Reason"
          busy={act.isPending}
          error={act.error}
          onClose={close}
          onConfirm={(reason) => act.mutate(() => commissionApi.cancel(id, reason))}
        />
      )}
      {dialog === 'answers' && (
        <AnswersDialog
          items={items.data ?? []}
          busy={act.isPending}
          error={act.error}
          onClose={close}
          onSave={(answers) => act.mutate(() => commissionApi.answer(id, answers))}
        />
      )}
      {dialog === 'collect' && (
        <CollectDialog
          billing={b}
          bank={settings.data?.collectionBank}
          busy={act.isPending}
          error={act.error}
          onClose={close}
          onCollect={(input) => act.mutate(() => commissionApi.collect(id, input))}
        />
      )}
    </div>
  );
}
