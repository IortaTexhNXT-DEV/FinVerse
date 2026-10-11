import { useQuery } from '@tanstack/react-query';
import { RefreshCw, Search } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, toQuery } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { LoadingPanel } from '@/components/ui/LoadingPanel';
import { PageHeader } from '@/components/ui/PageHeader';
import { useBaseCurrency, useCompanyId } from '@/context/workspaceContext';
import { cashieringApi } from './cashieringApi';
import type { DashboardGroup, DashboardItem } from './dashboardLogic';
import { NAVIGATION, currenciesOf, visibleGroups } from './dashboardLogic';
import './cashiering.css';

function groupColumns(currencies: readonly string[]): Column<DashboardItem>[] {
  return [
    { key: 'label', header: 'Item', render: (i) => i.label },
    {
      key: 'count',
      header: 'Count',
      numeric: true,
      render: (i) => i.count.toLocaleString('en-PH'),
    },
    ...currencies.map((c) => ({
      key: `amount-${c}`,
      header: `Amount (${c})`,
      numeric: true,
      render: (i: DashboardItem) => <Amount value={i.amounts[c] ?? 0} />,
    })),
  ];
}

function DashboardSearch() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [term, setTerm] = useState('');
  const search = async () => {
    const text = term.trim();
    if (text === '') {
      return;
    }
    const found = await cashieringApi.receipts({ companyId, receiptNo: text }, 0);
    const receipt = found.content[0];
    void navigate(
      receipt
        ? `/cashiering/receipts/${receipt.id}`
        : `/operations/invoices${toQuery({ q: text })}`,
    );
  };
  return (
    <form
      className="worklist-toolbar"
      onSubmit={(e) => {
        e.preventDefault();
        void search();
      }}
    >
      <input
        className="input"
        aria-label="Search account, invoice, ARN, AR or OR number"
        placeholder="Account, invoice, ARN, AR or OR number"
        value={term}
        onChange={(e) => setTerm(e.target.value)}
      />
      <Button type="submit" variant="secondary" icon={<Search size={16} />}>
        Search
      </Button>
    </form>
  );
}

/**
 * Cashiering dashboard (FRS.CSH.01.03): the count and the amount per currency of every item of
 * the user's role (records for posting per receipt type, returned records, unapplied payments for
 * disposition per type, payment files waiting for automatic processing), counted when the screen
 * opens or is refreshed; each item opens its list already filtered. The main navigation and the
 * search of an account or receipt complete the screen.
 */
export default function CashieringHomePage() {
  const companyId = useCompanyId();
  const base = useBaseCurrency();
  const navigate = useNavigate();
  const { can } = useAuth();
  const dashboard = useQuery({
    queryKey: ['cashiering', 'dashboard', companyId],
    queryFn: () => api.get<DashboardGroup[]>(`/cashiering/dashboard${toQuery({ companyId })}`),
    enabled: companyId > 0,
  });
  const groups = visibleGroups(dashboard.data ?? [], can);
  const columns = groupColumns(currenciesOf(groups, base));
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Cashiering Dashboard"
        description="Records for posting, returned records, unapplied payments and payment files of the Cashiering team."
        actions={
          <Button
            variant="secondary"
            icon={<RefreshCw size={16} />}
            busy={dashboard.isFetching}
            onClick={() => void dashboard.refetch()}
          >
            Refresh
          </Button>
        }
      />
      <DashboardSearch />
      <ErrorAlert error={dashboard.error} />
      {dashboard.isLoading && <LoadingPanel />}
      <div className="csh-dashboard-groups">
        {groups.map((g) => (
          <Card key={g.code} title={g.label}>
            <DataTable
              caption={g.label}
              columns={columns}
              rows={g.items}
              rowKey={(i) => i.code}
              emptyMessage="Nothing to show"
              onRowClick={(i) => void navigate(i.link)}
            />
          </Card>
        ))}
      </div>
      <Card title="Main Navigation">
        <ul className="csh-nav-links">
          {NAVIGATION.filter((n) => n.permissions.some((p) => can(p))).map((n) => (
            <li key={n.label}>
              <Link to={n.to}>{n.label}</Link>
            </li>
          ))}
        </ul>
      </Card>
    </div>
  );
}
