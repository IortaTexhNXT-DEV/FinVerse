import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Banknote, CalendarDays, FileText, Layers, Receipt, User } from 'lucide-react';
import { Fragment, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Attachments } from '@/components/attachments/Attachments';
import { RecordSummary } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { CASE_TYPE_LABELS } from './acsl';
import { acslApi, CASE_ENTITY } from './api';
import type { AcslCase } from './api';
import { CaseActions } from './CaseActions';

type TabId = 'details' | 'family' | 'documents';

const TABS: { id: TabId; label: string }[] = [
  { id: 'details', label: 'Details' },
  { id: 'family', label: 'Invoice Family Cases' },
  { id: 'documents', label: 'Documents' },
];

function Facts({ items }: Readonly<{ items: [string, string | undefined][] }>) {
  return (
    <dl className="detail-list">
      {items.map(([label, value]) => (
        <Fragment key={label}>
          <dt>{label}</dt>
          <dd>{value === undefined || value === '' ? '—' : value}</dd>
        </Fragment>
      ))}
    </dl>
  );
}

function facts(c: AcslCase): Fact[] {
  const amount = c.account.amount;
  return [
    { icon: Layers, label: 'Type', value: CASE_TYPE_LABELS[c.type] },
    { icon: FileText, label: 'Invoice', value: c.account.invoiceNo ?? '—' },
    { icon: Receipt, label: 'AR / OR No.', value: c.account.arNo ?? '—' },
    {
      icon: Banknote,
      label: 'Amount',
      value: amount === undefined ? '—' : `${c.account.currency ?? ''} ${formatAmount(amount)}`,
    },
    { icon: User, label: 'Requested By', value: c.requestedBy },
    { icon: CalendarDays, label: 'Received', value: formatDate(c.createdAt) },
  ];
}

function DetailsTab({ c }: Readonly<{ c: AcslCase }>) {
  return (
    <div className="stack">
      <Card title="Request">
        <Facts
          items={[
            ['Subject', c.subject],
            ['Details', c.details],
            ['Requesting Module', c.requesterModule ? humanize(c.requesterModule) : undefined],
            ['Requester Reference', c.requesterRef],
            ['Client', c.account.clientCode],
            ['Insurer', c.account.insurerCode],
            ['Root Invoice', c.account.rootInvoiceNo],
          ]}
        />
      </Card>
      <Card title="Investigation">
        <Facts
          items={[
            ['Findings', c.findings],
            ['Result', c.outcome ? humanize(c.outcome) : undefined],
            ['Remarks', c.resultRemarks],
            ['Result By', c.resultBy],
            ['Result Date', c.resultAt ? formatDateTime(c.resultAt) : undefined],
          ]}
        />
      </Card>
      {(c.reversalRef !== undefined || c.correctionId !== undefined) && (
        <Card title="Follow-up">
          <Facts
            items={[
              ['Payment Reversal', c.reversalRef],
              ['Reversal Status', c.reversalStatus ? humanize(c.reversalStatus) : undefined],
              ['Reversal Message', c.reversalMessage],
            ]}
          />
          {c.correctionId && (
            <Link to={`/acsl/corrections/${String(c.correctionId)}`}>
              Open the correction entry
            </Link>
          )}
        </Card>
      )}
    </div>
  );
}

const FAMILY_COLUMNS: Column<AcslCase>[] = [
  { key: 'no', header: 'Case No.', render: (c) => <strong>{c.caseNo}</strong> },
  { key: 'type', header: 'Type', render: (c) => CASE_TYPE_LABELS[c.type] },
  { key: 'invoice', header: 'Invoice', render: (c) => c.account.invoiceNo ?? '—' },
  { key: 'subject', header: 'Subject', render: (c) => c.subject },
  { key: 'date', header: 'Received', render: (c) => formatDate(c.createdAt) },
  { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.stage} /> },
];

function FamilyTab({ c }: Readonly<{ c: AcslCase }>) {
  const navigate = useNavigate();
  const invoiceNo = c.account.invoiceNo ?? '';
  const family = useQuery({
    queryKey: ['acsl', 'family', invoiceNo],
    queryFn: () => acslApi.familyCases(invoiceNo),
    enabled: invoiceNo !== '',
  });
  return (
    <Card flush>
      <ErrorAlert error={family.error} />
      <DataTable
        caption="Cases of the invoice family"
        columns={FAMILY_COLUMNS}
        rows={family.data ?? []}
        rowKey={(f) => f.id}
        loading={family.isLoading && invoiceNo !== ''}
        emptyMessage="No items to display"
        onRowClick={(f) => void navigate(`/acsl/cases/${String(f.id)}`)}
      />
    </Card>
  );
}

function TabBody({ tab, c }: Readonly<{ tab: TabId; c: AcslCase }>) {
  if (tab === 'family') {
    return <FamilyTab c={c} />;
  }
  if (tab === 'documents') {
    return (
      <Attachments
        entityType={CASE_ENTITY}
        entityId={c.id}
        title="Case Documents"
        reference={c.caseNo}
      />
    );
  }
  return <DetailsTab c={c} />;
}

/**
 * One ACSL case (ACSL 2.5.0-2.6.2): the summary of the account, the workflow panel with the
 * assignment, findings, result, correction, payment reversal and the message to the Account
 * Officer, and tabs for the details, the other cases of the invoice family and the documents.
 */
export default function CaseDetailPage() {
  const id = Number(useParams().id);
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('details');
  const found = useQuery({ queryKey: ['acsl', 'case', id], queryFn: () => acslApi.getCase(id) });
  if (found.data === undefined) {
    return found.error ? (
      <ErrorAlert error={found.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const c = found.data;
  return (
    <div className="stack">
      <PageHeader
        backTo="/acsl"
        section="Finance · ACSL"
        title={c.caseNo}
        description={`${CASE_TYPE_LABELS[c.type]}: ${c.subject}`}
      />
      <RecordSummary
        title={c.subject}
        chips={
          <>
            <ReferenceChip label="Case" value={c.caseNo} />
            {c.account.rootInvoiceNo && (
              <ReferenceChip label="Root Invoice" value={c.account.rootInvoiceNo} />
            )}
            <StatusBadge status={c.stage} />
          </>
        }
        facts={facts(c)}
      />
      <WorkflowPanel
        entityType={CASE_ENTITY}
        entityId={c.id}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['acsl'] })}
        renderBusinessActions={(actions) => <CaseActions c={c} actions={actions} />}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabBody tab={tab} c={c} />
    </div>
  );
}
