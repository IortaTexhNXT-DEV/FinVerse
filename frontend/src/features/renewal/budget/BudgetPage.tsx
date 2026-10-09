import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Upload } from 'lucide-react';
import { useState } from 'react';
import { renewalDashboardApi } from '@/api/renewalDashboard';
import type { BudgetBody, BudgetMonth, BudgetView } from '@/api/renewalDashboard';
import { LovLabel } from '@/components/broking/LovLabel';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDateTime } from '@/utils/format';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import { UploadPanel } from '../common/UploadPanel';
import '../renewal.css';
import { annualTotals } from './budgetTotals';

const MONTHS = [
  'January',
  'February',
  'March',
  'April',
  'May',
  'June',
  'July',
  'August',
  'September',
  'October',
  'November',
  'December',
];

const MEASURES: Record<string, string> = {
  PREMIUM: 'Basic Premium',
  COMMISSION: 'Gross Commission',
};

function emptyMonths(): BudgetMonth[] {
  return MONTHS.map((_, i) => ({
    monthNo: i + 1,
    newAmount: 0,
    renewalAmount: 0,
    organicAmount: 0,
  }));
}

function blank(year: number): BudgetBody {
  return {
    fiscalYear: year,
    measure: 'PREMIUM',
    segment: '',
    heads: { unitHead: null, sectionHead: null, teamHead: null, teamLead: null },
    months: emptyMonths(),
  };
}

function BudgetForm({ value, onClose }: Readonly<{ value: BudgetBody; onClose: () => void }>) {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const [b, setB] = useState<BudgetBody>(value);
  const save = useMutation({
    mutationFn: () => renewalDashboardApi.saveBudget(companyId, b),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['renewal', 'budgets'] });
      onClose();
    },
  });
  const totals = annualTotals(b.months);
  const setMonth = (i: number, k: keyof BudgetMonth, v: string) =>
    setB({ ...b, months: b.months.map((m, j) => (j === i ? { ...m, [k]: Number(v) } : m)) });
  const text = (k: 'region' | 'team' | 'subTeam' | 'accountOfficer', label: string) => (
    <Field label={label}>
      {(id) => (
        <input
          id={id}
          className="input"
          value={b[k] ?? ''}
          onChange={(e) => setB({ ...b, [k]: e.target.value })}
        />
      )}
    </Field>
  );
  return (
    <Modal
      title="Renewal Annual Budget"
      open
      size="lg"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button disabled={save.isPending} onClick={() => save.mutate()}>
            Save Budget
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <Field label="Fiscal Year" required>
          {(id) => (
            <input
              id={id}
              className="input"
              type="number"
              value={b.fiscalYear}
              onChange={(e) => setB({ ...b, fiscalYear: Number(e.target.value) })}
            />
          )}
        </Field>
        <Field label="Measure" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={b.measure}
              onChange={(e) => setB({ ...b, measure: e.target.value })}
            >
              {Object.entries(MEASURES).map(([code, label]) => (
                <option key={code} value={code}>
                  {label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Market Segment" required>
          {(id) => (
            <LovSelect
              id={id}
              type="MARKET_SEGMENT"
              value={b.segment}
              onChange={(code) => setB({ ...b, segment: code })}
            />
          )}
        </Field>
        {text('region', 'Region (not for Corporate)')}
        {text('team', 'Team (Corporate)')}
        {text('subTeam', 'Sub-Team (Corporate)')}
        {text('accountOfficer', 'Account Officer (not for CBG)')}
        <Field label="Team Lead">
          {(id) => (
            <input
              id={id}
              className="input"
              value={b.heads.teamLead ?? ''}
              onChange={(e) => setB({ ...b, heads: { ...b.heads, teamLead: e.target.value } })}
            />
          )}
        </Field>
      </div>
      <DataTable<BudgetMonth>
        rows={b.months}
        rowKey={(m) => m.monthNo}
        caption="Monthly budget"
        columns={[
          { key: 'm', header: 'Month', render: (m) => MONTHS[m.monthNo - 1] },
          ...(
            [
              ['newAmount', 'New Budget'],
              ['renewalAmount', 'Renewal Budget'],
              ['organicAmount', 'Organic Budget'],
            ] as const
          ).map(([k, label]) => ({
            key: k,
            header: label,
            kind: 'amount' as const,
            render: (m: BudgetMonth) => (
              <input
                className="input num"
                type="number"
                min={0}
                step="0.01"
                aria-label={`${label} ${MONTHS[m.monthNo - 1]}`}
                value={m[k]}
                onChange={(e) => setMonth(m.monthNo - 1, k, e.target.value)}
              />
            ),
          })),
        ]}
        footer={
          <p className="rnw-note">
            Annual New {formatAmount(totals.newTotal)} · Annual Renewal{' '}
            {formatAmount(totals.renewalTotal)} · Annual Organic {formatAmount(totals.organicTotal)}{' '}
            · Grand Total {formatAmount(totals.grandTotal)}
          </p>
        }
      />
    </Modal>
  );
}

function History({ budget, onClose }: Readonly<{ budget: BudgetView; onClose: () => void }>) {
  const companyId = useCompanyId();
  const history = useQuery({
    queryKey: ['renewal', 'budget-history', budget.id],
    queryFn: () => renewalDashboardApi.budgetHistory(companyId, budget.id),
  });
  return (
    <Modal
      title="Budget History"
      open
      size="lg"
      onClose={onClose}
      facts={[
        { label: 'Fiscal Year', value: budget.fiscalYear },
        { label: 'Market Segment', value: budget.segment },
        { label: 'Created By', value: <UserName login={budget.createdBy} /> },
        { label: 'Created Date', value: formatDateTime(budget.createdAt) },
      ]}
      footer={
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
      }
    >
      <ErrorAlert error={history.error} />
      <DataTable
        loading={history.isLoading}
        rows={history.data ?? []}
        rowKey={(h) => `${h.field}-${h.modifiedAt}`}
        emptyMessage="No changes since the budget was created"
        columns={[
          { key: 'f', header: 'Budget', render: (h) => h.field },
          {
            key: 'p',
            header: 'Previous Budget',
            kind: 'amount',
            render: (h) => formatAmount(h.previous),
          },
          {
            key: 'u',
            header: 'Updated Budget',
            kind: 'amount',
            render: (h) => formatAmount(h.updated),
          },
          { key: 'by', header: 'Modified By', render: (h) => <UserName login={h.modifiedBy} /> },
          {
            key: 'at',
            header: 'Modification Date',
            kind: 'datetime',
            render: (h) => formatDateTime(h.modifiedAt),
          },
        ]}
      />
    </Modal>
  );
}

/**
 * Renewal Annual Budget Maintenance (BDOI Renewal FRS FRRN.042): the Budget Inquiry of a fiscal
 * year by segment and hierarchy, creation and update with the monthly new, renewal and organic
 * budget and the annual totals, the history of every change and the budget upload.
 */
export default function BudgetPage() {
  const companyId = useCompanyId();
  const [year, setYear] = useState(new Date().getFullYear());
  const [edit, setEdit] = useState<BudgetBody>();
  const [history, setHistory] = useState<BudgetView>();
  const [upload, setUpload] = useState(false);
  const budgets = useQuery({
    queryKey: ['renewal', 'budgets', companyId, year],
    queryFn: () => renewalDashboardApi.budgets(companyId, year),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Renewal Annual Budget"
        description="New, renewal and organic budgets by fiscal year, segment and hierarchy."
        actions={
          <>
            <Button variant="secondary" icon={<Upload size={16} />} onClick={() => setUpload(true)}>
              Upload Budget
            </Button>
            <Button icon={<Plus size={16} />} onClick={() => setEdit(blank(year))}>
              New Budget
            </Button>
          </>
        }
      />
      {upload && (
        <UploadPanel label="Upload Budget" handler="RNW_BUDGET" onClose={() => setUpload(false)} />
      )}
      <div className="form-grid">
        <Field label="Fiscal Year">
          {(id) => (
            <input
              id={id}
              className="input"
              type="number"
              value={year}
              onChange={(e) => setYear(Number(e.target.value))}
            />
          )}
        </Field>
      </div>
      <ErrorAlert error={budgets.error} onRetry={() => void budgets.refetch()} />
      <Card flush>
        <DataTable<BudgetView>
          loading={budgets.isLoading}
          rows={budgets.data ?? []}
          rowKey={(b) => b.id}
          emptyMessage="No budget for this fiscal year"
          columns={[
            { key: 'fy', header: 'Fiscal Year', render: (b) => b.fiscalYear },
            { key: 'measure', header: 'Measure', render: (b) => MEASURES[b.measure] ?? b.measure },
            {
              key: 'seg',
              header: 'Market Segment',
              render: (b) => <LovLabel type="MARKET_SEGMENT" code={b.segment} />,
            },
            { key: 'region', header: 'Region', render: (b) => b.region ?? '' },
            { key: 'team', header: 'Team', render: (b) => b.team ?? '' },
            { key: 'sub', header: 'Sub-Team', render: (b) => b.subTeam ?? '' },
            {
              key: 'sh',
              header: 'Section Head',
              render: (b) =>
                b.heads.sectionHead === null ? '' : <UserName login={b.heads.sectionHead} />,
            },
            {
              key: 'th',
              header: 'Team Head',
              render: (b) =>
                b.heads.teamHead === null ? '' : <UserName login={b.heads.teamHead} />,
            },
            {
              key: 'tl',
              header: 'Team Lead',
              render: (b) =>
                b.heads.teamLead === null ? '' : <UserName login={b.heads.teamLead} />,
            },
            {
              key: 'ao',
              header: 'Account Officer',
              render: (b) =>
                b.accountOfficer === null ? '' : <UserName login={b.accountOfficer} />,
            },
            {
              key: 'n',
              header: 'Annual New Budget',
              kind: 'amount',
              render: (b) => formatAmount(b.totals.newTotal),
            },
            {
              key: 'r',
              header: 'Annual Renewal Budget',
              kind: 'amount',
              render: (b) => formatAmount(b.totals.renewalTotal),
            },
            {
              key: 'o',
              header: 'Annual Organic Budget',
              kind: 'amount',
              render: (b) => formatAmount(b.totals.organicTotal),
            },
            {
              key: 'g',
              header: 'Grand Total',
              kind: 'amount',
              render: (b) => formatAmount(b.totals.grandTotal),
            },
            {
              key: 'actions',
              header: '',
              kind: 'actions',
              render: (b) => (
                <RowActionMenu
                  label={`${b.fiscalYear} ${b.segment}`}
                  actions={[
                    {
                      label: 'Update Budget',
                      onSelect: () =>
                        setEdit({
                          fiscalYear: b.fiscalYear,
                          measure: b.measure,
                          segment: b.segment,
                          region: b.region ?? undefined,
                          team: b.team ?? undefined,
                          subTeam: b.subTeam ?? undefined,
                          accountOfficer: b.accountOfficer ?? undefined,
                          heads: b.heads,
                          months: b.months,
                        }),
                    },
                    { label: 'View Budget History', onSelect: () => setHistory(b) },
                  ]}
                />
              ),
            },
          ]}
        />
      </Card>
      {edit !== undefined && <BudgetForm value={edit} onClose={() => setEdit(undefined)} />}
      {history !== undefined && <History budget={history} onClose={() => setHistory(undefined)} />}
    </div>
  );
}
