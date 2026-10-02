import { useState } from 'react';
import type { ProfileInput, ProgrammeView } from '@/api/eb';
import { LovSelect } from '@/components/broking/LovSelect';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { DialogFooter } from '../common/DialogFooter';
import { EB_LOV } from '../common/ebCodes';
import { AccountOfficerSelect } from '../common/EbSelects';
import { ContactFields, LineFields } from './ProgrammeFields';
import { validateContact, validateLine } from './programmeForm';
import type { ContactForm, LineForm } from './programmeForm';

export interface DialogProps<T> {
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (value: T) => void;
}

/** Edit Programme: name, team, funding, account officer and renewal flag (FR-EB-021). */
export function ProfileDialog({
  programme,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps<ProfileInput> & { programme: ProgrammeView }>) {
  const [form, setForm] = useState<ProfileInput>({
    name: programme.name,
    teamCode: programme.teamCode,
    funding: programme.funding,
    accountOfficer: programme.accountOfficer,
    renewalEligible: programme.renewalEligible,
  });
  const [submitted, setSubmitted] = useState(false);
  const nameError = submitted && form.name.trim() === '' ? 'Enter the programme name' : undefined;
  const save = () => {
    setSubmitted(true);
    if (form.name.trim() !== '') {
      onSave({ ...form, name: form.name.trim() });
    }
  };
  return (
    <Modal
      open
      title="Edit Programme"
      onClose={onClose}
      footer={<DialogFooter busy={busy} label="Save Changes" onClose={onClose} onSave={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="form-grid">
          <Field label="Programme Name" required error={nameError}>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={200}
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            )}
          </Field>
          <Field label="Team" required>
            {(id) => (
              <LovSelect
                id={id}
                type={EB_LOV.team}
                value={form.teamCode}
                onChange={(v) => setForm({ ...form, teamCode: v })}
                required
              />
            )}
          </Field>
          <Field label="Funding" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.funding}
                onChange={(e) =>
                  setForm({ ...form, funding: e.target.value as ProfileInput['funding'] })
                }
              >
                <option value="EMPLOYER">Employer</option>
                <option value="VOLUNTARY">Voluntary</option>
              </select>
            )}
          </Field>
          <Field label="Account Officer">
            {(id) => (
              <AccountOfficerSelect
                id={id}
                value={form.accountOfficer ?? ''}
                placeholder="Keep current"
                onChange={(v) => setForm({ ...form, accountOfficer: v })}
              />
            )}
          </Field>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={form.renewalEligible}
              onChange={(e) => setForm({ ...form, renewalEligible: e.target.checked })}
            />
            Eligible for Renewal
          </label>
        </div>
      </div>
    </Modal>
  );
}

/** Add or change a benefit line. */
export function LineDialog({
  initial,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps<LineForm> & { initial: LineForm }>) {
  const [line, setLine] = useState(initial);
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? validateLine(line) : {};
  const save = () => {
    setSubmitted(true);
    if (Object.keys(validateLine(line)).length === 0) {
      onSave(line);
    }
  };
  return (
    <Modal
      open
      title={initial.benefitLine === '' ? 'Add Benefit Line' : 'Edit Benefit Line'}
      onClose={onClose}
      footer={<DialogFooter busy={busy} label="Save Line" onClose={onClose} onSave={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <LineFields line={line} errors={errors} onChange={setLine} />
      </div>
    </Modal>
  );
}

/** Add or change an HR contact. */
export function ContactDialog({
  initial,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps<ContactForm> & { initial: ContactForm }>) {
  const [contact, setContact] = useState(initial);
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? validateContact(contact) : {};
  const save = () => {
    setSubmitted(true);
    if (Object.keys(validateContact(contact)).length === 0) {
      onSave(contact);
    }
  };
  return (
    <Modal
      open
      title={initial.name === '' ? 'Add HR Contact' : 'Edit HR Contact'}
      onClose={onClose}
      footer={<DialogFooter busy={busy} label="Save Contact" onClose={onClose} onSave={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <ContactFields contact={contact} errors={errors} onChange={setContact} />
      </div>
    </Modal>
  );
}
