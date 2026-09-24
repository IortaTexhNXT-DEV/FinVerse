import { useNavigate } from 'react-router-dom';
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
import type { StageAgeing, UnitProduction } from '@/api/nbReports';
import { CHART, tooltipAmount } from '@/features/dashboard/widgets/widgetSupport';
import { formatCompact } from '@/utils/format';
import { accountsPath } from './dashboardData';

const HEIGHT = 260;

/** Age bands of the ageing chart, light to dark (older is darker; overdue is red). */
const AGE_BANDS = [
  { key: 'upToOneDay', name: 'Under 1 day', fill: 'var(--brand-field-blue)' },
  { key: 'upToThreeDays', name: '1 to 3 days', fill: CHART.primary },
  { key: 'upToSevenDays', name: '3 to 7 days', fill: CHART.accent },
  { key: 'overSevenDays', name: 'Over 7 days', fill: 'var(--color-danger)' },
] as const;

/** Open accounts per stage stacked by the time spent in the stage; a bar opens the stage list. */
export function AgeingChart({ ageing }: Readonly<{ ageing: StageAgeing[] }>) {
  const navigate = useNavigate();
  return (
    <div style={{ height: HEIGHT }}>
      <ResponsiveContainer>
        <BarChart data={ageing} layout="vertical" margin={{ left: 8, right: 16 }}>
          <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} horizontal={false} />
          <XAxis type="number" allowDecimals={false} />
          <YAxis type="category" dataKey="label" width={190} interval={0} tick={{ fontSize: 12 }} />
          <Tooltip />
          <Legend />
          {AGE_BANDS.map((b) => (
            <Bar
              key={b.key}
              dataKey={b.key}
              name={b.name}
              stackId="age"
              fill={b.fill}
              cursor="pointer"
              onClick={(entry: { payload?: StageAgeing }) => {
                if (entry.payload !== undefined) {
                  void navigate(accountsPath(entry.payload.stage));
                }
              }}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}

/** Booked premium against the target per team (this month). */
export function ProductionChart({ units }: Readonly<{ units: UnitProduction[] }>) {
  return (
    <div style={{ height: HEIGHT }}>
      <ResponsiveContainer>
        <BarChart data={units} margin={{ left: 8, right: 16 }}>
          <CartesianGrid strokeDasharray="3 3" stroke={CHART.grid} vertical={false} />
          <XAxis dataKey="name" tick={{ fontSize: 12 }} />
          <YAxis tickFormatter={(v: number) => formatCompact(v)} width={56} />
          <Tooltip formatter={tooltipAmount} />
          <Legend />
          <Bar dataKey="premium" name="Booked premium" fill={CHART.primary} radius={[4, 4, 0, 0]} />
          <Bar dataKey="targetPremium" name="Target" fill={CHART.secondary} radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
