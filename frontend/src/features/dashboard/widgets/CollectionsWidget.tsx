import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { dashboardApi } from '@/api/dashboard';
import type { LabelledAmount, MonthlyValue } from '@/api/dashboard';
import { formatCompact } from '@/utils/format';
import { hasData, monthLabel } from '../dashboardMath';
import { WidgetCard } from './WidgetCard';
import { CHART, CHART_HEIGHT, tooltipAmount, tooltipMonth, useWidget } from './widgetSupport';
import { Figure } from './WidgetParts';

function AgeingChart({ buckets }: Readonly<{ buckets: LabelledAmount[] }>) {
  return (
    <div style={{ height: CHART_HEIGHT }}>
      <ResponsiveContainer>
        <BarChart data={buckets}>
          <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} />
          <XAxis dataKey="label" />
          <YAxis tickFormatter={(v: number) => formatCompact(v)} width={56} />
          <Tooltip formatter={tooltipAmount} />
          <Bar dataKey="amount" name="Receivables" fill={CHART.primary} radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

function CollectionsChart({ months }: Readonly<{ months: MonthlyValue[] }>) {
  return (
    <div style={{ height: CHART_HEIGHT }}>
      <ResponsiveContainer>
        <BarChart data={months}>
          <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} />
          <XAxis dataKey="month" tickFormatter={monthLabel} />
          <YAxis tickFormatter={(v: number) => formatCompact(v)} width={56} />
          <Tooltip formatter={tooltipAmount} labelFormatter={tooltipMonth} />
          <Bar dataKey="amount" name="Collected" fill={CHART.secondary} radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

/** Collections (approved receipts) against the debtors' outstanding balance by age. */
export function CollectionsWidget({ enabled }: Readonly<{ enabled: boolean }>) {
  const q = useWidget('collections', dashboardApi.collections, enabled);
  const d = q.data;
  return (
    <WidgetCard
      title="Collections and receivables ageing"
      loading={q.isLoading}
      error={q.error}
      empty={d !== undefined && !hasData([d.receivables, ...d.monthly.map((m) => m.amount)])}
      emptyMessage="No receipts this year and no outstanding receivables."
    >
      {d !== undefined && (
        <>
          <div className="widget-figures">
            <Figure label="Collected month to date" value={d.collectedMonthToDate} />
            <Figure label="Collected year to date" value={d.collectedYearToDate} />
            <Figure
              label="Receivables"
              value={d.receivables}
              hint={`${formatCompact(d.notYetDue)} not yet due`}
            />
          </div>
          <div className="kpi-hint">Ageing by due date, slots {d.ageingSlots} days</div>
          <AgeingChart buckets={d.ageing} />
          {d.monthly.length > 0 && (
            <>
              <div className="kpi-hint">Collections per month</div>
              <CollectionsChart months={d.monthly} />
            </>
          )}
        </>
      )}
    </WidgetCard>
  );
}
