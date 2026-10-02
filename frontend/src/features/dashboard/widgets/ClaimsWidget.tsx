import { dashboardApi } from '@/api/dashboard';
import { hasData } from '../dashboardMath';
import { WidgetCard } from './WidgetCard';
import { useWidget } from './widgetSupport';
import { Figure, TrendChart } from './WidgetParts';

/** Claims paid (claims expense accounts) and the outstanding claims reserve. */
export function ClaimsWidget({ enabled }: Readonly<{ enabled: boolean }>) {
  const q = useWidget('claims', dashboardApi.claims, enabled);
  const d = q.data;
  const trend = d?.monthly ?? [];
  return (
    <WidgetCard
      title="Claims paid and outstanding"
      loading={q.isLoading}
      error={q.error}
      empty={
        d !== undefined &&
        !hasData([d.outstanding, ...trend.flatMap((m) => [m.current, m.priorYear])])
      }
      emptyMessage="No claims paid or reserved yet."
    >
      {d !== undefined && (
        <>
          <div className="widget-figures">
            <Figure label="Paid month to date" value={d.paidMonthToDate} />
            <Figure label="Paid year to date" value={d.paidYearToDate} />
            <Figure label="Outstanding" value={d.outstanding} hint="Outstanding claims reserve" />
          </div>
          <TrendChart data={trend} currentLabel="Paid this year" priorLabel="Paid prior year" />
        </>
      )}
    </WidgetCard>
  );
}
