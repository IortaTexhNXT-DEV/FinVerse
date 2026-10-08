import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, CalendarRange, Landmark, UserRound, Wallet } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { opsApi } from '@/api/operations';
import type { Invoice360 } from '@/api/operations';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { InsurerName } from '@/components/broking/LovLabel';
import { RecordSummary } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { PolicyTransactions } from '@/components/broking/PolicyTransactions';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { useProductName } from '@/components/broking/useLabels';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { OriginBadge } from '@/components/ui/OriginBadge';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatAmount, formatDate, formatPeriod, humanize } from '@/utils/format';
import { ComponentsTab, HistoryTab, MovementsTab, RelatedSectionTab } from './Invoice360Tabs';
import { InvoiceFamilyTab } from './InvoiceFamilyTab';
import { LegacyInvoiceBlock } from './LegacyInvoiceBlock';
import { RELATED_TABS, flagChips, invoiceTabs } from './opsLabels';
import type { Invoice360TabId } from './opsLabels';
import { displayNameOf } from '@/api/users';

function TabBody({ tab, view }: Readonly<{ tab: Invoice360TabId; view: Invoice360 }>) {
  const module = RELATED_TABS.find((t) => t.id === tab);
  if (module !== undefined) {
    return <RelatedSectionTab section={module.section} items={view.related[module.section]} />;
  }
  switch (tab) {
    case 'movements':
      return <MovementsTab movements={view.movements} />;
    case 'family':
      return <InvoiceFamilyTab invoiceNo={view.invoice.keys.invoiceNo} />;
    case 'transactions':
      return (
        <Card title="Policy Transactions" flush>
          <PolicyTransactions invoiceNo={view.invoice.keys.invoiceNo} />
        </Card>
      );
    case 'history':
      return <HistoryTab history={view.history} />;
    case 'documents':
      return (
        <div className="stack">
          {(view.related.DOCUMENTS?.length ?? 0) > 0 && (
            <RelatedSectionTab section="DOCUMENTS" items={view.related.DOCUMENTS} />
          )}
          <Attachments
            entityType="OpsInvoice"
            entityId={view.invoice.keys.invoiceNo}
            reference={view.invoice.keys.invoiceNo}
          />
        </div>
      );
    default:
      return <ComponentsTab view={view} />;
  }
}

function facts(view: Invoice360): Fact[] {
  const i = view.invoice;
  return [
    {
      icon: UserRound,
      label: 'Client / Payor',
      value: (
        <CellStack main={i.parties.payorName ?? i.parties.assuredName} sub={i.parties.clientCode} />
      ),
    },
    {
      icon: Building2,
      label: 'Insurers',
      value: (
        <>
          {i.shares.map((s, n) => (
            <span key={s.insurerCode}>
              {n > 0 && ', '}
              <InsurerName code={s.insurerCode} /> {formatAmount(s.sharePct)}%
            </span>
          ))}
        </>
      ),
    },
    {
      icon: CalendarRange,
      label: 'Period',
      value: formatPeriod(i.classification.inceptionDate, i.classification.expiryDate),
    },
    {
      icon: Wallet,
      label: 'Gross / Outstanding Premium',
      value: `${i.classification.currency} ${formatAmount(i.grossPremium)} / ${formatAmount(i.premiumBalance)}`,
    },
    {
      icon: Landmark,
      label: 'Booked / Account Officer',
      value: (
        <CellStack
          main={formatDate(i.classification.bookingDate)}
          sub={displayNameOf(i.classification.aoUsername)}
        />
      ),
    },
  ];
}

/** Whether the invoice belongs to the family of another (root) invoice (DIS 3.27.2). */
function isEndorsementOfFamily(i: Invoice360['invoice']): boolean {
  return i.keys.rootInvoiceNo !== undefined && i.keys.rootInvoiceNo !== i.keys.invoiceNo;
}

function Summary({ view }: Readonly<{ view: Invoice360 }>) {
  const i = view.invoice;
  const flags = flagChips(i.flags);
  return (
    <RecordSummary
      title={i.parties.assuredName}
      chips={
        <>
          <ReferenceChip label="Invoice" value={i.keys.invoiceNo} />
          <OriginBadge record={i} />
          {i.legacyInvoiceNo !== undefined && i.legacyInvoiceNo !== i.keys.invoiceNo && (
            <ReferenceChip label="Legacy Invoice" value={i.legacyInvoiceNo} />
          )}
          <ReferenceChip label="ARN" value={i.keys.arn} />
          {i.keys.policyNo !== undefined && (
            <ReferenceChip label="Policy No." value={i.keys.policyNo} />
          )}
          {i.keys.endorsementNo !== undefined && (
            <ReferenceChip label="Endorsement" value={i.keys.endorsementNo} />
          )}
          {isEndorsementOfFamily(i) && (
            <ReferenceChip label="Root Invoice" value={i.keys.rootInvoiceNo ?? ''} />
          )}
          <StatusBadge status={i.paymentStatus} />
          <StatusBadge status={i.remittanceStatus} />
        </>
      }
      flags={
        flags.length === 0
          ? undefined
          : flags.map((f) => (
              <span key={f} className="tag">
                {f}
              </span>
            ))
      }
      facts={facts(view)}
    />
  );
}

/**
 * Invoice 360 (RMTID.026/032/038, ADJID.024): the booked invoice as Operations sees it - the
 * premium receivable by component with its outstanding balance, the insurer shares, payment and
 * remittance status, flags and lock, every movement, the records of the Operations modules and
 * the history. An open legacy invoice migrated at cut-over shows a LEGACY badge and its legacy
 * block (the frozen original values).
 */
export default function Invoice360Page() {
  const invoiceNo = decodeURIComponent(useParams().no ?? '');
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<Invoice360TabId>('components');
  const productName = useProductName();
  const view = useQuery({
    queryKey: ['ops', 'invoice', invoiceNo],
    queryFn: () => opsApi.invoice(invoiceNo),
  });
  if (view.data === undefined) {
    return view.error ? (
      <ErrorAlert error={view.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const v = view.data;
  const bookedId = v.booking.bookedInvoiceId;
  const mayOpenBooking = can('BOOKING_PROCESS') || can('BOOKING_ADJUST');
  return (
    <div className="stack">
      <PageHeader
        section="Operations · Invoice 360"
        backTo="/operations/invoices"
        title={v.invoice.keys.invoiceNo}
        description={[
          humanize(v.invoice.keys.kind),
          productName(v.invoice.classification.riskCode),
          v.invoice.keys.policyNo,
        ]
          .filter((part) => part !== undefined && part !== '')
          .join(' · ')}
        actions={
          mayOpenBooking && bookedId !== undefined ? (
            <Link className="btn btn-secondary" to={`/booking/invoices/${String(bookedId)}`}>
              Open Booked Invoice
            </Link>
          ) : undefined
        }
      />
      <Summary view={v} />
      {v.legacy !== undefined && <LegacyInvoiceBlock legacy={v.legacy} />}
      {v.accountWorkflow && v.invoice.keys.accountId !== undefined && (
        <WorkflowPanel
          entityType="Account"
          entityId={v.invoice.keys.accountId}
          onChanged={() =>
            void queryClient.invalidateQueries({ queryKey: ['ops', 'invoice', invoiceNo] })
          }
        />
      )}
      <Tabs tabs={invoiceTabs(v.related)} active={tab} onChange={setTab} />
      <TabBody tab={tab} view={v} />
    </div>
  );
}
