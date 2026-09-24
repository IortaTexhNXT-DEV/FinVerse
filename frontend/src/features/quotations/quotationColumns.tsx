import type { QuotationListItem } from '@/api/quotations';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Amount } from '@/components/ui/Amount';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, today } from '@/utils/format';
import { daysLeft } from './quotationList';

const EXPIRY_WARNING_DAYS = 7;

function validity(q: QuotationListItem) {
  const left = daysLeft(q.validUntil, today());
  const open = q.status === 'APPROVED' || q.status === 'SENT_TO_CLIENT';
  return (
    <span>
      {formatDate(q.validUntil)}
      {open && left <= EXPIRY_WARNING_DAYS && (
        <div className={left < 0 ? 'text-danger' : 'muted'}>
          {left < 0 ? 'Expired' : `${left} day(s) left`}
        </div>
      )}
    </span>
  );
}

/** Columns of the quotation work list. */
export const QUOTATION_COLUMNS: Column<QuotationListItem>[] = [
  {
    key: 'no',
    header: 'Proposal No.',
    render: (q) => (
      <>
        <strong className="mono">{q.quotationNo}</strong>
        <div className="muted">v{q.currentVersion}</div>
      </>
    ),
  },
  {
    key: 'arn',
    header: 'ARN',
    render: (q) => (
      <span onClick={(e) => e.stopPropagation()} role="presentation">
        <ReferenceChip value={q.arn} />
      </span>
    ),
  },
  {
    key: 'client',
    header: 'Client',
    render: (q) => (
      <>
        <strong>{q.clientName}</strong>
        <div className="muted">{q.clientCode}</div>
      </>
    ),
  },
  { key: 'product', header: 'Product', render: (q) => q.productCode },
  { key: 'insurer', header: 'Insurer', render: (q) => q.insurerCode ?? 'To be advised' },
  {
    key: 'gross',
    header: 'Gross Premium',
    numeric: true,
    render: (q) => <Amount value={q.grossPremium} />,
  },
  { key: 'valid', header: 'Valid Until', render: validity },
  {
    key: 'status',
    header: 'Status',
    render: (q) => (
      <div className="row">
        <StatusBadge status={q.status} />
        {q.directPayment && <StatusBadge status="DIRECT_PAYMENT" />}
      </div>
    ),
  },
];
