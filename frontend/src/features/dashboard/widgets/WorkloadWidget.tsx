import { useNavigate } from 'react-router-dom';
import { dashboardApi } from '@/api/dashboard';
import { Button } from '@/components/ui/Button';
import { Kpi } from '@/components/ui/Kpi';
import { humanize } from '@/utils/format';
import { useWidget } from './widgetSupport';

function moduleSummary(byModule: Record<string, number>): string {
  const parts = Object.entries(byModule).map(([m, n]) => `${String(n)} ${humanize(m)}`);
  return parts.length === 0 ? 'Nothing waiting for you' : parts.join(' · ');
}

/** Open alerts of the company and the signed-in user's approval inbox, as KPI tiles. */
export function WorkloadWidget({ enabled }: Readonly<{ enabled: boolean }>) {
  const navigate = useNavigate();
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
          <Button
            variant="ghost"
            aria-label="Open my approvals"
            onClick={() => void navigate('/approvals')}
          >
            {d.pendingApprovals}
          </Button>
        }
        hint={moduleSummary(d.approvalsByModule)}
        accent={d.pendingApprovals > 0}
      />
      <Kpi
        label="Open alerts"
        value={
          <Button
            variant="ghost"
            aria-label="Open the alerts"
            onClick={() => void navigate('/alerts')}
          >
            {d.openAlerts}
          </Button>
        }
        hint="Open or acknowledged exceptions"
        accent={d.openAlerts > 0}
      />
    </>
  );
}
