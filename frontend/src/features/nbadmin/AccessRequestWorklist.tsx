import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Check, Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { nbadminApi } from '@/api/nbadmin';
import type {
  AccessRequest,
  AccessRequestFilters,
  AccessRequestStatus,
  AccessRequestType,
  AccessScope,
} from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { GROUP_TYPES, REQUEST_TYPE_LABELS, USER_TYPES } from './accessRequest';

const STATUSES: AccessRequestStatus[] = [
  'DRAFT',
  'PENDING',
  'PENDING_SECOND',
  'RETURNED',
  'SCHEDULED',
  'APPROVED',
  'FOR_IMPLEMENTATION',
  'IMPLEMENTED',
  'REJECTED',
  'CANCELLED',
];

interface WorklistProps {
  title: string;
  description: string;
  /** True for group-profile requests only (Group Profile Requests screen). */
  groupProfiles?: boolean;
  newLabel: string;
  newPath: string;
  /** Permissions that may raise a new request from this list. */
  newPermissions: string[];
}

function tabsFor(can: (p: string) => boolean): { id: AccessScope; label: string }[] {
  const tabs: { id: AccessScope; label: string }[] = [{ id: 'MINE', label: 'My Requests' }];
  if (can('ACCESS_APPROVE')) {
    tabs.push({ id: 'ASSIGNED', label: 'Assigned to Me' });
  }
  if (can('UAM_SECOND_APPROVE')) {
    tabs.push({ id: 'SECOND', label: 'Second Approval' });
  }
  if (can('ROLE_MANAGE')) {
    tabs.push({ id: 'IMPLEMENTATION', label: 'For Implementation' });
  }
  tabs.push({ id: 'ALL', label: 'All' });
  return tabs;
}

function firstTab(can: (p: string) => boolean): AccessScope {
  if (can('ACCESS_APPROVE')) {
    return 'ASSIGNED';
  }
  return can('UAM_SECOND_APPROVE') ? 'SECOND' : 'MINE';
}

function Filters({
  filters,
  types,
  onChange,
}: Readonly<{
  filters: AccessRequestFilters;
  types: AccessRequestType[];
  onChange: (patch: Partial<AccessRequestFilters>) => void;
}>) {
  const input = (label: string, key: 'requester' | 'approver' | 'from' | 'to', date = false) => (
    <Field label={label}>
      {(id) => (
        <input
          id={id}
          className="input"
          type={date ? 'date' : 'text'}
          value={filters[key] ?? ''}
          onChange={(e) => onChange({ [key]: e.target.value || undefined })}
        />
      )}
    </Field>
  );
  return (
    <div className="form-grid worklist-filters">
      <Field label="Status">
        {(id) => (
          <select
            id={id}
            className="select"
            value={filters.status ?? ''}
            onChange={(e) =>
              onChange({ status: (e.target.value || undefined) as AccessRequestStatus })
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
      <Field label="Request Type">
        {(id) => (
          <select
            id={id}
            className="select"
            value={filters.type ?? ''}
            onChange={(e) => onChange({ type: (e.target.value || undefined) as AccessRequestType })}
          >
            <option value="">All</option>
            {types.map((t) => (
              <option key={t} value={t}>
                {REQUEST_TYPE_LABELS[t]}
              </option>
            ))}
          </select>
        )}
      </Field>
      {input('Requested By', 'requester')}
      {input('Approver', 'approver')}
      {input('Requested From', 'from', true)}
      {input('Requested To', 'to', true)}
    </div>
  );
}

const COLUMNS: Column<AccessRequest>[] = [
  { key: 'no', header: 'Request', render: (r) => <strong className="mono">{r.requestNo}</strong> },
  { key: 'type', header: 'Type', render: (r) => REQUEST_TYPE_LABELS[r.type] },
  { key: 'summary', header: 'Change', render: (r) => r.summary },
  { key: 'by', header: 'Requested By', render: (r) => r.requestedBy },
  { key: 'at', header: 'Requested', render: (r) => formatDateTime(r.requestedAt) },
  {
    key: 'approver',
    header: 'Approver',
    render: (r) => r.lifecycle.assignedApprover ?? r.decidedBy ?? '',
  },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
];

/**
 * Work list of access requests (BRD 1.007.1.1, 1.008, 2.002; FR-UA-017, FR-UA-018, FR-UA-030):
 * tabs My Requests, Assigned to Me, Second Approval, For Implementation and All, search, filters
 * and the approval of the selected requests.
 */
export function AccessRequestWorklist({
  title,
  description,
  groupProfiles,
  newLabel,
  newPath,
  newPermissions,
}: Readonly<WorklistProps>) {
  const { can } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const tabs = tabsFor(can);
  const [filters, setFilters] = useState<AccessRequestFilters>({
    scope: firstTab(can),
    groupProfiles,
  });
  const [showFilters, setShowFilters] = useState(false);
  const requests = useQuery({
    queryKey: ['nbadmin', 'requests', filters],
    queryFn: () => nbadminApi.requests(filters),
  });
  const set = (patch: Partial<AccessRequestFilters>) => {
    selection.clear();
    setFilters({ ...filters, ...patch, page: 0 });
  };
  const decidable = filters.scope === 'ASSIGNED' || filters.scope === 'SECOND';
  const approveSelected = useMutation({
    mutationFn: async (ids: number[]) => {
      for (const id of ids) {
        await (filters.scope === 'SECOND' ? nbadminApi.secondApprove(id) : nbadminApi.approve(id));
      }
      return ids.length;
    },
    onSuccess: async (count) => {
      selection.clear();
      toast.success(`${String(count)} request(s) approved`);
      await queryClient.invalidateQueries({ queryKey: ['nbadmin'] });
    },
  });
  const rows = requests.data?.content ?? [];
  const columns = decidable
    ? [
        selectionColumn(
          rows,
          (r) => String(r.id),
          selection,
          (r) => r.requestNo,
        ),
        ...COLUMNS,
      ]
    : COLUMNS;
  let types = USER_TYPES;
  if (groupProfiles === true) {
    types = GROUP_TYPES;
  } else if (groupProfiles === undefined) {
    types = [...USER_TYPES, 'MODIFY_ROLES', ...GROUP_TYPES];
  }
  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title={title}
        description={description}
        actions={
          newPermissions.some(can) && (
            <Button icon={<Plus size={16} />} onClick={() => void navigate(newPath)}>
              {newLabel}
            </Button>
          )
        }
      />
      <Card flush>
        <Tabs tabs={tabs} active={filters.scope ?? 'ALL'} onChange={(scope) => set({ scope })} />
        <WorklistToolbar
          placeholder="Search Request No., User or Role"
          onSearch={(text) => set({ text: text || undefined })}
          filters={{ open: showFilters, onToggle: () => setShowFilters(!showFilters) }}
        >
          {decidable && (
            <Button
              icon={<Check size={16} />}
              disabled={selection.keys.length === 0}
              busy={approveSelected.isPending}
              onClick={() => approveSelected.mutate(selection.keys.map(Number))}
            >
              Approve Selected
            </Button>
          )}
        </WorklistToolbar>
        {showFilters && <Filters filters={filters} types={types} onChange={set} />}
        <ErrorAlert error={requests.error ?? approveSelected.error} />
        <DataTable<AccessRequest>
          loading={requests.isLoading}
          rows={rows}
          rowKey={(r) => r.id}
          onRowClick={(r) => void navigate(`/user-access/requests/${String(r.id)}`)}
          emptyMessage="No access request in this list."
          columns={columns}
        />
        <PageFooter
          data={requests.data}
          noun="requests"
          onPage={(page) => setFilters({ ...filters, page })}
        />
      </Card>
    </div>
  );
}
