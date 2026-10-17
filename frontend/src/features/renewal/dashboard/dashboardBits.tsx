import { useQuery } from '@tanstack/react-query';
import { Printer } from 'lucide-react';
import { useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import { exportLink } from '@/api/renewalDashboard';
import type { DashboardFilters, Drill, DrillRow } from '@/api/renewalDashboard';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import type { SortState } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useCompanyId } from '@/context/workspaceContext';
import { searchRows, sortRows } from './dashboardFormat';
import { drillColumns } from './drillColumns';

/** A count that opens its accounts. */
export function DrillCount({
  value,
  onOpen,
  label,
}: Readonly<{ value: ReactNode; onOpen: () => void; label: string }>) {
  return (
    <button type="button" className="link-button" onClick={onOpen} aria-label={label}>
      {value}
    </button>
  );
}

/** KPI cards that open the accounts they count. */
export function KpiCards({
  label,
  cards,
  active,
  onOpen,
}: Readonly<{
  label: string;
  cards: { key: string; label: string; count: number; hint?: string }[];
  active?: string;
  onOpen: (key: string) => void;
}>) {
  return (
    <ul className="rnw-kpi-cards" aria-label={label}>
      {cards.map((c) => (
        <li key={c.key}>
          <button
            type="button"
            className={`card kpi rnw-kpi-card ${c.key === active ? 'active' : ''}`}
            onClick={() => onOpen(c.key)}
          >
            <span className="kpi-label">{c.label}</span>
            <span className="kpi-value">{c.count.toLocaleString()}</span>
            {c.hint !== undefined && <span className="kpi-hint">{c.hint}</span>}
          </button>
        </li>
      ))}
    </ul>
  );
}

/** The dashboard filters: period, market segment and Account Officer. */
export function DashboardFilterBar({
  value,
  onChange,
}: Readonly<{ value: DashboardFilters; onChange: (f: DashboardFilters) => void }>) {
  const companyId = useCompanyId();
  const officers = useQuery({
    queryKey: ['renewal', 'officers', companyId],
    queryFn: () => renewalApi.officers(companyId),
    enabled: companyId > 0,
    retry: false,
  });
  return (
    <div className="form-grid">
      <Field label="Period From">
        {(id) => (
          <DateInput
            id={id}
            className="input"
            value={value.from ?? ''}
            onChange={(e) => onChange({ ...value, from: e.target.value })}
          />
        )}
      </Field>
      <Field label="Period To">
        {(id) => (
          <DateInput
            id={id}
            className="input"
            value={value.to ?? ''}
            onChange={(e) => onChange({ ...value, to: e.target.value })}
          />
        )}
      </Field>
      <Field label="Market Segment">
        {(id) => (
          <LovSelect
            id={id}
            type="MARKET_SEGMENT"
            value={value.segment ?? ''}
            placeholder="All segments"
            onChange={(code) => onChange({ ...value, segment: code })}
          />
        )}
      </Field>
      <Field label="Account Officer">
        {(id) => (
          <select
            id={id}
            className="select"
            value={value.officer ?? ''}
            onChange={(e) => onChange({ ...value, officer: e.target.value })}
          >
            <option value="">All officers</option>
            {(officers.data ?? []).map((o) => (
              <option key={o.username} value={o.username}>
                {o.fullName}
              </option>
            ))}
          </select>
        )}
      </Field>
    </div>
  );
}

/** The accounts of a figure, with search, sort, and Export and Print through the Report Centre. */
export function DrillDialog({
  title,
  drill,
  loading,
  error,
  exportParams,
  onClose,
}: Readonly<{
  title: string;
  drill: Drill | undefined;
  loading: boolean;
  error: unknown;
  exportParams: Record<string, string | number | undefined>;
  onClose: () => void;
}>) {
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [sort, setSort] = useState<SortState>();
  const rows = useMemo(
    () => sortRows(searchRows(drill?.rows ?? [], search), sort),
    [drill, search, sort],
  );
  return (
    <Modal
      title={title}
      open
      size="lg"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          <Button
            icon={<Printer size={16} />}
            onClick={() => void navigate(exportLink('RNW-DASHBOARD', exportParams))}
          >
            Export and Print
          </Button>
        </>
      }
    >
      <ErrorAlert error={error} />
      <div className="rnw-drill-tools">
        <input
          className="input"
          aria-label="Search the accounts"
          placeholder="Search all columns"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <span className="muted">{rows.length.toLocaleString()} accounts</span>
      </div>
      <DataTable<DrillRow>
        loading={loading}
        rows={rows}
        rowKey={(r) => `${r.ref}-${JSON.stringify(r.extra)}`}
        sort={sort}
        onSort={setSort}
        emptyMessage="No records found."
        columns={drillColumns(drill)}
      />
    </Modal>
  );
}
