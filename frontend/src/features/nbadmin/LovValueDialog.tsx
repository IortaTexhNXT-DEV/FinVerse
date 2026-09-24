import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { lovApi } from '@/api/lov';
import type { LovType, LovValue } from '@/api/lov';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import { lovForm, toLovRequest, validateLov } from './lovForm';
import type { LovForm } from './lovForm';

interface LovValueDialogProps {
  type: LovType;
  value?: LovValue;
  nextOrder: number;
  onClose: () => void;
}

type TextKey = Exclude<keyof LovForm, never>;

/** Add or change a list value (BRNB.083); the change waits for authorization by a checker. */
export function LovValueDialog({ type, value, nextOrder, onClose }: Readonly<LovValueDialogProps>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<LovForm>(() => lovForm(value, today(), nextOrder));
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? validateLov(form) : {};
  const save = useMutation({
    mutationFn: () =>
      value === undefined
        ? lovApi.create(type.code, toLovRequest(form))
        : lovApi.update(value.id, toLovRequest(form)),
    onSuccess: async (saved) => {
      toast.success(`${saved.label} saved; waiting for authorization`);
      await queryClient.invalidateQueries({ queryKey: ['lov'] });
      onClose();
    },
  });
  const submit = () => {
    setSubmitted(true);
    if (Object.keys(validateLov(form)).length === 0) {
      save.mutate();
    }
  };
  const input = (
    name: TextKey,
    label: string,
    props: { type?: string; required?: boolean; hint?: string } = {},
  ) => (
    <Field label={label} required={props.required} error={errors[name]} hint={props.hint}>
      {(id) => (
        <input
          id={id}
          className="input"
          type={props.type ?? 'text'}
          value={form[name]}
          disabled={name === 'code' && value !== undefined}
          onChange={(e) => setForm((f) => ({ ...f, [name]: e.target.value }))}
        />
      )}
    </Field>
  );
  return (
    <Modal
      open
      title={value === undefined ? `New value of ${type.name}` : `Change ${value.code}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={save.isPending} onClick={submit}>
            Save for authorization
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="form-grid">
          {input('code', 'Code', { required: true, hint: 'Capital letters, digits and _' })}
          {input('label', 'Label', { required: true })}
          {input('sortOrder', 'Sort order', { type: 'number', required: true })}
          {input('parentCode', 'Parent value', { hint: 'For dependent lists only' })}
          {input('effectiveFrom', 'Effective from', { type: 'date', required: true })}
          {input('effectiveTo', 'Effective to', { type: 'date', hint: 'Blank = no end date' })}
        </div>
      </div>
    </Modal>
  );
}
