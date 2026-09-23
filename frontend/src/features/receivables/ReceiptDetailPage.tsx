import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { receivablesApi } from '@/api/receivables';
import type { Receipt } from '@/api/receivables';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { ReceiptActions } from './ReceiptActions';

type Allocation = Receipt['allocations'][number];

function Detail({ label, value }: Readonly<{ label: string; value?: string | number | null }>) {
  return (
    <div>
      <div className="muted">{label}</div>
      <div>{value ?? '-'}</div>
    </div>
  );
}

function approval(receipt: Receipt): string | null {
  const by = receipt.summary.approvedBy;
  return by === undefined ? null : `${by} ${formatDateTime(receipt.approvedAt)}`;
}

function reversal(receipt: Receipt): string {
  return `${formatDate(receipt.reversalDate)} ${receipt.reversedBy ?? ''}: ${receipt.reversalReason ?? ''}`;
}

function ReceiptDetails({ receipt }: Readonly<{ receipt: Receipt }>) {
  const r = receipt.summary;
  const cheque = r.mode === 'CHEQUE' || r.mode === 'PDC';
  return (
    <Card title="Receipt details">
      <div className="form-grid">
        <Detail label="Payer type" value={humanize(r.payerType)} />
        <Detail label="Payer" value={`${r.partyCode ?? ''} ${r.payerName}`} />
        <Detail label="Mode" value={humanize(r.mode)} />
        <Detail label={cheque ? 'Cheque no.' : 'Reference'} value={r.instrumentNo} />
        {cheque && <Detail label="Cheque date" value={formatDate(r.instrumentDate)} />}
        {cheque && <Detail label="Drawee bank" value={receipt.draweeBank} />}
        <Detail label="Bank account" value={r.bankAccountCode} />
        <Detail label="Exchange rate" value={receipt.exchangeRate} />
        <Detail label="Base amount" value={formatAmount(receipt.baseAmount)} />
        <Detail label="Allocation" value={humanize(receipt.allocationMethod)} />
        <Detail label="Income account" value={receipt.incomeAccountCode} />
        <Detail label="Narration" value={receipt.narration} />
        <Detail label="Entered by" value={r.createdBy} />
        <Detail label="Approved by" value={approval(receipt)} />
        <Detail label="Journal" value={receipt.journalBatchNo} />
        <Detail label="Deposited on" value={formatDate(r.depositedOn)} />
        {receipt.reversalReason !== undefined && (
          <Detail label="Reversed / rejected" value={reversal(receipt)} />
        )}
      </div>
    </Card>
  );
}

function Allocations({ receipt }: Readonly<{ receipt: Receipt }>) {
  const pendingFifo =
    receipt.allocationMethod === 'FIFO' && receipt.summary.status === 'PENDING_APPROVAL';
  return (
    <Card title="Applied to debit notes" flush>
      <DataTable<Allocation>
        rows={receipt.allocations}
        rowKey={(a) => a.id}
        caption="Allocations"
        emptyMessage={
          pendingFifo
            ? 'Allocated first-in-first-out when the receipt is approved.'
            : 'Nothing allocated: the receipt is held on account.'
        }
        columns={[
          { key: 'doc', header: 'Debit note', render: (a) => a.documentNo },
          {
            key: 'amt',
            header: 'Amount',
            numeric: true,
            render: (a) => <Amount value={a.amount} />,
          },
          { key: 'on', header: 'Applied on', render: (a) => formatDate(a.appliedOn) },
          {
            key: 'st',
            header: 'Matched',
            render: (a) => <StatusBadge status={a.matched ? 'MATCHED' : 'UNMATCHED'} />,
          },
        ]}
      />
    </Card>
  );
}

/** Official receipt: header, amounts, allocations, audit information and workflow actions. */
export default function ReceiptDetailPage() {
  const id = Number(useParams().id);
  const query = useQuery({
    queryKey: ['receipt', id],
    queryFn: () => receivablesApi.receipt(id),
    enabled: id > 0,
  });
  const receipt = query.data;
  if (receipt === undefined) {
    return (
      <div className="stack">
        <ErrorAlert error={query.error} />
        {query.isLoading && <span className="spinner" aria-label="Loading" />}
      </div>
    );
  }
  const r = receipt.summary;
  return (
    <div className="stack">
      <PageHeader
        section="Receivables & Banking"
        title={`Official Receipt ${r.receiptNo}`}
        description={`${r.payerName} · ${formatDate(r.receiptDate)}`}
        actions={<ReceiptActions receipt={receipt} />}
      />
      <div className="grid-4">
        <Kpi label={`Amount (${r.currency})`} value={formatAmount(r.amount)} accent />
        <Kpi label="Applied to debit notes" value={formatAmount(r.appliedAmount)} />
        <Kpi label="On account" value={formatAmount(r.unappliedAmount)} />
        <Kpi
          label="Status"
          value={<StatusBadge status={r.status} />}
          hint={humanize(r.depositStatus)}
        />
      </div>
      <ReceiptDetails receipt={receipt} />
      <Allocations receipt={receipt} />
    </div>
  );
}
