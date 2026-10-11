import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import type { ProgrammeView } from '@/api/eb';
import type { MemberChange } from '@/api/ebMarket';
import { ebServiceApi } from '@/api/ebService';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Modal } from '@/components/ui/Modal';
import { DialogFooter } from '../common/DialogFooter';
import { EB_LOV } from '../common/ebCodes';
import { useEbMutation } from '../common/useEbMutation';
import { ACTIONS, BLANK_ROW, changeErrors, fieldsOf, toChangeInput } from './memberChangeForm';
import type { ChangeForm, ChangeRow } from './memberChangeForm';
import { Notice } from '@/components/ui/Notice';

const TEXT_FIELDS: readonly { key: keyof ChangeRow; label: string; kind: 'personal' | 'plan' }[] = [
  { key: 'lastName', label: 'Last Name', kind: 'personal' },
  { key: 'firstName', label: 'First Name', kind: 'personal' },
  { key: 'planCode', label: 'Plan', kind: 'plan' },
  { key: 'dependants', label: 'Dependants', kind: 'plan' },
];

/** One member line: the change, the employee, the effective date and the member data it needs. */
function RowFields({
  row,
  index,
  onChange,
  onRemove,
}: Readonly<{
  row: ChangeRow;
  index: number;
  onChange: (patch: Partial<ChangeRow>) => void;
  onRemove: () => void;
}>) {
  const shown = fieldsOf(row.action);
  const n = String(index + 1);
  return (
    <fieldset className="eb-group">
      <legend>
        Line {n}{' '}
        <Button
          variant="ghost"
          size="sm"
          aria-label={`Remove line ${n}`}
          icon={<Trash2 size={14} />}
          onClick={onRemove}
        />
      </legend>
      <div className="form-grid">
        <Field label="Change" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={row.action}
              onChange={(e) => onChange({ action: e.target.value as ChangeRow['action'] })}
            >
              <option value="">Select</option>
              {ACTIONS.map((a) => (
                <option key={a.code} value={a.code}>
                  {a.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Employee No." required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={row.employeeNo}
              onChange={(e) => onChange({ employeeNo: e.target.value })}
            />
          )}
        </Field>
        <Field label="Effective Date" required>
          {(id) => (
            <DateInput
              id={id}
              value={row.effectiveDate}
              onChange={(e) => onChange({ effectiveDate: e.target.value })}
            />
          )}
        </Field>
        {TEXT_FIELDS.filter((f) => shown[f.kind]).map((f) => (
          <Field key={f.key} label={f.label}>
            {(id) => (
              <input
                id={id}
                className="input"
                value={row[f.key]}
                onChange={(e) => onChange({ [f.key]: e.target.value })}
              />
            )}
          </Field>
        ))}
        {shown.personal && (
          <>
            <Field label="Birth Date">
              {(id) => (
                <DateInput
                  id={id}
                  value={row.birthDate}
                  onChange={(e) => onChange({ birthDate: e.target.value })}
                />
              )}
            </Field>
            <Field label="Gender">
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={row.gender}
                  onChange={(e) => onChange({ gender: e.target.value })}
                >
                  <option value="">Select</option>
                  <option value="M">Male</option>
                  <option value="F">Female</option>
                </select>
              )}
            </Field>
            <Field label="Civil Status">
              {(id) => (
                <LovSelect
                  id={id}
                  type={EB_LOV.civilStatus}
                  value={row.civilStatus}
                  onChange={(v) => onChange({ civilStatus: v })}
                />
              )}
            </Field>
          </>
        )}
      </div>
    </fieldset>
  );
}

/**
 * Capture Member Change: additions, deletions and plan or data changes of members of one
 * benefit line, checked against the accepted roster, with the client's request attached.
 */
export function MemberChangeDialog({
  programme,
  onClose,
}: Readonly<{ programme: ProgrammeView; onClose: () => void }>) {
  const lines = programme.lines.filter((l) => l.active);
  const [form, setForm] = useState<ChangeForm>({
    lineNo: lines.length === 1 ? String(lines[0]?.lineNo) : '',
    policyYear: '',
    source: '',
    financial: true,
    description: '',
    rows: [BLANK_ROW],
  });
  const [files, setFiles] = useState<File[]>([]);
  const [submitted, setSubmitted] = useState(false);
  const capture = useEbMutation(
    (companyId, f: ChangeForm) =>
      ebServiceApi.captureChange(companyId, programme.id, toChangeInput(f), files),
    (c: MemberChange) => `Member change ${c.changeNo} captured`,
    onClose,
  );
  const set = (patch: Partial<ChangeForm>) => setForm((f) => ({ ...f, ...patch }));
  const setRow = (index: number, patch: Partial<ChangeRow>) =>
    set({ rows: form.rows.map((r, i) => (i === index ? { ...r, ...patch } : r)) });
  const errors = submitted ? changeErrors(form) : {};
  const save = () => {
    setSubmitted(true);
    if (Object.keys(changeErrors(form)).length === 0) {
      capture.mutate(form);
    }
  };
  return (
    <Modal
      open
      title="Capture Member Change"
      onClose={onClose}
      footer={
        <DialogFooter busy={capture.isPending} label="Capture" onClose={onClose} onSave={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={capture.error} />
        <div className="form-grid">
          <Field label="Benefit Line" required error={errors.lineNo}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.lineNo}
                onChange={(e) => set({ lineNo: e.target.value })}
              >
                <option value="">Select</option>
                {lines.map((l) => (
                  <option key={l.lineNo} value={l.lineNo}>
                    Line {l.lineNo} – {l.benefitLine}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Requested By" required error={errors.source}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.source}
                onChange={(e) => set({ source: e.target.value as ChangeForm['source'] })}
              >
                <option value="">Select</option>
                <option value="CLIENT">Client</option>
                <option value="AO">Account Officer</option>
              </select>
            )}
          </Field>
          <Field label="Policy Year" hint="The current roster when blank">
            {(id) => (
              <input
                id={id}
                className="input"
                inputMode="numeric"
                value={form.policyYear}
                onChange={(e) => set({ policyYear: e.target.value })}
              />
            )}
          </Field>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={form.financial}
              onChange={(e) => set({ financial: e.target.checked })}
            />
            Affects the premium (billed by the insurer)
          </label>
        </div>
        <Field label="Description">
          {(id) => (
            <input
              id={id}
              className="input"
              value={form.description}
              onChange={(e) => set({ description: e.target.value })}
            />
          )}
        </Field>
        {errors.rows && <Notice tone="error">{errors.rows}</Notice>}
        {form.rows.map((r, i) => (
          <RowFields
            key={i}
            row={r}
            index={i}
            onChange={(patch) => setRow(i, patch)}
            onRemove={() => set({ rows: form.rows.filter((_, j) => j !== i) })}
          />
        ))}
        <div>
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={14} />}
            onClick={() => set({ rows: [...form.rows, BLANK_ROW] })}
          >
            Add Line
          </Button>
        </div>
        <Field label="Client's Request">
          {(id) => (
            <FileDropZone
              id={id}
              multiple
              accept=".pdf,.xls,.xlsx,.doc,.docx,.msg,.eml"
              maxSizeMb={10}
              onChange={setFiles}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
