import { useState } from 'react';
import type { BusinessType, FeedbackChannel } from '@/api/eb';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import type { DialogProps } from './ProgrammeDialogs';

export interface OpenCycleValue {
  businessType: BusinessType;
  policyYear?: number;
  targetInception?: string;
}

/** Open Cycle: business type (required), policy year and target inception (FR-EB-021 R2). */
export function OpenCycleDialog({
  renewalAllowed,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps<OpenCycleValue> & { renewalAllowed: boolean }>) {
  const [businessType, setBusinessType] = useState<BusinessType | ''>('');
  const [inception, setInception] = useState('');
  const [year, setYear] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const typeError =
    submitted && businessType === '' ? 'Select the business type of the cycle' : undefined;
  const yearError = year !== '' && !/^\d{4}$/.test(year) ? 'Enter a year such as 2027' : undefined;
  const save = () => {
    setSubmitted(true);
    if (businessType !== '' && yearError === undefined) {
      onSave({
        businessType,
        policyYear: year === '' ? undefined : Number(year),
        targetInception: inception === '' ? undefined : inception,
      });
    }
  };
  return (
    <Modal
      open
      title="Open Cycle"
      onClose={onClose}
      footer={<DialogFooter busy={busy} label="Open Cycle" onClose={onClose} onSave={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="form-grid">
          <Field label="Business Type" required error={typeError}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={businessType}
                onChange={(e) => setBusinessType(e.target.value as BusinessType | '')}
              >
                <option value="">Select business type</option>
                <option value="NEW_BUSINESS">New Business</option>
                {renewalAllowed && <option value="RENEWAL">Renewal</option>}
              </select>
            )}
          </Field>
          <Field label="Target Inception">
            {(id) => (
              <DateInput id={id} value={inception} onChange={(e) => setInception(e.target.value)} />
            )}
          </Field>
          <Field
            label="Policy Year"
            error={yearError}
            hint="The year of the target inception when empty"
          >
            {(id) => (
              <input
                id={id}
                className="input"
                inputMode="numeric"
                value={year}
                onChange={(e) => setYear(e.target.value)}
              />
            )}
          </Field>
        </div>
      </div>
    </Modal>
  );
}

export interface FeedbackValue {
  channel: FeedbackChannel | '';
  receivedOn: string;
  text: string;
  files: File[];
}

const CHANNELS: { code: FeedbackChannel; label: string }[] = [
  { code: 'EMAIL', label: 'E-mail' },
  { code: 'AO', label: 'Account Officer' },
  { code: 'PHONE', label: 'Telephone' },
  { code: 'MEETING', label: 'Meeting' },
  { code: 'LETTER', label: 'Letter' },
];

/** The field errors of the feedback form (FR-EB-023). */
function feedbackErrors(value: FeedbackValue): Record<string, string> {
  const errors: Record<string, string> = {};
  if (value.channel === '') {
    errors.channel = 'Select the channel';
  }
  if (value.receivedOn === '') {
    errors.receivedOn = 'Enter the date received';
  } else if (value.receivedOn > today()) {
    errors.receivedOn = 'The date received cannot be after today';
  }
  if (value.text.trim() === '' && value.files.length === 0) {
    errors.text = 'Enter the feedback or attach a file';
  }
  return errors;
}

/** Record Feedback: channel, date received, text and / or files (FR-EB-023). */
export function FeedbackDialog({
  cycleNo,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<DialogProps<FeedbackValue> & { cycleNo: string }>) {
  const [value, setValue] = useState<FeedbackValue>({
    channel: 'EMAIL',
    receivedOn: today(),
    text: '',
    files: [],
  });
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? feedbackErrors(value) : {};
  const save = () => {
    setSubmitted(true);
    if (Object.keys(feedbackErrors(value)).length === 0) {
      onSave(value);
    }
  };
  return (
    <Modal
      open
      title={`Record Feedback – ${cycleNo}`}
      onClose={onClose}
      footer={<DialogFooter busy={busy} label="Record Feedback" onClose={onClose} onSave={save} />}
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="form-grid">
          <Field label="Channel" required error={errors.channel}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={value.channel}
                onChange={(e) => setValue({ ...value, channel: e.target.value as FeedbackChannel })}
              >
                {CHANNELS.map((c) => (
                  <option key={c.code} value={c.code}>
                    {c.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Date Received" required error={errors.receivedOn}>
            {(id) => (
              <DateInput
                id={id}
                max={today()}
                value={value.receivedOn}
                onChange={(e) => setValue({ ...value, receivedOn: e.target.value })}
              />
            )}
          </Field>
        </div>
        <Field label="Feedback" error={errors.text}>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={4}
              maxLength={4000}
              value={value.text}
              onChange={(e) => setValue({ ...value, text: e.target.value })}
            />
          )}
        </Field>
        <FileDropZone
          label="Files"
          multiple
          accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.eml,.msg"
          maxSizeMb={10}
          onChange={(files) => setValue({ ...value, files })}
        />
      </div>
    </Modal>
  );
}
