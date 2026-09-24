import { LovSelect } from '@/components/broking/LovSelect';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { formatTin } from './clientForm';
import type { ClientForm, ClientFormErrors } from './clientForm';

type TextKey = {
  [K in keyof ClientForm]: ClientForm[K] extends string ? K : never;
}[keyof ClientForm];

interface SectionProps {
  form: ClientForm;
  errors: ClientFormErrors;
  set: (patch: Partial<ClientForm>) => void;
  /** Client type cannot change once the client is confirmed. */
  typeLocked?: boolean;
}

interface InputProps extends Omit<SectionProps, 'typeLocked'> {
  name: TextKey;
  label: string;
  required?: boolean;
  placeholder?: string;
  type?: string;
  hint?: string;
  format?: (value: string) => string;
}

function TextInput({
  form,
  errors,
  set,
  name,
  label,
  required = false,
  placeholder,
  type = 'text',
  hint,
  format,
}: Readonly<InputProps>) {
  return (
    <Field label={label} required={required} error={errors[name]} hint={hint}>
      {(id) => (
        <input
          id={id}
          className="input"
          type={type}
          placeholder={placeholder}
          value={form[name]}
          aria-invalid={errors[name] !== undefined}
          onChange={(e) => set({ [name]: format ? format(e.target.value) : e.target.value })}
        />
      )}
    </Field>
  );
}

function LovInput({
  form,
  set,
  name,
  label,
  list,
  errors,
  hint,
}: Readonly<Omit<InputProps, 'placeholder'> & { list: string }>) {
  return (
    <Field label={label} error={errors[name]} hint={hint}>
      {(id) => (
        <LovSelect
          id={id}
          type={list}
          placeholder="Not specified"
          value={form[name]}
          onChange={(code) => set({ [name]: code })}
        />
      )}
    </Field>
  );
}

function PersonFields(p: Readonly<SectionProps>) {
  return (
    <>
      <TextInput {...p} name="lastName" label="Last name" required />
      <TextInput {...p} name="firstName" label="First name" required />
      <TextInput {...p} name="middleName" label="Middle name" />
      <TextInput {...p} name="suffix" label="Suffix" placeholder="Jr., III" />
      <TextInput {...p} name="birthDate" label="Birth date" type="date" />
      <LovInput {...p} name="nationality" label="Nationality" list="NATIONALITY" />
      <LovInput {...p} name="civilStatus" label="Civil status" list="CIVIL_STATUS" />
      <TextInput {...p} name="occupation" label="Occupation" />
    </>
  );
}

/** Identity: type, names, birth date, nationality, TIN and ID (BRNB.030/048). */
export function IdentitySection(p: Readonly<SectionProps>) {
  const { form, set, typeLocked = false } = p;
  return (
    <Card title="Identity">
      <div className="form-grid">
        <Field label="Client type" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={form.clientType}
              disabled={typeLocked}
              onChange={(e) => set({ clientType: e.target.value as ClientForm['clientType'] })}
            >
              <option value="INDIVIDUAL">Individual</option>
              <option value="CORPORATE">Corporate</option>
            </select>
          )}
        </Field>
        {form.clientType === 'INDIVIDUAL' ? (
          <PersonFields {...p} />
        ) : (
          <>
            <TextInput {...p} name="corporateName" label="Registered name" required />
            <TextInput {...p} name="occupation" label="Nature of business" />
          </>
        )}
        <TextInput {...p} name="tin" label="TIN" placeholder="000-000-000-000" format={formatTin} />
        <LovInput {...p} name="idType" label="ID type" list="ID_TYPE" />
        <TextInput {...p} name="idNumber" label="ID number" />
      </div>
    </Card>
  );
}

/** Contact details and address. */
export function ContactSection(p: Readonly<SectionProps>) {
  return (
    <Card title="Contact & Address">
      <div className="form-grid">
        <TextInput {...p} name="email" label="E-mail" type="email" placeholder="name@example.ph" />
        <TextInput {...p} name="mobile" label="Mobile" placeholder="09xxxxxxxxx or +639xxxxxxxxx" />
        <TextInput {...p} name="phone" label="Landline" />
        <TextInput {...p} name="addressLine" label="Street address" />
        <TextInput {...p} name="city" label="City / municipality" />
        <TextInput {...p} name="province" label="Province" />
        <TextInput {...p} name="postalCode" label="Postal code" />
      </div>
    </Card>
  );
}

/** Market segment, BDO bank relationship and KYC risk profile. */
export function SegmentSection(p: Readonly<SectionProps>) {
  const { form, set } = p;
  return (
    <Card title="Segment & Bank relationship">
      <div className="form-grid">
        <LovInput {...p} name="marketSegment" label="Market segment" list="MARKET_SEGMENT" />
        <Field label="BDO bank client">
          {(id) => (
            <label className="checkbox-field" htmlFor={id}>
              <input
                id={id}
                type="checkbox"
                checked={form.bankClient}
                onChange={(e) => set({ bankClient: e.target.checked })}
              />
              The client banks with BDO
            </label>
          )}
        </Field>
        {form.bankClient && <TextInput {...p} name="bankCif" label="BDO CIF number" />}
        <LovInput {...p} name="sourceOfFunds" label="Source of funds" list="SOURCE_OF_FUNDS" />
        <LovInput
          {...p}
          name="riskRating"
          label="KYC risk rating"
          list="KYC_RISK_RATING"
          hint="High-risk clients are reviewed more often."
        />
      </div>
    </Card>
  );
}
