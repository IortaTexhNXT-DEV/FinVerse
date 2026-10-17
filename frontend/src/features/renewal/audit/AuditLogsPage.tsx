import { useState } from 'react';
import type { AuditFilters } from '@/api/renewalDashboard';
import { Card } from '@/components/ui/Card';
import { DateInput } from '@/components/ui/DateInput';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import { AuditLogTable } from './AuditLogTable';
import '../renewal.css';

const ACTION_TYPES = ['Create', 'Update Details', 'Update Status', 'Approve', 'Send', 'Delete'];

/**
 * Audit Logs (BDOI Renewal FRS FRRN.043.01): every recorded activity on the renewal accounts of the
 * user's scope, filtered by date range, action type, reference number and user, and exported as
 * CSV or Excel. The entries cannot be changed or deleted.
 */
export default function AuditLogsPage() {
  const [filters, setFilters] = useState<AuditFilters>({});
  const set = (k: keyof AuditFilters, v: string) =>
    setFilters({ ...filters, [k]: v === '' ? undefined : v });
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Audit Logs"
        description="Every recorded activity on the renewal accounts, with the old and new values."
      />
      <div className="form-grid">
        <Field label="Date From">
          {(id) => (
            <DateInput
              id={id}
              className="input"
              value={filters.from ?? ''}
              onChange={(e) => set('from', e.target.value)}
            />
          )}
        </Field>
        <Field label="Date To">
          {(id) => (
            <DateInput
              id={id}
              className="input"
              value={filters.to ?? ''}
              onChange={(e) => set('to', e.target.value)}
            />
          )}
        </Field>
        <Field label="Action Type">
          {(id) => (
            <select
              id={id}
              className="select"
              value={filters.actionType ?? ''}
              onChange={(e) => set('actionType', e.target.value)}
            >
              <option value="">All action types</option>
              {ACTION_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Reference Number">
          {(id) => (
            <input
              id={id}
              className="input"
              value={filters.ref ?? ''}
              onChange={(e) => set('ref', e.target.value)}
            />
          )}
        </Field>
        <Field label="User">
          {(id) => (
            <input
              id={id}
              className="input"
              value={filters.user ?? ''}
              onChange={(e) => set('user', e.target.value)}
            />
          )}
        </Field>
      </div>
      <Card flush>
        <AuditLogTable filters={filters} />
      </Card>
    </div>
  );
}
