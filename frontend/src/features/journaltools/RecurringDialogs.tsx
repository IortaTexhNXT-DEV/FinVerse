import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { recurringApi } from '@/api/journalAutomation';
import type { GenerationResult, RecurringTemplate } from '@/api/journalAutomation';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, today } from '@/utils/format';
import { RecurringHistory } from './RecurringHistory';
import { RecurringTemplateForm } from './RecurringTemplateForm';
import { templateProblems, toTemplateInput } from './recurringModel';
import type { TemplateForm } from './recurringModel';

function RunResult({ result }: Readonly<{ result: GenerationResult }>) {
  const tone = result.errors.length === 0 ? 'success' : 'warning';
  return (
    <div className="stack">
      <div className={`alert ${tone}`} role="status">
        {result.journals.length} journal(s) generated
        {result.errors.length > 0 && `, ${result.errors.length} failed`}.
      </div>
      <ul>
        {result.journals.map((j) => (
          <li key={j.batchId}>
            {formatDate(j.occurrenceDate)}: <strong>{j.batchNo}</strong>
            {j.reversalBatchNo !== undefined && ` (reversal ${j.reversalBatchNo})`}
            {j.submitted ? ' – submitted for approval' : ''} {j.note ?? ''}
          </li>
        ))}
        {result.errors.map((e) => (
          <li key={e} className="field-error">
            {e}
          </li>
        ))}
      </ul>
    </div>
  );
}

/** Create or edit a recurring template. */
export function TemplateDialog({
  form,
  onChange,
  onClose,
}: Readonly<{
  form: TemplateForm | null;
  onChange: (form: TemplateForm) => void;
  onClose: () => void;
}>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const save = useMutation({
    mutationFn: (f: TemplateForm) =>
      f.id === undefined
        ? recurringApi.create(toTemplateInput(companyId, f))
        : recurringApi.update(f.id, toTemplateInput(companyId, f)),
    onSuccess: async (t) => {
      await queryClient.invalidateQueries({ queryKey: ['recurring'] });
      toast.success(`Template ${t.name} saved`);
      onClose();
    },
  });
  const problems = form === null ? [] : templateProblems(form);
  return (
    <Modal
      title={form?.id === undefined ? 'New recurring template' : `Edit ${form.name}`}
      open={form !== null}
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          busy={save.isPending}
          disabled={problems.length > 0}
          onClick={() => form && save.mutate(form)}
        >
          Save Template
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      {problems.length > 0 && <div className="alert warning">{problems.join(' ')}</div>}
      {form !== null && <RecurringTemplateForm value={form} onChange={onChange} />}
    </Modal>
  );
}

/** Generate the due occurrences of a template up to a chosen date. */
export function RunDialog({
  template,
  onClose,
}: Readonly<{ template: RecurringTemplate | null; onClose: () => void }>) {
  const queryClient = useQueryClient();
  const [runDate, setRunDate] = useState(today());
  const run = useMutation({
    mutationFn: (t: RecurringTemplate) => recurringApi.run(t.id, runDate),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['recurring'] });
      await queryClient.invalidateQueries({ queryKey: ['journals'] });
    },
  });
  const close = () => {
    run.reset();
    onClose();
  };
  return (
    <Modal
      title={`Run ${template?.name ?? ''}`}
      open={template !== null}
      onClose={close}
      footer={
        <Button
          variant="accent"
          busy={run.isPending}
          onClick={() => template && run.mutate(template)}
        >
          Generate Due Journals
        </Button>
      }
    >
      <div className="stack">
        <p className="muted">
          Generates every occurrence due up to the run date that has not been generated yet.
        </p>
        <Field label="Run date" required>
          {(id) => (
            <input
              id={id}
              className="input"
              type="date"
              value={runDate}
              onChange={(e) => setRunDate(e.target.value)}
            />
          )}
        </Field>
        <ErrorAlert error={run.error} />
        {run.data !== undefined && <RunResult result={run.data} />}
      </div>
    </Modal>
  );
}

/** Generation history of a template. */
export function HistoryDialog({
  template,
  onClose,
}: Readonly<{ template: RecurringTemplate | null; onClose: () => void }>) {
  return (
    <Modal title={`History – ${template?.name ?? ''}`} open={template !== null} onClose={onClose}>
      {template !== null && <RecurringHistory templateId={template.id} />}
    </Modal>
  );
}
