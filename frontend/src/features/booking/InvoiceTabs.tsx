import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { bookingApi } from '@/api/booking';
import type {
  BookedInvoice,
  Endorsement,
  OpenItemLine,
  PostedLine,
  ServiceInvoice,
} from '@/api/booking';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { JournalLines } from './BookingParts';
import { labelOf } from './bookingForm';

/** Posted journal lines of an invoice, with links to the journals. */
export function JournalTab({ invoice }: Readonly<{ invoice: BookedInvoice }>) {
  const lines = useQuery({
    queryKey: ['booking', 'journal', invoice.id],
    queryFn: () => bookingApi.journal(invoice.id),
  });
  const batches = [...new Map((lines.data ?? []).map((l) => [l.batchNo, l.batchId])).entries()];
  return (
    <Card title="Journal">
      <div className="stack">
        <ErrorAlert error={lines.error} />
        <div className="row">
          {batches.map(([batchNo, batchId]) => (
            <Link key={batchNo} to={`/gl/journals/${String(batchId)}`}>
              Open Journal {batchNo}
            </Link>
          ))}
        </div>
        <JournalLines
          lines={(lines.data ?? []).map((l: PostedLine) => ({ ...l, insurerCode: l.batchNo }))}
          emptyMessage="Nothing posted for this invoice."
          groupHeader="Journal"
        />
      </div>
    </Card>
  );
}

/** Sub-ledger open items of an invoice: client PR, DTIP and commission receivable. */
export function OpenItemsTab({ invoice }: Readonly<{ invoice: BookedInvoice }>) {
  const items = useQuery({
    queryKey: ['booking', 'open-items', invoice.id],
    queryFn: () => bookingApi.openItems(invoice.id),
  });
  return (
    <Card title="Open items" flush>
      <ErrorAlert error={items.error} />
      <DataTable<OpenItemLine>
        caption="Open items"
        loading={items.isLoading}
        rows={items.data ?? []}
        rowKey={(i) => i.id}
        emptyMessage="No open items."
        columns={[
          { key: 'role', header: 'Item', render: (i) => humanize(i.role) },
          { key: 'party', header: 'Party', render: (i) => i.partyCode },
          { key: 'direction', header: 'Direction', render: (i) => humanize(i.direction) },
          { key: 'type', header: 'Document', render: (i) => humanize(i.documentType) },
          { key: 'due', header: 'Due', render: (i) => formatDate(i.dueDate) },
          { key: 'amount', header: 'Amount', numeric: true, render: (i) => formatAmount(i.amount) },
          {
            key: 'outstanding',
            header: 'Outstanding',
            numeric: true,
            render: (i) => formatAmount(i.outstanding),
          },
          { key: 'status', header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
        ]}
      />
    </Card>
  );
}

/** Service invoices and credits of an invoice (BRNB.100). */
export function ServiceInvoicesTab({ invoice }: Readonly<{ invoice: BookedInvoice }>) {
  const sis = useQuery({
    queryKey: ['booking', 'service-invoices', invoice.invoiceNo],
    queryFn: () => bookingApi.serviceInvoicesOf(invoice.invoiceNo ?? ''),
    enabled: invoice.invoiceNo !== undefined,
  });
  return (
    <Card title="Service invoices" flush>
      <ErrorAlert error={sis.error} />
      <ServiceInvoiceTable rows={sis.data ?? []} loading={sis.isLoading} />
    </Card>
  );
}

/** A list of service invoices linking to their page. */
export function ServiceInvoiceTable({
  rows,
  loading,
}: Readonly<{ rows: ServiceInvoice[]; loading: boolean }>) {
  return (
    <DataTable<ServiceInvoice>
      caption="Service invoices"
      loading={loading}
      rows={rows}
      rowKey={(s) => s.id}
      emptyMessage="No service invoice."
      columns={[
        {
          key: 'no',
          header: 'Service Invoice No.',
          render: (s) => <Link to={`/booking/service-invoices/${String(s.id)}`}>{s.siNo}</Link>,
        },
        { key: 'kind', header: 'Kind', render: (s) => <StatusBadge status={s.kind} /> },
        { key: 'type', header: 'Type', render: (s) => s.typeCode },
        { key: 'recipient', header: 'Recipient', render: (s) => s.recipientName },
        { key: 'invoice', header: 'Invoice No.', render: (s) => s.invoiceNo ?? '' },
        { key: 'date', header: 'Issue Date', render: (s) => formatDate(s.issueDate) },
        { key: 'net', header: 'Net', numeric: true, render: (s) => formatAmount(s.netAmount) },
        {
          key: 'dispatch',
          header: 'E-mail',
          render: (s) => <StatusBadge status={s.dispatchStatus} />,
        },
      ]}
    />
  );
}

/** Endorsements and cancellations of the account. */
export function EndorsementsTab({ invoice }: Readonly<{ invoice: BookedInvoice }>) {
  const rows = useQuery({
    queryKey: ['booking', 'account-endorsements', invoice.arn],
    queryFn: () => bookingApi.accountEndorsements(invoice.arn),
  });
  return (
    <Card title="Endorsements" flush>
      <ErrorAlert error={rows.error} />
      <EndorsementTable rows={rows.data ?? []} loading={rows.isLoading} />
    </Card>
  );
}

/** A list of endorsements. */
export function EndorsementTable({
  rows,
  loading,
}: Readonly<{ rows: Endorsement[]; loading: boolean }>) {
  return (
    <DataTable<Endorsement>
      caption="Endorsements"
      loading={loading}
      rows={rows}
      rowKey={(e) => e.id}
      emptyMessage="No endorsement."
      columns={[
        { key: 'no', header: 'Endorsement No.', render: (e) => e.endorsementNo },
        { key: 'arn', header: 'ARN', render: (e) => <ReferenceChip value={e.arn} /> },
        { key: 'type', header: 'Type', render: (e) => labelOf(e.cancellationKind ?? e.type) },
        { key: 'effective', header: 'Effective', render: (e) => formatDate(e.effectiveDate) },
        { key: 'description', header: 'Description', render: (e) => e.description },
        { key: 'invoice', header: 'Invoice No.', render: (e) => e.invoiceNo ?? 'Not financial' },
        { key: 'by', header: 'By', render: (e) => e.createdBy },
      ]}
    />
  );
}

/** Every invoice of the account: policy years (booked or scheduled), endorsements, cancellation. */
export function ScheduleTab({ invoice }: Readonly<{ invoice: BookedInvoice }>) {
  const rows = useQuery({
    queryKey: ['booking', 'schedule', invoice.arn],
    queryFn: () => bookingApi.schedule(invoice.arn),
  });
  return (
    <Card title="Invoices of the account" flush>
      <ErrorAlert error={rows.error} />
      <DataTable<BookedInvoice>
        caption="Invoices of the account"
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(i) => i.id}
        columns={[
          {
            key: 'no',
            header: 'Invoice No.',
            render: (i) =>
              i.invoiceNo ? (
                <Link to={`/booking/invoices/${String(i.id)}`}>{i.invoiceNo}</Link>
              ) : (
                'Not yet numbered'
              ),
          },
          { key: 'kind', header: 'Kind', render: (i) => labelOf(i.kind) },
          { key: 'year', header: 'Policy Year', render: (i) => i.policyYear },
          { key: 'policy', header: 'Policy No.', render: (i) => i.policyNo ?? '' },
          {
            key: 'period',
            header: 'Period',
            render: (i) => `${formatDate(i.inceptionDate)} – ${formatDate(i.expiryDate)}`,
          },
          {
            key: 'gross',
            header: 'Gross',
            numeric: true,
            render: (i) => formatAmount(i.premium.total),
          },
          { key: 'status', header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
        ]}
      />
    </Card>
  );
}
