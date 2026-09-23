import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { dashboardApi } from '@/api/dashboard';
import type { CompositionItem, DashboardSummary } from '@/api/dashboard';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';
import { formatAmount, formatCompact, formatDate } from '@/utils/format';

const BRAND_BLUE = '#0033A0';
const BRAND_GOLD = '#FDB913';
const BRAND_NAVY = '#00205B';

function CompositionList({ items }: Readonly<{ items: CompositionItem[] }>) {
  const total = items.reduce((acc, i) => acc + Math.abs(i.amount), 0) || 1;
  return (
    <ul className="stack" style={{ listStyle: 'none', padding: 0, margin: 0, gap: 10 }}>
      {items.map((i) => (
        <li key={i.label}>
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <span>{i.label}</span>
            <strong className="num">{formatAmount(i.amount)}</strong>
          </div>
          <div style={{ height: 6, background: '#E8EEF8', borderRadius: 4 }}>
            <div
              style={{
                width: `${(Math.abs(i.amount) / total) * 100}%`,
                height: 6,
                background: BRAND_BLUE,
                borderRadius: 4,
              }}
            />
          </div>
        </li>
      ))}
    </ul>
  );
}

function KpiGrid({ summary: d }: Readonly<{ summary: DashboardSummary }>) {
  return (
    <div className="grid-4">
      <Kpi
        label="Income (YTD)"
        value={formatCompact(d.totalIncomeYtd)}
        hint={formatAmount(d.totalIncomeYtd)}
      />
      <Kpi
        label="Expenses (YTD)"
        value={formatCompact(d.totalExpenseYtd)}
        hint={formatAmount(d.totalExpenseYtd)}
      />
      <Kpi
        label="Net result (YTD)"
        value={formatCompact(d.netResultYtd)}
        hint={formatAmount(d.netResultYtd)}
        accent
      />
      <Kpi
        label="Cash position"
        value={formatCompact(d.cashPosition)}
        hint="Cash and bank balances"
      />
      <Kpi label="Insurance receivables" value={formatCompact(d.receivables)} />
      <Kpi
        label="Technical reserves"
        value={formatCompact(d.technicalReserves)}
        hint="UPR, claims and IBNR reserves"
      />
      <Kpi label="Total assets" value={formatCompact(d.totalAssets)} />
      <Kpi
        label="Journals in progress"
        value={d.pendingJournals + d.draftJournals}
        hint={`${d.pendingJournals} pending · ${d.draftJournals} draft`}
        accent
      />
    </div>
  );
}

/** Executive finance dashboard: headline KPIs, monthly trend and composition. */
export default function DashboardPage() {
  const companyId = useCompanyId();
  const { company, branchId } = useWorkspace();
  const { user, can } = useAuth();
  const navigate = useNavigate();
  const summary = useQuery({
    queryKey: ['dashboard', companyId, branchId],
    queryFn: () => dashboardApi.summary(companyId, branchId),
    enabled: companyId > 0 && can('DASHBOARD_VIEW'),
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
      {d !== undefined && (
        <>
          <KpiGrid summary={d} />
          <div className="grid-2">
            <Card title="Monthly income vs expenses">
              <div style={{ height: 300 }}>
                <ResponsiveContainer>
                  <BarChart data={d.monthly}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#DDE3EC" />
                    <XAxis dataKey="month" />
                    <YAxis tickFormatter={(v: number) => formatCompact(v)} />
                    <Tooltip formatter={(v) => formatAmount(Number(v))} />
                    <Legend />
                    <Bar dataKey="income" name="Income" fill={BRAND_BLUE} radius={[4, 4, 0, 0]} />
                    <Bar
                      dataKey="expense"
                      name="Expenses"
                      fill={BRAND_GOLD}
                      radius={[4, 4, 0, 0]}
                    />
                    <Line dataKey="netResult" name="Net result" stroke={BRAND_NAVY} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </Card>
            <Card title="Income composition (YTD)">
              <CompositionList items={d.incomeComposition} />
            </Card>
            <Card title="Expense composition (YTD)">
              <CompositionList items={d.expenseComposition} />
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
