import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import { claimStatusApi } from './api';
import type { SettlementInput } from './api';
import { PHASE_LABELS, validateFollowUp, validateSettlement } from './statusLogic';
import type { SettlementForm } from './statusLogic';

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

/** Change Status: the statuses the matrix allows, with a remark (FR-CL-041/042). */
export function ChangeStatusDialog({
  claimId,
  companyId,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<
  DialogProps & {
    claimId: number;
    companyId: number;
    onSave: (statusCode: string, remark: string) => void;
  }
>) {
  const options = useQuery({
    queryKey: ['broker-claims', 'allowed-statuses', claimId],
    queryFn: () => claimStatusApi.allowedStatuses(claimId, companyId),
  });
  const [statusCode, setStatusCode] = useState('');
  const [remark, setRemark] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const missing = submitted && statusCode === '' ? 'Select the new status' : undefined;
  return (
    <Modal
      open
      title="Change Status"
      onClose={onClose}
      footer={
        <Footer
          label="Change Status"
          busy={busy}
          onClose={onClose}
          onConfirm={() => {
            setSubmitted(true);
            if (statusCode !== '') {
              onSave(statusCode, remark.trim());
            }
          }}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error ?? options.error} />
        <Field label="New status" required error={missing}>
          {(id) => (
            <select
              id={id}
              className="select"
              value={statusCode}
              onChange={(e) => setStatusCode(e.target.value)}
            >
              <option value="">Select…</option>
              {(options.data ?? []).map((o) => (
                <option key={o.code} value={o.code}>
                  {o.label} ({PHASE_LABELS[o.phase]})
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Remark" hint="Up to 500 characters; kept in the status history.">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={500}
              value={remark}
              onChange={(e) => setRemark(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Set Settlement: type, amount and date settled; a closing type closes the claim (FR-CL-044). */
export function SettlementDialog({
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps & { onSave: (input: SettlementInput) => void }>) {
  const [form, setForm] = useState<SettlementForm>({
    typeCode: '',
    amount: '',
    dateSettled: '',
    remark: '',
  });
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? validateSettlement(form, today()) : {};
  const set = (key: keyof SettlementForm, value: string) =>
    setForm((f) => ({ ...f, [key]: value }));
  const save = () => {
    setSubmitted(true);
    if (Object.keys(validateSettlement(form, today())).length === 0) {
      onSave({
        typeCode: form.typeCode,
        amount: form.amount === '' ? undefined : Number(form.amount),
        dateSettled: form.dateSettled || undefined,
        remark: form.remark.trim() || undefined,
      });
    }
  };
  return (
    <Modal
      open
      title="Set Settlement"
      onClose={onClose}
      footer={<Footer label="Save Settlement" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Requested type of settlement" required error={errors.typeCode}>
          {(id) => (
            <LovSelect
              id={id}
              type="BCL_SETTLEMENT_TYPE"
              value={form.typeCode}
              onChange={(v) => set('typeCode', v)}
              required
            />
          )}
        </Field>
        <div className="form-grid">
          <Field label="Settlement amount" error={errors.amount} hint="Required for settled types.">
            {(id) => (
              <input
                id={id}
                className="input"
                type="number"
                min="0"
                step="0.01"
                value={form.amount}
                onChange={(e) => set('amount', e.target.value)}
              />
            )}
          </Field>
          <Field label="Date settled" error={errors.dateSettled}>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                max={today()}
                value={form.dateSettled}
                onChange={(e) => set('dateSettled', e.target.value)}
              />
            )}
          </Field>
        </div>
        <Field label="Remark">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              maxLength={500}
              value={form.remark}
              onChange={(e) => set('remark', e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Override Follow-up Date with a reason (FR-CL-050). */
export function FollowUpDialog({
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps & { onSave: (date: string, reasonCode: string) => void }>) {
  const [date, setDate] = useState('');
  const [reason, setReason] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? validateFollowUp(date, reason, today()) : {};
  const save = () => {
    setSubmitted(true);
    if (Object.keys(validateFollowUp(date, reason, today())).length === 0) {
      onSave(date, reason);
    }
  };
  return (
    <Modal
      open
      title="Override Follow-up Date"
      onClose={onClose}
      footer={<Footer label="Override Date" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Next follow-up date" required error={errors.date}>
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              min={today()}
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          )}
        </Field>
        <Field label="Reason" required error={errors.reasonCode}>
          {(id) => (
            <LovSelect
              id={id}
              type="BCL_OVERRIDE_REASON"
              value={reason}
              onChange={setReason}
              required
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/** Assign Adjuster: the adjuster / appraiser of the claim (BRCLM.018). */
export function AdjusterDialog({
  current,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<
  DialogProps & { current?: string; onSave: (adjusterCode: string, remark: string) => void }
>) {
  const [code, setCode] = useState(current ?? '');
  const [remark, setRemark] = useState('');
  return (
    <Modal
      open
      title="Assign Adjuster"
      onClose={onClose}
      footer={
        <Footer
          label="Save Adjuster"
          busy={busy}
          onClose={onClose}
          onConfirm={() => onSave(code, remark.trim())}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Adjuster / appraiser" hint="Leave empty to remove the adjuster.">
          {(id) => (
            <LovSelect
              id={id}
              type="BCL_ADJUSTER"
              value={code}
              onChange={setCode}
              placeholder="No adjuster"
            />
          )}
        </Field>
        <Field label="Remark">
          {(id) => (
            <input
              id={id}
              className="input"
              value={remark}
              onChange={(e) => setRemark(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
