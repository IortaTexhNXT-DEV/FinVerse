import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import type { Commission, Premium, PreviewLine } from '@/api/booking';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount } from '@/utils/format';
import { journalTotals } from './bookingForm';
import './booking.css';

/** One icon + label / value fact of a record summary card. */
export function SummaryFact({
  icon: Icon,
  label,
  children,
}: Readonly<{ icon: LucideIcon; label: string; children: ReactNode }>) {
  return (
    <div className="summary-fact">
      <Icon size={20} aria-hidden="true" />
      <div>
        <div className="summary-fact-label">{label}</div>
        <div className="summary-fact-value">{children}</div>
      </div>
    </div>
  );
}

interface AmountLine {
  label: string;
  amount: number;
}

function premiumLines(premium: Premium): AmountLine[] {
  return [
    { label: 'Basic premium', amount: premium.basic },
    { label: 'Documentary stamp tax', amount: premium.dst },
    { label: 'Premium tax / VAT', amount: premium.premiumTaxVat },
    { label: 'Local government tax', amount: premium.lgt },
    { label: 'Fire service tax', amount: premium.fst },
    { label: 'Other charges', amount: premium.other },
    { label: 'Gross premium', amount: premium.total },
  ];
}

function commissionLines(commission: Commission): AmountLine[] {
  return [
    { label: `Commission (${commission.rate}%)`, amount: commission.commission },
    { label: 'VAT on commission', amount: commission.vatOnCommission },
    {
      label: `Withholding tax (${commission.wtaxRate}%)`,
      amount: -commission.wtaxAmount,
    },
    { label: 'Net commission', amount: commission.net },
  ];
}

function AmountTable({ caption, lines }: Readonly<{ caption: string; lines: AmountLine[] }>) {
  return (
    <DataTable<AmountLine>
      caption={caption}
      rows={lines}
      rowKey={(l) => l.label}
      columns={[
        { key: 'label', header: caption, render: (l) => l.label },
        { key: 'amount', header: 'Amount', numeric: true, render: (l) => formatAmount(l.amount) },
      ]}
    />
  );
}

/** Premium by component and commission of an invoice (BRNB.027). */
export function PremiumTables({
  premium,
  commission,
}: Readonly<{ premium: Premium; commission: Commission }>) {
  return (
    <div className="grid-2">
      <AmountTable caption="Premium component" lines={premiumLines(premium)} />
      <AmountTable caption="Commission" lines={commissionLines(commission)} />
    </div>
  );
}

/** Journal lines with their debit / credit totals. */
export function JournalLines({
  lines,
  emptyMessage = 'No journal lines.',
  groupHeader = 'Insurer',
}: Readonly<{ lines: PreviewLine[]; emptyMessage?: string; groupHeader?: string }>) {
  const totals = journalTotals(lines);
  return (
    <div className="stack">
      <DataTable<PreviewLine & { key: string }>
        caption="Journal"
        rows={lines.map((l, i) => ({ ...l, key: `${String(i)}-${l.accountCode}` }))}
        rowKey={(l) => l.key}
        emptyMessage={emptyMessage}
        columns={[
          { key: 'group', header: groupHeader, render: (l) => l.insurerCode },
          {
            key: 'account',
            header: 'Account',
            render: (l) => (
              <>
                <span className="mono">{l.accountCode}</span> {l.accountName}
              </>
            ),
          },
          { key: 'party', header: 'Party', render: (l) => l.partyCode ?? '' },
          {
            key: 'debit',
            header: 'Debit',
            numeric: true,
            render: (l) => (l.side === 'DEBIT' ? formatAmount(l.amount) : ''),
          },
          {
            key: 'credit',
            header: 'Credit',
            numeric: true,
            render: (l) => (l.side === 'CREDIT' ? formatAmount(l.amount) : ''),
          },
        ]}
      />
      {lines.length > 0 && (
        <div className="row">
          <span>
            Total debit <strong>{formatAmount(totals.debit)}</strong>
          </span>
          <span>
            Total credit <strong>{formatAmount(totals.credit)}</strong>
          </span>
          <StatusBadge status={totals.balanced ? 'BALANCED' : 'UNBALANCED'} />
        </div>
      )}
    </div>
  );
}
