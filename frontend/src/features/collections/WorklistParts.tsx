import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Tabs } from '@/components/ui/Tabs';
import { formatAmount } from '@/utils/format';
import { collectionsApi } from './api';
import { FILTER_FIELDS } from './collectionsLogic';
import type { WorklistFilters } from './collectionsLogic';

/** The filter panel of the PR worklist (BRCLXN.011/012). */
export function FilterPanel({
  initial,
  onApply,
}: Readonly<{ initial: WorklistFilters; onApply: (f: WorklistFilters) => void }>) {
  const [values, setValues] = useState<WorklistFilters>(initial);
  return (
    <div className="worklist-filters">
      {FILTER_FIELDS.map((f) => (
        <Field key={f.key} label={f.label}>
          {(id) => (
            <input
              id={id}
              className="input"
              value={(values[f.key] as string | undefined) ?? ''}
              onChange={(e) => setValues((v) => ({ ...v, [f.key]: e.target.value }))}
            />
          )}
        </Field>
      ))}
      <label className="checkbox">
        <input
          type="checkbox"
          checked={values.unassigned === true}
          onChange={(e) => setValues((v) => ({ ...v, unassigned: e.target.checked }))}
        />
        Unassigned only
      </label>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={values.escalated === true}
          onChange={(e) => setValues((v) => ({ ...v, escalated: e.target.checked || undefined }))}
        />
        Escalated only
      </label>
      <Button variant="secondary" onClick={() => onApply(values)}>
        Apply Filters
      </Button>
      <Button
        variant="ghost"
        onClick={() => {
          setValues({});
          onApply({});
        }}
      >
        Clear
      </Button>
    </div>
  );
}

/** Totals of the filtered accounts per client or account (BRCLXN.003). */
export function TotalsCard({ companyId, query }: Readonly<{ companyId: number; query: string }>) {
  const [groupBy, setGroupBy] = useState<'CLIENT' | 'ARN'>('CLIENT');
  const totals = useQuery({
    queryKey: ['collections', 'totals', companyId, query, groupBy],
    queryFn: () => collectionsApi.totals(companyId, query, groupBy),
    enabled: companyId > 0,
  });
  return (
    <Card
      title={groupBy === 'CLIENT' ? 'Totals by Client' : 'Totals by Account (ARN)'}
      actions={
        <Tabs
          tabs={[
            { id: 'CLIENT', label: 'By Client' },
            { id: 'ARN', label: 'By Account' },
          ]}
          active={groupBy}
          onChange={setGroupBy}
        />
      }
    >
      <ErrorAlert error={totals.error} />
      <DataTable
        caption="Totals"
        columns={[
          {
            key: 'key',
            header: groupBy === 'CLIENT' ? 'Client' : 'ARN',
            render: (t) =>
              groupBy === 'CLIENT' ? (
                <Link to={`/collections/clients/${encodeURIComponent(t.key)}`}>{t.key}</Link>
              ) : (
                t.key
              ),
          },
          { key: 'name', header: 'Assured', render: (t) => t.name ?? '' },
          { key: 'items', header: 'Accounts', numeric: true, render: (t) => t.items },
          {
            key: 'net',
            header: 'Outstanding',
            numeric: true,
            render: (t) => formatAmount(t.netOutstanding),
          },
        ]}
        rows={(totals.data ?? []).slice(0, 10)}
        rowKey={(t) => t.key}
        loading={totals.isLoading}
      />
    </Card>
  );
}
