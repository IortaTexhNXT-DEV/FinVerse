import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { renewalUploadsApi } from '@/api/renewalUploads';
import type { RmuOfficer } from '@/api/renewalUploads';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import '../renewal.css';

type Editing = { kind: 'add' } | { kind: 'edit'; row: RmuOfficer };

function OfficerDialog({
  editing,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  editing: Editing;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (v: { aoCode: string; aoName: string; remarks: string }) => void;
}>) {
  const row = editing.kind === 'edit' ? editing.row : undefined;
  const [code, setCode] = useState(row?.aoCode ?? '');
  const [name, setName] = useState(row?.aoName ?? '');
  const [remarks, setRemarks] = useState(row?.remarks ?? '');
  return (
    <Modal
      open
      size="md"
      title={row ? `Update RMU Account Officer ${row.aoCode}` : 'Add RMU Account Officer'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            disabled={code.trim() === '' || name.trim() === ''}
            onClick={() => onSave({ aoCode: code.trim(), aoName: name.trim(), remarks })}
          >
            Save
          </Button>
        </>
      }
    >
      <ErrorAlert error={error} />
      <Field label="AO Code" required>
        {(id) => (
          <input
            id={id}
            className="input"
            value={code}
            disabled={row !== undefined}
            onChange={(e) => setCode(e.target.value)}
          />
        )}
      </Field>
      <Field label="Account Officer Name" required>
        {(id) => (
          <input id={id} className="input" value={name} onChange={(e) => setName(e.target.value)} />
        )}
      </Field>
      <Field label="Remarks">
        {(id) => (
          <input
            id={id}
            className="input"
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}

/**
 * RMU Account Maintenance (FRRN.041.01): the Account Officer codes of the Remedial Management Unit;
 * a CBG loan of one of them has the loan status RMU in the LAMD upload.
 */
export default function RmuOfficersPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Editing>();
  const rows = useQuery({
    queryKey: ['renewal', 'rmu-officers', companyId],
    queryFn: () => renewalUploadsApi.rmuOfficers(companyId),
  });
  const save = useMutation({
    mutationFn: (input: { call: () => Promise<unknown>; done: string }) => input.call(),
    onSuccess: async (_r, input) => {
      setEditing(undefined);
      toast.success(input.done);
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'rmu-officers'] });
    },
  });
  const maintain = can('RNW_RMU_MAINTAIN');
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="RMU Account Maintenance"
        description="Account Officer codes of the Remedial Management Unit."
        actions={
          maintain && (
            <Button icon={<Plus size={16} />} onClick={() => setEditing({ kind: 'add' })}>
              Add AO Code
            </Button>
          )
        }
      />
      <ErrorAlert error={rows.error ?? (editing === undefined ? save.error : null)} />
      <Card flush>
        <DataTable<RmuOfficer>
          loading={rows.isLoading}
          rows={rows.data ?? []}
          rowKey={(r) => String(r.id)}
          emptyMessage="No RMU Account Officer"
          columns={[
            { key: 'code', header: 'AO Code', kind: 'code', render: (r) => r.aoCode },
            { key: 'name', header: 'Account Officer Name', render: (r) => r.aoName },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (r) => (
                <StatusBadge
                  status={r.active ? 'ACTIVE' : 'INACTIVE'}
                  label={r.active ? 'Active' : 'Inactive'}
                />
              ),
            },
            { key: 'remarks', header: 'Remarks', render: (r) => r.remarks ?? '' },
            {
              key: 'by',
              header: 'Updated By',
              render: (r) => <UserName login={r.updatedBy ?? r.createdBy} />,
            },
            {
              key: 'at',
              header: 'Updated',
              kind: 'datetime',
              render: (r) => formatDateTime(r.updatedAt ?? r.createdAt),
            },
            ...(maintain
              ? [
                  {
                    key: 'actions',
                    header: 'Actions',
                    render: (r: RmuOfficer) => (
                      <RowActionMenu
                        label={r.aoCode}
                        actions={[
                          { label: 'Update', onSelect: () => setEditing({ kind: 'edit', row: r }) },
                          {
                            label: r.active ? 'Deactivate' : 'Re-activate',
                            danger: r.active,
                            onSelect: () =>
                              save.mutate({
                                call: () =>
                                  renewalUploadsApi.updateRmuOfficer(companyId, r.id, {
                                    aoName: r.aoName,
                                    remarks: r.remarks ?? undefined,
                                    active: !r.active,
                                  }),
                                done: 'Record successfully updated.',
                              }),
                          },
                        ]}
                      />
                    ),
                  },
                ]
              : []),
          ]}
        />
      </Card>
      {editing && (
        <OfficerDialog
          editing={editing}
          busy={save.isPending}
          error={save.error}
          onClose={() => setEditing(undefined)}
          onSave={(v) =>
            save.mutate({
              call: () =>
                editing.kind === 'add'
                  ? renewalUploadsApi.addRmuOfficer(companyId, v)
                  : renewalUploadsApi.updateRmuOfficer(companyId, editing.row.id, {
                      aoName: v.aoName,
                      remarks: v.remarks,
                      active: editing.row.active,
                    }),
              done: 'Record successfully updated.',
            })
          }
        />
      )}
    </div>
  );
}
