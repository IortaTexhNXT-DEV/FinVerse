import { InsurerName } from '@/components/broking/LovLabel';
import type { OpsInvoiceSummary } from '@/api/operations';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { OriginBadge } from '@/components/ui/OriginBadge';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { formatDate } from '@/utils/format';
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
    width: '150px',
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
];
