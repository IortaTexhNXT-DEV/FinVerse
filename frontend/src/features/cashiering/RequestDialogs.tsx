import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { formatAmount, humanize } from '@/utils/format';
import { cashieringApi } from './cashieringApi';
import { TextField } from './CashFields';
import type { AcceptBody, AcceptForm, CollectorRequest, RefundValidation } from './requestsApi';
import { DEFAULT_TYPE, acceptBody, acceptErrors, requestsApi } from './requestsApi';

/** Dialogs of the requests Cashiering receives (wave C1-C). */

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
      <Button variant="accent" busy={busy} onClick={onConfirm}>
        {label}
      </Button>
    </>
  );
}

/**
 * Accepts a collector request (BRCLXN.030-033): the disposition type (the default of the
 * collector's action), the fields the collector could not give, and whether to process it at once.
 */
export function AcceptRequestDialog({
  request,
  busy,
  error,
  onClose,
  onAccept,
}: Readonly<{
  request: CollectorRequest;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onAccept: (body: AcceptBody) => void;
}>) {
  const types = useQuery({
    queryKey: ['cashiering', 'disposition-types'],
    queryFn: cashieringApi.dispositionTypes,
  });
  const [form, setForm] = useState<AcceptForm>({
    dispositionType: DEFAULT_TYPE[request.action],
    amount: '',
    targetClientCode: '',
    targetUnit: '',
    payeeName: '',
    remarks: request.remarks ?? '',
    submit: request.action === 'APPLY_TO_INVOICE',
  });
  const [errors, setErrors] = useState<Partial<Record<keyof AcceptForm, string>>>({});
  const set = (patch: Partial<AcceptForm>) => setForm((f) => ({ ...f, ...patch }));
  const accept = () => {
    const found = acceptErrors(request, form);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onAccept(acceptBody(form));
    }
  };
  return (
    <Modal
      title={`Accept ${request.requestNo}`}
      open
      onClose={onClose}
      footer={<Footer label="Accept Request" busy={busy} onClose={onClose} onConfirm={accept} />}
    >
      <div className="stack">
        <ErrorAlert error={types.error ?? error} />
        <p className="muted">
          {humanize(request.action)} of {request.unappliedRef}
          {request.invoiceNo === undefined ? '' : ` to ${request.invoiceNo}`}, balance{' '}
          {request.currency} {formatAmount(request.balance)}, requested by {request.requestedBy}
        </p>
        <Field label="Disposition Type" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.dispositionType}
              onChange={(e) => set({ dispositionType: e.target.value })}
            >
              {(types.data ?? []).map((t) => (
                <option key={t.code} value={t.code}>
                  {t.description}
                </option>
              ))}
            </select>
          )}
        </Field>
        <TextField
          label="Amount"
          type="number"
          value={form.amount}
          onChange={(v) => set({ amount: v })}
          error={errors.amount}
          hint="Leave empty for the requested amount or the whole balance"
        />
        {request.action === 'RECLASS' && (
          <TextField
            label="Client to Reclass To"
            required
            value={form.targetClientCode}
            onChange={(v) => set({ targetClientCode: v })}
            error={errors.targetClientCode}
            maxLength={30}
          />
        )}
        {request.action === 'TRANSFER' && (
          <TextField
            label="Marketing Unit"
            required
            value={form.targetUnit}
            onChange={(v) => set({ targetUnit: v })}
            error={errors.targetUnit}
            maxLength={40}
          />
        )}
        {request.action === 'REFUND' && (
          <TextField
            label="Refund Payee"
            value={form.payeeName}
            onChange={(v) => set({ payeeName: v })}
            hint="Leave empty for the payor"
            maxLength={250}
          />
        )}
        <TextField
          label="Remarks"
          value={form.remarks}
          onChange={(v) => set({ remarks: v })}
          maxLength={250}
        />
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.submit}
            onChange={(e) => set({ submit: e.target.checked })}
          />
          Submit the disposition now (an application is processed at once)
        </label>
      </div>
    </Modal>
  );
}

/**
 * Confirms a refund validation (MKT 1.11.0): the unapplied item that holds the returned premium,
 * with the new AR number (the item's AR when empty).
 */
export function ConfirmValidationDialog({
  task,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<{
  task: RefundValidation;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onConfirm: (unappliedId: number, newArNo?: string, remarks?: string) => void;
}>) {
  const candidates = useQuery({
    queryKey: ['cashiering', 'validation-candidates', task.id],
    queryFn: () => requestsApi.candidates(task.id),
  });
  const [item, setItem] = useState('');
  const [arNo, setArNo] = useState('');
  const [remarks, setRemarks] = useState('');
  const [missing, setMissing] = useState<string>();
  const confirm = () => {
    if (item === '') {
      setMissing('Choose the unapplied item that holds the returned premium');
      return;
    }
    onConfirm(Number(item), arNo.trim() || undefined, remarks.trim() || undefined);
  };
  return (
    <Modal
      title={`Confirm ${task.taskNo}`}
      open
      onClose={onClose}
      footer={
        <Footer label="Confirm Validation" busy={busy} onClose={onClose} onConfirm={confirm} />
      }
    >
      <div className="stack">
        <ErrorAlert error={candidates.error ?? error} />
        <p className="muted">
          Refund of {task.currency} {formatAmount(task.amount)} for{' '}
          {task.invoiceNo ?? task.clientCode} ({task.sourceModule} {task.sourceRef})
        </p>
        <Field label="Unapplied Item" required error={missing}>
          {(id) => (
            <select
              id={id}
              className="select"
              value={item}
              onChange={(e) => setItem(e.target.value)}
            >
              <option value="">Choose…</option>
              {(candidates.data ?? []).map((u) => (
                <option key={u.id} value={u.id}>
                  {u.reference} · {u.payorName ?? u.clientCode} · {u.currency}{' '}
                  {formatAmount(u.balance)}
                </option>
              ))}
            </select>
          )}
        </Field>
        <TextField
          label="New AR No."
          value={arNo}
          onChange={setArNo}
          maxLength={40}
          hint="Leave empty for the AR of the item"
        />
        <TextField label="Remarks" value={remarks} onChange={setRemarks} maxLength={250} />
      </div>
    </Modal>
  );
}

/** A rejection with its reason (collector request, refund validation, payment reversal). */
export function RejectDialog({
  title,
  busy,
  error,
  onClose,
  onReject,
}: Readonly<{
  title: string;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onReject: (reason: string) => void;
}>) {
  const [reason, setReason] = useState('');
  const [missing, setMissing] = useState<string>();
  const reject = () => {
    if (reason.trim() === '') {
      setMissing('Enter the reason');
      return;
    }
    onReject(reason.trim());
  };
  return (
    <Modal
      title={title}
      open
      onClose={onClose}
      footer={<Footer label="Reject" busy={busy} onClose={onClose} onConfirm={reject} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <TextField
          label="Reason"
          required
          value={reason}
          onChange={setReason}
          error={missing}
          maxLength={250}
        />
      </div>
    </Modal>
  );
}
