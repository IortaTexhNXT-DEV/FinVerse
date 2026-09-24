import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import type { HoldInput } from './api';
import { holdFormErrors } from './remittanceLabels';

interface DialogProps {
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

function Footer({
  label,
  busy,
  onClose,
  onConfirm,
}: Readonly<{ label: string; busy: boolean; onClose: () => void; onConfirm: () => void }>) {
  return (
    <>
      <Button variant="secondary" onClick={onClose}>
        Cancel
      </Button>
      <Button busy={busy} onClick={onConfirm}>
        {label}
      </Button>
    </>
  );
}

/** A new hold request (MKTID.003): invoice, reason, hold-until date and remarks. */
export function NewHoldDialog({
  companyId,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps & { companyId: number; onSave: (input: HoldInput) => void }>) {
  const [form, setForm] = useState({ invoiceNo: '', reasonCode: '', holdUntil: '', remarks: '' });
  const [submit, setSubmit] = useState(true);
  const [errors, setErrors] = useState<ReturnType<typeof holdFormErrors>>({});
  const set = (key: keyof typeof form, value: string) => setForm({ ...form, [key]: value });
  const save = () => {
    const found = holdFormErrors(form, today());
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSave({
        companyId,
        invoiceNo: form.invoiceNo.trim(),
        reasonCode: form.reasonCode,
        holdUntil: form.holdUntil,
        remarks: form.remarks.trim() || undefined,
        submit,
      });
    }
  };
  return (
    <Modal
      title="New Hold Request"
      open
      onClose={onClose}
      footer={
        <Footer
          label={submit ? 'Submit Hold Request' : 'Save Draft'}
          busy={busy}
          onClose={onClose}
          onConfirm={save}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Invoice No." required error={errors.invoiceNo}>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={40}
              value={form.invoiceNo}
              onChange={(e) => set('invoiceNo', e.target.value)}
            />
          )}
        </Field>
        <Field label="Reason" required error={errors.reasonCode}>
          {(id) => (
            <LovSelect
              id={id}
              type="HOLD_REASON"
              value={form.reasonCode}
              onChange={(code) => set('reasonCode', code)}
              required
            />
          )}
        </Field>
        <Field label="Hold Until" required error={errors.holdUntil}>
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={form.holdUntil}
              onChange={(e) => set('holdUntil', e.target.value)}
            />
          )}
        </Field>
        <Field label="Remarks">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={500}
              value={form.remarks}
              onChange={(e) => set('remarks', e.target.value)}
            />
          )}
        </Field>
        <label className="checkbox-field">
          <input type="checkbox" checked={submit} onChange={(e) => setSubmit(e.target.checked)} />{' '}
          Submit for approval now
        </label>
      </div>
    </Modal>
  );
}

/** A new hold-until date (MKTID.005). */
export function ExtendDialog({
  current,
  busy,
  error,
  onClose,
  onExtend,
}: Readonly<
  DialogProps & { current: string; onExtend: (until: string, comment?: string) => void }
>) {
  const [until, setUntil] = useState('');
  const [comment, setComment] = useState('');
  const [invalid, setInvalid] = useState<string>();
  return (
    <Modal
      title="Request Extension"
      open
      onClose={onClose}
      footer={
        <Footer
          label="Request Extension"
          busy={busy}
          onClose={onClose}
          onConfirm={() => {
            if (until === '' || until <= current) {
              setInvalid(`Choose a date after ${current}`);
              return;
            }
            onExtend(until, comment.trim() || undefined);
          }}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="New Hold Until" required error={invalid}>
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={until}
              onChange={(e) => {
                setUntil(e.target.value);
                setInvalid(undefined);
              }}
            />
          )}
        </Field>
        <Field label="Comment">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Assignment of an approved hold to a remittance processor (MKTID.004). */
export function AssignDialog({
  busy,
  error,
  onClose,
  onAssign,
}: Readonly<DialogProps & { onAssign: (username: string) => void }>) {
  const [username, setUsername] = useState('');
  const [invalid, setInvalid] = useState<string>();
  return (
    <Modal
      title="Assign to Processor"
      open
      onClose={onClose}
      footer={
        <Footer
          label="Assign"
          busy={busy}
          onClose={onClose}
          onConfirm={() => {
            if (username.trim() === '') {
              setInvalid('User ID is required');
              return;
            }
            onAssign(username.trim());
          }}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Remittance Processor (User ID)" required error={invalid}>
          {(id) => (
            <input
              id={id}
              className="input"
              value={username}
              onChange={(e) => {
                setUsername(e.target.value);
                setInvalid(undefined);
              }}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
