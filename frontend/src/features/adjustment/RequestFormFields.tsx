import type { ReactNode } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Field } from '@/components/ui/Field';
import type { RefundBasis } from './api';
import { AMOUNT_FIELDS, classOfType, modeOf } from './requestForm';
import type { FieldErrors, Mode, RequestForm } from './requestForm';

interface FieldsProps {
  form: RequestForm;
  errors: FieldErrors;
  onChange: (form: RequestForm) => void;
}

const PERIOD_TYPES = new Set(['NF_PERIOD_CHANGE', 'NF_PERIOD_EXTENSION']);

function DateInput({
  id,
  value,
  onChange,
}: Readonly<{ id: string; value: string; onChange: (v: string) => void }>) {
  return (
    <input
      id={id}
      className="input"
      type="date"
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}

function NumberInput({
  id,
  value,
  step = '0.01',
  onChange,
}: Readonly<{ id: string; value: string; step?: string; onChange: (v: string) => void }>) {
  return (
    <input
      id={id}
      className="input num"
      type="number"
      step={step}
      value={value}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}

function BasisSelect({ form, onChange }: Readonly<Omit<FieldsProps, 'errors'>>) {
  return (
    <Field
      label="Refund / Premium Basis"
      hint="Remaining term: pro-rata days or short-period table."
    >
      {(id) => (
        <select
          id={id}
          className="select"
          value={form.refundBasis}
          onChange={(e) => onChange({ ...form, refundBasis: e.target.value as RefundBasis })}
        >
          <option value="PRO_RATA">Pro-rata</option>
          <option value="SHORT_PERIOD">Short Period</option>
        </select>
      )}
    </Field>
  );
}

function ModeFields({ form, errors, onChange, mode }: Readonly<FieldsProps & { mode: Mode }>) {
  const parts: ReactNode[] = [];
  if (mode === 'CANCELLATION' || mode === 'TSI') {
    parts.push(<BasisSelect key="basis" form={form} onChange={onChange} />);
  }
  if (mode === 'TSI') {
    parts.push(
      <Field
        key="tsi"
        label="Sum Insured Change"
        required
        error={errors.sumInsuredChange}
        hint="Positive for an increase, negative for a decrease."
      >
        {(id) => (
          <NumberInput
            id={id}
            value={form.sumInsuredChange}
            onChange={(v) => onChange({ ...form, sumInsuredChange: v })}
          />
        )}
      </Field>,
      <Field key="rate" label="Premium Rate (%)" hint="Blank: the product rate.">
        {(id) => (
          <NumberInput
            id={id}
            step="0.0001"
            value={form.ratePercent}
            onChange={(v) => onChange({ ...form, ratePercent: v })}
          />
        )}
      </Field>,
    );
  }
  return <>{parts}</>;
}

/** Amount changes of an amount-change request (signed; the commission is derived when blank). */
export function AmountFields({ form, errors, onChange }: Readonly<FieldsProps>) {
  return (
    <div className="stack">
      <div className="adj-amounts">
        {AMOUNT_FIELDS.map(({ key, label }) => (
          <Field key={key} label={label}>
            {(id) => (
              <NumberInput
                id={id}
                value={form.amounts[key]}
                onChange={(v) => onChange({ ...form, amounts: { ...form.amounts, [key]: v } })}
              />
            )}
          </Field>
        ))}
      </div>
      {errors.amounts !== undefined ? (
        <span className="field-error" role="alert">
          {errors.amounts}
        </span>
      ) : (
        <span className="muted">
          Negative amounts return premium; leave the commission blank to derive it from the invoice
          rate.
        </span>
      )}
    </div>
  );
}

/**
 * Step 2 of a new request (ADJID.002/004): the endorsement type decides the class; the request
 * type decides the inputs (reason and basis of a cancellation, TSI change, amounts).
 */
export function RequestFormFields({ form, errors, onChange }: Readonly<FieldsProps>) {
  const requestClass = classOfType(form.endorsementType);
  const mode = modeOf(requestClass, form.requestType);
  return (
    <div className="stack">
      <div className="form-grid">
        <Field label="Endorsement Type" required error={errors.endorsementType}>
          {(id) => (
            <LovSelect
              id={id}
              type="ENDORSEMENT_TYPE"
              value={form.endorsementType}
              onChange={(v) => onChange({ ...form, endorsementType: v })}
              required
            />
          )}
        </Field>
        {requestClass !== 'NON_FINANCIAL' && (
          <Field
            label="Request Type"
            required={requestClass === 'FINANCIAL'}
            error={errors.requestType}
          >
            {(id) => (
              <LovSelect
                id={id}
                type="ENDORSEMENT_REQUEST_TYPE"
                value={form.requestType}
                placeholder="None"
                onChange={(v) => onChange({ ...form, requestType: v })}
              />
            )}
          </Field>
        )}
        <Field
          label="Reason for Cancellation"
          required={mode === 'CANCELLATION'}
          error={errors.reasonCode}
        >
          {(id) => (
            <LovSelect
              id={id}
              type="CANCELLATION_REASON"
              value={form.reasonCode}
              placeholder="None"
              onChange={(v) => onChange({ ...form, reasonCode: v })}
            />
          )}
        </Field>
        <Field label="Effective Date" required error={errors.effectiveDate}>
          {(id) => (
            <DateInput
              id={id}
              value={form.effectiveDate}
              onChange={(v) => onChange({ ...form, effectiveDate: v })}
            />
          )}
        </Field>
        <Field label="Insurer Endorsement Ref." hint="Checked for duplicates (ADJID.023).">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={60}
              value={form.endorsementRef}
              onChange={(e) => onChange({ ...form, endorsementRef: e.target.value })}
            />
          )}
        </Field>
        <ModeFields form={form} errors={errors} onChange={onChange} mode={mode} />
        {PERIOD_TYPES.has(form.endorsementType) && (
          <>
            <Field label="New Inception" required>
              {(id) => (
                <DateInput
                  id={id}
                  value={form.newPeriodFrom}
                  onChange={(v) => onChange({ ...form, newPeriodFrom: v })}
                />
              )}
            </Field>
            <Field label="New Expiry" required error={errors.newPeriodTo}>
              {(id) => (
                <DateInput
                  id={id}
                  value={form.newPeriodTo}
                  onChange={(v) => onChange({ ...form, newPeriodTo: v })}
                />
              )}
            </Field>
          </>
        )}
      </div>
      {mode === 'AMOUNTS' && <AmountFields form={form} errors={errors} onChange={onChange} />}
      <Field label="Description" required error={errors.description}>
        {(id) => (
          <textarea
            id={id}
            className="textarea"
            rows={3}
            maxLength={1000}
            value={form.description}
            onChange={(e) => onChange({ ...form, description: e.target.value })}
          />
        )}
      </Field>
      <Field label="Additional / Other Instructions" hint="Printed on the endorsement slip.">
        {(id) => (
          <textarea
            id={id}
            className="textarea"
            rows={2}
            maxLength={1000}
            value={form.instructions}
            onChange={(e) => onChange({ ...form, instructions: e.target.value })}
          />
        )}
      </Field>
    </div>
  );
}
