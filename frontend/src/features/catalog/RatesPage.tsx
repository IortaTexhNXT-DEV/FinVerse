import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import type { EffectiveDated, MotorLimit, ShortPeriodRate, TaxRate } from '@/api/catalog';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatDate, humanize } from '@/utils/format';
import { RateModal } from './RateModals';
import type { RateTable } from './RateModals';
import { RecordActions } from './RecordActions';

const TABS: readonly { id: RateTable; label: string }[] = [
  { id: 'taxes', label: 'Taxes & factors' },
  { id: 'short-period', label: 'Short-period table' },
  { id: 'motor-limits', label: 'BI / PD limits' },
];

const REFRESH = [['catalog', 'rates']] as const;

function dated<T extends EffectiveDated>(
  kind: 'RATE' | 'SHORT_PERIOD_RATE' | 'MOTOR_LIMIT',
): Column<T>[] {
  return [
    { key: 'f', header: 'From', render: (r) => formatDate(r.effectiveFrom) },
    { key: 't', header: 'To', render: (r) => formatDate(r.effectiveTo) || 'Open' },
    { key: 's', header: 'Status', render: (r) => <StatusBadge status={r.recordStatus} /> },
    {
      key: 'x',
      header: 'Actions',
      render: (r) => <RecordActions kind={kind} record={r} refresh={REFRESH} />,
    },
  ];
}

function Taxes() {
  const rows = useQuery({ queryKey: ['catalog', 'rates', 'taxes'], queryFn: catalogApi.taxes });
  return (
    <>
      <ErrorAlert error={rows.error} />
      <DataTable<TaxRate>
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => r.id}
        columns={[
          { key: 'c', header: 'Rate', render: (r) => <strong>{humanize(r.rateCode)}</strong> },
          { key: 'l', header: 'Line', render: (r) => r.lineCode ?? 'All lines' },
          { key: 'r', header: '%', numeric: true, render: (r) => r.rate },
          ...dated<TaxRate>('RATE'),
        ]}
      />
    </>
  );
}

function ShortPeriod() {
  const rows = useQuery({
    queryKey: ['catalog', 'rates', 'short-period'],
    queryFn: catalogApi.shortPeriod,
  });
  return (
    <>
      <ErrorAlert error={rows.error} />
      <DataTable<ShortPeriodRate>
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => r.id}
        columns={[
          {
            key: 'm',
            header: 'Months Covered (up To)',
            numeric: true,
            render: (r) => r.monthsCovered,
          },
          {
            key: 'p',
            header: '% of Annual Premium',
            numeric: true,
            render: (r) => r.percentOfAnnual,
          },
          ...dated<ShortPeriodRate>('SHORT_PERIOD_RATE'),
        ]}
      />
    </>
  );
}

function MotorLimits() {
  const rows = useQuery({
    queryKey: ['catalog', 'rates', 'motor-limits'],
    queryFn: catalogApi.motorLimits,
  });
  return (
    <>
      <ErrorAlert error={rows.error} />
      <DataTable<MotorLimit>
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => r.id}
        columns={[
          {
            key: 'c',
            header: 'Coverage',
            render: (r) => (r.coverage === 'BI' ? 'Bodily injury' : 'Property damage'),
          },
          {
            key: 'l',
            header: 'Limit',
            numeric: true,
            render: (r) => <Amount value={r.limitAmount} />,
          },
          {
            key: 'p',
            header: 'Premium',
            numeric: true,
            render: (r) => <Amount value={r.premium} />,
          },
          ...dated<MotorLimit>('MOTOR_LIMIT'),
        ]}
      />
    </>
  );
}

/**
 * Rates & taxes used by rating (BRNB.007-009): DST, premium tax, VAT, fire service tax, motor
 * own-damage factors, the short-period table and the BI / PD limit premiums, all effective-dated.
 */
export default function RatesPage() {
  const { can } = useAuth();
  const [tab, setTab] = useState<RateTable>('taxes');
  const [adding, setAdding] = useState(false);
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Rates & Taxes"
        description="Statutory charges and rating tables applied by the premium calculator. The rate in force on the period start is used."
        actions={
          can('MASTER_MAINTAIN') && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setAdding(true)}>
              New Rate
            </Button>
          )
        }
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      <Card flush>
        {tab === 'taxes' && <Taxes />}
        {tab === 'short-period' && <ShortPeriod />}
        {tab === 'motor-limits' && <MotorLimits />}
      </Card>
      {adding && <RateModal table={tab} onClose={() => setAdding(false)} />}
    </div>
  );
}
