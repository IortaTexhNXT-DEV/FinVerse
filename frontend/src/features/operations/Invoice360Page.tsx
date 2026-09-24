import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, CalendarRange, Landmark, ReceiptText, UserRound, Wallet } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { opsApi } from '@/api/operations';
import type { Invoice360 } from '@/api/operations';
import { useAuth } from '@/auth/authContext';
import { Attachments } from '@/components/attachments/Attachments';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { ComponentsTab, HistoryTab, MovementsTab, RelatedTab } from './Invoice360Tabs';
import { FlagChips, OpsFact } from './OpsParts';

const TABS = [
  { id: 'components', label: 'Components & Balances' },
  { id: 'movements', label: 'Movements' },
  { id: 'related', label: 'Receipts, Remittances & Adjustments' },
  { id: 'history', label: 'History' },
  { id: 'documents', label: 'Documents' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function TabBody({ tab, view }: Readonly<{ tab: TabId; view: Invoice360 }>) {
  switch (tab) {
    case 'movements':
      return <MovementsTab movements={view.movements} />;
    case 'related':
      return <RelatedTab related={view.related} />;
    case 'history':
      return <HistoryTab history={view.history} />;
    case 'documents':
      return (
        <Attachments
          entityType="OpsInvoice"
          entityId={view.invoice.keys.invoiceNo}
          reference={view.invoice.keys.invoiceNo}
        />
      );
    default:
      return <ComponentsTab view={view} />;
  }
}

function Summary({ view }: Readonly<{ view: Invoice360 }>) {
  const i = view.invoice;
  return (
    <Card>
      <div className="stack">
        <div className="ops-record-facts">
          <ReferenceChip label="Invoice" value={i.keys.invoiceNo} />
          <ReferenceChip label="ARN" value={i.keys.arn} />
          {i.keys.endorsementNo !== undefined && (
            <ReferenceChip label="Endorsement" value={i.keys.endorsementNo} />
          )}
          <StatusBadge status={i.paymentStatus} />
          <FlagChips flags={i.flags} />
        </div>
        <div className="ops-summary">
          <OpsFact icon={UserRound} label="Assured">
            {i.parties.assuredName} <span className="ops-muted">{i.parties.clientCode}</span>
          </OpsFact>
          <OpsFact icon={Building2} label="Insurer(s)">
            {i.shares.map((s) => `${s.insurerCode} ${formatAmount(s.sharePct)}%`).join(', ')}
          </OpsFact>
          <OpsFact icon={CalendarRange} label="Period">
            {formatDate(i.classification.inceptionDate)} – {formatDate(i.classification.expiryDate)}
          </OpsFact>
          <OpsFact icon={ReceiptText} label="Remittance">
            {humanize(i.remittanceStatus)}
          </OpsFact>
          <OpsFact icon={Wallet} label="Gross / Outstanding Premium">
            {i.classification.currency} {formatAmount(i.grossPremium)}{' '}
            <span className="ops-muted">/ {formatAmount(i.premiumBalance)}</span>
          </OpsFact>
          <OpsFact icon={Landmark} label="Booked">
            {formatDate(i.classification.bookingDate)}{' '}
            <span className="ops-muted">
              {i.classification.costCenter} {i.classification.aoUsername}
            </span>
          </OpsFact>
        </div>
      </div>
    </Card>
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
  const [tab, setTab] = useState<TabId>('components');
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
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <TabBody tab={tab} view={v} />
    </div>
  );
}
