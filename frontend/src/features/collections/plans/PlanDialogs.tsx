import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import type { NewPlan } from './api';
import type { PlanForm, PromiseForm } from './labels';
import {
  optionalAmount,
  optionalText,
  parseInvoiceList,
  planFormErrors,
  promiseFormErrors,
} from './labels';
import { DialogFooter, InputField, TextAreaField } from './Parts';

interface DialogProps {
  busy: boolean;
  error: unknown;
  onClose: () => void;
}

/**
 * A new installment plan (BRCLXN.053/058): over the policy years of a multi-year account, or the
 * outstanding premium of one invoice split in equal installments.
 */
export function NewPlanDialog({
  companyId,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps & { companyId: number; onSave: (plan: NewPlan) => void }>) {
  const [form, setForm] = useState<PlanForm>({
    kind: 'POLICY_YEARS',
    arn: '',
    invoiceNo: '',
    frequency: 'ANNUAL',
    firstDue: today(),
    count: '4',
  });
  const [remarks, setRemarks] = useState('');
  const [errors, setErrors] = useState<ReturnType<typeof planFormErrors>>({});
  const set = (key: keyof PlanForm, value: string) => setForm({ ...form, [key]: value });
  const save = () => {
    const found = planFormErrors(form);
    setErrors(found);
    if (Object.keys(found).length > 0) {
      return;
    }
    onSave(
      form.kind === 'POLICY_YEARS'
        ? {
            kind: 'POLICY_YEARS',
            companyId,
            arn: form.arn.trim(),
            frequency: form.frequency,
            remarks: optionalText(remarks),
          }
        : {
            kind: 'GENERATED',
            companyId,
            invoiceNo: form.invoiceNo.trim(),
            frequency: form.frequency,
            firstDue: form.firstDue,
            count: Number(form.count),
            remarks: optionalText(remarks),
          },
    );
  };
  const policyYears = form.kind === 'POLICY_YEARS';
  return (
    <Modal
      title="New Installment Plan"
      open
      onClose={onClose}
      footer={<DialogFooter label="Create Plan" busy={busy} onClose={onClose} onConfirm={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Plan Basis" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.kind}
              onChange={(e) => set('kind', e.target.value)}
            >
              <option value="POLICY_YEARS">Policy years of a multi-year account</option>
              <option value="GENERATED">Installments of one invoice</option>
            </select>
          )}
        </Field>
        {policyYears ? (
          <InputField
            label="Account Reference No. (ARN)"
            value={form.arn}
            onChange={(v) => set('arn', v)}
            required
            error={errors.arn}
            hint="Every policy year, booked or scheduled, is billed per cycle"
          />
        ) : (
          <>
            <InputField
              label="Invoice No."
              value={form.invoiceNo}
              onChange={(v) => set('invoiceNo', v)}
              required
              error={errors.invoiceNo}
              hint="Its outstanding premium is split in equal installments"
            />
            <InputField
              label="First Due Date"
              type="date"
              value={form.firstDue}
              onChange={(v) => set('firstDue', v)}
              required
              error={errors.firstDue}
            />
            <InputField
              label="Number of Installments"
              type="number"
              value={form.count}
              onChange={(v) => set('count', v)}
              required
              error={errors.count}
            />
          </>
        )}
        <Field label="Billing Frequency" required error={errors.frequency}>
          {(id) => (
            <LovSelect
              id={id}
              type="CLX_BILLING_FREQUENCY"
              value={form.frequency}
              onChange={(code) => set('frequency', code)}
              required
            />
          )}
        </Field>
        <TextAreaField label="Remarks" value={remarks} onChange={setRemarks} />
      </div>
    </Modal>
  );
}

/** What the promise dialog saves: one invoice, or the same promise on several. */
export interface PromiseDraft {
  invoiceNos: string[];
  promisedOn?: string;
  promisedDate: string;
  amount?: number;
  installmentId?: number;
  remarks?: string;
}

/**
 * A promise to pay (BRCLXN.055): the invoices (several at once need the bulk update permission),
 * the day of the promise, the promised date and amount (the whole outstanding when empty).
 */
export function PromiseDialog({
  initialInvoice = '',
  installmentId,
  allowMany,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<
  DialogProps & {
    initialInvoice?: string;
    installmentId?: number;
    allowMany: boolean;
    onSave: (draft: PromiseDraft) => void;
  }
>) {
  const [form, setForm] = useState<PromiseForm>({
    invoiceNo: initialInvoice,
    promisedOn: today(),
    promisedDate: '',
    amount: '',
  });
  const [remarks, setRemarks] = useState('');
  const [errors, setErrors] = useState<ReturnType<typeof promiseFormErrors>>({});
  const set = (key: keyof PromiseForm, value: string) => setForm({ ...form, [key]: value });
  const save = () => {
    const found = promiseFormErrors(form, today());
    const invoiceNos = parseInvoiceList(form.invoiceNo);
    if (!allowMany && invoiceNos.length > 1) {
      found.invoiceNo = 'Enter one invoice number';
    }
    setErrors(found);
    if (Object.keys(found).length === 0) {
      onSave({
        invoiceNos,
        promisedOn: optionalText(form.promisedOn),
        promisedDate: form.promisedDate,
        amount: optionalAmount(form.amount),
        installmentId,
        remarks: optionalText(remarks),
      });
    }
  };
  return (
    <Modal
      title="Record Promise to Pay"
      open
      onClose={onClose}
      footer={
        <DialogFooter label="Record Promise" busy={busy} onClose={onClose} onConfirm={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {allowMany ? (
          <TextAreaField
            label="Invoice Nos."
            value={form.invoiceNo}
            onChange={(v) => set('invoiceNo', v)}
            required
            error={errors.invoiceNo}
            hint="One per line; the same promise is recorded on each invoice"
          />
        ) : (
          <InputField
            label="Invoice No."
            value={form.invoiceNo}
            onChange={(v) => set('invoiceNo', v)}
            required
            error={errors.invoiceNo}
          />
        )}
        <InputField
          label="Promised On"
          type="date"
          value={form.promisedOn}
          onChange={(v) => set('promisedOn', v)}
          error={errors.promisedOn}
          hint="The day the client made the promise"
        />
        <InputField
          label="Promised Payment Date"
          type="date"
          value={form.promisedDate}
          onChange={(v) => set('promisedDate', v)}
          required
          error={errors.promisedDate}
        />
        <InputField
          label="Promised Amount"
          type="number"
          value={form.amount}
          onChange={(v) => set('amount', v)}
          error={errors.amount}
          hint="Leave empty for the whole outstanding premium"
        />
        <TextAreaField label="Remarks" value={remarks} onChange={setRemarks} />
      </div>
    </Modal>
  );
}
