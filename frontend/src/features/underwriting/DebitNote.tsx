import type { DocumentStatus, Policy, Premium } from '@/api/underwriting';
import { Card } from '@/components/ui/Card';
import { formatDate } from '@/utils/format';
import { PremiumSummary } from './PremiumSummary';

interface Props {
  policy: Policy;
  document: DocumentStatus;
  premium: Premium;
  reference: string;
}

/** Debit note (or credit note for return premium) issued to the client on approval. */
export function DebitNote({ policy, document, premium, reference }: Readonly<Props>) {
  if (document.debitNoteNo === undefined) {
    return (
      <Card title="Debit note">
        <p className="muted" style={{ margin: 0 }}>
          The debit note is issued when the document is approved.
        </p>
      </Card>
    );
  }
  const credit = premium.totalDue < 0;
  const facts: [string, string][] = [
    [credit ? 'Credit note no.' : 'Debit note no.', document.debitNoteNo],
    ['Date', formatDate(document.approvalDate)],
    ['Bill to', `${policy.customerCode} – ${policy.customerName}`],
    ['Insured', policy.insuredName],
    ['Reference', reference],
    ['Period', `${formatDate(policy.periodFrom)} – ${formatDate(policy.periodTo)}`],
    ['Intermediary', policy.intermediaryName ?? 'Direct business'],
    ['Broker / agent note', document.creditNoteNo ?? '—'],
    ['Premium journal', document.premiumBatchNo ?? '—'],
    ['Commission journal', document.commissionBatchNo ?? '—'],
  ];
  return (
    <Card title={credit ? 'Credit note' : 'Debit note'}>
      <div className="grid-2">
        <dl className="form-grid" style={{ margin: 0 }}>
          {facts.map(([label, value]) => (
            <div key={label}>
              <dt className="muted">{label}</dt>
              <dd style={{ margin: 0, fontWeight: 600 }}>{value}</dd>
            </div>
          ))}
        </dl>
        <PremiumSummary premium={premium} currency={policy.currency} />
      </div>
    </Card>
  );
}
