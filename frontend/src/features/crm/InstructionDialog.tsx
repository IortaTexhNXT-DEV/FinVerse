import { useState } from 'react';
import type { ClientInstruction, InstructionRequest } from '@/api/clients';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import { instructionErrors } from './instructionRules';

interface InstructionDialogProps {
  initial?: ClientInstruction;
  busy: boolean;
  error: unknown;
  onSave: (request: InstructionRequest) => void;
  onClose: () => void;
}

/** Add or change a special instruction (BRNB.091). */
export function InstructionDialog({
  initial,
  busy,
  error,
  onSave,
  onClose,
}: Readonly<InstructionDialogProps>) {
  const [form, setForm] = useState<InstructionRequest>({
    type: initial?.type ?? '',
    text: initial?.text ?? '',
    effectiveFrom: initial?.effectiveFrom ?? today(),
    effectiveTo: initial?.effectiveTo ?? '',
  });
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? instructionErrors(form) : {};
  const set = (patch: Partial<InstructionRequest>) => setForm((f) => ({ ...f, ...patch }));
  const save = () => {
    setSubmitted(true);
    if (Object.keys(instructionErrors(form)).length === 0) {
      onSave({
        ...form,
        text: form.text.trim(),
        effectiveTo: form.effectiveTo === '' ? undefined : form.effectiveTo,
      });
    }
  };
  return (
    <Modal
      open
      title={initial === undefined ? 'New special instruction' : 'Change special instruction'}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={busy} onClick={save}>
            Save instruction
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Type" required error={errors.type}>
          {(id) => (
            <LovSelect
              id={id}
              type="INSTRUCTION_TYPE"
              value={form.type}
              onChange={(type) => set({ type })}
            />
          )}
        </Field>
        <Field label="Instruction" required error={errors.text}>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={1000}
              value={form.text}
              onChange={(e) => set({ text: e.target.value })}
            />
          )}
        </Field>
        <div className="grid-2">
          <Field label="Effective from" required error={errors.effectiveFrom}>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={form.effectiveFrom}
                onChange={(e) => set({ effectiveFrom: e.target.value })}
              />
            )}
          </Field>
          <Field
            label="Effective to"
            error={errors.effectiveTo}
            hint="Blank = until further notice"
          >
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={form.effectiveTo ?? ''}
                onChange={(e) => set({ effectiveTo: e.target.value })}
              />
            )}
          </Field>
        </div>
      </div>
    </Modal>
  );
}
