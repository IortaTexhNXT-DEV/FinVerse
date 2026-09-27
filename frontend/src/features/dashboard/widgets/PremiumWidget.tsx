import { dashboardApi } from '@/api/dashboard';
import { changeText, hasData } from '../dashboardMath';
import { WidgetCard } from './WidgetCard';
import { useWidget } from './widgetSupport';
import { Figure, TrendChart } from './WidgetParts';

const PRIOR_YEAR = 'prior year';

/** Gross written premium month and year to date against the same period of the prior year. */
export function PremiumWidget({ enabled }: Readonly<{ enabled: boolean }>) {
  const q = useWidget('premium', dashboardApi.premium, enabled);
  const d = q.data;
  const trend = d?.monthly ?? [];
  return (
    <WidgetCard
      title="Gross written premium"
      loading={q.isLoading}
      error={q.error}
      empty={!hasData(trend.flatMap((m) => [m.current, m.priorYear]))}
      emptyMessage="No premium has been written in this or the prior fiscal year."
    >
      {d !== undefined && (
        <>
          <div className="widget-figures">
            <Figure
              label="Month to date"
              value={d.monthToDate}
              hint={changeText(d.monthToDate, d.monthToDatePriorYear, PRIOR_YEAR)}
            />
            <Figure
              label="Year to date"
              value={d.yearToDate}
              hint={changeText(d.yearToDate, d.yearToDatePriorYear, PRIOR_YEAR)}
            />
            <Figure label="Prior year to date" value={d.yearToDatePriorYear} />
          </div>
          <TrendChart data={trend} currentLabel="This year" priorLabel="Prior year" />
        </>
      )}
    </WidgetCard>
  );
}
