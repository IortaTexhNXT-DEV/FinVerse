import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import type { Choice } from './setupLabels';

/** A value of the Collections Setup being changed: a free value, one choice, or several choices. */
export interface Editing {
  title: string;
  label: string;
  value: string;
  hint?: string;
  /** The choices of the value, shown by their label; the code is what is saved. */
  choices?: readonly Choice[];
  /** Several choices may be ticked (saved separated by commas); none means every one. */
  multiple?: boolean;
  save: (value: string) => Promise<unknown>;
}

function split(value: string): string[] {
  return value
    .split(',')
    .map((v) => v.trim())
    .filter((v) => v !== '');
}

/** The input of the value: a drop-down of labels, check boxes of labels, or a text. */
function ValueInput({
  id,
  editing,
  value,
  onChange,
}: Readonly<{ id: string; editing: Editing; value: string; onChange: (v: string) => void }>) {
  const choices = editing.choices;
  if (choices !== undefined && editing.multiple === true) {
    const ticked = split(value);
    const toggle = (code: string, on: boolean) =>
      onChange(
        choices
          .map((c) => c.code)
          .filter((c) => (c === code ? on : ticked.includes(c)))
          .join(','),
      );
    return (
      <div id={id} className="stack" role="group" aria-label={editing.label}>
        {choices.map((c) => (
          <label key={c.code} className="checkbox">
            <input
              type="checkbox"
              checked={ticked.includes(c.code)}
              onChange={(e) => toggle(c.code, e.target.checked)}
            />{' '}
            {c.label}
          </label>
        ))}
      </div>
    );
  }
  if (choices !== undefined) {
    return (
      <select id={id} className="select" value={value} onChange={(e) => onChange(e.target.value)}>
        <option value="">Not set</option>
        {choices.map((c) => (
          <option key={c.code} value={c.code}>
            {c.label}
          </option>
        ))}
      </select>
    );
  }
  return (
    <input id={id} className="input" value={value} onChange={(e) => onChange(e.target.value)} />
  );
}

/** Changes one value of the Collections Setup and refreshes the screen. */
export function EditDialog({
  editing,
  onClose,
}: Readonly<{ editing: Editing; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [value, setValue] = useState(editing.value);
  const save = useMutation({
    mutationFn: () => editing.save(value.trim()),
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['collections', 'setup'] });
      toast.success(`${editing.label} saved`);
    },
  });
  return (
    <Modal
      title={editing.title}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} onClick={() => save.mutate()}>
            Save
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <Field label={editing.label} hint={editing.hint}>
          {(id) => <ValueInput id={id} editing={editing} value={value} onChange={setValue} />}
        </Field>
      </div>
    </Modal>
  );
}
