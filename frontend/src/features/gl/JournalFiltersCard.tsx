import type { ReactNode } from 'react';
import type { JournalFilters } from '@/api/gl';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';

/** Filters of the journal list, including the entries assigned to the user (FRBS 2.5.1). */
export type ListFilters = Omit<JournalFilters, 'companyId'> & { assignedTo?: string };

const STATUSES = ['', 'DRAFT', 'PENDING_APPROVAL', 'POSTED', 'REJECTED', 'CANCELLED', 'REVERSED'];

interface Props {
  filters: ListFilters;
  username: string;
  onChange: (patch: Partial<ListFilters>) => void;
  actions?: ReactNode;
}

/** Filter bar of the journal list with the bulk actions on the right. */
export function JournalFiltersCard({ filters, username, onChange, actions }: Readonly<Props>) {
  return (
    <Card actions={actions}>
      <div className="form-grid">
        <Field label="Status">
          {(id) => (
            <select
              id={id}
              className="select"
              value={filters.status ?? ''}
              onChange={(e) => onChange({ status: e.target.value || undefined })}
            >
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s === '' ? 'All' : s.replace('_', ' ')}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Batch no.">
          {(id) => (
            <input
              id={id}
              className="input"
              value={filters.batchNo ?? ''}
              onChange={(e) => onChange({ batchNo: e.target.value })}
            />
          )}
        </Field>
        <Field label="From">
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={filters.fromDate ?? ''}
              onChange={(e) => onChange({ fromDate: e.target.value })}
            />
          )}
        </Field>
        <Field label="To">
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={filters.toDate ?? ''}
              onChange={(e) => onChange({ toDate: e.target.value })}
            />
          )}
        </Field>
        <Field label="Inputter">
          {(id) => (
            <input
              id={id}
              className="input"
              value={filters.inputter ?? ''}
              onChange={(e) => onChange({ inputter: e.target.value })}
            />
          )}
        </Field>
        <Field label="Assignment">
          {(id) => (
            <select
              id={id}
              className="select"
              value={filters.assignedTo === undefined ? '' : 'MINE'}
              onChange={(e) =>
                onChange({ assignedTo: e.target.value === 'MINE' ? username : undefined })
              }
            >
              <option value="">All entries</option>
              <option value="MINE">Assigned to me</option>
            </select>
          )}
        </Field>
      </div>
    </Card>
  );
}
