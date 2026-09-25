import { Link } from 'react-router-dom';
import type {
  Invoice360,
  InvoiceComponentRow,
  Movement,
  RelatedItem,
  RelatedSection,
  StatusChange,
} from '@/api/operations';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { componentLabel, premiumTotals, visibleComponents } from './opsLabels';

const COMPONENT_COLUMNS: Column<InvoiceComponentRow>[] = [
  { key: 'c', header: 'Component', render: (r) => componentLabel(r.component) },
  { key: 'b', header: 'Booked', numeric: true, render: (r) => <Amount value={r.booked} /> },
  { key: 'a', header: 'Applied', numeric: true, render: (r) => <Amount value={r.applied} /> },
  { key: 'r', header: 'Reversed', numeric: true, render: (r) => <Amount value={r.reversed} /> },
  { key: 'm', header: 'Remitted', numeric: true, render: (r) => <Amount value={r.remitted} /> },
  { key: 'd', header: 'Adjusted', numeric: true, render: (r) => <Amount value={r.adjusted} /> },
  {
    key: 'w',
    header: 'Written Off',
    numeric: true,
    render: (r) => <Amount value={r.writtenOff} />,
  },
  {
    key: 'o',
    header: 'Balance',
    numeric: true,
    render: (r) => <strong>{formatAmount(r.balance)}</strong>,
  },
];

const MOVEMENT_COLUMNS: Column<Movement>[] = [
  { key: 'd', header: 'Value Date', render: (m) => formatDate(m.valueDate) },
  { key: 't', header: 'Movement', render: (m) => humanize(m.type) },
  { key: 'c', header: 'Component', render: (m) => componentLabel(m.component) },
  { key: 'a', header: 'Amount', numeric: true, render: (m) => <Amount value={m.amount} /> },
  {
    key: 's',
    header: 'Source',
    render: (m) => (
      <>
        {humanize(m.sourceModule)} <span className="ops-muted">{m.sourceRef}</span>
      </>
    ),
  },
  {
    key: 'r',
    header: 'Documents',
    render: (m) => [m.arNo, m.orNo, m.batchNo, m.journalBatchNo].filter(Boolean).join(' · '),
  },
  { key: 'u', header: 'Posted', render: (m) => `${formatDateTime(m.postedAt)} · ${m.postedBy}` },
];

const HISTORY_COLUMNS: Column<StatusChange>[] = [
  { key: 't', header: 'When', render: (c) => formatDateTime(c.changedAt) },
  { key: 'f', header: 'What', render: (c) => humanize(c.field) },
  { key: 'v', header: 'Change', render: (c) => `${c.from ?? '-'} → ${c.to ?? '-'}` },
  { key: 'm', header: 'Module', render: (c) => humanize(c.module) },
  { key: 'r', header: 'Reason', render: (c) => c.reason ?? '' },
  { key: 'u', header: 'By', render: (c) => c.changedBy },
];

const RELATED_COLUMNS: Column<RelatedItem>[] = [
  { key: 't', header: 'Record', render: (i) => humanize(i.type) },
  {
    key: 'r',
    header: 'Reference',
    render: (i) => (i.link === undefined ? i.reference : <Link to={i.link}>{i.reference}</Link>),
  },
  { key: 'd', header: 'Date', render: (i) => formatDate(i.date) },
  { key: 'a', header: 'Amount', numeric: true, render: (i) => <Amount value={i.amount} /> },
  {
    key: 's',
    header: 'Status',
    render: (i) => (i.status ? <StatusBadge status={i.status} /> : ''),
  },
  { key: 'x', header: 'Description', render: (i) => i.description ?? '' },
];

const RELATED_LABELS: Record<RelatedSection, string> = {
  RECEIPTS: 'Receipts and Applications',
  REMITTANCES: 'Remittances and Holds',
  ADJUSTMENTS: 'Endorsement Requests',
  RECONCILIATION: 'Production Reconciliation',
  COMMISSION: 'Commission Receivables',
  DOCUMENTS: 'Documents',
};

/** Components with their buckets, the premium receivable total, insurer shares and adjustments. */
export function ComponentsTab({ view }: Readonly<{ view: Invoice360 }>) {
  const i = view.invoice;
  const totals = premiumTotals(i.components);
  return (
    <div className="stack">
      <Card title="Components and Balances" flush>
        <DataTable
          caption="Invoice components"
          columns={COMPONENT_COLUMNS}
          rows={visibleComponents(i.components)}
          rowKey={(r) => r.component}
        />
      </Card>
      <div className="ops-balance-strip">
        <Card title="Premium Due">{formatAmount(totals.booked)}</Card>
        <Card title="Premium Collected">{formatAmount(totals.applied)}</Card>
        <Card title="Premium Outstanding">{formatAmount(totals.balance)}</Card>
      </div>
      <Card title="Insurer Shares" flush>
        <DataTable
          caption="Insurer shares"
          columns={[
            { key: 'i', header: 'Insurer', render: (s) => s.insurerCode },
            { key: 'p', header: 'Share %', numeric: true, render: (s) => formatAmount(s.sharePct) },
            { key: 'l', header: 'Lead', render: (s) => (s.lead ? 'Lead insurer' : '') },
          ]}
          rows={i.shares}
          rowKey={(s) => s.insurerCode}
        />
      </Card>
      {view.adjustments !== undefined && (
        <Card title={`Adjustments of ${view.adjustments.originalInvoiceNo}`}>
          <p>
            {view.adjustments.adjustmentCount} adjusting invoice(s): premium{' '}
            {formatAmount(view.adjustments.originalPremium)} adjusted by{' '}
            {formatAmount(view.adjustments.adjustedPremium)}; DTIP{' '}
            {formatAmount(view.adjustments.originalDtip)} adjusted by{' '}
            {formatAmount(view.adjustments.adjustedDtip)}.
          </p>
          {view.adjustments.overAdjusted && <StatusBadge status="OVER_ADJUSTED" />}
        </Card>
      )}
    </div>
  );
}

/** Movements in posting order (RMTID.038 payment and remittance history). */
export function MovementsTab({ movements }: Readonly<{ movements: Movement[] }>) {
  return (
    <Card title="Movements" flush>
      <DataTable
        caption="Invoice movements"
        columns={MOVEMENT_COLUMNS}
        rows={movements}
        rowKey={(m) => m.id}
      />
    </Card>
  );
}

/** Status, flag and lock history (RMTID.032/040). */
export function HistoryTab({ history }: Readonly<{ history: StatusChange[] }>) {
  return (
    <Card title="Status History" flush>
      <DataTable
        caption="Status history"
        columns={HISTORY_COLUMNS}
        rows={history}
        rowKey={(c) => c.id}
        emptyMessage="No status changes yet"
      />
    </Card>
  );
}

/** The records one Operations module keeps for the invoice (one Invoice 360 tab). */
export function RelatedSectionTab({
  section,
  items,
}: Readonly<{ section: RelatedSection; items: RelatedItem[] | undefined }>) {
  const title = RELATED_LABELS[section];
  if (items === undefined || items.length === 0) {
    return (
      <Card title={title}>
        <EmptyState message={`No ${title.toLowerCase()} for this invoice yet`} />
      </Card>
    );
  }
  return (
    <Card title={title} flush>
      <DataTable
        caption={title}
        columns={RELATED_COLUMNS}
        rows={items}
        rowKey={(i) => `${i.type}-${i.reference}`}
      />
    </Card>
  );
}
