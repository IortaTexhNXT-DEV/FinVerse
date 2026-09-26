import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDateTime } from '@/utils/format';
import type { Batch } from './api';
import { deductionsApi } from './deductionsApi';
import type { DeductionApplication } from './deductionsApi';
import './remittance.css';

const COLUMNS: Column<DeductionApplication>[] = [
  {
    key: 'ded',
    header: 'Deduction',
    render: (a) => (
      <Link to={`/remittance/deductions/${a.deductionId}`} onClick={(e) => e.stopPropagation()}>
        {a.deductionNo}
      </Link>
    ),
  },
  { key: 'cycle', header: 'Send Cycle', render: (a) => a.sendCycle },
  { key: 'on', header: 'Applied On', render: (a) => formatDateTime(a.createdAt) },
  { key: 'jv', header: 'Journal', render: (a) => a.journalBatchNo ?? '' },
  { key: 'amt', header: 'Amount', numeric: true, render: (a) => <Amount value={a.amount} /> },
  {
    key: 'st',
    header: 'Status',
    render: (a) => <StatusBadge status={a.reversed ? 'REVERSED' : 'POSTED'} />,
  },
];

/**
 * How the batch is settled with the insurer (DIS 3.29.1/3.29.2, ACSL 2.9.2, DIS 2.20.0): the early
 * and CPC2 incentives deducted, the early-incentive service invoice with its withholding tax, the
 * insurer deductions applied (capped at the amount payable), the amount due to the insurer and the
 * last cancelled DV with the send cycle of the current payment request.
 */
export function BatchSettlementTab({ batch }: Readonly<{ batch: Batch }>) {
  const s = batch.settlement;
  const t = batch.summary.totals;
  const applications = useQuery({
    queryKey: ['remittance', 'batch', batch.summary.id, 'deductions'],
    queryFn: () => deductionsApi.ofBatch(batch.summary.id),
  });
  return (
    <div className="stack">
      <Card title="Settlement">
        <dl className="detail-list">
          <dt>Net due</dt>
          <dd className="num">{formatAmount(t.netDue)}</dd>
          <dt>Early incentive with VAT</dt>
          <dd className="num">{formatAmount(t.incentive + t.incentiveVat)}</dd>
          <dt>CPC2 incentive with VAT</dt>
          <dd className="num">{formatAmount(t.cpc2 + t.cpc2Vat)}</dd>
          <dt>Deductions applied</dt>
          <dd className="num">{formatAmount(s.deductionAmount)}</dd>
          <dt>Amount due to the insurer</dt>
          <dd className="num">
            <strong>
              {batch.summary.currency} {formatAmount(s.amountDue)}
            </strong>
          </dd>
          <dt>Early-incentive service invoice</dt>
          <dd>
            {s.earlySiNo === undefined
              ? 'None'
              : `${s.earlySiNo} · withholding tax ${formatAmount(s.earlySiWtax)}`}
          </dd>
          <dt>Payment request reference</dt>
          <dd>
            {s.reference}
            {s.sendCycle > 1 ? ` (sent ${s.sendCycle} times)` : ''}
          </dd>
          <dt>Last cancelled DV</dt>
          <dd>
            {s.cancelledDvNo === undefined
              ? 'None'
              : `${s.cancelledDvNo} · ${s.cancelReason ?? ''} · ${formatDateTime(s.cancelledAt)}`}
          </dd>
        </dl>
      </Card>
      <Card title="Insurer Deductions">
        <ErrorAlert error={applications.error} />
        <DataTable
          caption="Deductions applied to this batch"
          columns={COLUMNS}
          rows={applications.data ?? []}
          rowKey={(a) => a.id}
          loading={applications.isLoading}
          emptyMessage="No deduction applied to this batch"
        />
      </Card>
    </div>
  );
}
