import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import { EMPTY_DEDUCTION, deductionErrors } from './deductionForm';
import type { DeductionErrors, DeductionForm } from './deductionForm';

const CURRENCIES = ['PHP', 'USD'];

type TextKey = Exclude<keyof DeductionForm, 'currency' | 'sourceType'>;

function TextField({
  label,
  field,
  form,
  errors,
  onChange,
  type = 'text',
  maxLength,
  required = false,
  hint,
}: Readonly<{
  label: string;
  field: TextKey;
  form: DeductionForm;
  errors: DeductionErrors;
  onChange: (field: TextKey, value: string) => void;
  type?: string;
  maxLength?: number;
  required?: boolean;
  hint?: string;
}>) {
  return (
    <Field label={label} required={required} error={errors[field]} hint={hint}>
      {(id) => (
        <input
          id={id}
          className="input"
          type={type}
          maxLength={maxLength}
          inputMode={field === 'amount' ? 'decimal' : undefined}
          value={form[field]}
          onChange={(e) => onChange(field, e.target.value)}
        />
      )}
    </Field>
  );
}

/**
 * Prepares or changes a remittance deduction (ACSL 2.9.2): insurer, currency, what it settles,
 * amount and the insurer's written confirmation, which is required before submission.
 */
export function DeductionDialog({
  title,
  initial = EMPTY_DEDUCTION,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  title: string;
  initial?: DeductionForm;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (form: DeductionForm) => void;
}>) {
  const [form, setForm] = useState<DeductionForm>(initial);
  const [errors, setErrors] = useState<DeductionErrors>({});
  const set = (field: keyof DeductionForm, value: string) => setForm({ ...form, [field]: value });
  const save = () => {
    const found = deductionErrors(form, today());
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSave(form);
    }
  };
  const common = { form, errors, onChange: set };
  return (
    <Modal
      title={title}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={save}>
            Save Deduction
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="form-grid">
          <TextField label="Insurer Code" field="insurerCode" maxLength={30} required {...common} />
          <Field label="Currency" required error={errors.currency}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.currency}
                onChange={(e) => set('currency', e.target.value)}
              >
                {CURRENCIES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Deduction Source" required error={errors.sourceType}>
            {(id) => (
              <LovSelect
                id={id}
                type="REMIT_DEDUCTION_SOURCE"
                value={form.sourceType}
                onChange={(code) => set('sourceType', code)}
                required
              />
            )}
          </Field>
          <TextField
            label="Source Reference"
            field="sourceRef"
            maxLength={60}
            required
            hint="AR insurer's refund, memo or OR number"
            {...common}
          />
          <TextField label="Invoice No." field="invoiceNo" maxLength={40} {...common} />
          <TextField label="Amount" field="amount" required {...common} />
          <TextField
            label="Insurer Confirmation Ref."
            field="confirmationRef"
            maxLength={100}
            hint="Required before submission"
            {...common}
          />
          <TextField label="Confirmation Date" field="confirmationDate" type="date" {...common} />
        </div>
        <Field label="Remarks">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={1000}
              value={form.remarks}
              onChange={(e) => set('remarks', e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
