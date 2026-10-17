import { LovSelect } from '@/components/broking/LovSelect';
import { DateInput } from '@/components/ui/DateInput';
import { Field } from '@/components/ui/Field';
import { EB_LOV } from '../common/ebCodes';
import { InsurerSelect } from '../common/EbSelects';
import type { ContactForm, Errors, LineForm } from './programmeForm';

const ROLES = [
  { code: 'HR_HEAD', label: 'HR Head' },
  { code: 'HR_OFFICER', label: 'HR Officer' },
  { code: 'FINANCE', label: 'Finance' },
] as const;

/** The fields of a benefit line (FR-EB-021: line, product, incumbent, policy, period, headcount). */
export function LineFields({
  line,
  errors,
  onChange,
}: Readonly<{
  line: LineForm;
  errors: Errors;
  onChange: (line: LineForm) => void;
}>) {
  const set = (key: keyof LineForm, value: string) => onChange({ ...line, [key]: value });
  return (
    <div className="form-grid">
      <Field label="Benefit Line" required error={errors.benefitLine}>
        {(id) => (
          <LovSelect
            id={id}
            type={EB_LOV.benefitLine}
            value={line.benefitLine}
            onChange={(v) => set('benefitLine', v)}
            required
          />
        )}
      </Field>
      <Field label="Incumbent Insurer">
        {(id) => (
          <InsurerSelect
            id={id}
            value={line.incumbentInsurer}
            placeholder="None"
            onChange={(v) => set('incumbentInsurer', v)}
          />
        )}
      </Field>
      <Field label="Current Policy No.">
        {(id) => (
          <input
            id={id}
            className="input"
            value={line.currentPolicyNo}
            onChange={(e) => set('currentPolicyNo', e.target.value)}
          />
        )}
      </Field>
      <Field label="Current ARN">
        {(id) => (
          <input
            id={id}
            className="input"
            value={line.currentArn}
            placeholder="ARN-yyyy-nnnnnn"
            onChange={(e) => set('currentArn', e.target.value)}
          />
        )}
      </Field>
      <Field label="Period From">
        {(id) => (
          <DateInput
            id={id}
            value={line.periodFrom}
            onChange={(e) => set('periodFrom', e.target.value)}
          />
        )}
      </Field>
      <Field label="Period To" error={errors.periodTo}>
        {(id) => (
          <DateInput
            id={id}
            value={line.periodTo}
            min={line.periodFrom || undefined}
            onChange={(e) => set('periodTo', e.target.value)}
          />
        )}
      </Field>
      <Field label="Headcount" error={errors.headcount}>
        {(id) => (
          <input
            id={id}
            className="input"
            inputMode="numeric"
            value={line.headcount}
            onChange={(e) => set('headcount', e.target.value)}
          />
        )}
      </Field>
      <Field label="Product">
        {(id) => (
          <input
            id={id}
            className="input"
            value={line.productCode}
            onChange={(e) => set('productCode', e.target.value)}
          />
        )}
      </Field>
    </div>
  );
}

/** The fields of an HR contact (FR-EB-021: name, e-mail, mobile, role, RA / SOA flags). */
export function ContactFields({
  contact,
  errors,
  onChange,
}: Readonly<{
  contact: ContactForm;
  errors: Errors;
  onChange: (contact: ContactForm) => void;
}>) {
  return (
    <div className="form-grid">
      <Field label="Name" required error={errors.name}>
        {(id) => (
          <input
            id={id}
            className="input"
            value={contact.name}
            onChange={(e) => onChange({ ...contact, name: e.target.value })}
          />
        )}
      </Field>
      <Field label="E-mail" required error={errors.email}>
        {(id) => (
          <input
            id={id}
            type="email"
            className="input"
            value={contact.email}
            onChange={(e) => onChange({ ...contact, email: e.target.value })}
          />
        )}
      </Field>
      <Field label="Mobile">
        {(id) => (
          <input
            id={id}
            className="input"
            value={contact.mobile}
            onChange={(e) => onChange({ ...contact, mobile: e.target.value })}
          />
        )}
      </Field>
      <Field label="Role" required error={errors.role}>
        {(id) => (
          <select
            id={id}
            className="select"
            value={contact.role}
            onChange={(e) => onChange({ ...contact, role: e.target.value as ContactForm['role'] })}
          >
            <option value="">Select role</option>
            {ROLES.map((r) => (
              <option key={r.code} value={r.code}>
                {r.label}
              </option>
            ))}
          </select>
        )}
      </Field>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={contact.receivesRa}
          onChange={(e) => onChange({ ...contact, receivesRa: e.target.checked })}
        />
        Receives Renewal Advice
      </label>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={contact.receivesSoa}
          onChange={(e) => onChange({ ...contact, receivesSoa: e.target.checked })}
        />
        Receives SOA
      </label>
    </div>
  );
}
