import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { today } from '@/utils/format';
import { claimsHomeApi } from '../home/api';
import { validateDiary } from './api';
import type { DiaryForm, DiaryInput } from './api';

/** Add Diary Entry: type, date, due date, assignee and text (FR-CL-052). */
export function DiaryDialog({
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (input: DiaryInput) => void;
}>) {
  const assignees = useQuery({
    queryKey: ['broker-claims', 'assignees'],
    queryFn: claimsHomeApi.assignees,
    staleTime: 5 * 60_000,
  });
  const [form, setForm] = useState<DiaryForm>({
    entryType: '',
    entryDate: today(),
    dueDate: '',
    assignee: '',
    text: '',
  });
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? validateDiary(form) : {};
  const set = (key: keyof DiaryForm, value: string) => setForm((f) => ({ ...f, [key]: value }));
  const save = () => {
    setSubmitted(true);
    if (Object.keys(validateDiary(form)).length === 0) {
      onSave({
        entryType: form.entryType,
        entryDate: form.entryDate || undefined,
        dueDate: form.dueDate || undefined,
        assignee: form.assignee || undefined,
        text: form.text.trim(),
      });
    }
  };
  return (
    <Modal
      open
      title="Add Diary Entry"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={save}>
            Add Entry
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error ?? assignees.error} />
        <div className="form-grid">
          <Field label="Type" required error={errors.entryType}>
            {(id) => (
              <LovSelect
                id={id}
                type="BCL_DIARY_TYPE"
                value={form.entryType}
                onChange={(v) => set('entryType', v)}
                required
              />
            )}
          </Field>
          <Field label="Date" required>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={form.entryDate}
                onChange={(e) => set('entryDate', e.target.value)}
              />
            )}
          </Field>
          <Field label="Due date" error={errors.dueDate}>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                min={form.entryDate}
                value={form.dueDate}
                onChange={(e) => set('dueDate', e.target.value)}
              />
            )}
          </Field>
          <Field label="Assignee" hint="Default: you.">
            {(id) => (
              <select
                id={id}
                className="select"
                value={form.assignee}
                onChange={(e) => set('assignee', e.target.value)}
              >
                <option value="">Me</option>
                {(assignees.data ?? []).map((a) => (
                  <option key={a.username} value={a.username}>
                    {a.username}
                    {a.unitCode === undefined ? '' : ` · ${a.unitCode}`}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </div>
        <Field label="Text" required error={errors.text} hint="Up to 2,000 characters.">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={4}
              value={form.text}
              onChange={(e) => set('text', e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
