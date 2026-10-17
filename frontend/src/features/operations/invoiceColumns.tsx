import type { ReactNode } from 'react';
import { InsurerName } from '@/components/broking/LovLabel';
import type { OpsInvoiceSummary } from '@/api/operations';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { OriginBadge } from '@/components/ui/OriginBadge';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { Link } from 'react-router-dom';
import { formatDate, formatDateTime } from '@/utils/format';
import { FlagChips } from './OpsParts';

/** Columns of Invoice Search: one value per cell with its second line muted under it. */
export const INVOICE_COLUMNS: Column<OpsInvoiceSummary>[] = [
  {
    key: 'no',
    header: 'Invoice No.',
    render: (i) => (
      <>
        <strong>{i.invoiceNo}</strong> <OriginBadge record={i} />
        <div className="ops-muted">
          {i.legacyInvoiceNo && i.legacyInvoiceNo !== i.invoiceNo
            ? `${i.arn} · legacy ${i.legacyInvoiceNo}`
            : i.arn}
        </div>
      </>
    ),
  },
  {
    key: 'client',
    header: 'Assured / Client Code',
    render: (i) => (
      <>
        {i.assuredName}
        <div className="ops-muted">{i.clientCode}</div>
      </>
    ),
  },
  { key: 'insurer', header: 'Insurer', render: (i) => <InsurerName code={i.insurerCode} /> },
  {
    key: 'date',
    header: 'Booked / Inception',
    render: (i) => (
      <>
        {formatDate(i.bookingDate)}
        <div className="ops-muted">{formatDate(i.inceptionDate)}</div>
      </>
    ),
  },
  {
    key: 'ao',
    header: 'Account Officer',
    defaultHidden: true,
    width: '180px',
    truncate: true,
    render: (i) => <UserName login={i.aoUsername} truncate />,
  },
  {
    // Gross premium with the outstanding premium under it (FRS: one Gross Premium / Outstanding
    // column).
    key: 'gross',
    header: 'Gross Premium / Outstanding',
    numeric: true,
    render: (i) => (
      <>
        <Amount value={i.grossPremium} />
        <div className="ops-muted">
          <Amount value={i.premiumBalance} />
        </div>
      </>
    ),
  },
  {
    // The payment status with the remittance status under it (FRS: one Status / Remittance
    // column), so the list fits its card without scrolling sideways.
    key: 'status',
    header: 'Status / Remittance',
    kind: 'status',
    render: (i) => (
      <span className="cell-stack">
        <span>
          <StatusBadge status={i.paymentStatus} />
        </span>
        <span>
          <StatusBadge status={i.remittanceStatus} />
        </span>
      </span>
    ),
  },
  { key: 'flags', header: 'Flags', render: (i) => <FlagChips flags={i.flags} /> },
  // The Invoice Master List values of the account (BR-175, FR-CR-090), chosen with Columns.
  {
    key: 'cover',
    header: 'Cover / Version',
    defaultHidden: true,
    render: (i) => (
      <>
        {i.master?.coverNo ?? '—'}
        <div className="ops-muted">
          {i.master?.versionNo === undefined || i.master.versionNo === null
            ? '—'
            : `Version ${i.master.versionNo}`}
        </div>
      </>
    ),
  },
  {
    key: 'assignees',
    header: 'Marketing / Processing',
    defaultHidden: true,
    width: '180px',
    render: (i) => (
      <>
        <UserName login={i.master?.marketing} truncate />
        <div className="ops-muted">
          <UserName login={i.master?.processing} truncate />
        </div>
      </>
    ),
  },
  {
    key: 'placement',
    header: 'Placement Status',
    kind: 'status',
    defaultHidden: true,
    render: (i) =>
      i.master?.placementStatus ? <StatusBadge status={i.master.placementStatus} /> : '—',
  },
  {
    key: 'approved',
    header: 'Approved by Processing',
    defaultHidden: true,
    render: (i) => (i.master?.approvedAt ? formatDateTime(i.master.approvedAt) : '—'),
  },
  {
    key: 'cancellation',
    header: 'Cancellation',
    defaultHidden: true,
    render: (i) => cancellation(i),
  },
];

/** "In force", or the cancellation of the invoice or its cover with the date and the reason. */
function cancellation(i: OpsInvoiceSummary): ReactNode {
  const on = i.master?.cancelledOn;
  if (!i.flags.cancelled && (on === undefined || on === null)) {
    return <>In force</>;
  }
  return (
    <>
      {on ? `Cancelled ${formatDate(on)}` : 'Cancelled'}
      {i.master?.cancellationReason && (
        <div className="ops-muted">{i.master.cancellationReason}</div>
      )}
    </>
  );
}

/** The audit trail of an invoice since its booking (Administration > Audit Trail). */
export function invoiceAuditLink(i: OpsInvoiceSummary): string {
  const params = new URLSearchParams({
    entityType: 'OpsInvoice',
    entityId: i.invoiceNo,
    from: i.bookingDate,
  });
  return `/admin/audit?${params.toString()}`;
}

/** Link to the audit trail of the invoice, for the users who may read the audit trail. */
export const AUDIT_COLUMN: Column<OpsInvoiceSummary> = {
  key: 'audit',
  header: 'Audit Trail',
  defaultHidden: true,
  render: (i) => (
    <Link to={invoiceAuditLink(i)} onClick={(e) => e.stopPropagation()}>
      Audit Trail
    </Link>
  ),
};
