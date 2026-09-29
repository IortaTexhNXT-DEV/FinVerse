import { CellStack } from '@/components/ui/CellStack';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Building2,
  CalendarClock,
  CalendarRange,
  FileCheck2,
  FileText,
  Hourglass,
  Layers,
  Pencil,
  ShieldCheck,
  User,
} from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { saveFile } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { InsurerName, ProductName } from '@/components/broking/LovLabel';
import { PolicyTransactions } from '@/components/broking/PolicyTransactions';
import { RecordSummary } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { formatDate, formatDays, formatPeriod } from '@/utils/format';
import { adjustmentApi, REQUEST_ENTITY } from './api';
import type { EndorsementRequest } from './api';
import { RequestActions } from './RequestActions';
import { RequestFlags, RequestStatus } from './RequestParts';
import { AccountingTab, DetailsTab, HistoryTab, PolicyTab, RecomputeTab } from './RequestTabs';
import { useRequestLabels } from './useRequestLabels';
import { UserName } from '@/components/ui/UserName';
import { Notice } from '@/components/ui/Notice';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'policy', label: 'Policy' },
  { id: 'recompute', label: 'Recompute' },
  { id: 'accounting', label: 'Accounting' },
  { id: 'transactions', label: 'Policy Transactions' },
  { id: 'documents', label: 'Documents' },
  { id: 'history', label: 'History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const VALIDATED = new Set(['FOR_APPROVAL', 'FOR_POSTING', 'AWAITING_REAPPLICATION', 'POSTED']);

function facts(r: EndorsementRequest, typeLabel: string): Fact[] {
  const p = r.policy;
  return [
    { icon: ShieldCheck, label: 'Policy No.', value: p?.policyNo ?? r.invoice.policyNo ?? '—' },
    { icon: FileText, label: 'Invoice', value: r.invoice.invoiceNo },
    {
      icon: Building2,
      label: 'Insurer / Product',
      value: (
        <CellStack
          main={<InsurerName code={r.invoice.insurerCode} />}
          sub={p?.productCode ? <ProductName code={p.productCode} /> : undefined}
        />
      ),
    },
    {
      icon: CalendarRange,
      label: 'Period / Effective',
      value: `${formatPeriod(p?.periodFrom, p?.periodTo) || '—'} · effective ${formatDate(r.terms.effectiveDate)}`,
    },
    { icon: Layers, label: 'Type', value: typeLabel },
    { icon: User, label: 'Requested By', value: <UserName login={r.createdBy} /> },
    { icon: Hourglass, label: 'Aging', value: formatDays(r.agingDays) },
    {
      icon: CalendarClock,
      label: 'Placement Slip / Batch',
      value: [p?.slipNo, r.outcome.batchNo].filter(Boolean).join(' · ') || '—',
    },
  ];
}

function TabBody({ tab, request }: Readonly<{ tab: TabId; request: EndorsementRequest }>) {
  switch (tab) {
    case 'details':
      return <DetailsTab request={request} />;
    case 'policy':
      return <PolicyTab request={request} />;
    case 'recompute':
      return <RecomputeTab request={request} />;
    case 'transactions':
      return (
        <Card title="Policy Transactions" flush>
          <PolicyTransactions
            invoiceNo={request.invoice.invoiceNo}
            highlightRequestNo={request.requestNo}
          />
        </Card>
      );
    case 'accounting':
      return <AccountingTab request={request} />;
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
      return <HistoryTab id={request.id} />;
  }
}

function Downloads({ request }: Readonly<{ request: EndorsementRequest }>) {
  const download = useMutation({
    mutationFn: (fetch: () => Promise<DownloadedFile>) => fetch(),
    onSuccess: (f) => saveFile(f.blob, f.fileName),
  });
  return (
    <>
      {request.requestClass !== 'INTERNAL' && request.stage !== 'CANCELLED' && (
        <Button
          variant="secondary"
          icon={<FileText size={16} />}
          busy={download.isPending}
          onClick={() => download.mutate(() => adjustmentApi.endorsementSlip(request.id))}
        >
          Endorsement Slip
        </Button>
      )}
      {VALIDATED.has(request.stage) && (
        <Button
          variant="secondary"
          icon={<FileCheck2 size={16} />}
          onClick={() => download.mutate(() => adjustmentApi.validationSlip(request.id))}
        >
          Validation Slip
        </Button>
      )}
      <ErrorAlert error={download.error} />
    </>
  );
}

/** Header of a request: the policy, ARN and invoice chips, the status, flags and key facts. */
function RequestSummary({ request: r }: Readonly<{ request: EndorsementRequest }>) {
  const labels = useRequestLabels();
  return (
    <>
      <RecordSummary
        title={r.invoice.assuredName}
        chips={
          <>
            {r.policy?.policyNo && <ReferenceChip label="Policy No." value={r.policy.policyNo} />}
            <ReferenceChip label="ARN" value={r.invoice.arn} />
            <ReferenceChip label="Invoice" value={r.invoice.invoiceNo} />
            <RequestStatus stage={r.stage} />
          </>
        }
        flags={
          <RequestFlags
            negative={r.control.negative}
            quotationRequired={r.control.quotationRequired}
            duplicateOverride={r.control.duplicateOverride !== undefined}
          />
        }
        facts={facts(r, labels.type(r.terms.endorsementType))}
      />
      {r.stage === 'RETURNED' && (
        <Notice tone="warning" title="Returned">
          {labels.returnReason(r.control.returnReason ?? 'OTHERS')}
          {r.control.returnComment ? ` – ${r.control.returnComment}` : ''}. Change the request and
          resubmit it, or cancel it.
        </Notice>
      )}
    </>
  );
}

/** Title line of a request: its type and the policy (or, before the policy is issued, the invoice). */
function describe(r: EndorsementRequest, typeLabel: string): string {
  const target = r.policy?.policyNo
    ? `policy ${r.policy.policyNo}`
    : `invoice ${r.invoice.invoiceNo}`;
  return `${typeLabel} on ${target}: ${r.terms.description}`;
}

/**
 * One endorsement request (ADJID.001-025, MKTID.008): header with the request number, ARN and
 * invoice chips and the status; the summary; the workflow panel with the business actions; and
 * tabs for the details, the recompute, the accounting, the supporting documents and the history.
 */
export default function RequestDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('details');
  const labels = useRequestLabels();
  const request = useQuery({
    queryKey: ['adjustment', 'request', id],
    queryFn: () => adjustmentApi.get(id),
  });
  if (request.data === undefined) {
    return request.error ? (
      <ErrorAlert error={request.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const r = request.data;
  const editable =
    (r.stage === 'DRAFT' || r.stage === 'RETURNED') && (can('ADJ_REQUEST') || can('ADJ_PROCESS'));
  return (
    <div className="stack">
      <PageHeader
        backTo="/adjustment"
        section="Client & Policy · Adjustment"
        title={r.requestNo}
        description={describe(r, labels.type(r.terms.endorsementType))}
        actions={
          <>
            <Downloads request={r} />
            {editable && (
              <Link className="btn btn-secondary" to={`/adjustment/requests/${String(r.id)}/edit`}>
                <Pencil size={16} aria-hidden="true" /> Edit
              </Link>
            )}
          </>
        }
      />
      <RequestSummary request={r} />
      <WorkflowPanel
        entityType={REQUEST_ENTITY}
        entityId={r.id}
        recordStatus={r.stage}
        showHistory={false}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['adjustment'] })}
        renderBusinessActions={(actions) => <RequestActions request={r} actions={actions} />}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabBody tab={tab} request={r} />
    </div>
  );
}
