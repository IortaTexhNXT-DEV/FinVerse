import type { CandidateRow } from '@/api/renewal';
import type { Column } from '@/components/ui/DataTable';
import { UserName } from '@/components/ui/UserName';
import { formatAmount, formatDate } from '@/utils/format';
import { FlagChips } from '../common/RenewalBits';
import { dispositionLabel } from '../common/renewalCodes';

/** The panels of the renewal accounts (FRRN.002.05) by the third-bucket setting. */
export function panelTabs(thirdBucket: string): { id: string; label: string }[] {
  return [
    { id: 'BUCKET_CLEAN', label: 'Clean' },
    { id: 'BUCKET_REVIEW', label: 'Review' },
    {
      id: 'BUCKET_NON_RENEWABLE',
      label: thirdBucket === 'EXCEPTION' ? 'Exception' : 'Non-Renewable',
    },
    { id: 'ALL', label: 'All' },
  ];
}

const money = (v: number | null | undefined) =>
  v === null || v === undefined ? '' : formatAmount(v);
const rate = (v: number | null | undefined) =>
  v === null || v === undefined ? '' : `${v.toFixed(4)}%`;

/** BDOI's columns of the bucket panels (FRRN.002.05), the same in every panel. */
export function bucketColumns(): Column<CandidateRow>[] {
  return [
    {
      key: 'inv',
      sortKey: 'invoice',
      header: 'Expiring Invoice Number',
      kind: 'code',
      render: (r) => r.policy.expiringInvoiceNo ?? '',
    },
    {
      key: 'ref',
      sortKey: 'ref',
      header: 'Renewal Reference Number',
      kind: 'code',
      render: (r) => (
        <span className="rnw-ref">
          <span className="nowrap">{r.renewalRef}</span>
          <FlagChips row={r} />
        </span>
      ),
    },
    { key: 'claim', header: 'With Claim', render: (r) => (r.flags.claims ? 'Yes' : 'No') },
    {
      key: 'newinv',
      header: 'Invoice Number',
      kind: 'code',
      render: (r) => r.bdoi?.invoiceNo ?? '',
    },
    {
      key: 'assured',
      sortKey: 'assured',
      header: "Assured's Name",
      render: (r) => r.parties.assuredName ?? r.parties.clientName,
    },
    { key: 'client', sortKey: 'client', header: 'Client', render: (r) => r.parties.clientName },
    {
      key: 'expiry',
      sortKey: 'expiry',
      header: 'Expiry Date',
      kind: 'date',
      render: (r) => formatDate(r.expiry),
    },
    {
      key: 'si',
      sortKey: 'sumInsured',
      header: 'Sum Insured',
      kind: 'amount',
      render: (r) => money(r.money.sumInsured),
    },
    {
      key: 'rate',
      header: 'Premium Rate',
      kind: 'amount',
      render: (r) => rate(r.money.premiumRate),
    },
    {
      key: 'premium',
      sortKey: 'premium',
      header: 'Premium',
      kind: 'amount',
      render: (r) => money(r.money.basicPremium),
    },
    {
      key: 'crate',
      header: 'Commission Rate',
      kind: 'amount',
      render: (r) => rate(r.money.commissionRate),
    },
    {
      key: 'comm',
      header: 'Commission Amount',
      kind: 'amount',
      render: (r) => money(r.bdoi?.commissionAmount),
    },
    { key: 'address', header: 'Mailing Address', render: (r) => r.bdoi?.mailingAddress ?? '' },
    {
      key: 'mortgaged',
      header: 'Mortgaged (Y/N)',
      render: (r) => (r.policy.mortgaged ? 'Y' : 'N'),
    },
    {
      key: 'status',
      sortKey: 'stage',
      header: 'Renewal Status',
      render: (r) => r.bdoi?.statusName ?? r.stageLabel,
    },
    { key: 'disp', header: 'Renewal Disposition', render: (r) => dispositionLabel(r.disposition) },
    {
      key: 'user',
      header: 'Assigned User',
      render: (r) => {
        const user = r.parties.assignedPo ?? r.parties.assignedAo;
        return user === null ? '' : <UserName login={user} />;
      },
    },
  ];
}
