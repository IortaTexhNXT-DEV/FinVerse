import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { renewalApi } from '@/api/renewal';
import type { CheckSettingView } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { ApprovalCell } from './setupBits';
import { SEVERITIES, pending, severityLabel } from './setupCodes';

function CheckDialog({ row, onClose }: Readonly<{ row: CheckSettingView; onClose: () => void }>) {
  const queryClient = useQueryClient();
  const [active, setActive] = useState(row.active);
  const [severity, setSeverity] = useState(row.severity);
  const [parameters, setParameters] = useState(row.parameters ?? '');
  const save = useMutation({
    mutationFn: () =>
      renewalApi.updateCheck(row.checkCode, {
        active,
        severity,
        parameters: parameters.trim() || undefined,
      }),
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'setup'] });
    },
  });
  return (
    <Modal
      open
      title={row.checkName}
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} onClick={() => save.mutate()}>
            Save for Authorization
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <label className="checkbox">
        <input type="checkbox" checked={active} onChange={(e) => setActive(e.target.checked)} />
        Active
      </label>
      <Field label="Severity" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={severity}
            onChange={(e) => setSeverity(e.target.value)}
          >
            {SEVERITIES.map((s) => (
              <option key={s.code} value={s.code}>
                {s.label}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Parameters" hint="Optional settings of the check, e.g. days=30">
        {(id) => (
          <input
            id={id}
            className="input"
            value={parameters}
            onChange={(e) => setParameters(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

/** Check settings (BRRN.020): active, severity and parameters of each sanitation check. */
export function ChecksTab() {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<CheckSettingView>();
  const rows = useQuery({
    queryKey: ['renewal', 'setup', 'checks'],
    queryFn: renewalApi.checkSettings,
  });
  const authorize = useMutation({
    mutationFn: renewalApi.authorizeCheck,
    onSuccess: async (c) => {
      toast.success(`${c.checkName} authorized`);
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'setup'] });
    },
  });
  const maintain = can('RNW_SETUP');
  return (
    <Card title="Sanitation checks" flush>
      <ErrorAlert error={rows.error ?? authorize.error} />
      <DataTable<CheckSettingView>
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => r.checkCode}
        emptyMessage="No checks"
        columns={[
          { key: 'name', header: 'Check', render: (r) => r.checkName },
          { key: 'active', header: 'Active', render: (r) => (r.active ? 'Yes' : 'No') },
          { key: 'severity', header: 'Severity', render: (r) => severityLabel(r.severity) },
          { key: 'params', header: 'Parameters', render: (r) => r.parameters ?? '' },
          {
            key: 'status',
            header: 'Status',
            render: (r) => <ApprovalCell approval={r.approval} />,
          },
          {
            key: 'act',
            header: '',
            render: (r) =>
              maintain && (
                <span className="rnw-actions">
                  {pending(r.approval) && (
                    <Button size="sm" onClick={() => authorize.mutate(r.checkCode)}>
                      Authorize
                    </Button>
                  )}
                  <Button size="sm" variant="ghost" onClick={() => setEditing(r)}>
                    Edit
                  </Button>
                </span>
              ),
          },
        ]}
      />
      {editing !== undefined && <CheckDialog row={editing} onClose={() => setEditing(undefined)} />}
    </Card>
  );
}
