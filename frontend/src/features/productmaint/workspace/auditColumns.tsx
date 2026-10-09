import type { AuditEntry } from '@/api/admin';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';

const TIMESTAMP: Column<AuditEntry> = {
  key: 't',
  header: 'Timestamp',
  kind: 'datetime',
  sortKey: 'occurredAt',
  render: (a) => formatDateTime(a.occurredAt),
};

const ACTION: Column<AuditEntry> = {
  key: 'a',
  header: 'Action Type',
  kind: 'status',
  sortKey: 'action',
  render: (a) => <StatusBadge status={a.actionLabel ?? a.action} tone="neutral" />,
};

const VALUES_AND_USER: Column<AuditEntry>[] = [
  { key: 's', header: 'Description', render: (a) => a.summary },
  { key: 'f', header: 'Old Value', render: (a) => a.oldValue ?? '' },
  { key: 'n', header: 'New Value', render: (a) => a.newValue ?? '' },
  {
    key: 'u',
    header: 'Performed By',
    sortKey: 'username',
    render: (a) => <UserName login={a.username} />,
  },
  { key: 'r', header: 'Remarks', render: (a) => a.remarks ?? '' },
];

/**
 * The columns of the Product Maintenance audit logs (BDOI FRS FRPM.021.01): Timestamp, Module,
 * Reference Number, Client/Assured's Name, Action Type, Description, Old Value, New Value,
 * Performed By and Remarks.
 */
export const PM_AUDIT_COLUMNS: Column<AuditEntry>[] = [
  TIMESTAMP,
  { key: 'm', header: 'Module', render: (a) => a.module ?? '' },
  { key: 'k', header: 'Reference Number', sortKey: 'entityId', render: (a) => a.entityId ?? '' },
  { key: 'c', header: "Client/Assured's Name", render: (a) => a.subject ?? '' },
  ACTION,
  ...VALUES_AND_USER,
];

/**
 * The audit logs of one record (BDOI FRS FRPM.021.02): Timestamp, Action Type, Description, Old
 * Value, New Value, Performed By and Remarks.
 */
export const RECORD_AUDIT_COLUMNS: Column<AuditEntry>[] = [TIMESTAMP, ACTION, ...VALUES_AND_USER];
