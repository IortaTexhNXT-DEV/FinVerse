import { dashboardApi } from '@/api/dashboard';
import { KpiTile } from '@/components/ui/KpiTile';
import { approvalLines, useWidget } from './widgetSupport';

/** Open alerts of the company and the signed-in user's approval inbox, as KPI tiles. */
export function WorkloadWidget({ enabled }: Readonly<{ enabled: boolean }>) {
  const q = useWidget('workload', (companyId) => dashboardApi.workload(companyId), enabled);
  const d = q.data;
  if (d === undefined) {
    return null;
  }
  return (
    <>
      <KpiTile
        label="Pending Approvals"
        value={d.pendingApprovals}
        to="/approvals"
        qualifier={
          d.pendingApprovals === 0 ? 'Nothing waiting for you' : 'Waiting for your decision'
        }
        alert={d.pendingApprovals > 0}
        breakdown={{
          label: 'Pending approvals by area',
          items: approvalLines(d.approvalsByModule),
          allTo: '/approvals',
        }}
      />
      <KpiTile
        label="Open Alerts"
        value={d.openAlerts}
        to="/alerts"
        qualifier="Open or acknowledged exceptions"
        alert={d.openAlerts > 0}
      />
    </>
  );
}
