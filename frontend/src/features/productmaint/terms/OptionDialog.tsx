import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { pmTermsApi } from '@/api/pmTerms';
import type { OptionColumn, TermsAnswer, TermsRecordType, TermsView } from '@/api/pmTerms';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { optionProblems } from './termsTable';
import type { OptionForm } from './termsTable';

interface OptionDialogProps {
  type: TermsRecordType;
  id: number;
  view: TermsView;
  /** The option edited; a new option of the insurer when optionNo is null. */
  column: OptionColumn;
  optionNo: number | null;
  onSaved: (view: TermsView) => void;
  onClose: () => void;
}

/**
 * Captures a quotation option of an insurer in the comparative table (BDOI FRS FRPM.006.02,
 * FRPM.012.02): the insurer response Approved, Not Covered or Others (with the insurer's wording)
 * and the value of each field shown.
 */
export function OptionDialog({
  type,
  id,
  view,
  column,
  optionNo,
  onSaved,
  onClose,
}: Readonly<OptionDialogProps>) {
  const [form, setForm] = useState<OptionForm>({
    answer: optionNo === null ? '' : (column.answer ?? ''),
    otherAnswer: optionNo === null ? '' : (column.otherAnswer ?? ''),
    values: Object.fromEntries(
      view.table.shown.map((k) => [k, optionNo === null ? '' : (column.values[k] ?? '')]),
    ),
  });
  const [tried, setTried] = useState(false);
  const problems = tried ? optionProblems(form) : {};
  const save = useMutation({
    mutationFn: () =>
      pmTermsApi.saveOption(type, id, column.insurerCode, {
        optionNo,
        answer: form.answer as TermsAnswer,
        otherAnswer: form.answer === 'OTHERS' ? form.otherAnswer : null,
        values: form.values,
      }),
    onSuccess: (saved) => {
      onSaved(saved);
      onClose();
    },
  });
  const send = () => {
    setTried(true);
    if (Object.keys(optionProblems(form)).length === 0) {
      save.mutate();
    }
  };
  return (
    <Modal
      title={optionNo === null ? 'Add Quotation Option' : `Edit Option ${optionNo}`}
      open
      size="lg"
      onClose={onClose}
      facts={[{ label: 'Insurer', value: column.insurerName }]}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} onClick={send}>
            Save Option
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <Field label="Insurer Response" required error={problems.answer}>
          {(fid) => (
            <select
              id={fid}
              className="select"
              value={form.answer}
              onChange={(e) => setForm({ ...form, answer: e.target.value as TermsAnswer | '' })}
            >
              <option value="">Choose the response</option>
              {Object.entries(view.answers).map(([code, name]) => (
                <option key={code} value={code}>
                  {name}
                </option>
              ))}
            </select>
          )}
        </Field>
        {form.answer === 'OTHERS' && (
          <Field label="Insurer's Response" required error={problems.otherAnswer}>
            {(fid) => (
              <input
                id={fid}
                className="input"
                maxLength={500}
                value={form.otherAnswer}
                onChange={(e) => setForm({ ...form, otherAnswer: e.target.value })}
              />
            )}
          </Field>
        )}
        {view.table.shown.map((key) => (
          <Field key={key} label={view.labels[key] ?? key}>
            {(fid) => (
              <input
                id={fid}
                className="input"
                maxLength={1000}
                value={form.values[key] ?? ''}
                onChange={(e) =>
                  setForm({ ...form, values: { ...form.values, [key]: e.target.value } })
                }
              />
            )}
          </Field>
        ))}
      </div>
    </Modal>
  );
}
