import { Link } from 'react-router-dom';
import { dashboardApi } from '@/api/dashboard';
import { Kpi } from '@/components/ui/Kpi';
import { humanize } from '@/utils/format';
import { useWidget } from './widgetSupport';

function moduleSummary(byModule: Record<string, number>): string {
  const parts = Object.entries(byModule).map(([m, n]) => `${String(n)} ${humanize(m)}`);
  return parts.length === 0 ? 'Nothing waiting for you' : parts.join(' · ');
}

/** Open alerts of the company and the signed-in user's approval inbox, as KPI tiles. */
export function WorkloadWidget({ enabled }: Readonly<{ enabled: boolean }>) {
  const q = useWidget('workload', (companyId) => dashboardApi.workload(companyId), enabled);
  const d = q.data;
  if (d === undefined) {
    return null;
  }
  return (
    <>
      <Kpi
        label="Pending approvals"
        value={
          <Link className="kpi-link" to="/approvals" aria-label="Open my approvals">
            {d.pendingApprovals}
          </Link>
        }
        hint={moduleSummary(d.approvalsByModule)}
        accent={d.pendingApprovals > 0}
      />
      <Kpi
        label="Open alerts"
        value={
          <Link className="kpi-link" to="/alerts" aria-label="Open the alerts">
            {d.openAlerts}
          </Link>
        }
        hint="Open or acknowledged exceptions"
        accent={d.openAlerts > 0}
      />
    </>
  );
}
