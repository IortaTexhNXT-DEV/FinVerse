import { BadgePercent, Coins } from 'lucide-react';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import type { InvoicePreview, PaymentPreview } from './cashieringApi';
import { allocationRows, componentLabel, matchMessage, previewTotals } from './cashieringLogic';

function InvoiceAllocation({ invoice }: Readonly<{ invoice: InvoicePreview }>) {
  const rows = allocationRows(invoice.balances);
  return (
    <div className="csh-invoice">
      <div className="csh-invoice-head">
        <span>
          <strong>{invoice.invoiceNo}</strong>
          <span className="cell-sub">
            {invoice.arn} · {invoice.assuredName ?? ''}
          </span>
        </span>
        <span className="tag-list">
          {invoice.cwt && <span className="tag">2% CWT</span>}
          {!invoice.receivable && <StatusBadge status="NOT_APPLICABLE" />}
        </span>
      </div>
      <table className="csh-alloc">
        <caption className="visually-hidden">
          Application of {invoice.invoiceNo} by component
        </caption>
        <thead>
          <tr>
            <th scope="col">Component</th>
            <th scope="col">Balance</th>
            <th scope="col">Applied</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.component}>
              <td>{componentLabel(r.component)}</td>
              <td>
                <Amount value={r.amount} />
              </td>
              <td>
                <Amount value={invoice.allocation[r.component] ?? 0} />
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td>Total</td>
            <td>
              <Amount value={invoice.outstanding} />
            </td>
            <td>
              <Amount value={invoice.applied} />
            </td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
}

/**
 * Live application preview of the over-the-counter screen (CSHID.020/022): what the payment
 * matches, how it is applied component by component (DST, VAT, LGT, other, basic), the 98% CWT
 * cap of 2307 clients, the excess that stays unapplied and the BOOK rate of the currency.
 */
export function PaymentPreviewPane({
  preview,
  loading,
  error,
}: Readonly<{ preview: PaymentPreview | undefined; loading: boolean; error: unknown }>) {
  const totals = previewTotals(preview);
  return (
    <Card title="Application Preview">
      <div className="stack">
        <div className="csh-preview-head">
          {preview && <StatusBadge status={preview.match} />}
          {preview && (
            <span className="csh-rate-chip" title="BOOK rate used for the posting">
              <Coins size={14} aria-hidden="true" />
              {preview.currency} · BOOK {preview.bookRate.toFixed(4)}
            </span>
          )}
          {totals.cwt && (
            <span className="tag" title="2307 client: applied up to 98% of the premium due">
              <BadgePercent size={12} aria-hidden="true" /> CWT {preview?.cwtPercent ?? 98}% applied
            </span>
          )}
        </div>
        <p className="muted">{loading ? 'Calculating…' : matchMessage(preview)}</p>
        <ErrorAlert error={error} />
        {preview?.invoices.length === 0 && <EmptyState message="No booked invoice to apply to" />}
        {preview?.invoices.map((i) => (
          <InvoiceAllocation key={i.invoiceNo} invoice={i} />
        ))}
        <div className="csh-totals">
          <div className="csh-total">
            <span className="csh-total-label">Applied</span>
            <span className="csh-total-value">
              <Amount value={totals.applied} />
            </span>
          </div>
          <div className="csh-total">
            <span className="csh-total-label">2% CWT Withheld</span>
            <span className="csh-total-value">
              <Amount value={preview?.cwtWithheld ?? 0} />
            </span>
          </div>
          <div className="csh-total csh-total-excess">
            <span className="csh-total-label">Excess to Unapplied</span>
            <span className="csh-total-value">
              <Amount value={totals.excess} />
            </span>
          </div>
        </div>
      </div>
    </Card>
  );
}
