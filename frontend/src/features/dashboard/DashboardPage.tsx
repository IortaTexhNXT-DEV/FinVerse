import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { dashboardApi } from '@/api/dashboard';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { BudgetWidget } from './widgets/BudgetWidget';
import { CashWidget } from './widgets/CashWidget';
import { ClaimsWidget } from './widgets/ClaimsWidget';
import { CollectionsWidget } from './widgets/CollectionsWidget';
import { PayablesWidget } from './widgets/PayablesWidget';
import { PremiumWidget } from './widgets/PremiumWidget';
import { SummaryCharts, SummaryKpis } from './widgets/SummaryWidgets';
import { showsInsurerWidgets } from './insurerWidgets';
import { WorkloadWidget } from './widgets/WorkloadWidget';

/** The insurer KPI widgets (premium, claims), shown to insurer roles only. */
function InsurerWidgets({
  allowed,
  roles,
}: Readonly<{ allowed: boolean; roles: readonly string[] | undefined }>) {
  if (!allowed || !showsInsurerWidgets(roles)) {
    return null;
  }
  return (
    <>
      <PremiumWidget enabled />
      <ClaimsWidget enabled />
    </>
  );
}

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

/** "Financial position as of 25-Sep-2026 · amounts in PHP". */
function positionLine(d: { asOf: string } | undefined, ccy: string): string {
  return d === undefined
    ? 'Loading financial position…'
    : `Financial position as of ${formatDate(d.asOf)} · amounts in ${ccy}`;
}

/**
 * Executive finance dashboard: headline KPIs of the ledger, then one widget per area
 * (collections, payables, cash, budget). Every widget loads on its own and shows a message
 * instead of a chart when it has no data. The insurer premium and claims widgets are hidden for
 * every BDOI role (they show insurer KPIs).
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
  const d = summary.data;
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
      <div className="grid-4">
        {d !== undefined && <SummaryKpis summary={d} />}
        <WorkloadWidget enabled={allowed} />
      </div>
      <div className="grid-2">
        <InsurerWidgets allowed={allowed} roles={user?.roles} />
        <CollectionsWidget enabled={allowed} />
        <PayablesWidget enabled={allowed} />
        <CashWidget enabled={allowed} />
        <BudgetWidget enabled={allowed} />
        {d !== undefined && <SummaryCharts summary={d} />}
      </div>
    </div>
  );
}
