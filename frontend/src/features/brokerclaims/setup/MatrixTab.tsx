import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { claimsSetupApi } from './api';
import type { MatrixRow } from './api';

function AddRowDialog({
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (status: string, role: string, unit: string) => void;
}>) {
  const roles = useQuery({
    queryKey: ['broker-claims', 'setup', 'roles'],
    queryFn: claimsSetupApi.roles,
  });
  const [status, setStatus] = useState('');
  const [role, setRole] = useState('');
  const [unit, setUnit] = useState('');
  const [submitted, setSubmitted] = useState(false);
  return (
    <Modal
      open
      title="Add Matrix Row"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            onClick={() => {
              setSubmitted(true);
              if (status !== '' && role !== '') {
                onSave(status, role, unit);
              }
            }}
          >
            Submit for Authorization
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error ?? roles.error} />
        <Field
          label="Status"
          required
          error={submitted && status === '' ? 'Select the status' : undefined}
        >
          {(id) => (
            <LovSelect
              id={id}
              type="BCL_CLAIM_STATUS"
              value={status}
              onChange={setStatus}
              required
            />
          )}
        </Field>
        <Field
          label="Role"
          required
          error={submitted && role === '' ? 'Select the role' : undefined}
        >
          {(id) => (
            <select
              id={id}
              className="select"
              value={role}
              onChange={(e) => setRole(e.target.value)}
            >
              <option value="">Select…</option>
              {(roles.data ?? []).map((r) => (
                <option key={r.code} value={r.code}>
                  {r.name} ({r.code})
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Unit" hint="Blank = any unit.">
          {(id) => (
            <LovSelect
              id={id}
              type="BCL_UNIT"
              value={unit}
              onChange={setUnit}
              placeholder="Any unit"
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * Status access matrix (BRCLM.012/013, FR-CL-041): which roles in which units may select each
 * status; a new row takes effect once another user authorizes it; rows are deactivated, never
 * deleted.
 */
export function MatrixTab() {
  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [adding, setAdding] = useState(false);
  const [filter, setFilter] = useState('');
  const rows = useQuery({
    queryKey: ['broker-claims', 'setup', 'matrix'],
    queryFn: claimsSetupApi.matrix,
  });
  const action = useMutation({
    mutationFn: (run: () => Promise<unknown>) => run(),
    onSuccess: async () => {
      setAdding(false);
      await queryClient.invalidateQueries({ queryKey: ['broker-claims', 'setup'] });
      toast.success('Status access matrix updated');
    },
  });
  const shown = useMemo(() => {
    const term = filter.trim().toLowerCase();
    return (rows.data ?? []).filter(
      (r) =>
        term === '' ||
        r.statusLabel.toLowerCase().includes(term) ||
        r.roleCode.toLowerCase().includes(term),
    );
  }, [rows.data, filter]);
  const me = user?.username.toLowerCase();
  return (
    <>
      <div className="worklist-toolbar">
        <input
          className="input"
          aria-label="Filter by status or role"
          placeholder="Filter by status or role"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        />
        <div className="worklist-actions">
          <Button icon={<Plus size={16} />} onClick={() => setAdding(true)}>
            Add Row
          </Button>
        </div>
      </div>
      <ErrorAlert error={rows.error ?? (adding ? undefined : action.error)} />
      <DataTable<MatrixRow>
        caption="Status access matrix"
        loading={rows.isLoading}
        rows={shown}
        rowKey={(r) => r.id}
        emptyMessage="No matrix rows"
        columns={[
          { key: 'status', header: 'Status', render: (r) => r.statusLabel },
          { key: 'role', header: 'Role', render: (r) => r.roleCode },
          { key: 'unit', header: 'Unit', render: (r) => r.unitCode ?? 'Any unit' },
          { key: 'rec', header: 'Record', render: (r) => <StatusBadge status={r.status} /> },
          { key: 'maker', header: 'Maker', render: (r) => r.maker ?? '' },
          {
            key: 'act',
            header: '',
            render: (r) => (
              <span className="tag-list">
                {r.status === 'PENDING_AUTHORIZATION' && r.maker?.toLowerCase() !== me && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => action.mutate(() => claimsSetupApi.authorizeRow(r.id))}
                  >
                    Authorize
                  </Button>
                )}
                {r.status !== 'INACTIVE' && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => action.mutate(() => claimsSetupApi.deactivateRow(r.id))}
                  >
                    Deactivate
                  </Button>
                )}
              </span>
            ),
          },
        ]}
      />
      {adding && (
        <AddRowDialog
          busy={action.isPending}
          error={action.error}
          onClose={() => setAdding(false)}
          onSave={(status, role, unit) =>
            action.mutate(() => claimsSetupApi.addRow(status, role, unit))
          }
        />
      )}
    </>
  );
}
