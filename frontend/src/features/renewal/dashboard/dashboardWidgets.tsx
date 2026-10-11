import { useNavigate } from 'react-router-dom';
import type { ClosingRow, DashboardCell, RenewalDashboard } from '@/api/renewalDashboard';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { formatAmount } from '@/utils/format';
import { DrillCount } from './dashboardBits';
import { CATEGORY_LABELS, STAGE_LABELS, percent, persistencyLabel } from './dashboardFormat';

/** A figure to open: its metric key and the title of its list. */
export interface Open {
  metric: string;
  title: string;
}

type OpenFn = (o: Open) => void;

const categoryOf = (c: string) => CATEGORY_LABELS[c] ?? c;

/** Basic Premium and Gross Commission against the budget (FRRN.002.02.01). */
export function ProductionTable({ d, open }: Readonly<{ d: RenewalDashboard; open: OpenFn }>) {
  return (
    <Card title="Basic Premium and Gross Commission" flush>
      <DataTable
        rows={d.production}
        rowKey={(r) => r.measure}
        columns={[
          { key: 'label', header: 'Measure', render: (r) => r.label },
          {
            key: 'actual',
            header: 'Actual',
            kind: 'amount',
            render: (r) => (
              <DrillCount
                value={formatAmount(r.actual)}
                label={`Show details of ${r.label}`}
                onOpen={() => open({ metric: 'PRODUCTION', title: `${r.label} - Show Details` })}
              />
            ),
          },
          {
            key: 'budget',
            header: 'Budget',
            kind: 'amount',
            render: (r) => formatAmount(r.budget),
          },
          {
            key: 'pct',
            header: '% vs Budget',
            kind: 'amount',
            render: (r) => percent(r.percentVsBudget),
          },
          {
            key: 'var',
            header: 'Variance',
            kind: 'amount',
            render: (r) => formatAmount(r.variance),
          },
          { key: 'growth', header: 'Growth', kind: 'amount', render: (r) => percent(r.growth) },
          {
            key: 'adj',
            header: 'Adjustments',
            kind: 'amount',
            render: (r) => formatAmount(r.adjustments),
          },
        ]}
      />
    </Card>
  );
}

/** Count, premium and commission per key (ageing buckets, product lines). */
export function CellTable({
  title,
  keyHeader,
  cells,
  labels,
  open,
}: Readonly<{
  title: string;
  keyHeader: string;
  cells: DashboardCell[];
  labels: Record<string, string>;
  open: OpenFn;
}>) {
  const name = (k: string) => labels[k] ?? k;
  return (
    <Card title={title} flush>
      <DataTable<DashboardCell>
        rows={cells}
        rowKey={(c) => c.key}
        emptyMessage="No accounts"
        columns={[
          { key: 'key', header: keyHeader, render: (c) => name(c.key) },
          {
            key: 'count',
            header: 'Count',
            kind: 'amount',
            render: (c) => (
              <DrillCount
                value={c.count.toLocaleString()}
                label={`Accounts of ${name(c.key)}`}
                onOpen={() => open({ metric: c.metric, title: `${title} - ${name(c.key)}` })}
              />
            ),
          },
          {
            key: 'premium',
            header: 'Premium',
            kind: 'amount',
            render: (c) => formatAmount(c.premium),
          },
          {
            key: 'commission',
            header: 'Commission',
            kind: 'amount',
            render: (c) => formatAmount(c.commission),
          },
        ]}
      />
    </Card>
  );
}

/** Pipeline Summary by business type and stage (FRRN.002.02.02). */
export function PipelineTable({ d, open }: Readonly<{ d: RenewalDashboard; open: OpenFn }>) {
  const rows = [...d.pipeline.rows, d.pipeline.total];
  const keys = [...d.pipeline.total.cells.map((c) => c.key), 'TOTAL'];
  const cellOf = (r: (typeof rows)[number], s: string) =>
    s === 'TOTAL' ? r.total : r.cells.find((c) => c.key === s);
  return (
    <Card title="Pipeline Summary" flush>
      <DataTable
        rows={rows}
        rowKey={(r) => r.category}
        columns={[
          { key: 'cat', header: 'Business Type', render: (r) => categoryOf(r.category) },
          ...keys.map((s) => ({
            key: s,
            header: STAGE_LABELS[s] ?? s,
            kind: 'amount' as const,
            render: (r: (typeof rows)[number]) => {
              const cell = cellOf(r, s) ?? { ...r.total, count: 0, premium: 0, commission: 0 };
              const title = `Pipeline - ${categoryOf(r.category)} - ${STAGE_LABELS[s] ?? s}`;
              return (
                <DrillCount
                  value={
                    <>
                      {cell.count.toLocaleString()}
                      <span className="muted rnw-cell-amounts">
                        {formatAmount(cell.premium)} / {formatAmount(cell.commission)}
                      </span>
                    </>
                  }
                  label={title}
                  onOpen={() => open({ metric: cell.metric, title })}
                />
              );
            },
          })),
        ]}
      />
      <p className="muted rnw-note">Count, then premium / commission of each stage.</p>
    </Card>
  );
}

const TIERS = [
  ['inProcess', 'In-Process Pipeline', 'IN_PROCESS'],
  ['posted', 'Posted Pipeline', 'POSTED'],
  ['booked', 'Booked', 'BOOKED'],
  ['total', 'Total Accounts', 'ALL'],
] as const;

/** Closing ratio per business type with its pipeline tiers (FRRN.002.02.04). */
export function ClosingTable({ d, open }: Readonly<{ d: RenewalDashboard; open: OpenFn }>) {
  return (
    <Card title="Closing Ratio" flush>
      <DataTable<ClosingRow>
        rows={d.closing}
        rowKey={(r) => r.category}
        columns={[
          { key: 'cat', header: 'Business Type', render: (r) => categoryOf(r.category) },
          ...TIERS.map(([k, label, tier]) => ({
            key: k,
            header: label,
            kind: 'amount' as const,
            render: (r: ClosingRow) => (
              <DrillCount
                value={r[k].toLocaleString()}
                label={`${label} of ${categoryOf(r.category)}`}
                onOpen={() =>
                  open({
                    metric: `CLOSING|${r.category}|${tier}`,
                    title: `Closing Ratio - ${categoryOf(r.category)} - ${label}`,
                  })
                }
              />
            ),
          })),
          {
            key: 'ratio',
            header: 'Closing Ratio',
            kind: 'amount',
            render: (r) => percent(r.ratio),
          },
        ]}
      />
    </Card>
  );
}

/** Biggest Open Deals, Top 50, 75 or 100 (FRRN.002.02.05). */
export function DealsTable({
  d,
  top,
  onTop,
}: Readonly<{ d: RenewalDashboard; top: number; onTop: (n: number) => void }>) {
  const navigate = useNavigate();
  return (
    <Card
      title="Biggest Open Deals"
      flush
      actions={
        <select
          className="select"
          aria-label="Number of deals"
          value={top}
          onChange={(e) => onTop(Number(e.target.value))}
        >
          {d.topOptions.map((n) => (
            <option key={n} value={n}>
              Top {n}
            </option>
          ))}
        </select>
      }
    >
      <DataTable
        rows={d.topDeals}
        rowKey={(r) => r.renewalRef}
        onRowClick={(r) => void navigate(`/renewal/candidates/${encodeURIComponent(r.renewalRef)}`)}
        emptyMessage="No open deals"
        columns={[
          {
            key: 'rank',
            header: 'Ranking',
            kind: 'amount',
            render: (r) => d.topDeals.indexOf(r) + 1,
          },
          {
            key: 'acc',
            header: 'Account Number',
            kind: 'code',
            render: (r) => r.accountNo ?? r.renewalRef,
          },
          { key: 'assured', header: 'Assured Name', render: (r) => r.assured ?? '' },
          { key: 'risk', header: 'Risk Code', kind: 'code', render: (r) => r.riskCode ?? '' },
          { key: 'bt', header: 'Business Type', render: (r) => r.businessType },
          { key: 'team', header: 'Team', render: (r) => r.team ?? '' },
          {
            key: 'prem',
            header: 'Premium Amount',
            kind: 'amount',
            render: (r) => formatAmount(r.premium),
          },
          {
            key: 'comm',
            header: 'Commission Amount',
            kind: 'amount',
            render: (r) => formatAmount(r.commission),
          },
        ]}
      />
    </Card>
  );
}

/** Renewal Persistency Ratio by count, premium and commission (FRRN.002.02.06). */
export function PersistencyTable({ d, open }: Readonly<{ d: RenewalDashboard; open: OpenFn }>) {
  return (
    <Card title="Renewal Persistency Ratio" flush>
      <DataTable
        rows={d.persistency}
        rowKey={(r) => r.key}
        columns={[
          { key: 'p', header: 'Period', render: (r) => persistencyLabel(r.key, r.label) },
          {
            key: 'n',
            header: 'Renewed / Renewable',
            kind: 'amount',
            render: (r) => (
              <DrillCount
                value={`${r.renewed.toLocaleString()} / ${r.renewable.toLocaleString()}`}
                label={`Renewable accounts of ${r.label}`}
                onOpen={() =>
                  open({
                    metric: `PERSISTENCY|${r.key}|ALL`,
                    title: `Renewable Accounts - ${r.label}`,
                  })
                }
              />
            ),
          },
          { key: 'c', header: 'Count', kind: 'amount', render: (r) => percent(r.countRatio) },
          { key: 'pr', header: 'Premium', kind: 'amount', render: (r) => percent(r.premiumRatio) },
          {
            key: 'cm',
            header: 'Commission',
            kind: 'amount',
            render: (r) => percent(r.commissionRatio),
          },
        ]}
      />
    </Card>
  );
}
