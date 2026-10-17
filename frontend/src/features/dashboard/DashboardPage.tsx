import type { ReactNode } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { dashboardApi } from '@/api/dashboard';
import type { DashboardSummary } from '@/api/dashboard';
import { roleHomesApi } from '@/api/roleHomes';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { BudgetWidget } from './widgets/BudgetWidget';
import { CashWidget } from './widgets/CashWidget';
import { CollectionsWidget } from './widgets/CollectionsWidget';
import { PayablesWidget } from './widgets/PayablesWidget';
import { SummaryCharts, SummaryKpis } from './widgets/SummaryWidgets';
import { WorkloadWidget } from './widgets/WorkloadWidget';
import { arrangeWidgets } from './homeWidgets';
import { showsWidget } from './widgetModules';

/** Journals awaiting the user's authorization, as the page's call to action. */
function PendingJournals({
  summary,
  onOpen,
}: Readonly<{ summary: { pendingJournals: number } | undefined; onOpen: () => void }>) {
  const count = summary?.pendingJournals ?? 0;
  if (count <= 0) {
    return null;
  }
  return (
    <Button variant="accent" onClick={onOpen}>
      {count} Journal(s) Awaiting Authorization
    </Button>
  );
}

/** "Financial position as of 25-Sep-2026 · amounts in <base currency>". */
function positionLine(d: { asOf: string } | undefined, ccy: string): string {
  return d === undefined
    ? 'Loading financial position…'
    : `Financial position as of ${formatDate(d.asOf)} · amounts in ${ccy}`;
}

/** The widgets of the home page by code; a widget of a switched-off module is null. */
function homeWidgets(
  d: DashboardSummary | undefined,
  allowed: boolean,
  can: (permission: string) => boolean,
): Record<string, ReactNode> {
  return {
    KPIS: d === undefined ? null : <SummaryKpis key="KPIS" summary={d} />,
    WORKLOAD: <WorkloadWidget key="WORKLOAD" enabled={allowed} />,
    COLLECTIONS: <CollectionsWidget key="COLLECTIONS" enabled={allowed} />,
    PAYABLES: showsWidget('payables', can) ? (
      <PayablesWidget key="PAYABLES" enabled={allowed} />
    ) : null,
    CASH: <CashWidget key="CASH" enabled={allowed} />,
    BUDGET: showsWidget('budget', can) ? <BudgetWidget key="BUDGET" enabled={allowed} /> : null,
    CHARTS: d === undefined ? null : <SummaryCharts key="CHARTS" summary={d} />,
  };
}

/**
 * Executive finance dashboard: headline KPIs of the ledger, then one widget per area
 * (collections, payables, cash, budget). Every widget loads on its own and shows a message
 * instead of a chart when it has no data. The widgets and their order follow the user's roles (Role
 * Home Set-up); the widgets of a product module (payables, budget) follow the module switches.
 */
export default function DashboardPage() {
  const companyId = useCompanyId();
  const { company, branchId } = useWorkspace();
  const { user, can } = useAuth();
  const navigate = useNavigate();
  const allowed = can('DASHBOARD_VIEW');
  const summary = useQuery({
    queryKey: ['dashboard', companyId, branchId],
    queryFn: () => dashboardApi.summary(companyId, branchId),
    enabled: companyId > 0 && allowed,
  });
  const mine = useQuery({ queryKey: ['role-homes', 'mine'], queryFn: roleHomesApi.mine });
  const d = summary.data;
  const { top, grid } = arrangeWidgets(mine.data);
  const widgets = homeWidgets(d, allowed, can);
  const widget = (code: string): ReactNode => widgets[code] ?? null;
  const ccy = company?.baseCurrency ?? '';

  return (
    <div className="stack">
      <PageHeader
        section="Dashboard"
        title={`Welcome, ${user?.fullName.split(' ')[0] ?? ''}`}
        description={positionLine(d, ccy)}
        actions={
          can('JOURNAL_AUTHORIZE') && (
            <PendingJournals summary={d} onOpen={() => void navigate('/gl/journals')} />
          )
        }
      />
      <ErrorAlert error={summary.error} />
      {top.length > 0 && <div className="grid-4">{top.map(widget)}</div>}
      {grid.length > 0 && <div className="grid-2">{grid.map(widget)}</div>}
    </div>
  );
}
