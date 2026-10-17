import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { dashboardExportParams, pmWorkspaceApi } from '@/api/pmWorkspace';
import type { PmDashboardFilters, PmKpi } from '@/api/pmWorkspace';
import { WorkTiles } from '@/components/broking/WorkTiles';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { DashboardFilters } from './DashboardFilters';
import type { DashboardFilterValues } from './DashboardFilters';
import { DrillDownTable } from './DrillDownTable';
import { ExportButtons } from './ExportButtons';
import { KPI_COUNTS, KPI_HINTS, packagedFlag } from './pmDashboard';
import { StageTiles } from './StageTiles';

/** The first day of the default period: the last 90 days. */
function quarterAgo(): string {
  const d = new Date(`${today()}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() - 90);
  return d.toISOString().slice(0, 10);
}

function toApi(f: DashboardFilterValues): PmDashboardFilters {
  return {
    from: f.from || undefined,
    to: f.to || undefined,
    tsuOfficer: f.tsuOfficer || undefined,
    lineCode: f.lineCode || undefined,
    packaged: packagedFlag(f.packageType),
  };
}

/**
 * Product Maintenance Dashboard (BDOI FRS FRPM.001.01): the KPIs Incoming Requests, In-Progress
 * Requests, For Approval, Expiring Packages, Issued Proposals and Deactivation Requests for the
 * period, TSU officer, product line and package type; a KPI opens its requests with their aging;
 * the data exports to Excel, PDF and CSV; below, the package requests by stage with their service
 * levels.
 */
export default function PmDashboardPage() {
  const companyId = useCompanyId();
  const [values, setValues] = useState<DashboardFilterValues>({
    from: quarterAgo(),
    to: today(),
    tsuOfficer: '',
    lineCode: '',
    packageType: '',
  });
  const [kpi, setKpi] = useState<PmKpi>('INCOMING');
  const [page, setPage] = useState(0);
  const filters = toApi(values);
  const rangeError =
    values.from !== '' && values.to !== '' && values.to < values.from
      ? 'The Period To date must be on or after the Period From date'
      : null;
  const enabled = companyId > 0 && rangeError === null;
  const dashboard = useQuery({
    queryKey: ['pm-dashboard', companyId, filters],
    queryFn: () => pmWorkspaceApi.dashboard(companyId, filters),
    enabled,
  });
  const rows = useQuery({
    queryKey: ['pm-dashboard', 'rows', companyId, filters, kpi, page],
    queryFn: () => pmWorkspaceApi.drillDown(companyId, filters, kpi, page),
    enabled,
  });
  const kpis = dashboard.data?.kpis ?? [];
  const label = kpis.find((k) => k.code === kpi)?.label ?? '';
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Product Maintenance Dashboard"
        description="Where requests stand, what waits for approval and which packages end soon."
        actions={
          <ExportButtons
            report="PM-DASHBOARD"
            params={dashboardExportParams(companyId, filters, kpi)}
          />
        }
      />
      <Card>
        <DashboardFilters
          value={values}
          onChange={(next) => {
            setValues(next);
            setPage(0);
          }}
        />
      </Card>
      {rangeError !== null && <Notice tone="error">{rangeError}</Notice>}
      <ErrorAlert error={dashboard.error ?? rows.error} />
      <Card title="Key figures">
        <WorkTiles
          label="Key figures"
          tiles={kpis.map((k) => ({
            key: k.code,
            label: k.label,
            value: dashboard.data?.counts[KPI_COUNTS[k.code]] ?? 0,
            active: k.code === kpi,
            onClick: () => {
              setKpi(k.code);
              setPage(0);
            },
          }))}
        />
      </Card>
      <Card title={label} actions={<span className="muted">{KPI_HINTS[kpi]}</span>} flush>
        <DrillDownTable data={rows.data} loading={rows.isLoading} onPage={setPage} />
      </Card>
      <StageTiles />
    </div>
  );
}
