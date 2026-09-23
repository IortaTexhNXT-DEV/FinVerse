import { useState } from 'react';
import type { EventType } from '@/api/accounting';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { humanize } from '@/utils/format';
import { components } from './ruleModel';
import { useAccountingLookups } from './useAccountingLookups';

/** Catalogue of accounting event types with their amount components and rule coverage. */
export default function EventTypesPage() {
  const { eventTypes, rules, loading, error } = useAccountingLookups();
  const [category, setCategory] = useState('');
  const categories = [...new Set(eventTypes.map((t) => t.category))].sort((a, b) =>
    a.localeCompare(b),
  );
  const rows = eventTypes.filter((t) => category === '' || t.category === category);
  const ruleCount = (code: string) => rules.filter((r) => r.eventType === code).length;

  return (
    <div className="stack">
      <PageHeader
        section="Accounting Engine"
        title="Event Types"
        description="Business events published by the operational modules. Each needs at least one authorized rule for the company before it can post."
      />
      <Card>
        <div className="form-grid">
          <Field label="Category">
            {(id) => (
              <select
                id={id}
                className="select"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              >
                <option value="">All categories</option>
                {categories.map((c) => (
                  <option key={c} value={c}>
                    {humanize(c)}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={error} />
      <Card flush>
        <DataTable<EventType>
          loading={loading}
          rows={rows}
          rowKey={(t) => t.code}
          caption="Event types"
          columns={[
            { key: 'c', header: 'Code', render: (t) => <strong>{t.code}</strong> },
            { key: 'n', header: 'Name', render: (t) => t.name },
            { key: 'cat', header: 'Category', render: (t) => humanize(t.category) },
            { key: 'j', header: 'Journal type', render: (t) => humanize(t.journalType) },
            { key: 'comp', header: 'Amount components', render: (t) => components(t).join(', ') },
            {
              key: 'r',
              header: 'Rules',
              numeric: true,
              render: (t) => ruleCount(t.code) || <span className="muted">none</span>,
            },
            { key: 'd', header: 'Description', render: (t) => t.description ?? '' },
          ]}
        />
      </Card>
    </div>
  );
}
