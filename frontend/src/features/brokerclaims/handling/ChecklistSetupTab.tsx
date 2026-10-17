import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { LovLabel } from '@/components/broking/LovLabel';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { DialogFooter, LovField } from '../record/FormParts';
import type { DocumentRequirement } from './api';
import { handlingApi } from './api';

type Form = Omit<DocumentRequirement, 'id'>;

function RequirementDialog({
  initial,
  onClose,
}: Readonly<{ initial?: DocumentRequirement; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Form>(
    initial ?? { claimType: '', documentType: '', required: true, active: true },
  );
  const save = useMutation({
    mutationFn: () => handlingApi.saveRequirement(form),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['broker-claims', 'setup'] });
      toast.success('Checklist saved');
      onClose();
    },
  });
  return (
    <Modal
      title="Checklist Document"
      open
      onClose={onClose}
      footer={
        <DialogFooter
          label="Save"
          busy={save.isPending}
          disabled={form.claimType === '' || form.documentType === ''}
          onClose={onClose}
          onConfirm={() => save.mutate()}
        />
      }
    >
      <ErrorAlert error={save.error} />
      <div className="stack">
        <LovField
          label="Claim type"
          type="BCL_CLAIM_TYPE"
          required
          value={form.claimType}
          onChange={(claimType) => setForm({ ...form, claimType })}
        />
        <LovField
          label="Document"
          type="DOCUMENT_TYPE"
          required
          value={form.documentType}
          onChange={(documentType) => setForm({ ...form, documentType })}
        />
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.required}
            onChange={(e) => setForm({ ...form, required: e.target.checked })}
          />
          Required before the claim is filed with complete documents
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.active}
            onChange={(e) => setForm({ ...form, active: e.target.checked })}
          />
          Active
        </label>
      </div>
    </Modal>
  );
}

/**
 * The standard claim document checklist per claim type (FRS BRD-07 FRCLM.006.01), maintained by
 * the Claims Unit Head.
 */
export function ChecklistSetupTab() {
  const [editing, setEditing] = useState<DocumentRequirement | 'new'>();
  const list = useQuery({
    queryKey: ['broker-claims', 'setup', 'document-requirements'],
    queryFn: handlingApi.requirements,
  });
  return (
    <>
      <div className="worklist-toolbar">
        <div className="worklist-actions">
          <Button icon={<Plus size={16} />} onClick={() => setEditing('new')}>
            Add Document
          </Button>
        </div>
      </div>
      <ErrorAlert error={list.error} />
      <DataTable<DocumentRequirement>
        caption="Document checklist"
        rows={list.data ?? []}
        rowKey={(r) => r.id}
        emptyMessage="No document in the checklist."
        onRowClick={(r) => setEditing(r)}
        columns={[
          {
            key: 'type',
            header: 'Claim Type',
            render: (r) => <LovLabel type="BCL_CLAIM_TYPE" code={r.claimType} />,
          },
          {
            key: 'doc',
            header: 'Document',
            render: (r) => <LovLabel type="DOCUMENT_TYPE" code={r.documentType} />,
          },
          { key: 'req', header: 'Required', render: (r) => (r.required ? 'Yes' : 'Optional') },
          {
            key: 'active',
            header: 'Status',
            render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} />,
          },
        ]}
      />
      {editing !== undefined && (
        <RequirementDialog
          initial={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(undefined)}
        />
      )}
    </>
  );
}
