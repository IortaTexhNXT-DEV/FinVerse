import { dashboardApi } from '@/api/dashboard';
import { sharePct } from '../dashboardMath';
import { WidgetCard } from './WidgetCard';
import { useWidget } from './widgetSupport';
import { Figure } from './WidgetParts';

function DueBar({ label, part, whole }: Readonly<{ label: string; part: number; whole: number }>) {
  return (
    <div>
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <span className="muted">{label}</span>
        <span className="num">{sharePct(part, whole).toFixed(0)}%</span>
      </div>
      <div className="widget-bar">
        <div style={{ width: `${sharePct(part, whole)}%` }} />
      </div>
    </div>
  );
}

/** Vendor payables overdue and falling due within 7 and 30 days. */
export function PayablesWidget({ enabled }: Readonly<{ enabled: boolean }>) {
  const q = useWidget('payables', dashboardApi.payables, enabled);
  const d = q.data;
  return (
    <WidgetCard
      title="Payables due"
      loading={q.isLoading}
      error={q.error}
      empty={d?.openItems === 0}
      emptyMessage="No outstanding supplier payables."
    >
      {d !== undefined && (
        <>
          <div className="widget-figures">
            <Figure label="Overdue" value={d.overdue} danger={d.overdue > 0} />
            <Figure label="Due in 7 days" value={d.dueIn7Days} />
            <Figure label="Due in 30 days" value={d.dueIn30Days} />
            <Figure label="Total payables" value={d.total} hint={`${d.openItems} open item(s)`} />
          </div>
          <div className="stack" style={{ gap: 10 }}>
            <DueBar label="Overdue share" part={d.overdue} whole={d.total} />
            <DueBar label="Due within 30 days" part={d.dueIn30Days} whole={d.total} />
          </div>
        </>
      )}
    </WidgetCard>
  );
}
