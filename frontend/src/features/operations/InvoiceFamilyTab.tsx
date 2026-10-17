import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { opsApi } from '@/api/operations';
import type { OpsInvoice } from '@/api/operations';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { familyTotals } from './opsLabels';

function columns(current: string): Column<OpsInvoice>[] {
  return [
    {
      key: 'no',
      header: 'Invoice No.',
      render: (i) =>
        i.keys.invoiceNo === current ? (
          <strong>{i.keys.invoiceNo} (this invoice)</strong>
        ) : (
          <Link
            to={`/operations/invoices/${encodeURIComponent(i.keys.invoiceNo)}`}
            onClick={(e) => e.stopPropagation()}
          >
            {i.keys.invoiceNo}
          </Link>
        ),
    },
    {
      key: 'kind',
      header: 'Transaction',
      render: (i) => (
        <>
          {humanize(i.keys.kind)}
          <div className="ops-muted">{i.keys.endorsementNo ?? i.keys.policyNo ?? ''}</div>
        </>
      ),
    },
    { key: 'booked', header: 'Booked', render: (i) => formatDate(i.classification.bookingDate) },
    {
      key: 'gross',
      header: 'Gross Premium',
      numeric: true,
      render: (i) => <Amount value={i.grossPremium} />,
    },
    {
      key: 'balance',
      header: 'Outstanding',
      numeric: true,
      render: (i) => <Amount value={i.premiumBalance} />,
    },
    { key: 'pay', header: 'Payment', render: (i) => <StatusBadge status={i.paymentStatus} /> },
    {
      key: 'remit',
      header: 'Remittance',
      render: (i) => <StatusBadge status={i.remittanceStatus} />,
    },
  ];
}

/**
 * The invoice family (DIS 3.27.2, ACSL 2.5.3/2.16.0): the original booking with every endorsement
 * and cancellation sharing its root invoice number, root first, with the family totals.
 */
export function InvoiceFamilyTab({ invoiceNo }: Readonly<{ invoiceNo: string }>) {
  const family = useQuery({
    queryKey: ['ops', 'invoice', invoiceNo, 'family'],
    queryFn: () => opsApi.family(invoiceNo),
  });
  const rows = family.data ?? [];
  const totals = familyTotals(rows);
  const root = rows[0]?.keys.rootInvoiceNo ?? invoiceNo;
  return (
    <Card title={`Invoice Family of ${root}`}>
      <div className="stack">
        <ErrorAlert error={family.error} />
        <DataTable
          caption="Invoices of the family"
          columns={columns(invoiceNo)}
          rows={rows}
          rowKey={(i) => i.keys.invoiceNo}
          loading={family.isLoading}
          emptyMessage="No related invoice"
        />
        {rows.length > 0 && (
          <dl className="detail-list">
            <dt>Invoices in the family</dt>
            <dd>{rows.length}</dd>
            <dt>Family gross premium</dt>
            <dd className="num">{formatAmount(totals.gross)}</dd>
            <dt>Family outstanding premium</dt>
            <dd className="num">{formatAmount(totals.outstanding)}</dd>
          </dl>
        )}
      </div>
    </Card>
  );
}
