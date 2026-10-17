import { LovSelect } from '@/components/broking/LovSelect';
import { UserLookup } from '@/components/broking/Lookups';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { SOURCES } from './requestDetails';
import type { DetailsForm } from './requestDetails';

interface RequestDetailsCardProps {
  source: string;
  details: DetailsForm;
  errors: Record<string, string>;
  onSource: (source: string) => void;
  onDetails: (patch: Partial<DetailsForm>) => void;
}

const WORDING: readonly [keyof DetailsForm, string][] = [
  ['typeOfCover', 'Type of Cover'],
  ['descriptionOfCover', 'Description of Cover'],
  ['cover', 'Cover / Undertaking'],
  ['extensions', 'Extensions of Cover'],
  ['warranties', 'Warranties, Clauses'],
  ['otherInstructions', 'Other Instructions'],
  ['maximumLimits', 'Maximum Limit/s'],
];

/**
 * The Source of a package request and its package details of BDOI's FRS (FRPM.011.02): business
 * origin, Account Officer and Unit Head, Insured's Name, estimated policies and premium, the cover
 * wording, premium and the incentive. A TSU or Insurer source goes to TSU review at once; an
 * Insurer source needs no quotation slip.
 */
export function RequestDetailsCard({
  source,
  details,
  errors,
  onSource,
  onDetails,
}: Readonly<RequestDetailsCardProps>) {
  const text = (key: keyof DetailsForm, label: string) => (
    <TextInput
      key={key}
      label={label}
      value={String(details[key])}
      error={errors[key]}
      onChange={(v) => onDetails({ [key]: v })}
    />
  );
  return (
    <Card title="Source and Package Details">
      <div className="form-grid">
        <SelectInput
          label="Source"
          required
          value={source}
          options={SOURCES.map((s) => ({ value: s.value, label: s.label }))}
          onChange={onSource}
          hint={
            source === 'MARKETING'
              ? 'Marketing approval first, then TSU review'
              : 'Goes to TSU review at once'
          }
        />
        <Field label="Business Origin">
          {(id) => (
            <LovSelect
              id={id}
              type="PKG_BUSINESS_ORIGIN"
              value={details.businessOrigin}
              onChange={(businessOrigin) => onDetails({ businessOrigin })}
              placeholder="Select"
            />
          )}
        </Field>
        <Field label="Account Officer">
          {(id) => (
            <UserLookup
              id={id}
              emptyLabel="None"
              value={details.accountOfficer}
              onChange={(accountOfficer) => onDetails({ accountOfficer })}
            />
          )}
        </Field>
        <Field label="Unit Head">
          {(id) => (
            <UserLookup
              id={id}
              emptyLabel="None"
              value={details.unitHead}
              onChange={(unitHead) => onDetails({ unitHead })}
            />
          )}
        </Field>
        {text('insuredName', "Insured's Name")}
        {text('estimatedPolicies', 'Estimated No. of Policies to be Issued')}
        {text('estimatedPremium', 'Estimated Total Basic Premium to be Issued')}
        {text('premium', 'Premium')}
        {WORDING.map(([key, label]) => text(key, label))}
        <label className="checkbox">
          <input
            type="checkbox"
            checked={details.incentiveEligible}
            onChange={(e) => onDetails({ incentiveEligible: e.target.checked })}
          />
          Incentive Eligible
        </label>
        {details.incentiveEligible && text('incentiveAmount', 'Incentive Amount')}
        {details.incentiveEligible && text('incentiveRate', 'Incentive Commission Rate (%)')}
      </div>
    </Card>
  );
}
