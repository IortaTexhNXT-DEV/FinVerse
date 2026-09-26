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
import { WorkloadWidget } from './widgets/WorkloadWidget';

/**
 * Executive finance dashboard: headline KPIs of the ledger, then one widget per area (premium,
 * claims, collections, payables, cash, budget). Every widget loads on its own and shows a message
 * instead of a chart when it has no data.
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
        description={
          d
            ? `Financial position as of ${formatDate(d.asOf)} · fiscal year from ${formatDate(d.fiscalYearStart)} · amounts in ${ccy}`
            : 'Loading financial position…'
        }
        actions={
          can('JOURNAL_AUTHORIZE') &&
          d !== undefined &&
          d.pendingJournals > 0 && (
            <Button variant="accent" onClick={() => void navigate('/gl/journals')}>
              {d.pendingJournals} journal(s) awaiting authorization
            </Button>
          )
        }
      />
      <ErrorAlert error={summary.error} />
      <div className="grid-4">
        {d !== undefined && <SummaryKpis summary={d} />}
        <WorkloadWidget enabled={allowed} />
      </div>
      <div className="grid-2">
        <PremiumWidget enabled={allowed} />
        <ClaimsWidget enabled={allowed} />
        <CollectionsWidget enabled={allowed} />
        <PayablesWidget enabled={allowed} />
        <CashWidget enabled={allowed} />
        <BudgetWidget enabled={allowed} />
        {d !== undefined && <SummaryCharts summary={d} />}
      </div>
    </div>
  );
}
