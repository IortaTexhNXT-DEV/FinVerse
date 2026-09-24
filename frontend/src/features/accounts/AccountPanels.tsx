import { useQuery } from '@tanstack/react-query';
import { ACCOUNT_ENTITY } from '@/api/accounts';
import type { Account, AccountItem } from '@/api/accounts';
import { workflowApi } from '@/api/workflow';
import { StageTimeline } from '@/components/broking/StageTimeline';
import { workflowKey } from '@/components/broking/workflowKey';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DetailList } from '@/features/catalog/DetailList';
import type { DetailRow } from '@/features/catalog/DetailList';
import { formatDate, formatDateTime, humanize } from '@/utils/format';

function detailRows(a: Account): DetailRow[] {
  return [
    ['Client', `${a.clientCode ?? 'Prospect'} – ${a.clientName}`],
    ['Product', `${a.productCode} (${a.lineCode})`],
    ['Market segment', a.marketSegment],
    ['Source', a.sourceChannel],
    ['Insurer', [a.insurerCode, a.insurerBranch].filter(Boolean).join(' / ') || '—'],
    ['Period', `${formatDate(a.periodFrom)} to ${formatDate(a.periodTo)}`],
    ['Term', a.multiYear ? `${a.termYears} years` : '1 year'],
    ['Total sum insured', <Amount key="tsi" value={a.totalSumInsured} />],
    ['Mortgagee bank', a.mortgageeBank],
    ['Loan application', a.loanApplicationNo],
    ['PN numbers', a.pnNumbers.join(', ') || '—'],
    ['Quotation / proposal', [a.quotationRef, a.proposalRef].filter(Boolean).join(' / ') || '—'],
    [
      'Contact',
      [a.contact.name, a.contact.email, a.contact.mobile].filter(Boolean).join(' · ') || '—',
    ],
    ['Account officer', a.sales.accountOfficer],
    [
      'Sales unit',
      [a.sales.region, a.sales.department, a.sales.team].filter(Boolean).join(' / ') || '—',
    ],
    ['Cost center', a.sales.costCenter],
    ['Created', `${a.createdBy} ${formatDateTime(a.createdAt)}`],
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
          { key: 'r', header: 'Rate %', numeric: true, render: (i) => i.rate ?? '' },
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
      <StageTimeline history={detail.data?.history ?? []} />
    </Card>
  );
}
