import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ebApi } from '@/api/eb';
import { ClientPicker } from '@/components/broking/ClientPicker';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useUnsavedChangesGuard } from '@/components/ui/useUnsavedChangesGuard';
import { useCompanyId } from '@/context/workspaceContext';
import { EB_SECTION } from '../EbPlaceholder';
import '../eb.css';
import { EB_LOV } from '../common/ebCodes';
import { AccountOfficerSelect } from '../common/EbSelects';
import { ContactFields, LineFields } from './ProgrammeFields';
import {
  emptyContact,
  emptyLine,
  emptyProgramme,
  errorsOf,
  programmeInput,
  validateProgramme,
} from './programmeForm';
import type { ContactForm, LineForm, ProgrammeForm } from './programmeForm';

/**
 * New Programme (BRID-006, 022.01; FR-EB-021): the client (prospect or confirmed), the programme
 * name, team, funding, account officer and renewal flag, the benefit lines with the incumbent
 * insurer, current policy and period, and the HR contacts who receive the renewal advice and SOAs.
 */
export default function NewProgrammePage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ProgrammeForm>(emptyProgramme);
  const [dirty, setDirty] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? validateProgramme(form) : {};
  const guard = useUnsavedChangesGuard(dirty);
  const change = (next: Partial<ProgrammeForm>) => {
    setDirty(true);
    setForm((f) => ({ ...f, ...next }));
  };
  const setLine = (index: number, line: LineForm) =>
    change({ lines: form.lines.map((l, i) => (i === index ? line : l)) });
  const setContact = (index: number, contact: ContactForm) =>
    change({ contacts: form.contacts.map((c, i) => (i === index ? contact : c)) });
  const create = useMutation({
    mutationFn: () => ebApi.create(companyId, programmeInput(form)),
    onSuccess: async (created) => {
      setDirty(false);
      toast.success(`Programme ${created.programmeNo} created`);
      await queryClient.invalidateQueries({ queryKey: ['eb'] });
      void navigate(`/eb/programmes/${String(created.id)}`);
    },
  });
  const save = () => {
    setSubmitted(true);
    if (Object.keys(validateProgramme(form)).length === 0) {
      create.mutate();
    }
  };
  return (
    <div className="stack">
      {guard}
      <PageHeader
        section={EB_SECTION}
        title="New Programme"
        description="A client's employee benefits with their lines and HR contacts."
        backTo="/eb/programmes"
      />
      <ErrorAlert error={create.error} />
      <Card title="Programme">
        <div className="form-grid">
          <Field label="Client" required error={errors.clientId}>
            {(id) => (
              <ClientPicker
                id={id}
                value={form.clientId}
                allowProspect
                onChange={(c) => change({ clientId: c?.id })}
              />
            )}
          </Field>
          <Field label="Programme Name" required error={errors.name}>
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={200}
                value={form.name}
                onChange={(e) => change({ name: e.target.value })}
              />
            )}
          </Field>
          <Field label="Team" required error={errors.teamCode}>
            {(id) => (
              <LovSelect
                id={id}
                type={EB_LOV.team}
                value={form.teamCode}
                onChange={(v) => change({ teamCode: v })}
                required
              />
            )}
          </Field>
          <Field label="Funding" required error={errors.funding}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.funding}
                onChange={(e) => change({ funding: e.target.value as ProgrammeForm['funding'] })}
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
                value={form.accountOfficer}
                onChange={(v) => change({ accountOfficer: v })}
              />
            )}
          </Field>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={form.renewalEligible}
              onChange={(e) => change({ renewalEligible: e.target.checked })}
            />
            Eligible for Renewal
          </label>
        </div>
      </Card>
      <Card
        title="Benefit Lines"
        actions={
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={14} />}
            onClick={() => change({ lines: [...form.lines, emptyLine()] })}
          >
            Add Line
          </Button>
        }
      >
        <div className="stack">
          {errors.lines && <div className="alert danger">{errors.lines}</div>}
          {form.lines.map((line, i) => (
            <fieldset key={`line-${String(i)}`} className="eb-group">
              <legend>
                Line {i + 1}
                {form.lines.length > 1 && (
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={`Remove line ${String(i + 1)}`}
                    icon={<Trash2 size={14} />}
                    onClick={() => change({ lines: form.lines.filter((_, j) => j !== i) })}
                  />
                )}
              </legend>
              <LineFields
                line={line}
                errors={errorsOf(errors, `lines.${String(i)}`)}
                onChange={(l) => setLine(i, l)}
              />
            </fieldset>
          ))}
        </div>
      </Card>
      <Card
        title="HR Contacts"
        actions={
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={14} />}
            onClick={() => change({ contacts: [...form.contacts, emptyContact()] })}
          >
            Add Contact
          </Button>
        }
      >
        <div className="stack">
          {errors.contacts && <div className="alert danger">{errors.contacts}</div>}
          {form.contacts.map((contact, i) => (
            <fieldset key={`contact-${String(i)}`} className="eb-group">
              <legend>
                Contact {i + 1}
                {form.contacts.length > 1 && (
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={`Remove contact ${String(i + 1)}`}
                    icon={<Trash2 size={14} />}
                    onClick={() => change({ contacts: form.contacts.filter((_, j) => j !== i) })}
                  />
                )}
              </legend>
              <ContactFields
                contact={contact}
                errors={errorsOf(errors, `contacts.${String(i)}`)}
                onChange={(c) => setContact(i, c)}
              />
            </fieldset>
          ))}
        </div>
      </Card>
      <div className="form-actions">
        <Button variant="secondary" onClick={() => void navigate('/eb/programmes')}>
          Cancel
        </Button>
        <Button variant="accent" busy={create.isPending} onClick={save}>
          Create Programme
        </Button>
      </div>
    </div>
  );
}
