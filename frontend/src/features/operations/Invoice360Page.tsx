import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, CalendarRange, Landmark, UserRound, Wallet } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { opsApi } from '@/api/operations';
import type { Invoice360 } from '@/api/operations';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { RecordSummary } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { ComponentsTab, HistoryTab, MovementsTab, RelatedSectionTab } from './Invoice360Tabs';
import { InvoiceFamilyTab } from './InvoiceFamilyTab';
import { RELATED_TABS, flagChips, invoiceTabs } from './opsLabels';
import type { Invoice360TabId } from './opsLabels';

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
      label: 'Client',
      value: [i.parties.clientCode, i.parties.payorName].filter(Boolean).join(' · payor '),
    },
    {
      icon: Building2,
      label: 'Insurer(s)',
      value: i.shares.map((s) => `${s.insurerCode} ${formatAmount(s.sharePct)}%`).join(', '),
    },
    {
      icon: CalendarRange,
      label: 'Period',
      value: `${formatDate(i.classification.inceptionDate)} – ${formatDate(i.classification.expiryDate)}`,
    },
    {
      icon: Wallet,
      label: 'Gross / Outstanding Premium',
      value: `${i.classification.currency} ${formatAmount(i.grossPremium)} / ${formatAmount(i.premiumBalance)}`,
    },
    {
      icon: Landmark,
      label: 'Booked',
      value: [
        formatDate(i.classification.bookingDate),
        i.classification.costCenter,
        i.classification.aoUsername,
      ]
        .filter(Boolean)
        .join(' · '),
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
          <ReferenceChip label="ARN" value={i.keys.arn} />
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
 * the history.
 */
export default function Invoice360Page() {
  const invoiceNo = decodeURIComponent(useParams().no ?? '');
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<Invoice360TabId>('components');
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
          v.invoice.classification.riskCode,
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
      {v.invoice.keys.accountId !== undefined && (
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
