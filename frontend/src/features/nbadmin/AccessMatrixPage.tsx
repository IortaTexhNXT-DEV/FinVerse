import { useMutation, useQuery } from '@tanstack/react-query';
import { Check, FileDown } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessActionMatrix, AccessMatrix, MatrixRole } from '@/api/nbadmin';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { ACTION_LABELS, actionText, areaLabel } from './accessMatrix';

type View = 'permission' | 'action';

const VIEWS = [
  { id: 'permission', label: 'By Permission' },
  { id: 'action', label: 'By Action' },
] as const;

function RoleHeaders({ roles }: Readonly<{ roles: MatrixRole[] }>) {
  return (
    <>
      {roles.map((r) => (
        <th key={r.code} scope="col" title={r.name}>
          {r.code}
          <div className="muted">
            {r.enabledUsers} {r.enabledUsers === 1 ? 'user' : 'users'}
          </div>
        </th>
      ))}
    </>
  );
}

function PermissionTable({ matrix, needle }: Readonly<{ matrix: AccessMatrix; needle: string }>) {
  const rows = matrix.permissions.filter(
    (p) => p.permission.includes(needle) || (p.area ?? '').includes(needle),
  );
  if (rows.length === 0) {
    return <EmptyState message="No permission matches the search." />;
  }
  return (
    <table className="matrix">
      <caption className="visually-hidden">Roles against permissions</caption>
      <thead>
        <tr>
          <th scope="col">Permission</th>
          <th scope="col">Area</th>
          <th scope="col">Action class</th>
          <RoleHeaders roles={matrix.roles} />
        </tr>
      </thead>
      <tbody>
        {rows.map((p) => (
          <tr key={p.permission}>
            <th scope="row">{p.permission}</th>
            <td>{p.area === undefined ? '—' : areaLabel(p.area)}</td>
            <td>{actionText(p.actions) || '—'}</td>
            {matrix.roles.map((r) => (
              <td key={r.code}>
                {p.roles.includes(r.code) && (
                  <Check size={14} className="matrix-granted" aria-label="granted" />
                )}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function ActionTable({ matrix, needle }: Readonly<{ matrix: AccessActionMatrix; needle: string }>) {
  const rows = matrix.rows.filter(
    (r) => r.area.includes(needle) || r.permissions.some((p) => p.includes(needle)),
  );
  if (rows.length === 0) {
    return <EmptyState message="No area or permission matches the search." />;
  }
  return (
    <table className="matrix">
      <caption className="visually-hidden">Roles against areas and action classes</caption>
      <thead>
        <tr>
          <th scope="col">Area</th>
          <th scope="col">Action class</th>
          <RoleHeaders roles={matrix.roles} />
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={`${row.area}-${row.action}`}>
            <th scope="row" className="matrix-area" title={row.permissions.join(', ')}>
              {areaLabel(row.area)}
            </th>
            <td>{ACTION_LABELS[row.action]}</td>
            {matrix.roles.map((r) => (
              <td key={r.code} className="matrix-cell-permissions">
                {row.grants[r.code]?.join(', ')}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

/**
 * The agreed User Access Matrix (BRD 3.3.4, PMADD05): every role against every permission as
 * granted today, or against functional areas and action classes (view only, create, amend,
 * approve / validate) with the permissions behind each cell. Read-only; the Excel export holds both
 * views.
 */
export default function AccessMatrixPage() {
  const toast = useToast();
  const [view, setView] = useState<View>('permission');
  const [filter, setFilter] = useState('');
  const matrix = useQuery({ queryKey: ['nbadmin', 'matrix'], queryFn: nbadminApi.matrix });
  const byAction = useQuery({
    queryKey: ['nbadmin', 'matrix', 'by-action'],
    queryFn: nbadminApi.matrixByAction,
    enabled: view === 'action',
  });
  const download = useMutation({
    mutationFn: nbadminApi.exportMatrix,
    onSuccess: (file) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${file.fileName} downloaded`);
    },
  });
  const needle = filter.trim().toUpperCase().replaceAll(' ', '_');
  const current = view === 'permission' ? matrix : byAction;
  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title="User Access Matrix"
        description="Roles and the functions they grant, by permission or by area and action class (view only, create, amend, approve). Changes to roles go through access requests."
        actions={
          <Button
            variant="secondary"
            icon={<FileDown size={16} />}
            busy={download.isPending}
            onClick={() => download.mutate()}
          >
            Export to Excel
          </Button>
        }
      />
      <ErrorAlert error={current.error ?? download.error} />
      <Card>
        <Field
          label={view === 'permission' ? 'Find a permission or area' : 'Find an area or permission'}
        >
          {(id) => (
            <input
              id={id}
              className="input"
              placeholder="e.g. PACKAGE"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            />
          )}
        </Field>
      </Card>
      <Card flush>
        <Tabs tabs={VIEWS} active={view} onChange={setView} />
        {current.isLoading && <span className="spinner" aria-label="Loading" />}
        <div className="matrix-wrap">
          {view === 'permission' && matrix.data && (
            <PermissionTable matrix={matrix.data} needle={needle} />
          )}
          {view === 'action' && byAction.data && (
            <ActionTable matrix={byAction.data} needle={needle} />
          )}
        </div>
      </Card>
    </div>
  );
}
