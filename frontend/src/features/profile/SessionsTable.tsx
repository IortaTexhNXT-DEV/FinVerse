import type { UserSessionEntry } from '@/api/auth';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import { sessionStatus } from './profileRules';

/**
 * Sign-in sessions of the session log (FR-UA-004): sign-in, last activity (kept every five
 * minutes), end and status. With `onEnd` an administrator ends an open session.
 */
export function SessionsTable({
  rows,
  loading,
  showUser = false,
  onEnd,
}: Readonly<{
  rows: UserSessionEntry[];
  loading: boolean;
  showUser?: boolean;
  onEnd?: (s: UserSessionEntry) => void;
}>) {
  const columns: Column<UserSessionEntry>[] = [
    { key: 'issued', header: 'Signed in', render: (s) => formatDateTime(s.issuedAt) },
    { key: 'seen', header: 'Last activity', render: (s) => formatDateTime(s.lastSeenAt) },
    {
      key: 'ended',
      header: 'Ended',
      render: (s) => (s.endedAt === undefined ? '—' : formatDateTime(s.endedAt)),
    },
    { key: 'status', header: 'Status', render: (s) => <StatusBadge status={sessionStatus(s)} /> },
  ];
  if (showUser) {
    columns.unshift({ key: 'user', header: 'User', render: (s) => s.username });
  }
  if (onEnd !== undefined) {
    columns.push({
      key: 'actions',
      header: 'Actions',
      render: (s) =>
        s.open ? (
          <Button size="sm" variant="secondary" onClick={() => onEnd(s)}>
            End Session
          </Button>
        ) : null,
    });
  }
  return (
    <DataTable
      columns={columns}
      rows={rows}
      rowKey={(s) => s.sessionId}
      loading={loading}
      emptyMessage="No sessions recorded yet"
      caption="Sign-in sessions"
    />
  );
}
