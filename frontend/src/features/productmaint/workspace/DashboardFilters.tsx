import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '@/api/catalog';
import { pmWorkspaceApi } from '@/api/pmWorkspace';
import { Combobox } from '@/components/ui/Combobox';
import { DateInput } from '@/components/ui/DateInput';
import { Field } from '@/components/ui/Field';
import { useDisplayName } from '@/components/ui/useDisplayName';
import { PACKAGE_TYPES } from './pmDashboard';

/** The filters of the dashboard as entered (dates ISO, codes). */
export interface DashboardFilterValues {
  from: string;
  to: string;
  tsuOfficer: string;
  lineCode: string;
  packageType: string;
}

/**
 * The filters of BDOI's FRS FRPM.001.01: Period From Date, Period To Date, TSU Officer, Product
 * Line and Package Type; the dashboard refreshes as soon as one changes.
 */
export function DashboardFilters({
  value,
  onChange,
}: Readonly<{ value: DashboardFilterValues; onChange: (next: DashboardFilterValues) => void }>) {
  const name = useDisplayName();
  const officers = useQuery({
    queryKey: ['pm-dashboard', 'officers'],
    queryFn: () => pmWorkspaceApi.officers(),
    staleTime: 10 * 60_000,
  });
  const lines = useQuery({
    queryKey: ['catalog', 'lines'],
    queryFn: () => catalogApi.lines(),
    staleTime: 10 * 60_000,
  });
  const set = (patch: Partial<DashboardFilterValues>) => onChange({ ...value, ...patch });
  return (
    <div className="form-grid">
      <Field label="Period From">
        {(id) => (
          <DateInput id={id} value={value.from} onChange={(e) => set({ from: e.target.value })} />
        )}
      </Field>
      <Field label="Period To">
        {(id) => (
          <DateInput id={id} value={value.to} onChange={(e) => set({ to: e.target.value })} />
        )}
      </Field>
      <Field label="TSU Officer">
        {(id) => (
          <Combobox
            id={id}
            emptyLabel="All"
            value={value.tsuOfficer}
            loading={officers.isLoading}
            options={(officers.data ?? []).map((u) => ({ value: u, label: name(u) }))}
            onChange={(tsuOfficer) => set({ tsuOfficer })}
          />
        )}
      </Field>
      <Field label="Product Line">
        {(id) => (
          <Combobox
            id={id}
            emptyLabel="All"
            value={value.lineCode}
            loading={lines.isLoading}
            options={(lines.data ?? []).map((l) => ({ value: l.code, label: l.name }))}
            onChange={(lineCode) => set({ lineCode })}
          />
        )}
      </Field>
      <Field label="Package Type">
        {(id) => (
          <Combobox
            id={id}
            emptyLabel="All"
            value={value.packageType}
            options={PACKAGE_TYPES}
            onChange={(packageType) => set({ packageType })}
          />
        )}
      </Field>
    </div>
  );
}
