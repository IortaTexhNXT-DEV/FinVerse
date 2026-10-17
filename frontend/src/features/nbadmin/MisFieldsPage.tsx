import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Ban, Check, Pencil, Plus, RotateCcw } from 'lucide-react';
import { useState } from 'react';
import { misFieldsApi } from '@/api/misFields';
import type { MisField, MisFieldInput } from '@/api/misFields';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActions } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { humanize } from '@/utils/format';
import { MIS_ENTITIES, fieldErrors, inputOf, sourceOptions } from './misFieldForm';

const KEY = ['mis-fields'];

function FieldDialog({ field, onClose }: Readonly<{ field?: MisField; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [input, setInput] = useState<MisFieldInput>(() => inputOf(field));
  const [tried, setTried] = useState(false);
  const errors = tried ? fieldErrors(input) : {};
  const editing = field !== undefined;
  const save = useMutation({
    mutationFn: () => (editing ? misFieldsApi.change(field.id, input) : misFieldsApi.create(input)),
    onSuccess: async (f) => {
      toast.success(`${f.label} saved – pending authorization`);
      await queryClient.invalidateQueries({ queryKey: KEY });
      onClose();
    },
  });
  const submit = () => {
    setTried(true);
    if (Object.keys(fieldErrors(input)).length === 0) {
      save.mutate();
    }
  };
  return (
    <Modal
      title={editing ? `Edit MIS Field ${field.code}` : 'New MIS Field'}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} onClick={submit}>
            Save for Authorization
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <TextInput
          label="Code"
          required
          upper
          disabled={editing}
          error={errors.code}
          value={input.code}
          onChange={(code) => setInput((i) => ({ ...i, code }))}
        />
        <TextInput
          label="Label"
          required
          error={errors.label}
          value={input.label}
          onChange={(label) => setInput((i) => ({ ...i, label }))}
        />
        <SelectInput
          label="Entity"
          required
          disabled={editing}
          value={input.entity}
          options={MIS_ENTITIES}
          onChange={(v) => {
            const entity = MIS_ENTITIES.find((e) => e.value === v)?.value ?? 'ACCOUNT';
            setInput((i) => ({ ...i, entity, source: sourceOptions(entity)[0]?.value ?? '' }));
          }}
        />
        <SelectInput
          label="Source"
          required
          disabled={editing}
          value={input.source}
          options={sourceOptions(input.entity)}
          onChange={(source) => setInput((i) => ({ ...i, source }))}
        />
        <TextInput
          label="List of values"
          upper
          value={input.lovType ?? undefined}
          onChange={(v) => setInput((i) => ({ ...i, lovType: v === '' ? null : v }))}
        />
      </div>
    </Modal>
  );
}

/**
 * MIS Fields (Broking Setup, FR-CR-062): the business label of each data element of the core
 * entities, offered with the same label for grouping in the Layout panel of every report. The
 * System Administrator adds, changes, deactivates and reactivates a field; another administrator
 * authorizes it. Changes are in the Master Data Change Log.
 */
export default function MisFieldsPage() {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<MisField | 'new' | null>(null);
  const fields = useQuery({ queryKey: KEY, queryFn: misFieldsApi.list });
  const maintain = can('MIS_FIELD_MAINTAIN');
  const authorize = can('MIS_FIELD_AUTHORIZE');
  const act = useMutation({
    mutationFn: ({
      f,
      action,
    }: {
      f: MisField;
      action: 'authorize' | 'deactivate' | 'reactivate';
    }) => misFieldsApi[action](f.id),
    onSuccess: async (f) => {
      toast.success(`${f.label}: ${humanize(f.recordStatus)}`);
      await queryClient.invalidateQueries({ queryKey: KEY });
    },
  });
  const mine = (f: MisField) => user !== null && f.maker === user.username;
  const actions = (f: MisField) => (
    <RowActions
      record={`MIS Field ${f.code}`}
      actions={[
        {
          label: 'Edit',
          icon: <Pencil size={14} />,
          hidden: !maintain || f.recordStatus === 'INACTIVE',
          onSelect: () => setEditing(f),
        },
        {
          label: 'Authorize',
          icon: <Check size={14} />,
          hidden: !authorize || f.recordStatus !== 'PENDING_AUTHORIZATION' || mine(f),
          confirm: {
            title: `Authorize MIS Field ${f.code}`,
            record: f.label,
            effect: 'The field becomes active: the reports offer it with this label.',
          },
          onSelect: () => act.mutateAsync({ f, action: 'authorize' }),
        },
        {
          label: 'Deactivate',
          icon: <Ban size={14} />,
          hidden: !maintain || f.recordStatus === 'INACTIVE',
          danger: true,
          confirm: {
            title: `Deactivate MIS Field ${f.code}`,
            record: f.label,
            effect:
              'The reports no longer offer the field; saved layouts that use it show a warning.',
            destructive: true,
          },
          onSelect: () => act.mutateAsync({ f, action: 'deactivate' }),
        },
        {
          label: 'Reactivate',
          icon: <RotateCcw size={14} />,
          hidden: !maintain || f.recordStatus !== 'INACTIVE',
          confirm: {
            title: `Reactivate MIS Field ${f.code}`,
            record: f.label,
            effect: 'The field waits for its authorization before the reports offer it again.',
          },
          onSelect: () => act.mutateAsync({ f, action: 'reactivate' }),
        },
      ]}
    />
  );
  return (
    <div className="stack">
      <PageHeader
        section="Broking Setup"
        title="MIS Fields"
        description="The business label of each data element of the core entities, used for grouping in every report."
        actions={
          maintain && (
            <Button icon={<Plus size={16} />} onClick={() => setEditing('new')}>
              New MIS Field
            </Button>
          )
        }
      />
      <ErrorAlert error={fields.error ?? act.error} onRetry={() => void fields.refetch()} />
      <Card flush>
        <DataTable<MisField>
          loading={fields.isLoading}
          rows={fields.data ?? []}
          rowKey={(f) => f.id}
          emptyMessage="No MIS field defined"
          columns={[
            { key: 'code', header: 'Code', render: (f) => f.code },
            { key: 'label', header: 'Label', render: (f) => f.label },
            {
              key: 'entity',
              header: 'Entity',
              render: (f) => MIS_ENTITIES.find((e) => e.value === f.entity)?.label ?? f.entity,
            },
            { key: 'source', header: 'Source', render: (f) => humanize(f.source) },
            {
              key: 'columns',
              header: 'Report Columns',
              render: (f) => f.columnKeys.join(', '),
            },
            { key: 'lov', header: 'List of Values', render: (f) => f.lovType ?? '' },
            {
              key: 'status',
              header: 'Status',
              render: (f) => <StatusBadge status={f.recordStatus} />,
            },
            { key: 'maker', header: 'Maker', render: (f) => <UserName login={f.maker} /> },
            { key: 'actions', header: '', render: actions },
          ]}
        />
      </Card>
      {editing !== null && (
        <FieldDialog
          field={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}
