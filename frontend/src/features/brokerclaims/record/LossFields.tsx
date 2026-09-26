import type { ClaimForm, ClaimFormErrors } from './recordLogic';
import { InputField, LovField, TextAreaField } from './FormParts';

/** The loss fields of a claim form, shared by Record Claim and Change Loss Details. */
export type LossFieldKey =
  | 'lossDate'
  | 'reportedDate'
  | 'lossNature'
  | 'claimType'
  | 'lossDescription'
  | 'lossPlace'
  | 'catastropheCode'
  | 'catastropheEvent'
  | 'claimAmount'
  | 'deductible'
  | 'initialReserve';

/**
 * The loss of a claim (p.24-25; FR-CM-011/033): dates, nature and type, description, place,
 * catastrophe tag and amounts, with their field errors.
 */
export function LossFields({
  form,
  errors,
  onChange,
  currency,
  reportedDateEditable = true,
}: Readonly<{
  form: Pick<ClaimForm, LossFieldKey>;
  errors: ClaimFormErrors;
  onChange: (key: LossFieldKey, value: string) => void;
  currency: string;
  reportedDateEditable?: boolean;
}>) {
  return (
    <div className="stack">
      <div className="form-grid">
        <InputField
          label="Loss Date"
          type="date"
          required
          value={form.lossDate}
          error={errors.lossDate}
          onChange={(v) => onChange('lossDate', v)}
        />
        {reportedDateEditable && (
          <InputField
            label="Reported Date"
            type="date"
            required
            value={form.reportedDate}
            error={errors.reportedDate}
            hint="Date the loss was reported to BDOI; every claim age starts here"
            onChange={(v) => onChange('reportedDate', v)}
          />
        )}
        <LovField
          label="Nature of Loss"
          type="BCL_LOSS_NATURE"
          required
          value={form.lossNature}
          error={errors.lossNature}
          onChange={(v) => onChange('lossNature', v)}
        />
        <LovField
          label="Claim Type"
          type="BCL_CLAIM_TYPE"
          required
          value={form.claimType}
          error={errors.claimType}
          onChange={(v) => onChange('claimType', v)}
        />
        <LovField
          label="Catastrophe Code"
          type="BCL_CATASTROPHE"
          placeholder="None"
          value={form.catastropheCode}
          onChange={(v) => onChange('catastropheCode', v)}
        />
        <InputField
          label="Event Name"
          value={form.catastropheEvent}
          maxLength={100}
          error={errors.catastropheEvent}
          hint="e.g. the typhoon's name"
          onChange={(v) => onChange('catastropheEvent', v)}
        />
      </div>
      <TextAreaField
        label="Loss Description"
        required
        value={form.lossDescription}
        error={errors.lossDescription}
        onChange={(v) => onChange('lossDescription', v)}
      />
      <div className="form-grid">
        <InputField
          label="Place of Loss"
          value={form.lossPlace}
          maxLength={200}
          hint="Motor: where the accident happened"
          onChange={(v) => onChange('lossPlace', v)}
        />
        <InputField
          label={`Claim Amount (${currency})`}
          type="number"
          value={form.claimAmount}
          error={errors.claimAmount}
          onChange={(v) => onChange('claimAmount', v)}
        />
        <InputField
          label={`Deductible (${currency})`}
          type="number"
          value={form.deductible}
          error={errors.deductible}
          onChange={(v) => onChange('deductible', v)}
        />
        <InputField
          label={`Initial Loss Reserve (${currency})`}
          type="number"
          value={form.initialReserve}
          error={errors.initialReserve}
          onChange={(v) => onChange('initialReserve', v)}
        />
      </div>
    </div>
  );
}
