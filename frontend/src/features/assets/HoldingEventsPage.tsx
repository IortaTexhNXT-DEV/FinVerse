import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { investmentsApi } from '@/api/investments';
import type { Holding, HoldingTransaction } from '@/api/investments';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { formatDate, humanize, today } from '@/utils/format';
import { HoldingEventModal } from './HoldingEventModal';
import type { HoldingEvent } from './HoldingEventModal';
import { actualDays, maturityBucket } from './investmentMath';
import { useAssetLookups } from './useAssetLookups';

function byMaturity(a: Holding, b: Holding): number {
  return (a.maturityDate ?? '9999').localeCompare(b.maturityDate ?? '9999');
}

/** Held investments by maturity: coupons, maturities, sales and fair value updates. */
export default function HoldingEventsPage() {
  const { companyId } = useAssetLookups();
  const { can } = useAuth();
  const [selected, setSelected] = useState<Holding | null>(null);
  const [event, setEvent] = useState<{ holding: Holding; event: HoldingEvent } | null>(null);
  const asOf = today();
  const holdings = useQuery({
    queryKey: ['holdings', companyId, 'ACTIVE'],
    queryFn: () => investmentsApi.holdings(companyId, 'ACTIVE'),
    enabled: companyId > 0,
  });
  const history = useQuery({
    queryKey: ['holding-transactions', selected?.id],
    queryFn: () => investmentsApi.transactions(selected?.id ?? 0),
    enabled: selected !== null,
  });
  const rows = [...(holdings.data ?? [])].sort(byMaturity);
  const manage = can('INVESTMENT_MANAGE');

  const actions = (h: Holding) => {
    const open = (e: HoldingEvent) => (click: { stopPropagation: () => void }) => {
      click.stopPropagation();
      setEvent({ holding: h, event: e });
    };
    const debt = h.instrumentType !== 'EQUITY';
    return (
      manage && (
        <div className="row">
          {debt && h.couponRate > 0 && (
            <Button size="sm" variant="ghost" onClick={open('COUPON')}>
              Coupon
            </Button>
          )}
          {debt && (
            <Button size="sm" variant="ghost" onClick={open('MATURITY')}>
              Maturity
            </Button>
          )}
          <Button size="sm" variant="ghost" onClick={open('SALE')}>
            Sale
          </Button>
          {h.classification !== 'AMORTIZED_COST' && (
            <Button size="sm" variant="ghost" onClick={open('FAIR_VALUE')}>
              Fair value
            </Button>
          )}
        </div>
      )
    );
  };

  return (
    <div className="stack">
      <PageHeader
        section="Assets & Investments"
        title="Maturities & Sales"
        description="Held investments ordered by maturity. Record coupons received, redemptions at maturity, sales (realized gain or loss) and fair value updates of FVOCI / FVPL holdings."
      />
      <ErrorAlert error={holdings.error ?? history.error} />
      <Card flush>
        <DataTable<Holding>
          loading={holdings.isLoading}
          rows={rows}
          rowKey={(h) => h.id}
          onRowClick={setSelected}
          columns={[
            { key: 'n', header: 'Holding', render: (h) => <strong>{h.holdingNo}</strong> },
            { key: 'd', header: 'Description', render: (h) => h.description },
            { key: 'k', header: 'Class', render: (h) => humanize(h.classification) },
            { key: 'm', header: 'Maturity', render: (h) => formatDate(h.maturityDate) },
            {
              key: 'r',
              header: 'Days left',
              numeric: true,
              render: (h) => (h.maturityDate === undefined ? '' : actualDays(asOf, h.maturityDate)),
            },
            { key: 'b', header: 'Bucket', render: (h) => maturityBucket(asOf, h.maturityDate) },
            {
              key: 'f',
              header: 'Face',
              numeric: true,
              render: (h) => <Amount value={h.faceValue} />,
            },
            {
              key: 'c',
              header: 'Carrying',
              numeric: true,
              render: (h) => <Amount value={h.carryingAmount} />,
            },
            {
              key: 'a',
              header: 'Accrued',
              numeric: true,
              render: (h) => <Amount value={h.accruedInterest} />,
            },
            { key: 'x', header: 'Actions', render: actions },
          ]}
        />
      </Card>
      {selected !== null && (
        <Card title={`History – ${selected.holdingNo}`} flush>
          <DataTable<HoldingTransaction>
            loading={history.isLoading}
            rows={history.data ?? []}
            rowKey={(t) => t.id}
            columns={[
              { key: 'd', header: 'Date', render: (t) => formatDate(t.txnDate) },
              { key: 't', header: 'Event', render: (t) => humanize(t.txnType) },
              {
                key: 'a',
                header: 'Amount',
                numeric: true,
                render: (t) => <Amount value={t.amount} />,
              },
              {
                key: 'c',
                header: 'Cash',
                numeric: true,
                render: (t) => <Amount value={t.cashAmount} />,
              },
              {
                key: 'g',
                header: 'Gain / (loss)',
                numeric: true,
                render: (t) => <Amount value={t.gainLoss} />,
              },
              {
                key: 'ca',
                header: 'Carrying after',
                numeric: true,
                render: (t) => <Amount value={t.carryingAfter} />,
              },
              { key: 'j', header: 'Journal', render: (t) => t.batchNo ?? '' },
            ]}
          />
        </Card>
      )}
      {event !== null && (
        <HoldingEventModal
          holding={event.holding}
          event={event.event}
          onClose={() => setEvent(null)}
        />
      )}
    </div>
  );
}
