import { useQuery } from '@tanstack/react-query';
import { UserCog } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { nbadminApi } from '@/api/nbadmin';
import type {
  AccessRequest,
  AccessRequestFilters,
  AccessRequestStatus,
  AccessRequestType,
} from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { AccessRequestDetailDialog } from './AccessRequestDetailDialog';
import { AccessRequestFormDialog } from './AccessRequestFormDialog';
import { REQUEST_TYPE_LABELS } from './accessRequest';

const STATUSES: AccessRequestStatus[] = ['PENDING', 'APPROVED', 'REJECTED'];
const TYPES = Object.keys(REQUEST_TYPE_LABELS) as AccessRequestType[];

function Filters({
  filters,
  onChange,
}: Readonly<{ filters: AccessRequestFilters; onChange: (f: AccessRequestFilters) => void }>) {
  const set = (patch: Partial<AccessRequestFilters>) => onChange({ ...filters, ...patch, page: 0 });
  return (
    <Card>
      <div className="form-grid">
        <Field label="Status">
          {(id) => (
            <select
              id={id}
              className="select"
              value={filters.status ?? ''}
              onChange={(e) =>
                set({ status: (e.target.value || undefined) as AccessRequestStatus })
              }
            >
              <option value="">All</option>
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {humanize(s)}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Request">
          {(id) => (
            <select
              id={id}
              className="select"
              value={filters.type ?? ''}
              onChange={(e) => set({ type: (e.target.value || undefined) as AccessRequestType })}
            >
              <option value="">All</option>
              {TYPES.map((t) => (
                <option key={t} value={t}>
                  {REQUEST_TYPE_LABELS[t]}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="User name or request number">
          {(id) => (
            <input
              id={id}
              className="input"
              value={filters.text ?? ''}
              onChange={(e) => set({ text: e.target.value || undefined })}
            />
          )}
        </Field>
      </div>
    </Card>
  );
}

/**
 * User access requests (BRNB.085, BRD 3.3.5 / 3.4.2): the Business Administrator requests user
 * creation, role changes or disabling; the Approver approves (the change is applied) or rejects.
 */
export default function AccessRequestsPage() {
  const { can } = useAuth();
  const toast = useToast();
  const [params, setParams] = useSearchParams();
  const [filters, setFilters] = useState<AccessRequestFilters>({ status: 'PENDING' });
  const [creating, setCreating] = useState(false);
  const openId = params.get('id');
  const requests = useQuery({
    queryKey: ['nbadmin', 'requests', filters],
    queryFn: () => nbadminApi.requests(filters),
  });
  const open = (id: number | null) =>
    setParams(id === null ? {} : { id: String(id) }, { replace: true });
  return (
    <div className="stack">
      <PageHeader
        section="Broking Setup"
        title="Access Requests"
        description="Requests to create users, change their roles or disable them. Nothing changes until an approver other than the requester approves."
        actions={
          can('ACCESS_REQUEST') && (
            <Button variant="accent" icon={<UserCog size={16} />} onClick={() => setCreating(true)}>
              New request
            </Button>
          )
        }
      />
      <Filters filters={filters} onChange={setFilters} />
      <ErrorAlert error={requests.error} />
      <Card flush>
        <DataTable<AccessRequest>
          loading={requests.isLoading}
          rows={requests.data?.content ?? []}
          rowKey={(r) => r.id}
          onRowClick={(r) => open(r.id)}
          emptyMessage="No access request matches the filters."
          columns={[
            {
              key: 'no',
              header: 'Request',
              render: (r) => <span className="mono">{r.requestNo}</span>,
            },
            { key: 'type', header: 'Type', render: (r) => REQUEST_TYPE_LABELS[r.type] },
            { key: 'summary', header: 'Change', render: (r) => r.summary },
            { key: 'by', header: 'Requested by', render: (r) => r.requestedBy },
            { key: 'at', header: 'Requested', render: (r) => formatDateTime(r.requestedAt) },
            { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
            { key: 'decided', header: 'Decided by', render: (r) => r.decidedBy ?? '' },
          ]}
        />
        <PageFooter
          data={requests.data}
          noun="requests"
          onPage={(page) => setFilters({ ...filters, page })}
        />
      </Card>
      {creating && (
        <AccessRequestFormDialog
          onClose={() => setCreating(false)}
          onSubmitted={(r) => {
            setCreating(false);
            toast.success(`${r.requestNo} submitted for approval`);
          }}
        />
      )}
      {openId !== null && (
        <AccessRequestDetailDialog requestId={Number(openId)} onClose={() => open(null)} />
      )}
    </div>
  );
}
