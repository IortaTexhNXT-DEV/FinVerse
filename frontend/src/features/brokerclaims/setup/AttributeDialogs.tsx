import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import type { SettlementAttributesInput, StatusAttributesInput, ValueAttributes } from './api';
import {
  OUTCOMES,
  STATUS_PHASES,
  WAITING_ON,
  settlementForm,
  statusForm,
  validateSettlementForm,
  validateStatusForm,
} from './setupLogic';

interface DialogProps<T> {
  value: ValueAttributes;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (input: T) => void;
}

function Footer({
  busy,
  onClose,
  onSave,
}: Readonly<{ busy: boolean; onClose: () => void; onSave: () => void }>) {
  return (
    <>
      <Button variant="secondary" onClick={onClose}>
        Cancel
      </Button>
      <Button busy={busy} onClick={onSave}>
        Submit for Authorization
      </Button>
    </>
  );
}

/** Status attributes: phase, waiting party, follow-up days, awaiting remittance (FR-CL-040). */
export function StatusAttributesDialog({
  value,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps<StatusAttributesInput>>) {
  const [form, setForm] = useState<StatusAttributesInput>(() => statusForm(value));
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? validateStatusForm(form) : {};
  const save = () => {
    setSubmitted(true);
    if (Object.keys(validateStatusForm(form)).length === 0) {
      onSave({ ...form, followUpDays: form.followUpDays.trim() });
    }
  };
  return (
    <Modal
      open
      title={`Status Attributes · ${value.label}`}
      onClose={onClose}
      footer={<Footer busy={busy} onClose={onClose} onSave={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="form-grid">
          <Field label="Phase" required error={errors.phase}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.phase}
                onChange={(e) => setForm({ ...form, phase: e.target.value })}
              >
                <option value="">Select…</option>
                {STATUS_PHASES.map((p) => (
                  <option key={p.code} value={p.code}>
                    {p.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Waiting on" required error={errors.waitingOn}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.waitingOn}
                onChange={(e) => setForm({ ...form, waitingOn: e.target.value })}
              >
                <option value="">Select…</option>
                {WAITING_ON.map((w) => (
                  <option key={w.code} value={w.code}>
                    {w.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field
            label="Follow-up days"
            error={errors.followUpDays}
            hint="1-365; blank = parameter BCL_FOLLOW_UP_DAYS."
          >
            {(id) => (
              <input
                id={id}
                className="input"
                inputMode="numeric"
                value={form.followUpDays}
                onChange={(e) => setForm({ ...form, followUpDays: e.target.value })}
              />
            )}
          </Field>
        </div>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.awaitingPremiumRemittance}
            onChange={(e) => setForm({ ...form, awaitingPremiumRemittance: e.target.checked })}
          />
          Awaiting premium remittance (feeds the claims special remittance)
        </label>
      </div>
    </Modal>
  );
}

/** Settlement type attributes: outcome, closes the claim, amount required (FR-CL-043). */
export function SettlementAttributesDialog({
  value,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps<SettlementAttributesInput>>) {
  const [form, setForm] = useState<SettlementAttributesInput>(() => settlementForm(value));
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? validateSettlementForm(form) : {};
  const save = () => {
    setSubmitted(true);
    if (Object.keys(validateSettlementForm(form)).length === 0) {
      onSave(form);
    }
  };
  return (
    <Modal
      open
      title={`Settlement Type Attributes · ${value.label}`}
      onClose={onClose}
      footer={<Footer busy={busy} onClose={onClose} onSave={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Outcome" required error={errors.outcome}>
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.outcome}
              onChange={(e) => setForm({ ...form, outcome: e.target.value })}
            >
              <option value="">Select…</option>
              {OUTCOMES.map((o) => (
                <option key={o.code} value={o.code}>
                  {o.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.closesClaim}
            onChange={(e) => setForm({ ...form, closesClaim: e.target.checked })}
          />
          Closes the claim permanently (needs BCL_CLOSE)
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.requiresSettlementAmount}
            onChange={(e) => setForm({ ...form, requiresSettlementAmount: e.target.checked })}
          />
          Requires the settlement amount and the date settled
        </label>
      </div>
    </Modal>
  );
}
