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
  User,
} from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { saveFile } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { RecordSummary } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { formatDate, humanize } from '@/utils/format';
import { adjustmentApi, REQUEST_ENTITY } from './api';
import type { EndorsementRequest } from './api';
import { RequestActions } from './RequestActions';
import { RequestFlags, RequestStatus } from './RequestParts';
import { AccountingTab, DetailsTab, HistoryTab, RecomputeTab } from './RequestTabs';

const TABS = [
  { id: 'details', label: 'Details' },
  { id: 'recompute', label: 'Recompute' },
  { id: 'accounting', label: 'Accounting' },
  { id: 'documents', label: 'Documents' },
  { id: 'history', label: 'History' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const VALIDATED = new Set(['FOR_APPROVAL', 'FOR_POSTING', 'AWAITING_REAPPLICATION', 'POSTED']);

function facts(r: EndorsementRequest): Fact[] {
  return [
    { icon: FileText, label: 'Invoice', value: r.invoice.invoiceNo },
    { icon: Building2, label: 'Insurer', value: r.invoice.insurerCode },
    { icon: CalendarRange, label: 'Effective', value: formatDate(r.terms.effectiveDate) },
    {
      icon: Layers,
      label: 'Type',
      value: [r.terms.endorsementType, r.terms.requestType]
        .filter((c): c is string => c !== undefined)
        .map(humanize)
        .join(' · '),
    },
    { icon: User, label: 'Requested By', value: r.createdBy },
    { icon: Hourglass, label: 'Aging', value: `${String(r.agingDays)} day(s)` },
    { icon: CalendarClock, label: 'Batch', value: r.outcome.batchNo ?? '—' },
  ];
}

function TabBody({ tab, request }: Readonly<{ tab: TabId; request: EndorsementRequest }>) {
  switch (tab) {
    case 'details':
      return <DetailsTab request={request} />;
    case 'recompute':
      return <RecomputeTab request={request} />;
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
        description={`${humanize(r.requestClass)} request on ${r.invoice.invoiceNo}: ${r.terms.description}`}
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
      <RecordSummary
        title={r.invoice.assuredName}
        chips={
          <>
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
        facts={facts(r)}
      />
      {r.stage === 'RETURNED' && (
        <div className="alert warning" role="status">
          Returned: {humanize(r.control.returnReason ?? 'OTHERS')}
          {r.control.returnComment ? ` – ${r.control.returnComment}` : ''}. Change the request and
          resubmit it, or cancel it.
        </div>
      )}
      <WorkflowPanel
        entityType={REQUEST_ENTITY}
        entityId={r.id}
        showHistory={false}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['adjustment'] })}
        renderBusinessActions={(actions) => <RequestActions request={r} actions={actions} />}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabBody tab={tab} request={r} />
    </div>
  );
}
