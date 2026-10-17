import { useMutation, useQuery } from '@tanstack/react-query';
import { FileSpreadsheet } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { changeHistoryApi } from '@/api/changeHistory';
import type { ChangeLogFilters, FieldChange } from '@/api/changeHistory';
import { checkerState, fieldLabel } from '@/components/history/changeLabels';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { formatDateTime } from '@/utils/format';

/**
 * Master Data Change Log (G05): the field changes of every master record of the programme, with
 * the values before and after, the maker and the checker, filtered by record type, date range,
 * maker and checker, within the reader's data access; exported to Excel with the same filters.
 */
export default function MasterChangeLogPage() {
  const [filters, setFilters] = useState<ChangeLogFilters>({});
  const set = (patch: ChangeLogFilters) => setFilters((f) => ({ ...f, ...patch }));
  const types = useQuery({ queryKey: ['change-log', 'types'], queryFn: changeHistoryApi.types });
  const log = useQuery({
    queryKey: ['change-log', filters],
    queryFn: () => changeHistoryApi.log(filters),
  });
  const exportLog = useMutation({
    mutationFn: () => changeHistoryApi.exportLog(filters),
    onSuccess: (f) => saveFile(f.blob, f.fileName),
  });
  const text = (label: string, key: 'maker' | 'checker') => (
    <Field label={label}>
      {(id) => (
        <input
          id={id}
          className="input"
          placeholder="User ID"
          maxLength={50}
          value={filters[key] ?? ''}
          onChange={(e) => set({ [key]: e.target.value || undefined })}
        />
      )}
    </Field>
  );
  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Master Data Change Log"
        description="Field changes of the master records with maker and checker."
        actions={
          <Button
            variant="secondary"
            icon={<FileSpreadsheet size={16} aria-hidden="true" />}
            busy={exportLog.isPending}
            onClick={() => exportLog.mutate()}
          >
            Export to Excel
          </Button>
        }
      />
      <Card>
        <div className="filter-bar">
          <Field label="Record Type">
            {(id) => (
              <select
                id={id}
                className="select"
                value={filters.entityType ?? ''}
                onChange={(e) => set({ entityType: e.target.value || undefined })}
              >
                <option value="">All</option>
                {(types.data ?? []).map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="From">
            {(id) => (
              <DateInput
                id={id}
                value={filters.from ?? ''}
                onChange={(e) => set({ from: e.target.value || undefined })}
              />
            )}
          </Field>
          <Field label="To">
            {(id) => (
              <DateInput
                id={id}
                value={filters.to ?? ''}
                onChange={(e) => set({ to: e.target.value || undefined })}
              />
            )}
          </Field>
          {text('Maker', 'maker')}
          {text('Checker', 'checker')}
        </div>
      </Card>
      <ErrorAlert error={log.error ?? exportLog.error} onRetry={() => void log.refetch()} />
      <Card flush>
        <DataTable<FieldChange>
          loading={log.isLoading}
          rows={log.data ?? []}
          rowKey={(c) => c.id}
          emptyMessage="No change matches the filters."
          caption="Master data changes"
          columns={[
            { key: 'type', header: 'Record Type', render: (c) => c.entityType },
            { key: 'id', header: 'Record ID', numeric: true, render: (c) => c.entityId },
            { key: 'field', header: 'Field', render: (c) => fieldLabel(c) },
            { key: 'old', header: 'Old Value', render: (c) => c.oldValue, truncate: true },
            { key: 'new', header: 'New Value', render: (c) => c.newValue, truncate: true },
            { key: 'maker', header: 'Maker', render: (c) => c.maker },
            { key: 'made', header: 'Made At', render: (c) => formatDateTime(c.madeAt) },
            { key: 'checker', header: 'Checker', render: (c) => checkerState(c, true) },
            { key: 'checked', header: 'Checked At', render: (c) => formatDateTime(c.checkedAt) },
          ]}
        />
      </Card>
    </div>
  );
}
