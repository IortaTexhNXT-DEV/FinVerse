import {
  Bar,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { CompositionItem, DashboardSummary } from '@/api/dashboard';
import { Card } from '@/components/ui/Card';
import { KpiTile } from '@/components/ui/KpiTile';
import { formatAmount, formatCompact } from '@/utils/format';
import { nounFor } from '@/utils/wording';
import { monthLabel, sharePct } from '../dashboardMath';
import { CHART, tooltipAmount, tooltipMonth } from './widgetSupport';

/** Statement lines with their share of the total as bars. */
function CompositionList({ items }: Readonly<{ items: CompositionItem[] }>) {
  const total = items.reduce((acc, i) => acc + Math.abs(i.amount), 0);
  if (items.length === 0) {
    return <p className="muted">No postings this fiscal year.</p>;
  }
  return (
    <ul className="stack" style={{ listStyle: 'none', padding: 0, margin: 0, gap: 10 }}>
      {items.map((i) => (
        <li key={i.label}>
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <span>{i.label}</span>
            <strong className="num">{formatAmount(i.amount)}</strong>
          </div>
          <div className="widget-bar">
            <div style={{ width: `${sharePct(Math.abs(i.amount), total)}%` }} />
          </div>
        </li>
      ))}
    </ul>
  );
}

/** Headline KPI tiles of the general ledger summary: one tile anatomy, no dot-separated lines. */
export function SummaryKpis({ summary: d }: Readonly<{ summary: DashboardSummary }>) {
  return (
    <>
      <KpiTile
        label="Income (YTD)"
        value={formatCompact(d.totalIncomeYtd)}
        qualifier={formatAmount(d.totalIncomeYtd)}
      />
      <KpiTile
        label="Expenses (YTD)"
        value={formatCompact(d.totalExpenseYtd)}
        qualifier={formatAmount(d.totalExpenseYtd)}
      />
      <KpiTile
        label="Net Result (YTD)"
        value={formatCompact(d.netResultYtd)}
        qualifier={formatAmount(d.netResultYtd)}
      />
      <KpiTile
        label="Cash Position"
        value={formatCompact(d.cashPosition)}
        qualifier="Cash and bank balances"
      />
      <KpiTile
        label="Insurance Receivables"
        value={formatCompact(d.receivables)}
        qualifier={formatAmount(d.receivables)}
      />
      <KpiTile
        label="Total Assets"
        value={formatCompact(d.totalAssets)}
        qualifier={formatAmount(d.totalAssets)}
      />
      <KpiTile
        label="Journals in Progress"
        value={d.pendingJournals + d.draftJournals}
        to="/gl/journals"
        qualifier="Waiting for approval or still in draft"
        breakdown={{
          label: 'Journals in progress by status',
          items: [
            {
              key: 'pending',
              label: 'Waiting for approval',
              count: d.pendingJournals,
              to: '/gl/journals',
            },
            {
              key: 'draft',
              label: nounFor(d.draftJournals, 'Draft'),
              count: d.draftJournals,
              to: '/gl/journals',
            },
          ],
        }}
      />
    </>
  );
}

/** Monthly income against expenses of the fiscal year, and the income and expense mix. */
export function SummaryCharts({ summary: d }: Readonly<{ summary: DashboardSummary }>) {
  return (
    <>
      <Card title="Monthly income vs expenses">
        <div style={{ height: 300 }}>
          <ResponsiveContainer>
            <ComposedChart data={d.monthly}>
              <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} />
              <XAxis dataKey="month" tickFormatter={monthLabel} />
              <YAxis tickFormatter={(v: number) => formatCompact(v)} width={56} />
              <Tooltip formatter={tooltipAmount} labelFormatter={tooltipMonth} />
              <Legend />
              <Bar dataKey="income" name="Income" fill={CHART.primary} radius={[4, 4, 0, 0]} />
              <Bar dataKey="expense" name="Expenses" fill={CHART.secondary} radius={[4, 4, 0, 0]} />
              <Line dataKey="netResult" name="Net result" stroke={CHART.accent} dot={false} />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </Card>
      <Card title="Income composition (YTD)">
        <CompositionList items={d.incomeComposition} />
      </Card>
      <Card title="Expense composition (YTD)">
        <CompositionList items={d.expenseComposition} />
      </Card>
    </>
  );
}
