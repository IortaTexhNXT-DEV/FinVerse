import { useQuery } from '@tanstack/react-query';
import { FileBarChart2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { budgetApi } from '@/api/budget';
import type { VarianceLine } from '@/api/budget';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatCompact, today } from '@/utils/format';
import { MONTH_LABELS, sum, utilizationLevel } from './budgetMath';

const CHART_ACCOUNTS = 10;
const LEVEL_BADGE = { over: 'danger', warning: 'warning', ok: 'success', none: 'neutral' };

function percent(value: number | undefined): string {
  return value === undefined ? '–' : `${value.toFixed(2)}%`;
}

function UtilizationBadge({
  line,
  threshold,
}: Readonly<{ line: VarianceLine; threshold: number }>) {
  const level = utilizationLevel(line.utilizationPct, threshold);
  return <span className={`badge ${LEVEL_BADGE[level]}`}>{percent(line.utilizationPct)}</span>;
}

/** Budget against actual for the month and year to date, with utilization alerts. */
export default function BudgetVsActualPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [asOf, setAsOf] = useState(today());
  const [byCostCenter, setByCostCenter] = useState(false);
  const [threshold, setThreshold] = useState(90);

  const comparison = useQuery({
    queryKey: ['budget-variance', companyId, asOf, byCostCenter],
    queryFn: () => budgetApi.variance(companyId, asOf, byCostCenter),
    enabled: companyId > 0,
  });
  const alerts = useQuery({
    queryKey: ['budget-alerts', companyId, asOf, threshold],
    queryFn: () => budgetApi.alerts(companyId, asOf, threshold),
    enabled: companyId > 0,
  });
  const lines = comparison.data?.lines ?? [];
  const byClass = (cls: VarianceLine['accountClass']) =>
    lines.filter((l) => l.accountClass === cls);
  const chart = [...lines]
    .sort((a, b) => b.budgetYtd - a.budgetYtd)
    .slice(0, CHART_ACCOUNTS)
    .map((l) => ({ name: l.accountCode, budget: l.budgetYtd, actual: l.actualYtd }));
  const month = comparison.data ? MONTH_LABELS[comparison.data.periodNo - 1] : '';

  return (
    <div className="stack">
      <PageHeader
        section="Planning & Closing"
        title="Budget vs Actual"
        description={
          comparison.data?.budgetVersion
            ? `Approved budget v${comparison.data.budgetVersion}, FY ${comparison.data.fiscalYear} · month ${month ?? ''} and year to date`
            : 'No approved budget for the fiscal year of the selected date'
        }
        actions={
          <Button
            variant="secondary"
            icon={<FileBarChart2 size={16} />}
            onClick={() => void navigate('/reports/GL-BVA')}
          >
            Report & export
          </Button>
        }
      />
      <Card>
        <div className="form-grid">
          <Field label="As of date">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={asOf}
                onChange={(e) => setAsOf(e.target.value)}
              />
            )}
          </Field>
          <Field label="Alert threshold %">
            {(id) => (
              <input
                id={id}
                className="input num"
                type="number"
                value={threshold}
                onChange={(e) => setThreshold(Number(e.target.value))}
              />
            )}
          </Field>
          <label className="checkbox" style={{ alignSelf: 'end' }}>
            <input
              type="checkbox"
              checked={byCostCenter}
              onChange={(e) => setByCostCenter(e.target.checked)}
            />
            Split by cost centre
          </label>
        </div>
      </Card>
      <ErrorAlert error={comparison.error ?? alerts.error} />
      <div className="grid-4">
        <Kpi
          label="Income budget (YTD)"
          value={formatCompact(sum(byClass('INCOME').map((l) => l.budgetYtd)))}
        />
        <Kpi
          label="Income actual (YTD)"
          value={formatCompact(sum(byClass('INCOME').map((l) => l.actualYtd)))}
        />
        <Kpi
          label="Expense budget (YTD)"
          value={formatCompact(sum(byClass('EXPENSE').map((l) => l.budgetYtd)))}
        />
        <Kpi
          label="Expense actual (YTD)"
          value={formatCompact(sum(byClass('EXPENSE').map((l) => l.actualYtd)))}
          hint={`${alerts.data?.length ?? 0} account(s) over ${threshold}% of the annual budget`}
          accent
        />
      </div>
      <div className="grid-2">
        <Card title="Budget vs actual, year to date (largest budgets)">
          <div style={{ height: 300 }}>
            <ResponsiveContainer>
              <BarChart data={chart}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                <XAxis dataKey="name" />
                <YAxis tickFormatter={(v: number) => formatCompact(v)} />
                <Tooltip formatter={(v) => formatAmount(Number(v))} />
                <Legend />
                <Bar
                  dataKey="budget"
                  name="Budget"
                  fill="var(--brand-blue)"
                  radius={[4, 4, 0, 0]}
                />
                <Bar
                  dataKey="actual"
                  name="Actual"
                  fill="var(--brand-gold)"
                  radius={[4, 4, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card title="Budget threshold exceeded" flush>
          <DataTable<VarianceLine>
            loading={alerts.isLoading}
            rows={alerts.data ?? []}
            rowKey={(l) => l.accountCode}
            emptyMessage="No expense account has reached the threshold."
            columns={[
              { key: 'a', header: 'Account', render: (l) => `${l.accountCode} ${l.accountName}` },
              {
                key: 'b',
                header: 'Annual budget',
                numeric: true,
                render: (l) => <Amount value={l.annualBudget} />,
              },
              {
                key: 'y',
                header: 'Actual YTD',
                numeric: true,
                render: (l) => <Amount value={l.actualYtd} />,
              },
              {
                key: 'u',
                header: 'Used',
                render: (l) => <UtilizationBadge line={l} threshold={threshold} />,
              },
            ]}
          />
        </Card>
      </div>
      <Card title="Variance by account" flush>
        <DataTable<VarianceLine>
          loading={comparison.isLoading}
          rows={lines}
          rowKey={(l) => `${l.accountCode}|${l.costCenter ?? ''}`}
          columns={[
            { key: 'a', header: 'Account', render: (l) => <strong>{l.accountCode}</strong> },
            { key: 'n', header: 'Name', render: (l) => l.accountName },
            { key: 'c', header: 'Cost centre', render: (l) => l.costCenter ?? '' },
            {
              key: 'bm',
              header: 'Budget (month)',
              numeric: true,
              render: (l) => <Amount value={l.budgetMonth} />,
            },
            {
              key: 'am',
              header: 'Actual (month)',
              numeric: true,
              render: (l) => <Amount value={l.actualMonth} />,
            },
            {
              key: 'vm',
              header: 'Var %',
              numeric: true,
              render: (l) => percent(l.monthVariancePct),
            },
            {
              key: 'by',
              header: 'Budget (YTD)',
              numeric: true,
              render: (l) => <Amount value={l.budgetYtd} />,
            },
            {
              key: 'ay',
              header: 'Actual (YTD)',
              numeric: true,
              render: (l) => <Amount value={l.actualYtd} />,
            },
            {
              key: 'vy',
              header: 'Variance (YTD)',
              numeric: true,
              render: (l) => <Amount value={l.ytdVariance} />,
            },
            {
              key: 'f',
              header: 'Assessment',
              render: (l) => (
                <span className={`badge ${l.favourable ? 'success' : 'danger'}`}>
                  {l.favourable ? 'Favourable' : 'Unfavourable'}
                </span>
              ),
            },
            {
              key: 'u',
              header: 'Utilization',
              render: (l) => <UtilizationBadge line={l} threshold={threshold} />,
            },
          ]}
        />
      </Card>
    </div>
  );
}
