import { useMutation, useQuery } from '@tanstack/react-query';
import { Check, FileDown } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import type { RefObject } from 'react';
import { saveFile } from '@/api/client';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessActionMatrix, AccessMatrix, MatrixRole } from '@/api/nbadmin';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { EmptyRow } from '@/components/ui/EmptyRow';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { PermissionName } from '@/components/ui/PermissionName';
import { permissionDescription, permissionLabel, permissionLabels } from '@/utils/permissionLabel';
import { ACTION_LABELS, actionText, areaLabel } from './accessMatrix';
import { LoadingPanel } from '@/components/ui/LoadingPanel';

type View = 'permission' | 'action';

/** Whether a name matches the search (the search is kept in the code form, A_B). */
function matchesName(name: string, needle: string): boolean {
  return name.toUpperCase().replace(/ /g, '_').includes(needle);
}

/** Highlights the profile column under the pointer (header and cells). */
function useColumnHover(ref: RefObject<HTMLDivElement | null>): void {
  useEffect(() => {
    const box = ref.current;
    if (box === null) {
      return undefined;
    }
    const clear = () =>
      box
        .querySelectorAll('.matrix-col-hover')
        .forEach((c) => c.classList.remove('matrix-col-hover'));
    const over = (e: MouseEvent) => {
      const cell = (e.target as HTMLElement).closest('td, th');
      clear();
      if (!(cell instanceof HTMLTableCellElement)) {
        return;
      }
      const head = box.querySelectorAll('thead th')[cell.cellIndex];
      if (head?.hasAttribute('data-role') !== true) {
        return;
      }
      box
        .querySelectorAll('tr')
        .forEach((tr) => tr.children[cell.cellIndex]?.classList.add('matrix-col-hover'));
    };
    box.addEventListener('mouseover', over);
    box.addEventListener('mouseleave', clear);
    return () => {
      box.removeEventListener('mouseover', over);
      box.removeEventListener('mouseleave', clear);
    };
  }, [ref]);
}

const VIEWS = [
  { id: 'permission', label: 'By Permission' },
  { id: 'action', label: 'By Action' },
] as const;

function RoleHeaders({ roles }: Readonly<{ roles: MatrixRole[] }>) {
  return (
    <>
      {roles.map((r) => (
        <th key={r.code} scope="col" title={r.name} data-role={r.code}>
          <span className="matrix-role-name">{r.name}</span>
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
    (p) =>
      p.permission.includes(needle) ||
      (p.area ?? '').includes(needle) ||
      matchesName(permissionLabel(p.permission), needle) ||
      matchesName(permissionDescription(p.permission), needle),
  );
  return (
    <table className="matrix">
      <caption className="visually-hidden">Roles against permissions</caption>
      <thead>
        <tr>
          <th scope="col" className="matrix-sticky-col">
            Permission
          </th>
          <th scope="col" className="matrix-narrow">
            Area
          </th>
          <th scope="col" className="matrix-narrow">
            Action class
          </th>
          <RoleHeaders roles={matrix.roles} />
        </tr>
      </thead>
      <tbody>
        {rows.length === 0 && (
          <EmptyRow columns={3 + matrix.roles.length} message="No permission matches the search." />
        )}
        {rows.map((p) => (
          <tr key={p.permission}>
            <th scope="row" className="matrix-sticky-col">
              <PermissionName code={p.permission} />
            </th>
            <td className="matrix-narrow">{p.area === undefined ? '—' : areaLabel(p.area)}</td>
            <td className="matrix-narrow">{actionText(p.actions) || '—'}</td>
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
    (r) =>
      r.area.includes(needle) ||
      r.permissions.some((p) => p.includes(needle) || matchesName(permissionLabel(p), needle)),
  );
  return (
    <table className="matrix">
      <caption className="visually-hidden">Roles against areas and action classes</caption>
      <thead>
        <tr>
          <th scope="col" className="matrix-sticky-col">
            Area
          </th>
          <th scope="col" className="matrix-narrow">
            Action class
          </th>
          <RoleHeaders roles={matrix.roles} />
        </tr>
      </thead>
      <tbody>
        {rows.length === 0 && (
          <EmptyRow
            columns={2 + matrix.roles.length}
            message="No area or permission matches the search."
          />
        )}
        {rows.map((row) => (
          <tr key={`${row.area}-${row.action}`}>
            <th
              scope="row"
              className="matrix-sticky-col matrix-area"
              title={permissionLabels(row.permissions)}
            >
              {areaLabel(row.area)}
            </th>
            <td className="matrix-narrow">{ACTION_LABELS[row.action]}</td>
            {matrix.roles.map((r) => (
              <td key={r.code} className="matrix-cell-permissions">
                {permissionLabels(row.grants[r.code] ?? [])}
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
  // The matrix scrolls in one box of its own, sized to the window (see .matrix-wrap).
  const wrap = useRef<HTMLDivElement>(null);
  useColumnHover(wrap);
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
        description="Roles and the functions they grant, by permission or by area and action class."
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
              placeholder="Search by permission, description or area"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            />
          )}
        </Field>
      </Card>
      <Card flush>
        <Tabs tabs={VIEWS} active={view} onChange={setView} />
        {current.isLoading && <LoadingPanel />}
        <div className="matrix-wrap" ref={wrap}>
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
