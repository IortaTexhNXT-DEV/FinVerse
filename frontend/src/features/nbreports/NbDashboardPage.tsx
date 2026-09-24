import { useQuery } from '@tanstack/react-query';
import { FileBarChart2 } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { nbReportsApi } from '@/api/nbReports';
import type { NbDashboard, UnitProduction } from '@/api/nbReports';
import { useAuth } from '@/auth/authContext';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId, useWorkspace } from '@/context/workspaceContext';
import { formatAmount, formatDate, today } from '@/utils/format';
import { AgeingChart, ProductionChart } from './DashboardCharts';
import { BarList, DrillTile } from './DashboardParts';
import {
  accountsPath,
  funnelPath,
  funnelShare,
  groupName,
  overdueHint,
  requestPath,
  requestsHint,
  total,
} from './dashboardData';
import './nbreports.css';

function Tiles({ d }: Readonly<{ d: NbDashboard }>) {
  const newRequests = d.requests.find((r) => r.group === 'REQUEST' && r.code === 'NEW');
  const overdue = total(d.overdue);
  return (
    <div className="grid-4">
      <DrillTile
        label="New Requests"
        value={newRequests?.count ?? 0}
        hint={requestsHint(d)}
        to="/quotations/requests"
      />
      <DrillTile
        label="Quotations Sent This Month"
        value={d.quotationsSent}
        hint={`${String(d.awaitingClient)} waiting for the client's answer`}
        to="/quotations?tab=sent"
      />
      <DrillTile
        label="Overdue (SLA Breaches)"
        value={overdue}
        hint={overdueHint(d.overdue)}
        to="/reports/NB-ACC-STATUS"
        alert={overdue > 0}
      />
      <DrillTile
        label="Booked This Month"
        value={d.booked.count}
        hint={`Premium PHP ${formatAmount(d.booked.premium)} · Commission PHP ${formatAmount(d.booked.commission)}`}
        to="/booking?tab=BOOKED"
      />
    </div>
  );
}

function Sections({ d }: Readonly<{ d: NbDashboard }>) {
  return (
    <div className="nb-sections">
      <Card title="Requests by Status">
        {d.requests.length === 0 ? (
          <EmptyState message="No open requests" />
        ) : (
          <BarList
            label="Requests, quotations and proposal requests by status"
            items={d.requests.map((r) => ({
              key: `${r.group}-${r.code}`,
              label: `${groupName(r.group)} · ${r.label}`,
              value: r.count,
              to: requestPath(r),
            }))}
          />
        )}
      </Card>
      <Card title="Accounts by Stage">
        {d.accounts.length === 0 ? (
          <EmptyState message="No accounts yet" />
        ) : (
          <BarList
            label="Accounts by stage"
            items={d.accounts.map((a) => ({
              key: a.code,
              label: a.label,
              value: a.count,
              to: accountsPath(a.code),
            }))}
          />
        )}
      </Card>
      <Card title="Quotation to Booking Funnel (Year to Date)">
        <p className="muted nb-note">
          Quotation steps are shown against the quotations and PRFs raised; account steps against
          the accounts created (accounts may also come from direct creation or bulk upload).
        </p>
        <BarList
          label="Quotation to booking funnel"
          items={d.funnel.map((f) => ({
            key: f.code,
            label: f.label,
            value: f.count,
            to: funnelPath(f.code),
            note: `${String(funnelShare(d.funnel, f.code))} %`,
          }))}
        />
      </Card>
      <Card title="Ageing of Open Accounts by Stage">
        {d.ageing.length === 0 ? (
          <EmptyState message="No open accounts" />
        ) : (
          <AgeingChart ageing={d.ageing} />
        )}
      </Card>
    </div>
  );
}

const PRODUCTION_COLUMNS: Column<UnitProduction>[] = [
  { key: 'team', header: 'Team', render: (u) => u.name },
  {
    key: 'bookings',
    header: 'Bookings / Target',
    numeric: true,
    render: (u) => `${String(u.bookings)} / ${String(u.targetCount)}`,
  },
  {
    key: 'premium',
    header: 'Premium (PHP)',
    numeric: true,
    render: (u) => formatAmount(u.premium),
  },
  {
    key: 'target',
    header: 'Target (PHP)',
    numeric: true,
    render: (u) => formatAmount(u.targetPremium),
  },
  {
    key: 'achievement',
    header: 'Achievement',
    numeric: true,
    render: (u) => (u.achievement === null ? '–' : `${u.achievement.toFixed(1)} %`),
  },
];

function Production({ d }: Readonly<{ d: NbDashboard }>) {
  const { can } = useAuth();
  return (
    <Card
      title="Production vs Target by Team (This Month)"
      actions={
        can('WORK_ASSIGN') && (
          <Link className="btn btn-secondary btn-sm" to="/reports/NB-PRODUCTION">
            <FileBarChart2 size={16} aria-hidden="true" /> Production Statistics
          </Link>
        )
      }
    >
      {d.production.length === 0 ? (
        <EmptyState message="No bookings or targets this month" />
      ) : (
        <div className="nb-production">
          <ProductionChart units={d.production} />
          <DataTable
            caption="Production against target per team"
            rows={d.production}
            rowKey={(u) => u.code}
            columns={PRODUCTION_COLUMNS}
          />
        </div>
      )}
    </Card>
  );
}

/**
 * New Business dashboard (BRNB.012): the landing page of the broking roles. Headline tiles, the
 * requests and accounts by status, the quotation-to-booking funnel, the ageing of open accounts
 * and production against target; every figure opens its filtered list.
 */
export default function NbDashboardPage() {
  const companyId = useCompanyId();
  const { company } = useWorkspace();
  const [asOf, setAsOf] = useState(today());
  const query = useQuery({
    queryKey: ['nb-dashboard', companyId, asOf],
    queryFn: () => nbReportsApi.dashboard(companyId, asOf),
    enabled: companyId > 0,
  });
  const d = query.data;
  return (
    <div className="stack">
      <PageHeader
        section="New Business"
        title="New Business Dashboard"
        description={`Requests, quotations, accounts and bookings of ${company?.name ?? 'the company'} as of ${formatDate(asOf)}.`}
        actions={
          <>
            <Field label="As of">
              {(id) => (
                <input
                  id={id}
                  type="date"
                  className="input"
                  value={asOf}
                  max={today()}
                  onChange={(e) => setAsOf(e.target.value || today())}
                />
              )}
            </Field>
            <Link className="btn btn-secondary" to="/nb/reports">
              <FileBarChart2 size={16} aria-hidden="true" /> New Business Reports
            </Link>
          </>
        }
      />
      <ErrorAlert error={query.error} />
      {query.isLoading && <span className="spinner" aria-label="Loading" />}
      {d !== undefined && (
        <>
          <Tiles d={d} />
          <Sections d={d} />
          <Production d={d} />
        </>
      )}
    </div>
  );
}
