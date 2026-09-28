import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { csfApi } from '@/api/csf';
import type { Payment, PaymentApplication } from '@/api/csf';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { TAB_UNAVAILABLE } from './AccountsTab';
import { InBaseCurrency } from '@/components/ui/InBaseCurrency';

const KIND_LABELS: Record<Payment['kind'], string> = {
  PAYMENT: 'Payment',
  REVERSAL: 'Reversal',
  LEGACY: 'Before BIBS',
};

const PAYMENT_COLUMNS: Column<Payment>[] = [
  {
    key: 'receipt',
    header: 'Receipt No.',
    kind: 'code',
    render: (p) => <CellStack main={p.receiptNo} sub={p.orNo && p.arNo ? p.arNo : undefined} />,
  },
  { key: 'date', header: 'Date', kind: 'date', render: (p) => formatDate(p.valueDate) },
  { key: 'mode', header: 'Mode', render: (p) => (p.mode ? humanize(p.mode) : '') },
  {
    key: 'kind',
    header: 'Kind',
    kind: 'status',
    render: (p) => (
      <StatusBadge
        status={p.kind}
        label={KIND_LABELS[p.kind]}
        tone={p.kind === 'REVERSAL' ? 'danger' : 'success'}
      />
    ),
  },
  {
    key: 'amount',
    header: <InBaseCurrency label="Amount" />,
    kind: 'amount',
    render: (p) => formatAmount(p.amount),
  },
];

const APPLICATION_COLUMNS: Column<PaymentApplication>[] = [
  { key: 'invoice', header: 'Invoice', kind: 'code', render: (a) => a.invoiceNo },
  { key: 'arn', header: 'ARN', kind: 'code', render: (a) => a.arn },
  {
    key: 'applied',
    header: <InBaseCurrency label="Applied" />,
    kind: 'amount',
    render: (a) => formatAmount(a.amount),
  },
  {
    key: 'balance',
    header: <InBaseCurrency label="Balance" />,
    kind: 'amount',
    render: (a) => (a.invoiceBalance === null ? '' : formatAmount(a.invoiceBalance)),
  },
  {
    key: 'status',
    header: 'Payment Status',
    kind: 'status',
    render: (a) => (a.invoicePaymentStatus ? <StatusBadge status={a.invoicePaymentStatus} /> : ''),
  },
];

/**
 * The Payments tab (FR-CSF-013; BRCSF-005): the payments of the window (12 months by default),
 * newest first, each with the invoices it paid and their current balance and payment status;
 * filter by account and Show Older to extend the window.
 */
export function PaymentsTab({
  companyId,
  clientId,
  arns,
}: Readonly<{ companyId: number; clientId: number; arns: string[] }>) {
  const [months, setMonths] = useState<number | undefined>(undefined);
  const [arn, setArn] = useState('');
  const history = useQuery({
    queryKey: ['csf', 'payments', companyId, clientId, months, arn],
    queryFn: () => csfApi.payments(companyId, clientId, months, arn),
  });
  const shown = history.data?.months ?? months ?? 12;
  const rows = history.data?.payments ?? [];
  return (
    <Card
      flush
      title={history.data ? `Payments since ${formatDate(history.data.from)}` : 'Payments'}
      actions={
        <div className="filter-bar">
          <div className="field">
            <label htmlFor="csf-payment-arn">Account</label>
            <select
              id="csf-payment-arn"
              className="select"
              value={arn}
              onChange={(e) => setArn(e.target.value)}
            >
              <option value="">All accounts</option>
              {arns.map((a) => (
                <option key={a} value={a}>
                  {a}
                </option>
              ))}
            </select>
          </div>
          <div className="filter-bar-actions">
            <Button
              variant="secondary"
              disabled={shown >= 120}
              onClick={() => setMonths(Math.min(120, shown + 12))}
            >
              Show Older
            </Button>
          </div>
        </div>
      }
    >
      <ErrorAlert
        error={history.error}
        title={TAB_UNAVAILABLE}
        onRetry={() => void history.refetch()}
      />
      <DataTable
        columns={PAYMENT_COLUMNS}
        rows={rows}
        rowKey={(p) => `${p.receiptNo}-${p.kind}`}
        loading={history.isLoading}
        expanded={new Set(rows.map((p) => `${p.receiptNo}-${p.kind}`))}
        renderExpanded={(p) => (
          <DataTable
            columns={APPLICATION_COLUMNS}
            rows={p.applications}
            rowKey={(a) => `${a.invoiceNo}-${a.arn}`}
            caption={`Invoices paid by ${p.receiptNo}`}
          />
        )}
        caption="Payments"
      />
    </Card>
  );
}
