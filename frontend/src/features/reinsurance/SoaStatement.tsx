import type { Soa } from '@/api/reinsurance';
import { Amount } from '@/components/ui/Amount';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';
import { statementRows } from './soa';

/** Printable statement of account: income / outgo with the balance on the smaller side. */
export function SoaStatement({ soa }: Readonly<{ soa: Soa }>) {
  const rows = statementRows(soa.layout);
  return (
    <div className="stack">
      <div className="grid-2">
        <div>
          <div className="muted">Reinsurer</div>
          <strong>
            {soa.reinsurerCode} – {soa.reinsurerName}
          </strong>
          <div className="muted">
            Treaty {soa.treatyCode} – {soa.treatyName}
          </div>
        </div>
        <div>
          <div>
            Statement {soa.soaNo} <StatusBadge status={soa.status} />
          </div>
          <div className="muted">
            Q{soa.quarter} {soa.year}: {formatDate(soa.periodFrom)} – {formatDate(soa.periodTo)}
          </div>
          <div className="muted">
            Statement date {formatDate(soa.statementDate)}, amounts in {soa.currency}
          </div>
        </div>
      </div>
      <div className="table-wrap">
        <table className="table">
          <caption className="visually-hidden">Statement of account {soa.soaNo}</caption>
          <thead>
            <tr>
              <th>Particulars</th>
              <th className="num">Income</th>
              <th className="num">Outgo</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.label} className={r.kind === 'line' ? undefined : 'total-row'}>
                <td>{r.kind === 'line' ? r.label : <strong>{r.label}</strong>}</td>
                <td className="num">{r.income === undefined ? '' : <Amount value={r.income} />}</td>
                <td className="num">{r.outgo === undefined ? '' : <Amount value={r.outgo} />}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p>
        <strong>Amount in words:</strong> {soa.layout.amountInWords}
      </p>
      {soa.settlementDate !== undefined && (
        <p className="muted">
          Settled on {formatDate(soa.settlementDate)} through {soa.bankAccountCode} (journal{' '}
          {soa.settlementBatchNo}).
        </p>
      )}
    </div>
  );
}
