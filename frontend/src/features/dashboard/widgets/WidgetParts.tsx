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
import type { TrendPoint } from '@/api/dashboard';
import { formatAmount, formatCompact } from '@/utils/format';
import { monthLabel } from '../dashboardMath';
import { CHART, CHART_HEIGHT, tooltipAmount, tooltipMonth } from './widgetSupport';

interface FigureProps {
  label: string;
  value: number;
  hint?: string;
  danger?: boolean;
}

/** One labelled amount of a widget (compact value, exact amount on hover). */
export function Figure({ label, value, hint, danger = false }: Readonly<FigureProps>) {
  return (
    <div>
      <div className="kpi-label">{label}</div>
      <div className={`widget-figure-value ${danger ? 'danger' : ''}`} title={formatAmount(value)}>
        {formatCompact(value)}
      </div>
      {hint !== undefined && <div className="kpi-hint">{hint}</div>}
    </div>
  );
}

interface TrendChartProps {
  data: TrendPoint[];
  currentLabel: string;
  priorLabel: string;
}

/** Monthly bars of the current fiscal year with the prior year as a line. */
export function TrendChart({ data, currentLabel, priorLabel }: Readonly<TrendChartProps>) {
  return (
    <div style={{ height: CHART_HEIGHT }}>
      <ResponsiveContainer>
        <ComposedChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} />
          <XAxis dataKey="month" tickFormatter={monthLabel} />
          <YAxis tickFormatter={(v: number) => formatCompact(v)} width={56} />
          <Tooltip formatter={tooltipAmount} labelFormatter={tooltipMonth} />
          <Legend />
          <Bar dataKey="current" name={currentLabel} fill={CHART.primary} radius={[4, 4, 0, 0]} />
          <Line
            dataKey="priorYear"
            name={priorLabel}
            stroke={CHART.secondary}
            strokeWidth={2}
            dot={false}
          />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
}
