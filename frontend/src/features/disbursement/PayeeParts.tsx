import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { disbursementApi } from './api';
import type { AccountInput, Mode, Payee } from './api';
import { MODE_LABELS, MODES } from './labels';
import { EMPTY_ACCOUNT, accountErrors } from './payeeForm';
import type { PayeeForm } from './payeeForm';
import { DialogFooter } from './VoucherDialogs';
import './disbursement.css';

/** The payee fields (DIS 2.2.2): class, name, contact, TIN, modes, currency and types. */
export function PayeeFields({
  form,
  errors,
  creating,
  disabled,
  onChange,
}: Readonly<{
  form: PayeeForm;
  errors: Record<string, string>;
  creating: boolean;
  disabled: boolean;
  onChange: (patch: Partial<PayeeForm>) => void;
}>) {
  const text = (
    key: 'name' | 'address' | 'email' | 'tin' | 'defaultCostCenter',
    label: string,
    required = false,
  ) => (
    <Field label={label} required={required} error={errors[key]}>
      {(id) => (
        <input
          id={id}
          className="input"
          disabled={disabled}
          value={form[key]}
          onChange={(e) => onChange({ [key]: e.target.value })}
        />
      )}
    </Field>
  );
  const toggle = (mode: Mode) =>
    onChange({
      allowedModes: form.allowedModes.includes(mode)
        ? form.allowedModes.filter((m) => m !== mode)
        : [...form.allowedModes, mode],
    });
  return (
    <div className="dsb-form">
      <Field
        label="Payee Code"
        required
        error={errors.payeeCode}
        hint="The party code of the client, insurer, supplier or employee."
      >
        {(id) => (
          <input
            id={id}
            className="input"
            disabled={!creating}
            value={form.payeeCode}
            onChange={(e) => onChange({ payeeCode: e.target.value.toUpperCase() })}
          />
        )}
      </Field>
      <Field label="Payee Class" required error={errors.payeeClass}>
        {(id) => (
          <LovSelect
            id={id}
            type="PAYEE_CLASS"
            value={form.payeeClass}
            disabled={disabled}
            onChange={(payeeClass) => onChange({ payeeClass })}
          />
        )}
      </Field>
      {text('name', 'Name', true)}
      {text('tin', 'TIN')}
      {text('email', 'E-mail')}
      {text('address', 'Address')}
      <Field label="Default Mode" required error={errors.defaultMode}>
        {(id) => (
          <select
            id={id}
            className="select"
            disabled={disabled}
            value={form.defaultMode}
            onChange={(e) => onChange({ defaultMode: e.target.value as Mode })}
          >
            {MODES.map((m) => (
              <option key={m} value={m}>
                {MODE_LABELS[m]}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Currency" required error={errors.currency}>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={3}
            disabled={disabled}
            value={form.currency}
            onChange={(e) => onChange({ currency: e.target.value.toUpperCase() })}
          />
        )}
      </Field>
      {text('defaultCostCenter', 'Default Cost Centre')}
      <fieldset className="dsb-form-wide dsb-modes" disabled={disabled}>
        <legend>Allowed Modes</legend>
        {MODES.map((m) => (
          <label key={m} className="checkbox">
            <input
              type="checkbox"
              checked={form.allowedModes.includes(m)}
              onChange={() => toggle(m)}
            />
            {MODE_LABELS[m]}
          </label>
        ))}
      </fieldset>
      <div className="dsb-form-wide">
        <Field label="Remarks">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              maxLength={500}
              disabled={disabled}
              value={form.remarks}
              onChange={(e) => onChange({ remarks: e.target.value })}
            />
          )}
        </Field>
      </div>
    </div>
  );
}

/** The inputs of a payee bank account. */
export function AccountFields({
  value,
  errors,
  onChange,
}: Readonly<{
  value: AccountInput;
  errors: Record<string, string>;
  onChange: (a: AccountInput) => void;
}>) {
  const text = (
    key: 'bankName' | 'bankBranch' | 'accountNo' | 'accountName',
    label: string,
    required = false,
  ) => (
    <Field label={label} required={required} error={errors[key]}>
      {(id) => (
        <input
          id={id}
          className="input"
          value={value[key] ?? ''}
          onChange={(e) => onChange({ ...value, [key]: e.target.value })}
        />
      )}
    </Field>
  );
  return (
    <div className="dsb-form">
      {text('bankName', 'Bank', true)}
      {text('bankBranch', 'Branch')}
      {text('accountNo', 'Account No.', true)}
      {text('accountName', 'Account Name', true)}
      <Field label="Currency" required>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={3}
            value={value.currency}
            onChange={(e) => onChange({ ...value, currency: e.target.value.toUpperCase() })}
          />
        )}
      </Field>
      <Field label="Mode" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={value.mode}
            onChange={(e) => onChange({ ...value, mode: e.target.value as Mode })}
          >
            {MODES.map((m) => (
              <option key={m} value={m}>
                {MODE_LABELS[m]}
              </option>
            ))}
          </select>
        )}
      </Field>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={value.primary}
          onChange={(e) => onChange({ ...value, primary: e.target.checked })}
        />
        Primary account
      </label>
    </div>
  );
}

/** Adds a bank account to a maintained payee (DIS 2.2.4); the change is authorised again. */
export function AccountDialog({
  payee,
  onClose,
  onDone,
}: Readonly<{ payee: Payee; onClose: () => void; onDone: (p: Payee) => void }>) {
  const [account, setAccount] = useState<AccountInput>({
    ...EMPTY_ACCOUNT,
    currency: payee.summary.currency,
  });
  const [touched, setTouched] = useState(false);
  const errors = accountErrors(account);
  const add = useMutation({
    mutationFn: () => disbursementApi.addAccount(payee.summary.id, account),
    onSuccess: onDone,
  });
  const confirm = () => {
    setTouched(true);
    if (Object.keys(errors).length === 0) {
      add.mutate();
    }
  };
  return (
    <Modal
      open
      title="Add Bank Account"
      onClose={onClose}
      footer={
        <DialogFooter
          label="Add Account"
          busy={add.isPending}
          onClose={onClose}
          onConfirm={confirm}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={add.error} />
        <AccountFields value={account} errors={touched ? errors : {}} onChange={setAccount} />
      </div>
    </Modal>
  );
}
