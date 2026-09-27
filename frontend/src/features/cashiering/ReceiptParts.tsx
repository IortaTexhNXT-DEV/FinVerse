import { Banknote, CalendarDays, Coins, CreditCard, Printer, Scale, User } from 'lucide-react';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, humanize } from '@/utils/format';
import type { ReceiptDetail } from './cashieringApi';

/** Summary card of a receipt: payor, number, status, flags and key facts. */
export function ReceiptSummaryCard({ receipt }: Readonly<{ receipt: ReceiptDetail }>) {
  const s = receipt.summary;
  const mode = [humanize(s.mode), receipt.checkNo].filter(Boolean).join(' · ');
  return (
    <RecordSummary
      title={s.payorName}
      chips={
        <>
          <ReferenceChip value={s.receiptNo} label={s.kind} />
          <StatusBadge status={s.status} />
        </>
      }
      flags={
        <>
          {s.unappliedAmount > 0 && <span className="tag">With Unapplied</span>}
          {s.printedCount > 0 && <span className="tag">Printed {s.printedCount}x</span>}
          {receipt.reinstatedAmount !== undefined && <span className="tag">Reinstated</span>}
        </>
      }
      facts={[
        { icon: CalendarDays, label: 'Receipt Date', value: formatDate(s.receiptDate) },
        { icon: User, label: 'Payor', value: s.payorName },
        { icon: Banknote, label: 'Amount', value: <Amount value={s.amount} /> },
        {
          icon: Scale,
          label: 'Applied / Unapplied',
          value: `${s.appliedAmount.toFixed(2)} / ${s.unappliedAmount.toFixed(2)}`,
        },
        { icon: CreditCard, label: 'Mode', value: mode },
        {
          icon: Coins,
          label: 'Currency / BOOK Rate',
          value: `${s.currency} · ${receipt.bookRate.toFixed(4)}`,
        },
      ]}
    />
  );
}

/** Page actions of a receipt: Print, and Cancel or Reinstate when no request is open. */
export function ReceiptHeaderActions({
  receipt,
  requestOpen,
  printing,
  onPrint,
  onCancel,
  onReinstate,
}: Readonly<{
  receipt: ReceiptDetail;
  requestOpen: boolean;
  printing: boolean;
  onPrint: () => void;
  onCancel: () => void;
  onReinstate: () => void;
}>) {
  const { can } = useAuth();
  const cancelled = receipt.summary.status === 'CANCELLED';
  return (
    <>
      <Button variant="secondary" icon={<Printer size={16} />} busy={printing} onClick={onPrint}>
        Print
      </Button>
      {!cancelled && !requestOpen && can('CASH_CANCEL') && (
        <Button variant="danger" onClick={onCancel}>
          Cancel Receipt
        </Button>
      )}
      {cancelled && !requestOpen && can('CASH_REINSTATE') && (
        <Button variant="accent" onClick={onReinstate}>
          Reinstate
        </Button>
      )}
    </>
  );
}
