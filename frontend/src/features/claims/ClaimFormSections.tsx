import type { PolicyCover } from '@/api/claims';
import { Card } from '@/components/ui/Card';
import { DateField, NumberField, SelectField, TextField } from '@/features/underwriting/FormFields';
import { NATURES_OF_LOSS } from './claimForm';
import type { ClaimForm } from './claimForm';
import { partyOption } from './useClaimLookups';
import type { useClaimLookups } from './useClaimLookups';

type Change = (patch: Partial<ClaimForm>) => void;

/** Loss details of the notification form: dates, risk, nature, cause, place and narrative. */
export function LossDetailsCard({
  form,
  policy,
  onChange,
}: Readonly<{ form: ClaimForm; policy: PolicyCover | undefined; onChange: Change }>) {
  return (
    <Card title="Loss details">
      <div className="form-grid">
        <DateField
          label="Reported on"
          required
          value={form.reportedDate}
          onChange={(v) => onChange({ reportedDate: v })}
        />
        <SelectField
          label="Risk"
          value={form.riskLineNo === undefined ? '' : String(form.riskLineNo)}
          emptyLabel="Whole policy"
          options={(policy?.risks ?? []).map((r) => ({
            value: String(r.lineNo),
            label: `${String(r.lineNo)}. ${r.description}`,
          }))}
          onChange={(v) => onChange({ riskLineNo: v === '' ? undefined : Number(v) })}
        />
        <SelectField
          label="Nature of loss"
          required
          value={form.natureOfLoss}
          emptyLabel="Select…"
          options={NATURES_OF_LOSS.map((n) => ({ value: n, label: n }))}
          onChange={(v) => onChange({ natureOfLoss: v })}
        />
        <TextField
          label="Cause of loss"
          required
          value={form.causeOfLoss}
          onChange={(v) => onChange({ causeOfLoss: v })}
        />
        <TextField
          label="Place of loss"
          required
          value={form.lossLocation}
          onChange={(v) => onChange({ lossLocation: v })}
        />
        <TextField
          label="Description"
          required
          value={form.description}
          onChange={(v) => onChange({ description: v })}
        />
      </div>
    </Card>
  );
}

/** Involved parties and initial reserves of the notification form. */
export function PartiesCard({
  form,
  lookups,
  onChange,
}: Readonly<{ form: ClaimForm; lookups: ReturnType<typeof useClaimLookups>; onChange: Change }>) {
  return (
    <Card title="Parties and initial reserve (100 %)">
      <div className="form-grid">
        <SelectField
          label="Claimant"
          value={form.claimantCode}
          emptyLabel="Policyholder"
          options={lookups.clients.map(partyOption)}
          onChange={(v) => onChange({ claimantCode: v })}
        />
        <SelectField
          label="Surveyor"
          value={form.surveyorCode}
          emptyLabel="None"
          options={lookups.surveyors.map(partyOption)}
          onChange={(v) => onChange({ surveyorCode: v })}
        />
        <SelectField
          label="Third party"
          value={form.thirdPartyCode}
          emptyLabel="None"
          options={lookups.clients.map(partyOption)}
          onChange={(v) => onChange({ thirdPartyCode: v })}
        />
        <SelectField
          label="Garage"
          value={form.garageCode}
          emptyLabel="None"
          options={lookups.garages.map(partyOption)}
          onChange={(v) => onChange({ garageCode: v })}
        />
        <NumberField
          label="Initial loss reserve"
          hint="Submitted for approval"
          value={form.initialLossReserve}
          onChange={(v) => onChange({ initialLossReserve: v })}
        />
        <NumberField
          label="Initial expense reserve"
          value={form.initialExpenseReserve}
          onChange={(v) => onChange({ initialExpenseReserve: v })}
        />
      </div>
    </Card>
  );
}
