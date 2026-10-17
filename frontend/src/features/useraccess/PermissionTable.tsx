import { Download } from 'lucide-react';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { humanize } from '@/utils/format';
import { permissionDescription, permissionLabel } from '@/utils/permissionLabel';
import { saveCsv } from './csv';
import { areaOf, matchesPermission, sortPermissions } from './permissionRows';
import type { PermissionRow } from './permissionRows';

interface PermissionTableProps {
  /** Card title, typically the count ("37 permissions through 2 active group profiles"). */
  title: ReactNode;
  rows: readonly PermissionRow[];
  loading?: boolean;
  error?: unknown;
  /** Name of the CSV file of the export. */
  fileName: string;
  caption: string;
  emptyMessage: string;
  /** Shows the Granted Through column (the group profiles behind each permission). */
  grantedThrough?: boolean;
  /** Shows the Action Class column. */
  actionClasses?: boolean;
}

/**
 * A list of permissions as a table: the permission's name, what it allows, the group profiles it
 * comes through and its area, sorted by area and name, with a search and a CSV export.
 */
export function PermissionTable({
  title,
  rows,
  loading = false,
  error,
  fileName,
  caption,
  emptyMessage,
  grantedThrough = false,
  actionClasses = false,
}: Readonly<PermissionTableProps>) {
  const [text, setText] = useState('');
  const sorted = sortPermissions(rows);
  const shown = sorted.filter((r) => matchesPermission(r, text));
  const columns: Column<PermissionRow>[] = [
    {
      key: 'permission',
      header: 'Permission',
      width: '24%',
      render: (r) => <strong>{permissionLabel(r.permission)}</strong>,
    },
    { key: 'allows', header: 'What It Allows', render: (r) => permissionDescription(r.permission) },
  ];
  if (grantedThrough) {
    columns.push({
      key: 'through',
      header: 'Granted Through',
      render: (r) => (r.groupProfiles ?? []).join(', '),
    });
  }
  columns.push({ key: 'area', header: 'Area', render: areaOf });
  if (actionClasses) {
    columns.push({
      key: 'actions',
      header: 'Action Class',
      render: (r) => (r.actions ?? []).map(humanize).join(', '),
    });
  }
  const exportRows = () =>
    saveCsv(
      fileName,
      [
        'Permission',
        'Code',
        'What It Allows',
        ...(grantedThrough ? ['Granted Through'] : []),
        'Area',
        ...(actionClasses ? ['Action Class'] : []),
      ],
      shown.map((r) => [
        permissionLabel(r.permission),
        r.permission,
        permissionDescription(r.permission),
        ...(grantedThrough ? [(r.groupProfiles ?? []).join('; ')] : []),
        areaOf(r),
        ...(actionClasses ? [(r.actions ?? []).map(humanize).join('; ')] : []),
      ]),
    );
  return (
    <Card
      flush
      title={title}
      actions={
        <Button
          variant="secondary"
          icon={<Download size={16} />}
          disabled={shown.length === 0}
          onClick={exportRows}
        >
          Export
        </Button>
      }
    >
      <ErrorAlert error={error} />
      <WorklistToolbar placeholder="Search permission, area or group profile" onSearch={setText} />
      <DataTable<PermissionRow>
        caption={caption}
        loading={loading}
        rows={shown}
        rowKey={(r) => r.permission}
        emptyMessage={text === '' ? emptyMessage : 'No permission matches the search'}
        columns={columns}
      />
    </Card>
  );
}
