import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { TextField } from './CashFields';
import type { ReceiptSummary, ReinstateBody } from './cashieringApi';
import { reinstateErrors } from './cashieringLogic';

type Group = 'PREMIUM' | 'DIRECT_PAYMENT';

const TEXT_FIELDS: readonly [keyof ReinstateBody, string, boolean][] = [
  ['invoiceNo', 'Invoice No.', false],
  ['documentNo', 'AR / OR No.', false],
  ['payorName', 'Assured / Payor', false],
  ['accountOfficer', 'Account Officer', true],
  ['unitHead', 'Unit Head', true],
  ['teamLeader', 'Team Leader', true],
];

/**
 * Reinstatement of a cancelled receipt (CSHID.004/005): full or partial, a reason of the
 * premium or direct-payment group and the encoded fields the group needs. The request goes to the
 * checker for approval.
 */
export function ReinstateDialog({
  receipt,
  busy,
  error,
  onSubmit,
  onClose,
}: Readonly<{
  receipt: ReceiptSummary;
  busy: boolean;
  error: unknown;
  onSubmit: (body: ReinstateBody) => void;
  onClose: () => void;
}>) {
  const [group, setGroup] = useState<Group>(receipt.kind === 'AR' ? 'PREMIUM' : 'DIRECT_PAYMENT');
  const [body, setBody] = useState<ReinstateBody>({
    full: true,
    reasonCode: '',
    documentNo: receipt.receiptNo,
    payorName: receipt.assuredName ?? receipt.payorName,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const premium = group === 'PREMIUM';
  const set = (patch: Partial<ReinstateBody>) => setBody((b) => ({ ...b, ...patch }));
  const submit = () => {
    const found = reinstateErrors(body, receipt.amount, premium);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSubmit(body);
    }
  };
  return (
    <Modal
      open
      title={`Reinstate ${receipt.receiptNo}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={submit}>
            Submit for Approval
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="row" role="radiogroup" aria-label="Reinstatement">
          <label className="checkbox">
            <input
              type="radio"
              checked={body.full}
              onChange={() => set({ full: true, amount: undefined })}
            />
            Full ({receipt.amount.toFixed(2)})
          </label>
          <label className="checkbox">
            <input type="radio" checked={!body.full} onChange={() => set({ full: false })} />
            Partial
          </label>
        </div>
        <div className="form-grid">
          <Field label="Reason Group" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={group}
                onChange={(e) => {
                  setGroup(e.target.value as Group);
                  set({ reasonCode: '' });
                }}
              >
                <option value="PREMIUM">Premium</option>
                <option value="DIRECT_PAYMENT">Direct Payment</option>
              </select>
            )}
          </Field>
          <Field label="Reason" required error={errors.reasonCode}>
            {(id) => (
              <LovSelect
                id={id}
                type="REINSTATEMENT_REASON"
                parentCode={group}
                value={body.reasonCode}
                onChange={(reasonCode) => set({ reasonCode })}
              />
            )}
          </Field>
          {!body.full && (
            <TextField
              label="Amount to Reinstate"
              type="number"
              required
              value={String(body.amount ?? '')}
              onChange={(v) => set({ amount: v === '' ? undefined : Number(v) })}
              error={errors.amount}
            />
          )}
          {TEXT_FIELDS.filter(([, , premiumOnly]) => premium || !premiumOnly).map(
            ([key, label]) => (
              <TextField
                key={key}
                label={label}
                required
                value={String(body[key] ?? '')}
                onChange={(v) => set({ [key]: v })}
                error={errors[key]}
                maxLength={250}
              />
            ),
          )}
          <TextField
            label="Remarks"
            value={body.reasonText ?? ''}
            onChange={(reasonText) => set({ reasonText })}
            maxLength={250}
          />
        </div>
      </div>
    </Modal>
  );
}
