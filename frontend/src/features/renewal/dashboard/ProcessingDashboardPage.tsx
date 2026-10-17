import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Printer } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import { exportLink, renewalDashboardApi } from '@/api/renewalDashboard';
import type { DashboardFilters, ProcessingRow } from '@/api/renewalDashboard';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import { DashboardFilterBar, KpiCards } from './dashboardBits';
import { defaultPeriod } from './dashboardFormat';
import { processingColumns } from './processingColumns';
import '../renewal.css';

type View = 'ALL' | 'NEW_BUSINESS' | 'RENEWAL';
type Tab = 'ASSIGNED' | 'UNASSIGNED';

const VIEWS: { id: View; label: string }[] = [
  { id: 'ALL', label: 'Combined View' },
  { id: 'NEW_BUSINESS', label: 'New Business View' },
  { id: 'RENEWAL', label: 'Renewal View' },
];

function AssignBar({ arns, onDone }: Readonly<{ arns: string[]; onDone: () => void }>) {
  const companyId = useCompanyId();
  const [who, setWho] = useState('');
  const officers = useQuery({
    queryKey: ['renewal', 'processing-officers'],
    queryFn: () => renewalApi.processingOfficers(),
  });
  const assign = useMutation({
    mutationFn: () => renewalDashboardApi.assignProcessor(companyId, arns, who),
    onSuccess: onDone,
  });
  return (
    <div className="rnw-drill-tools">
      <Field label="Placement Processor">
        {(id) => (
          <select id={id} className="select" value={who} onChange={(e) => setWho(e.target.value)}>
            <option value="">Choose the processor</option>
            {(officers.data ?? []).map((o) => (
              <option key={o.username} value={o.username}>
                {o.fullName}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Button
        disabled={who === '' || arns.length === 0 || assign.isPending}
        onClick={() => assign.mutate()}
      >
        Assign {arns.length.toLocaleString()} selected
      </Button>
      <ErrorAlert error={assign.error} />
    </div>
  );
}

function matches(r: ProcessingRow, words: string[]): boolean {
  const all = Object.values(r)
    .map((v) => (typeof v === 'string' || typeof v === 'number' ? String(v) : ''))
    .join(' ')
    .toLowerCase();
  return words.every((w) => all.includes(w));
}

/** The accounts of a card, with the For Placement tabs, the assignment, search and export. */
function CardAccounts({
  card,
  label,
  filters,
}: Readonly<{ card: string; label: string; filters: DashboardFilters & { businessType: View } }>) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { can } = useAuth();
  const [tab, setTab] = useState<Tab>('UNASSIGNED');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<string[]>([]);
  const placement = card === 'FOR_PLACEMENT';
  const shownTab = placement ? tab : undefined;
  const rows = useQuery({
    queryKey: ['renewal', 'processing-drill', companyId, filters, card, shownTab],
    queryFn: () => renewalDashboardApi.processingDrill(companyId, filters, card, shownTab),
    enabled: companyId > 0,
  });
  const shown = useMemo(() => {
    const words = search
      .toLowerCase()
      .split(/\s+/)
      .filter((w) => w !== '');
    return (rows.data ?? []).filter((r) => matches(r, words));
  }, [rows.data, search]);
  const select: Column<ProcessingRow> = {
    key: 'sel',
    header: '',
    kind: 'actions',
    render: (r) => (
      <input
        type="checkbox"
        aria-label={`Select ${r.arn}`}
        checked={selected.includes(r.arn)}
        onChange={(e) =>
          setSelected((s) => (e.target.checked ? [...s, r.arn] : s.filter((x) => x !== r.arn)))
        }
      />
    ),
  };
  const mayAssign = placement && can('RNW_PROCESS_ASSIGN');
  return (
    <Card
      title={label}
      flush
      actions={
        <Button
          variant="secondary"
          icon={<Printer size={16} />}
          onClick={() =>
            void navigate(
              exportLink('RNW-PROCESSING-DASHBOARD', { card, tab: shownTab, ...filters }),
            )
          }
        >
          Export and Print
        </Button>
      }
    >
      {placement && (
        <Tabs
          tabs={[
            { id: 'UNASSIGNED' as const, label: 'Unassigned' },
            { id: 'ASSIGNED' as const, label: 'Assigned' },
          ]}
          active={tab}
          onChange={setTab}
        />
      )}
      {mayAssign && (
        <AssignBar
          arns={selected}
          onDone={() => {
            setSelected([]);
            void queryClient.invalidateQueries({ queryKey: ['renewal', 'processing-drill'] });
            void queryClient.invalidateQueries({ queryKey: ['renewal', 'processing-dashboard'] });
          }}
        />
      )}
      <div className="rnw-drill-tools">
        <input
          className="input"
          aria-label="Search the accounts"
          placeholder="Search all columns"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <span className="muted">{shown.length.toLocaleString()} accounts</span>
      </div>
      <ErrorAlert error={rows.error} />
      <DataTable<ProcessingRow>
        loading={rows.isLoading}
        rows={shown}
        rowKey={(r) => r.arn}
        emptyMessage="No records found."
        columns={mayAssign ? [select, ...processingColumns(card)] : processingColumns(card)}
      />
    </Card>
  );
}

/**
 * Processing Dashboard (BDOI Renewal FRS FRRN.003): one dashboard of New Business and Renewal
 * processing, in the Combined, New Business or Renewal view, with the KPI cards and the drill-down
 * of each card (search, Export and Print, open the account).
 */
export default function ProcessingDashboardPage() {
  const companyId = useCompanyId();
  const [view, setView] = useState<View>('ALL');
  const [filters, setFilters] = useState<DashboardFilters>(defaultPeriod());
  const [card, setCard] = useState<string>();
  const f = { ...filters, businessType: view };
  const dash = useQuery({
    queryKey: ['renewal', 'processing-dashboard', companyId, f],
    queryFn: () => renewalDashboardApi.processing(companyId, f),
    enabled: companyId > 0,
  });
  const cards = dash.data?.cards ?? [];
  const label = cards.find((c) => c.key === card)?.label ?? '';
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Processing Dashboard"
        description="Placement, policy, booking and release of New Business and renewal accounts."
      />
      <Tabs tabs={VIEWS} active={view} onChange={setView} />
      <DashboardFilterBar value={filters} onChange={setFilters} />
      <ErrorAlert error={dash.error} onRetry={() => void dash.refetch()} />
      <KpiCards
        label="Processing KPI cards"
        active={card}
        cards={cards.map((c) => ({
          key: c.key,
          label: c.label,
          count: c.count,
          hint: `New Business ${c.newBusiness.toLocaleString()} · Renewal ${c.renewal.toLocaleString()}`,
        }))}
        onOpen={setCard}
      />
      {card !== undefined && <CardAccounts key={card} card={card} label={label} filters={f} />}
    </div>
  );
}
