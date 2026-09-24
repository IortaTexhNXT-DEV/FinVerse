import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, X } from 'lucide-react';
import { useState } from 'react';
import { clientsApi } from '@/api/clients';
import type {
  ClientDetail,
  ClientInstruction,
  InstructionRequest,
  NoteHistory,
} from '@/api/clients';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime, humanize, today } from '@/utils/format';
import { InstructionDialog } from './InstructionDialog';
import { inForce } from './instructionRules';

type Editing = { instruction?: ClientInstruction } | null;

function Tags({ client, editable }: Readonly<{ client: ClientDetail; editable: boolean }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tag, setTag] = useState('');
  const banner = useQuery({
    queryKey: ['crm', 'banner', client.id],
    queryFn: () => clientsApi.banner(client.id),
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['crm'] });
  const add = useMutation({
    mutationFn: () => clientsApi.addTag(client.id, tag),
    onSuccess: async () => {
      setTag('');
      toast.success('Tag added');
      await refresh();
    },
  });
  const remove = useMutation({
    mutationFn: (code: string) => clientsApi.removeTag(client.id, code),
    onSuccess: async () => {
      toast.success('Tag removed');
      await refresh();
    },
  });
  const tags = banner.data?.tags ?? [];
  return (
    <Card title="Tags">
      <div className="stack">
        <ErrorAlert error={banner.error ?? add.error ?? remove.error} />
        <div className="record-facts">
          {tags.length === 0 && <span className="muted">No tags.</span>}
          {tags.map((t) => (
            <span key={t.code} className="client-tag">
              {t.label}
              {editable && (
                <button
                  type="button"
                  className="client-tag-remove"
                  aria-label={`Remove tag ${t.label}`}
                  onClick={() => remove.mutate(t.code)}
                >
                  <X size={12} aria-hidden="true" />
                </button>
              )}
            </span>
          ))}
        </div>
        {editable && (
          <div className="row">
            <Field label="Add a tag">
              {(id) => <LovSelect id={id} type="CLIENT_TAG" value={tag} onChange={setTag} />}
            </Field>
            <Button
              variant="secondary"
              icon={<Plus size={16} />}
              disabled={tag === ''}
              busy={add.isPending}
              onClick={() => add.mutate()}
            >
              Add tag
            </Button>
          </div>
        )}
      </div>
    </Card>
  );
}

function instructionState(i: ClientInstruction, date: string): string {
  if (inForce(i, date)) {
    return 'ACTIVE';
  }
  return i.active && i.effectiveFrom > date ? 'FUTURE' : 'INACTIVE';
}

function historyText(h: NoteHistory): string {
  if (h.action === 'CHANGED') {
    return `${h.fromValue ?? ''} → ${h.toValue ?? ''}`;
  }
  return h.toValue ?? h.fromValue ?? '';
}

/** Tags & Instructions (BRNB.091): tags, special instructions with effectivity, change history. */
export function NotesTab({ client }: Readonly<{ client: ClientDetail }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Editing>(null);
  const editable = can('CLIENT_MAINTAIN') && client.status !== 'INACTIVE';
  const notes = useQuery({
    queryKey: ['crm', 'notes', client.id],
    queryFn: () => clientsApi.notes(client.id),
  });
  const done = async (message: string) => {
    setEditing(null);
    toast.success(message);
    await queryClient.invalidateQueries({ queryKey: ['crm'] });
  };
  const save = useMutation({
    mutationFn: (r: InstructionRequest) =>
      editing?.instruction === undefined
        ? clientsApi.addInstruction(client.id, r)
        : clientsApi.changeInstruction(client.id, editing.instruction.id, r),
    onSuccess: () => done('Instruction saved'),
  });
  const end = useMutation({
    mutationFn: (i: ClientInstruction) => clientsApi.endInstruction(client.id, i.id),
    onSuccess: () => done('Instruction ended'),
  });
  const now = today();
  return (
    <div className="stack">
      <Tags client={client} editable={editable} />
      <Card
        title="Special instructions"
        flush
        actions={
          editable && (
            <Button
              size="sm"
              variant="secondary"
              icon={<Plus size={14} />}
              onClick={() => setEditing({})}
            >
              New instruction
            </Button>
          )
        }
      >
        <ErrorAlert error={notes.error ?? end.error} />
        <DataTable<ClientInstruction>
          loading={notes.isLoading}
          rows={notes.data?.instructions ?? []}
          rowKey={(i) => i.id}
          emptyMessage="No special instructions."
          columns={[
            { key: 'type', header: 'Type', render: (i) => humanize(i.type) },
            { key: 'text', header: 'Instruction', render: (i) => i.text },
            {
              key: 'dates',
              header: 'Effective',
              render: (i) =>
                `${formatDate(i.effectiveFrom)} – ${i.effectiveTo ? formatDate(i.effectiveTo) : 'open'}`,
            },
            {
              key: 'state',
              header: 'Status',
              render: (i) => <StatusBadge status={instructionState(i, now)} />,
            },
            {
              key: 'actions',
              header: '',
              render: (i) =>
                editable && i.active ? (
                  <div className="row">
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => setEditing({ instruction: i })}
                    >
                      Change
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => end.mutate(i)}>
                      End
                    </Button>
                  </div>
                ) : null,
            },
          ]}
        />
      </Card>
      <Card title="Change history" flush>
        <DataTable<NoteHistory>
          rows={notes.data?.history ?? []}
          rowKey={(h) => h.id}
          emptyMessage="No changes yet."
          columns={[
            { key: 'when', header: 'When', render: (h) => formatDateTime(h.occurredAt) },
            { key: 'who', header: 'User', render: (h) => h.actor },
            { key: 'what', header: 'Item', render: (h) => humanize(h.kind) },
            { key: 'action', header: 'Change', render: (h) => humanize(h.action) },
            { key: 'value', header: 'From → to', render: historyText },
          ]}
        />
      </Card>
      {editing !== null && (
        <InstructionDialog
          initial={editing.instruction}
          busy={save.isPending}
          error={save.error}
          onSave={(r) => save.mutate(r)}
          onClose={() => {
            save.reset();
            setEditing(null);
          }}
        />
      )}
    </div>
  );
}
