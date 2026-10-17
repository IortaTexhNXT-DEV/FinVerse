import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { renewalDashboardApi } from '@/api/renewalDashboard';
import type { DashboardFilters } from '@/api/renewalDashboard';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import { DashboardFilterBar, DrillDialog, KpiCards } from './dashboardBits';
import { AGEING_LABELS, CARD_LABELS, defaultPeriod } from './dashboardFormat';
import {
  CellTable,
  ClosingTable,
  DealsTable,
  PersistencyTable,
  PipelineTable,
  ProductionTable,
} from './dashboardWidgets';
import { InsurerTables, WorkloadTable } from './dashboardTables';
import type { Open } from './dashboardWidgets';
import '../renewal.css';

/**
 * Renewal Dashboard (BDOI Renewal FRS FRRN.002.02): production against the Annual Renewal Budget,
 * pipeline, outstanding renewal accounts, closing ratio, biggest open deals, renewal persistency,
 * product mix, insurer renewal approvals and the KPI cards, with the period, market segment and
 * Account Officer filters; every figure opens the accounts it counts.
 */
export default function RenewalDashboardPage() {
  const companyId = useCompanyId();
  const [filters, setFilters] = useState<DashboardFilters>(defaultPeriod());
  const [top, setTop] = useState(50);
  const [open, setOpen] = useState<Open>();
  const dash = useQuery({
    queryKey: ['renewal', 'dashboard', companyId, filters, top],
    queryFn: () => renewalDashboardApi.dashboard(companyId, filters, top),
    enabled: companyId > 0,
  });
  const drill = useQuery({
    queryKey: ['renewal', 'dashboard-drill', companyId, filters, top, open?.metric],
    queryFn: () => renewalDashboardApi.drill(companyId, filters, open?.metric ?? '', top),
    enabled: companyId > 0 && open !== undefined,
  });
  const d = dash.data;
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Renewal Dashboard"
        description="Renewal production, pipeline and work in your scope; click any figure to see its accounts."
      />
      <DashboardFilterBar value={filters} onChange={setFilters} />
      <ErrorAlert error={dash.error} onRetry={() => void dash.refetch()} />
      {d !== undefined && (
        <>
          <KpiCards
            label="KPI summary cards"
            cards={d.cards.map((c) => ({
              key: c.key,
              label: CARD_LABELS[c.key] ?? c.key,
              count: c.count,
            }))}
            onOpen={(key) => setOpen({ metric: `CARD|${key}`, title: CARD_LABELS[key] ?? key })}
          />
          <ProductionTable d={d} open={setOpen} />
          <PipelineTable d={d} open={setOpen} />
          <div className="rnw-grid">
            <CellTable
              title="Outstanding Renewal Accounts"
              keyHeader="Ageing"
              cells={d.ageing}
              labels={AGEING_LABELS}
              open={setOpen}
            />
            <CellTable
              title="Product Mix"
              keyHeader="Product Line"
              cells={d.productMix}
              labels={{ TOTAL: 'Total' }}
              open={setOpen}
            />
          </div>
          <ClosingTable d={d} open={setOpen} />
          <DealsTable d={d} top={top} onTop={setTop} />
          <PersistencyTable d={d} open={setOpen} />
          <InsurerTables d={d} open={setOpen} />
          <WorkloadTable d={d} open={setOpen} />
        </>
      )}
      {open !== undefined && (
        <DrillDialog
          title={open.title}
          drill={drill.data}
          loading={drill.isLoading}
          error={drill.error}
          exportParams={{ metric: open.metric, ...filters, top }}
          onClose={() => setOpen(undefined)}
        />
      )}
    </div>
  );
}
