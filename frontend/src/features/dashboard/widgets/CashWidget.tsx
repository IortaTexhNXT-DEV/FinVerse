import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { dashboardApi } from '@/api/dashboard';
import { formatAmount, formatCompact } from '@/utils/format';
import { hasData, monthLabel } from '../dashboardMath';
import { WidgetCard } from './WidgetCard';
import { CHART, CHART_HEIGHT, tooltipAmount, tooltipMonth, useWidget } from './widgetSupport';
import { Figure } from './WidgetParts';

/** Cash and bank position: balance per account and the month-end trend of the fiscal year. */
export function CashWidget({ enabled }: Readonly<{ enabled: boolean }>) {
  const q = useWidget('cash', dashboardApi.cash, enabled);
  const d = q.data;
  return (
    <WidgetCard
      title="Cash and bank position"
      loading={q.isLoading}
      error={q.error}
      empty={d?.accounts.length === 0 && !hasData(d.monthly.map((m) => m.amount))}
      emptyMessage="No cash or bank balances."
    >
      {d !== undefined && (
        <>
          <div className="widget-figures">
            <Figure label="Cash and bank" value={d.total} danger={d.total < 0} />
          </div>
          <ul className="stack" style={{ listStyle: 'none', padding: 0, margin: 0, gap: 6 }}>
            {d.accounts.map((a) => (
              <li key={a.label} className="row" style={{ justifyContent: 'space-between' }}>
                <span>{a.label}</span>
                <strong className="num">{formatAmount(a.amount)}</strong>
              </li>
            ))}
          </ul>
          <div style={{ height: CHART_HEIGHT, marginTop: 12 }}>
            <ResponsiveContainer>
              <AreaChart data={d.monthly}>
                <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} />
                <XAxis dataKey="month" tickFormatter={monthLabel} />
                <YAxis tickFormatter={(v: number) => formatCompact(v)} width={56} />
                <Tooltip formatter={tooltipAmount} labelFormatter={tooltipMonth} />
                <Area
                  dataKey="amount"
                  name="Month-end balance"
                  stroke={CHART.accent}
                  fill={CHART.primary}
                  fillOpacity={0.15}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </>
      )}
    </WidgetCard>
  );
}
