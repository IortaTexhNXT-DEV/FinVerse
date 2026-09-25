import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Banknote, CalendarDays, FileText, Layers, Pencil, User, Wallet } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { RecordSummary } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { payRequestApi, REQUEST_ENTITY } from './api';
import type { PayRequest } from './api';
import { LiquidationTab } from './LiquidationTab';
import { RequestActions } from './RequestActions';
import { KIND_LABELS } from './requestForm';
import { DetailsTab, DisbursementTab, ValidationTab } from './RequestTabs';

type TabId = 'details' | 'validation' | 'disbursement' | 'liquidation' | 'documents';

function tabsOf(r: PayRequest): { id: TabId; label: string }[] {
  const tabs: { id: TabId; label: string }[] = [{ id: 'details', label: 'Details' }];
  if (r.validationRequired) {
    tabs.push({ id: 'validation', label: 'Validation' });
  }
  if (r.kind !== 'CHECK_CANCELLATION') {
    tabs.push({ id: 'disbursement', label: 'Disbursement' });
  }
  if (r.kind === 'CASH_ADVANCE' && r.stage === 'DISBURSED') {
    tabs.push({ id: 'liquidation', label: 'Liquidation' });
  }
  tabs.push({ id: 'documents', label: 'Documents' });
  return tabs;
}

function facts(r: PayRequest): Fact[] {
  return [
    { icon: Layers, label: 'Kind', value: KIND_LABELS[r.kind] },
    { icon: User, label: 'Payee', value: `${r.payee.name} (${r.payee.code})` },
    { icon: Banknote, label: 'Amount', value: `${r.content.currency} ${formatAmount(r.amount)}` },
    { icon: Wallet, label: 'Mode of Payment', value: humanize(r.payee.mode) },
    { icon: CalendarDays, label: 'Request Date', value: formatDate(r.requestDate) },
    { icon: FileText, label: 'DV No.', value: r.track.dvNo ?? '—' },
  ];
}

function TabBody({ tab, request }: Readonly<{ tab: TabId; request: PayRequest }>) {
  switch (tab) {
    case 'validation':
      return <ValidationTab request={request} />;
    case 'disbursement':
      return <DisbursementTab request={request} />;
    case 'liquidation':
      return <LiquidationTab request={request} />;
    case 'documents':
      return (
        <Attachments
          entityType={REQUEST_ENTITY}
          entityId={request.id}
          title="Supporting Documents"
          reference={request.requestNo}
        />
      );
    default:
      return <DetailsTab request={request} />;
  }
}

function editPath(r: PayRequest): string | undefined {
  if (r.kind === 'REFUND') {
    return `/payment-requests/requests/${String(r.id)}/edit-refund`;
  }
  return r.kind === 'CASH_ADVANCE'
    ? `/payment-requests/requests/${String(r.id)}/edit-cash-advance`
    : undefined;
}

/** Header buttons of a request: its form, the change and the cancellation of a disbursed check. */
function HeaderActions({
  request: r,
  editable,
  downloading,
  onDownload,
}: Readonly<{
  request: PayRequest;
  editable: boolean;
  downloading: boolean;
  onDownload: () => void;
}>) {
  const { can } = useAuth();
  const edit = editPath(r);
  return (
    <>
      {r.kind !== 'CHECK_CANCELLATION' && (
        <Button
          variant="secondary"
          icon={<FileText size={16} />}
          busy={downloading}
          onClick={onDownload}
        >
          {r.kind === 'REFUND' ? 'Refund Request Form' : 'Request for Payment'}
        </Button>
      )}
      {edit && editable && can('PRQ_CREATE') && (
        <Link className="btn btn-secondary" to={edit}>
          <Pencil size={16} aria-hidden="true" /> Edit
        </Link>
      )}
      {r.stage === 'DISBURSED' && r.payee.mode === 'CHECK' && can('PRQ_CREATE') && (
        <Link
          className="btn btn-secondary"
          to={`/payment-requests/check-cancellation?request=${encodeURIComponent(r.requestNo)}`}
        >
          Cancel the Check
        </Link>
      )}
    </>
  );
}

/**
 * One refund, cash-advance or check-cancellation request (MKT 1.5.0, 2.26.0): header with the
 * request number and status, the summary, the workflow panel with the business actions and the
 * history, and tabs for the form, the validations, the payment in Disbursement, the liquidation of
 * a cash advance and the supporting documents (MKT 1.12.0, 2.22.0).
 */
export default function RequestDetailPage() {
  const id = Number(useParams().id);
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('details');
  const request = useQuery({
    queryKey: ['payrequest', 'request', id],
    queryFn: () => payRequestApi.get(id),
  });
  const download = useMutation({
    mutationFn: () => payRequestApi.form(id),
    onSuccess: (f) => saveFile(f.blob, f.fileName),
  });
  if (request.data === undefined) {
    return request.error ? (
      <ErrorAlert error={request.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const r = request.data;
  const editable = r.stage === 'DRAFT' || r.stage === 'PREPARING';
  const tabs = tabsOf(r);
  const active = tabs.some((t) => t.id === tab) ? tab : 'details';
  return (
    <div className="stack">
      <PageHeader
        backTo="/payment-requests"
        section="Finance · Refund & Cash Advance Requests"
        title={r.requestNo}
        description={`${KIND_LABELS[r.kind]} of ${r.payee.name}: ${r.content.purpose}`}
        actions={
          <HeaderActions
            request={r}
            editable={editable}
            downloading={download.isPending}
            onDownload={() => download.mutate()}
          />
        }
      />
      <ErrorAlert error={download.error} />
      <RecordSummary
        title={r.payee.name}
        chips={
          <>
            <ReferenceChip label="Request" value={r.requestNo} />
            {r.lines[0]?.rootInvoiceNo && (
              <ReferenceChip label="Root Invoice" value={r.lines[0].rootInvoiceNo} />
            )}
            <StatusBadge status={r.stage} />
          </>
        }
        flags={r.validationRequired ? <span className="tag">Cancelled Policy</span> : undefined}
        facts={facts(r)}
      />
      {r.trail.returnReason && editable && (
        <div className="alert warning" role="status">
          Returned: {humanize(r.trail.returnReason)}
          {r.trail.returnComment ? ` – ${r.trail.returnComment}` : ''}. Change the request and
          submit it again, or cancel it.
        </div>
      )}
      <WorkflowPanel
        entityType={REQUEST_ENTITY}
        entityId={r.id}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['payrequest'] })}
        renderBusinessActions={(actions) => <RequestActions request={r} actions={actions} />}
      />
      <Tabs tabs={tabs} active={active} onChange={setTab} />
      <TabBody tab={active} request={r} />
    </div>
  );
}
