import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Ban,
  Building2,
  CalendarRange,
  FilePlus2,
  Landmark,
  ReceiptText,
  UserRound,
  Wallet,
} from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { bookingApi, ACCOUNT_ENTITY } from '@/api/booking';
import type { BookedInvoice } from '@/api/booking';
import { useAuth } from '@/auth/authContext';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatAmount, formatDate } from '@/utils/format';
import { PremiumTables, SummaryFact } from './BookingParts';
import { labelOf } from './bookingForm';
import { CancellationDialog } from './CancellationDialog';
import {
  EndorsementsTab,
  JournalTab,
  OpenItemsTab,
  ScheduleTab,
  ServiceInvoicesTab,
} from './InvoiceTabs';

const TABS = [
  { id: 'premium', label: 'Premium & Commission' },
  { id: 'journal', label: 'Journal' },
  { id: 'items', label: 'Open Items' },
  { id: 'service', label: 'Service Invoices' },
  { id: 'endorsements', label: 'Endorsements' },
  { id: 'schedule', label: 'Invoices of the Account' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function TabBody({ tab, invoice }: Readonly<{ tab: TabId; invoice: BookedInvoice }>) {
  switch (tab) {
    case 'journal':
      return <JournalTab invoice={invoice} />;
    case 'items':
      return <OpenItemsTab invoice={invoice} />;
    case 'service':
      return <ServiceInvoicesTab invoice={invoice} />;
    case 'endorsements':
      return <EndorsementsTab invoice={invoice} />;
    case 'schedule':
      return <ScheduleTab invoice={invoice} />;
    default:
      return (
        <Card title="Premium and commission">
          <PremiumTables premium={invoice.premium} commission={invoice.commission} />
        </Card>
      );
  }
}

function Summary({ invoice: i }: Readonly<{ invoice: BookedInvoice }>) {
  return (
    <Card>
      <div className="stack">
        <div className="record-facts">
          <ReferenceChip label="Invoice" value={i.invoiceNo ?? i.transactionNo} />
          <ReferenceChip label="ARN" value={i.arn} />
          {i.endorsementNo && <ReferenceChip label="Endorsement" value={i.endorsementNo} />}
          <StatusBadge status={i.status} />
          {i.flags.directPayment && <StatusBadge status="DIRECT_PAYMENT" />}
          {i.flags.cwt2Percent && <StatusBadge status="CWT_2_PERCENT" />}
          {i.flags.incentiveEligible && <StatusBadge status="INCENTIVE_ELIGIBLE" />}
        </div>
        <div className="summary-card">
          <SummaryFact icon={UserRound} label="Client">
            {i.facts.clientName} <span className="muted">{i.facts.clientCode}</span>
          </SummaryFact>
          <SummaryFact icon={Building2} label="Insurer(s)">
            {i.shares.map((s) => `${s.insurerCode} ${String(s.sharePct)}%`).join(', ')}
          </SummaryFact>
          <SummaryFact icon={CalendarRange} label="Period">
            {formatDate(i.inceptionDate)} – {formatDate(i.expiryDate)}
          </SummaryFact>
          <SummaryFact icon={ReceiptText} label="Booked">
            {formatDate(i.bookingDate)}{' '}
            {i.bookedBy && <span className="muted">by {i.bookedBy}</span>}
          </SummaryFact>
          <SummaryFact icon={Wallet} label="Gross premium">
            {i.currency} {formatAmount(i.premium.total)}
          </SummaryFact>
          <SummaryFact icon={Landmark} label="Cost center">
            {i.facts.costCenter} <span className="muted">{i.facts.department}</span>
          </SummaryFact>
        </div>
      </div>
    </Card>
  );
}

function Actions({
  invoice,
  onCancel,
}: Readonly<{ invoice: BookedInvoice; onCancel: () => void }>) {
  const { can } = useAuth();
  const open = invoice.status === 'BOOKED' && invoice.kind !== 'CANCELLATION';
  return (
    <>
      {open && (can('BOOKING_PROCESS') || can('BOOKING_ADJUST')) && (
        <Link
          className="btn btn-secondary"
          to={`/booking/endorsements/new?arn=${encodeURIComponent(invoice.arn)}`}
        >
          <FilePlus2 size={16} aria-hidden="true" /> New Endorsement
        </Link>
      )}
      {open && can('BOOKING_ADJUST') && (
        <Button variant="danger" icon={<Ban size={16} />} onClick={onCancel}>
          Cancel Booking
        </Button>
      )}
    </>
  );
}

/**
 * A booked invoice (BRNB.027/100/108): summary with the invoice number, ARN, status and key
 * facts; the account's work case; tabs for the premium and commission, journal, open items,
 * service invoices, endorsements and every invoice of the account.
 */
export default function InvoiceDetailPage() {
  const id = Number(useParams().id);
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('premium');
  const [cancelling, setCancelling] = useState(false);
  const invoice = useQuery({
    queryKey: ['booking', 'invoice', id],
    queryFn: () => bookingApi.invoice(id),
  });
  if (invoice.data === undefined) {
    return invoice.error ? (
      <ErrorAlert error={invoice.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const i = invoice.data;
  return (
    <div className="stack">
      <PageHeader
        section="Booking · Booked Invoice"
        backTo="/booking"
        title={i.invoiceNo ?? i.arn}
        description={[
          labelOf(i.kind),
          i.facts.riskCode,
          `policy year ${String(i.policyYear)}`,
          i.policyNo,
        ]
          .filter((part) => part !== undefined && part !== '')
          .join(' · ')}
        actions={<Actions invoice={i} onCancel={() => setCancelling(true)} />}
      />
      <Summary invoice={i} />
      <WorkflowPanel
        entityType={ACCOUNT_ENTITY}
        entityId={i.accountId}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['booking'] })}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabBody tab={tab} invoice={i} />
      {cancelling && <CancellationDialog invoice={i} onClose={() => setCancelling(false)} />}
    </div>
  );
}
