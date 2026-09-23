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
import { reservesApi } from '@/api/reserves';
import type { SummaryRow } from '@/api/reserves';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { formatAmount, formatCompact, today } from '@/utils/format';
import { RESERVE_ORDER, reserveByLine, reserveLabel, totalsByReserve } from './reserveMath';

const CHART_HEIGHT = 300;
const RESERVE_OPTIONS = RESERVE_ORDER.map((r) => ({ value: r, label: reserveLabel(r) }));

function netOf(totals: ReturnType<typeof totalsByReserve>, ...codes: string[]): number {
  return totals.filter((t) => codes.includes(t.reserve)).reduce((s, t) => s + t.net, 0);
}

/** Technical reserves dashboard: gross vs net per reserve and line of business, vs last month. */
export default function ReserveDashboardPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const [asOf, setAsOf] = useState(today());
  const [reserve, setReserve] = useState<string>('UPR');
  const summary = useQuery({
    queryKey: ['reserve-summary', companyId, asOf],
    queryFn: () => reservesApi.summary(companyId, asOf),
    enabled: companyId > 0 && asOf !== '',
  });
  const rows = summary.data?.rows ?? [];
  const totals = totalsByReserve(rows);
  const chart = totals.map((t) => ({ name: t.reserve, gross: t.gross, net: t.net }));
  const technical = netOf(totals, 'UPR', 'OSLR', 'IBNR', 'ULAE', 'MFAD', 'PDR');

  return (
    <div className="stack">
      <PageHeader
        section="Actuarial Reserves"
        title="Reserve Summary"
        description={
          summary.data?.currentDate
            ? `Valuation ${summary.data.currentDate}, compared with ${summary.data.previousDate ?? 'no previous posted valuation'}`
            : 'No valuation run on or before the selected date'
        }
        actions={
          <Button
            variant="secondary"
            icon={<FileBarChart2 size={16} />}
            onClick={() => void navigate('/reports/RSV-SUMMARY')}
          >
            Report & export
          </Button>
        }
      />
      <Card>
        <div className="form-grid">
          <TextInput
            label="Valuation month (any date)"
            type="date"
            value={asOf}
            onChange={setAsOf}
          />
          <SelectInput
            label="Reserve by line of business"
            value={reserve}
            options={RESERVE_OPTIONS}
            onChange={setReserve}
          />
          {summary.data?.currentStatus && (
            <div style={{ alignSelf: 'end' }}>
              <StatusBadge status={summary.data.currentStatus} />
            </div>
          )}
        </div>
      </Card>
      <ErrorAlert error={summary.error} />
      <div className="grid-4">
        <Kpi label="Net technical reserves" value={formatCompact(technical)} accent />
        <Kpi label="Net UPR" value={formatCompact(netOf(totals, 'UPR'))} />
        <Kpi label="Net DAC (DAC − UCR)" value={formatCompact(netOf(totals, 'DAC', 'UCR'))} />
        <Kpi label="Net claims reserves" value={formatCompact(netOf(totals, 'OSLR', 'IBNR'))} />
      </div>
      <div className="grid-2">
        <Card title="Gross vs net by reserve">
          <div style={{ height: CHART_HEIGHT }}>
            <ResponsiveContainer>
              <BarChart data={chart}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                <XAxis dataKey="name" />
                <YAxis tickFormatter={(v: number) => formatCompact(v)} />
                <Tooltip formatter={(v) => formatAmount(Number(v))} />
                <Legend />
                <Bar dataKey="gross" name="Gross" fill="var(--brand-blue)" radius={[4, 4, 0, 0]} />
                <Bar dataKey="net" name="Net" fill="var(--brand-gold)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
        <Card title={`${reserveLabel(reserve)} by line of business`}>
          <div style={{ height: CHART_HEIGHT }}>
            <ResponsiveContainer>
              <BarChart data={reserveByLine(rows, reserve)}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--color-border)" />
                <XAxis dataKey="line" />
                <YAxis tickFormatter={(v: number) => formatCompact(v)} />
                <Tooltip formatter={(v) => formatAmount(Number(v))} />
                <Legend />
                <Bar dataKey="gross" name="Gross" fill="var(--brand-blue)" radius={[4, 4, 0, 0]} />
                <Bar dataKey="net" name="Net" fill="var(--brand-gold)" radius={[4, 4, 0, 0]} />
                <Bar
                  dataKey="previousNet"
                  name="Previous net"
                  fill="var(--brand-navy)"
                  radius={[4, 4, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>
      <Card title="Technical reserves by line of business" flush>
        <DataTable<SummaryRow>
          loading={summary.isLoading}
          rows={rows}
          rowKey={(r) => `${r.reserve}|${r.businessLine}`}
          emptyMessage="No reserves: prepare a valuation run first."
          columns={[
            { key: 'r', header: 'Reserve', render: (r) => reserveLabel(r.reserve) },
            { key: 'l', header: 'Line of business', render: (r) => r.businessLine },
            { key: 'g', header: 'Gross', numeric: true, render: (r) => <Amount value={r.gross} /> },
            { key: 'i', header: 'RI share', numeric: true, render: (r) => <Amount value={r.ri} /> },
            { key: 'n', header: 'Net', numeric: true, render: (r) => <Amount value={r.net} /> },
            {
              key: 'p',
              header: 'Previous net',
              numeric: true,
              render: (r) => <Amount value={r.previousNet} />,
            },
            {
              key: 'c',
              header: 'Change',
              numeric: true,
              render: (r) => <Amount value={r.net - r.previousNet} />,
            },
          ]}
        />
      </Card>
    </div>
  );
}
