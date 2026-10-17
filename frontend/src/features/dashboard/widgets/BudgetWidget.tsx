import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { dashboardApi } from '@/api/dashboard';
import type { BudgetWidgetData } from '@/api/dashboard';
import { formatCompact } from '@/utils/format';
import { WidgetCard } from './WidgetCard';
import { CHART, CHART_HEIGHT, tooltipAmount, useWidget } from './widgetSupport';
import { Figure } from './WidgetParts';

const UTILIZATION_WARNING = 100;

function budgetHint(d: BudgetWidgetData): string {
  if (d.budgetVersion === null || d.budgetVersion === undefined) {
    return `No approved budget for ${String(d.fiscalYear)}`;
  }
  const used = d.utilizationPct ?? '—';
  return `Version ${String(d.budgetVersion)} · ${String(used)}% of the annual budget used`;
}

/** Expense budget against actual of the current fiscal year, with the largest accounts. */
export function BudgetWidget({ enabled }: Readonly<{ enabled: boolean }>) {
  const q = useWidget('budget', (companyId) => dashboardApi.budget(companyId), enabled);
  const d = q.data;
  return (
    <WidgetCard
      title="Expense budget vs actual"
      loading={q.isLoading}
      error={q.error}
      empty={d?.lines.length === 0}
      emptyMessage="No expense budget or actual expenses this fiscal year."
    >
      {d !== undefined && (
        <>
          <div className="widget-figures">
            <Figure label="Annual budget" value={d.annualBudget} hint={budgetHint(d)} />
            <Figure label="Budget to date" value={d.budgetToDate} />
            <Figure
              label="Actual to date"
              value={d.actualToDate}
              danger={(d.utilizationPct ?? 0) > UTILIZATION_WARNING}
            />
          </div>
          <div style={{ height: CHART_HEIGHT }}>
            <ResponsiveContainer>
              <BarChart data={d.lines} layout="vertical" margin={{ left: 8 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} />
                <XAxis type="number" tickFormatter={(v: number) => formatCompact(v)} />
                <YAxis type="category" dataKey="account" width={150} />
                <Tooltip formatter={tooltipAmount} />
                <Legend />
                <Bar dataKey="budgetToDate" name="Budget to date" fill={CHART.muted} />
                <Bar dataKey="actualToDate" name="Actual" fill={CHART.primary} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </>
      )}
    </WidgetCard>
  );
}
