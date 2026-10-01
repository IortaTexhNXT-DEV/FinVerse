import { useQuery } from '@tanstack/react-query';
import { ACCOUNT_ENTITY } from '@/api/accounts';
import type { Account, AccountItem } from '@/api/accounts';
import { workflowApi } from '@/api/workflow';
import { HistoryTable } from '@/components/broking/HistoryTable';
import { workflowKey } from '@/components/broking/workflowKey';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DetailList } from '@/features/catalog/DetailList';
import type { DetailRow } from '@/features/catalog/DetailList';
import { formatDate, formatDateTime, formatPeriod, formatRate, humanize } from '@/utils/format';
import { displayNameOf } from '@/api/users';
import { LovLabel, ProductLineLabel, InsurerWithBranch } from '@/components/broking/LovLabel';

function detailRows(a: Account): DetailRow[] {
  return [
    ['Client', `${a.clientCode ?? 'Prospect'} – ${a.clientName}`],
    ['Product', <ProductLineLabel key="p" product={a.productCode} line={a.lineCode} />],
    ['Market segment', <LovLabel key="m" type="MARKET_SEGMENT" code={a.marketSegment} />],
    ['Source', <LovLabel key="s" type="SOURCE_CHANNEL" code={a.sourceChannel} />],
    ['Insurer', <InsurerWithBranch key="i" insurer={a.insurerCode} branch={a.insurerBranch} />],
    ['Period', formatPeriod(a.periodFrom, a.periodTo)],
    ['Term', a.multiYear ? `${a.termYears} years` : '1 year'],
    ['Total sum insured', <Amount key="tsi" value={a.totalSumInsured} />],
    ['Mortgagee bank', <LovLabel key="b" type="MORTGAGEE_BANK" code={a.mortgageeBank} />],
    ['Loan application', a.loanApplicationNo],
    ['PN numbers', a.pnNumbers.join(', ') || '—'],
    ['Quotation / proposal', [a.quotationRef, a.proposalRef].filter(Boolean).join(' / ') || '—'],
    [
      'Contact',
      [a.contact.name, a.contact.email, a.contact.mobile].filter(Boolean).join(' · ') || '—',
    ],
    ['Account officer', displayNameOf(a.sales.accountOfficer)],
    [
      'Sales unit',
      [a.sales.region, a.sales.department, a.sales.team].filter(Boolean).join(' / ') || '—',
    ],
    ['Cost center', a.sales.costCenter],
    ['Created', `${displayNameOf(a.createdBy)} ${formatDateTime(a.createdAt)}`],
  ];
}

function lifecycleRows(a: Account): DetailRow[] {
  const l = a.lifecycle;
  return [
    ['Payment', l.paymentStatus ? humanize(l.paymentStatus) : '—'],
    ['Placement slip', l.placementSlipRef],
    ['Placed', formatDateTime(l.placedAt) || '—'],
    ['Hold cover', l.holdCoverStatus ? humanize(l.holdCoverStatus) : '—'],
    ['Policy numbers', a.policyNumbers.join(', ') || '—'],
    ['E-policy received', l.epolicyReceived ? 'Yes' : 'No'],
    ['Booking', l.bookingRef ? `${l.bookingRef} ${formatDate(l.bookedAt)}` : '—'],
    ['Direct booking', a.directBooking ? 'Yes' : 'No'],
  ];
}

/** Account data and where it stands after validation (placement, policy, booking). */
export function DetailsPanel({ account }: Readonly<{ account: Account }>) {
  return (
    <>
      <Card title="Account">
        <DetailList rows={detailRows(account)} />
      </Card>
      <Card title="Placement and booking">
        <DetailList rows={lifecycleRows(account)} />
      </Card>
    </>
  );
}

function identifiers(i: AccountItem): string {
  if (i.vehicle) {
    const v = i.vehicle;
    return [v.plateNo ?? v.conductionSticker, v.make, v.model, v.yearModel]
      .filter(Boolean)
      .join(' ');
  }
  if (i.location) {
    return [i.location.address, i.location.city].filter(Boolean).join(', ');
  }
  return i.person?.name ?? i.description ?? '';
}

/** Risk items with their identifiers, sum insured, rate and premium. */
export function ItemsPanel({ account }: Readonly<{ account: Account }>) {
  return (
    <Card title="Risk items" flush>
      <DataTable<AccountItem>
        rows={account.items}
        rowKey={(i) => i.id}
        emptyMessage="No risk item yet."
        columns={[
          { key: 'n', header: '#', numeric: true, render: (i) => i.itemNo },
          { key: 'k', header: 'Kind', render: (i) => humanize(i.kind) },
          { key: 'l', header: 'Risk', render: (i) => <strong>{identifiers(i)}</strong> },
          { key: 'd', header: 'Description', render: (i) => i.description ?? '' },
          {
            key: 's',
            header: 'Sum Insured',
            numeric: true,
            render: (i) => <Amount value={i.sumInsured} />,
          },
          { key: 'r', header: 'Rate %', numeric: true, render: (i) => formatRate(i.rate) },
          {
            key: 'p',
            header: 'Premium',
            numeric: true,
            render: (i) => <Amount value={i.premium} />,
          },
        ]}
      />
    </Card>
  );
}

/** Status history of the account's work case (who, when, reason, comment). */
export function HistoryPanel({ accountId }: Readonly<{ accountId: number }>) {
  const detail = useQuery({
    queryKey: workflowKey(ACCOUNT_ENTITY, accountId),
    queryFn: () => workflowApi.byRecord(ACCOUNT_ENTITY, accountId),
  });
  return (
    <Card title="History">
      <HistoryTable history={detail.data?.history ?? []} terminal={detail.data?.stageTerminal} />
    </Card>
  );
}
