import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import type { UnappliedRow } from './api';
import { unappliedApi } from './api';
import type { PanelValues } from './labels';
import { AGE_BUCKETS, EMPTY_PANEL, TAB_LABELS, cashieringStatus } from './labels';

/** Shared pieces of the unapplied-payment screens (BRCLXN.034-036, 040). */

/** The Filters panel: market segment, collector disposition and age (BRCLXN.035). */
export function UnappliedFilterPanel({
  initial,
  onApply,
}: Readonly<{ initial: PanelValues; onApply: (v: PanelValues) => void }>) {
  const [values, setValues] = useState<PanelValues>(initial);
  const rules = useQuery({
    queryKey: ['collections', 'unapplied', 'rules'],
    queryFn: unappliedApi.rules,
  });
  return (
    <div className="worklist-filters">
      <Field label="Market Segment">
        {(id) => (
          <input
            id={id}
            className="input"
            value={values.segment}
            onChange={(e) => setValues((v) => ({ ...v, segment: e.target.value }))}
          />
        )}
      </Field>
      <Field label="Collector Disposition">
        {(id) => (
          <select
            id={id}
            className="select"
            value={values.disposition}
            onChange={(e) => setValues((v) => ({ ...v, disposition: e.target.value }))}
          >
            <option value="">Any</option>
            <option value="NONE">No disposition yet</option>
            {(rules.data?.rules ?? []).map((r) => (
              <option key={r.code} value={r.code}>
                {r.label}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Age">
        {(id) => (
          <select
            id={id}
            className="select"
            value={values.age}
            onChange={(e) => setValues((v) => ({ ...v, age: e.target.value }))}
          >
            {AGE_BUCKETS.map((b) => (
              <option key={b.id} value={b.id}>
                {b.label}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Button variant="secondary" onClick={() => onApply(values)}>
        Apply Filters
      </Button>
      <Button
        variant="ghost"
        onClick={() => {
          setValues(EMPTY_PANEL);
          onApply(EMPTY_PANEL);
        }}
      >
        Clear
      </Button>
    </div>
  );
}

/** The Cashiering tab and status of an item. */
export function CashieringCell({ row }: Readonly<{ row: UnappliedRow }>) {
  const status = cashieringStatus(row.cashieringStatus);
  return (
    <>
      <div>{TAB_LABELS[row.cashieringTab]}</div>
      {status !== undefined && <StatusBadge status={status} />}
    </>
  );
}
