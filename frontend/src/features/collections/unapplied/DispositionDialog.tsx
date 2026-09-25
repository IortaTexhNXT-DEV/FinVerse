import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { formatAmount } from '@/utils/format';
import { DialogFooter, InputField, TextAreaField } from '../plans/Parts';
import type { DispositionDraft, UnappliedRow } from './api';
import { unappliedApi } from './api';
import type { DispositionErrors, DispositionForm } from './labels';
import { ACTION_LABELS, dispositionErrors, toDraft } from './labels';

/**
 * Records a collector disposition on an unapplied payment (BRCLXN.031/033): the active values of
 * CLX_UPP_DISPOSITION with what Cashiering is asked to do; the invoice number appears and is
 * mandatory when the value requires it ("For application to invoice", BRCLXN.047/048).
 */
export function DispositionDialog({
  item,
  initialCode,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  item: Pick<UnappliedRow, 'unappliedRef' | 'balance' | 'currency' | 'invoiceNo'>;
  initialCode?: string;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (draft: DispositionDraft) => void;
}>) {
  const rules = useQuery({
    queryKey: ['collections', 'unapplied', 'rules'],
    queryFn: unappliedApi.rules,
  });
  const [form, setForm] = useState<DispositionForm>({
    dispositionCode: initialCode ?? '',
    invoiceNo: item.invoiceNo ?? '',
    amount: '',
    remarks: '',
  });
  const [errors, setErrors] = useState<DispositionErrors>({});
  const rule = rules.data?.rules.find((r) => r.code === form.dispositionCode);
  const set = (patch: Partial<DispositionForm>) => setForm((f) => ({ ...f, ...patch }));
  const save = () => {
    const found = dispositionErrors(form, rule, rules.data?.invoicePattern ?? '', item.balance);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSave(toDraft(form));
    }
  };
  const confirmLabel =
    rule?.cashieringAction === 'APPLY_TO_INVOICE' ? 'Request Application' : 'Save Disposition';
  return (
    <Modal
      title={`Disposition of ${item.unappliedRef}`}
      open
      onClose={onClose}
      footer={<DialogFooter label={confirmLabel} busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={rules.error ?? error} />
        <p className="muted">
          Unapplied balance {item.currency} {formatAmount(item.balance)}
        </p>
        <Field
          label="Disposition"
          required
          error={errors.dispositionCode}
          hint={
            rule === undefined ? undefined : `Cashiering: ${ACTION_LABELS[rule.cashieringAction]}`
          }
        >
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.dispositionCode}
              onChange={(e) => set({ dispositionCode: e.target.value })}
            >
              <option value="">Choose…</option>
              {(rules.data?.rules ?? []).map((r) => (
                <option key={r.code} value={r.code}>
                  {r.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        {(rule?.requiresInvoice === true || form.invoiceNo !== '') && (
          <InputField
            label="Invoice No."
            required={rule?.requiresInvoice === true}
            value={form.invoiceNo}
            onChange={(v) => set({ invoiceNo: v })}
            error={errors.invoiceNo}
            hint="EBIX I######## or BrokerVerse BI-… (checked against the invoice ledger)"
            maxLength={40}
          />
        )}
        <InputField
          label="Amount"
          type="number"
          value={form.amount}
          onChange={(v) => set({ amount: v })}
          error={errors.amount}
          hint="Leave empty for the whole unapplied balance"
        />
        <TextAreaField label="Remarks" value={form.remarks} onChange={(v) => set({ remarks: v })} />
      </div>
    </Modal>
  );
}
