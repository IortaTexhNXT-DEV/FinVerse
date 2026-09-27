import { CalendarRange, FileText, Landmark, Layers, UserRound, Wallet } from 'lucide-react';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import type { ClaimDraft, PremiumCheck, UnpaidInvoice } from '../cover/api';
import { PREMIUM_LABELS } from './recordLogic';

/** The premium check of a cover with the invoices not fully paid (BRCLM.001). */
export function PremiumPanel({ premium }: Readonly<{ premium: PremiumCheck }>) {
  return (
    <div className="stack">
      <div className={premium.blocking ? 'alert warning' : 'alert success'} role="status">
        <strong>Premium: {PREMIUM_LABELS[premium.status]}.</strong>{' '}
        {premium.blocking
          ? 'The claim can be recorded; the authorization code stays disabled until the premium is paid.'
          : 'The premium check does not block the authorization code.'}
      </div>
      {premium.unpaid.length > 0 && (
        <DataTable<UnpaidInvoice>
          caption="Invoices not fully paid"
          rows={premium.unpaid}
          rowKey={(i) => i.invoiceNo}
          columns={[
            {
              key: 'n',
              header: 'Invoice',
              render: (i) => <span className="mono">{i.invoiceNo}</span>,
            },
            { key: 'k', header: 'Kind', render: (i) => humanize(i.kind) },
            {
              key: 's',
              header: 'Payment',
              render: (i) => <StatusBadge status={i.paymentStatus} />,
            },
            {
              key: 'b',
              header: 'Balance',
              numeric: true,
              render: (i) => `${i.currency} ${formatAmount(i.balance)}`,
            },
          ]}
        />
      )}
    </div>
  );
}

/**
 * The cover card of Record Claim (BRCLM.001/003/007/009/016/039): policy number, cover version at
 * the loss date, period, sum insured, Marketing team, AO and branch, and the premium check. Policy
 * data is shown, never entered.
 */
export function CoverCard({ draft }: Readonly<{ draft: ClaimDraft }>) {
  const h = draft.header;
  return (
    <div className="stack">
      <RecordSummary
        title={h.assuredName}
        chips={
          <>
            <ReferenceChip label="ARN" value={h.arn} />
            <span className="tag">{draft.versionLabel}</span>
          </>
        }
        facts={[
          { icon: Landmark, label: 'Policy No.', value: draft.policyNo ?? 'Policy number pending' },
          {
            icon: CalendarRange,
            label: `Policy Year ${draft.policyYear}`,
            value: `${formatDate(draft.periodFrom)} – ${formatDate(draft.periodTo)}`,
          },
          {
            icon: Wallet,
            label: `Sum Insured (${draft.currency})`,
            value: formatAmount(draft.sumInsured),
          },
          {
            icon: Layers,
            label: 'Product / Insurer',
            value: `${h.productCode} · ${h.insurerCode ?? ''}`,
          },
          {
            icon: UserRound,
            label: 'Marketing Team / AO',
            value: `${draft.salesTeam ?? '—'} · ${draft.accountOfficer ?? '—'}`,
          },
          {
            icon: FileText,
            label: 'Invoicing Branch',
            value:
              draft.invoicingBranchId === undefined ? '—' : `Branch ${draft.invoicingBranchId}`,
          },
        ]}
      />
      <PremiumPanel premium={draft.premium} />
    </div>
  );
}
