import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Save, Send } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { glApi } from '@/api/gl';
import type { Journal } from '@/api/gl';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useDefaultBranchId, useWorkspace } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { ConfirmPostingDialog } from './ConfirmPostingDialog';
import { JournalHeaderFields } from './JournalHeaderFields';
import { JournalLinesEditor } from './JournalLinesEditor';
import { journalValues, newJournalValues, toJournalInput } from './journalForm';
import type { JournalFormValues } from './journalForm';
import { isBlankLine, lineProblems, totals } from './journalMath';
import { useJournalSave } from './useJournalSave';
import type { SaveMode } from './useJournalSave';

function SubmitConfirmation({
  open,
  values,
  busy,
  onConfirm,
  onClose,
}: Readonly<{
  open: boolean;
  values: JournalFormValues;
  busy: boolean;
  onConfirm: () => void;
  onClose: () => void;
}>) {
  const sums = totals(values.lines);
  const reverseOn = values.header.reverseOn;
  return (
    <ConfirmPostingDialog
      open={open}
      title="Submit journal for posting"
      intro="Check the voucher before it goes to the authorizer. A submitted voucher can no longer be edited unless it is returned."
      facts={[
        { label: 'Value date', value: formatDate(values.header.valueDate) },
        {
          label: 'Reverse on',
          value: reverseOn === '' ? 'No automatic reversal' : formatDate(reverseOn),
        },
        { label: 'Lines', value: values.lines.filter((l) => !isBlankLine(l)).length },
        { label: 'Total debit', value: <Amount value={sums.debit} /> },
        { label: 'Total credit', value: <Amount value={sums.credit} /> },
      ]}
      confirmLabel="Save & Submit"
      busy={busy}
      onConfirm={onConfirm}
      onClose={onClose}
    />
  );
}

function reversalProblem(values: JournalFormValues): boolean {
  const { reverseOn, valueDate } = values.header;
  return reverseOn !== '' && reverseOn <= valueDate;
}

function submittable(values: JournalFormValues, blocked: boolean): boolean {
  return totals(values.lines).balanced && values.header.narration.trim() !== '' && !blocked;
}

function JournalForm({
  initial,
  editingId,
}: Readonly<{ initial: JournalFormValues; editingId?: number }>) {
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { company } = useWorkspace();
  const [values, setValues] = useState(initial);
  const [checked, setChecked] = useState(false);
  const [confirming, setConfirming] = useState(false);

  const { save, draftId, savedDraft } = useJournalSave(editingId, async (journal: Journal) => {
    await queryClient.invalidateQueries({ queryKey: ['journal', journal.id] });
    toast.success(`Journal ${journal.batchNo} saved (${journal.status.replace('_', ' ')})`);
    await navigate(`/gl/journals/${journal.id}`);
  });
  const mode = save.variables?.mode;
  const problems = lineProblems(values.lines);
  const hasProblems = Object.keys(problems).length > 0;
  const blocked = hasProblems || reversalProblem(values);
  const canSubmit = submittable(values, blocked);
  const run = (saveMode: SaveMode) => {
    setChecked(true);
    setConfirming(false);
    if (!blocked) {
      save.mutate({ mode: saveMode, body: toJournalInput(company?.id ?? 0, values) });
    }
  };
  const confirmSubmit = () => {
    setChecked(true);
    setConfirming(!blocked);
  };

  return (
    <div className="stack">
      <PageHeader
        section="General Ledger"
        title={draftId === undefined ? 'New Journal Voucher' : 'Edit Journal Voucher'}
        description="Enter a balanced voucher. Drafts can be saved incomplete; submission sends it to an authorizer."
        actions={
          <>
            <Button
              variant="secondary"
              icon={<Save size={16} />}
              busy={save.isPending && mode === 'draft'}
              onClick={() => run('draft')}
            >
              Save Draft
            </Button>
            <Button
              variant="accent"
              icon={<Send size={16} />}
              busy={save.isPending && mode === 'submit'}
              disabled={!canSubmit}
              onClick={confirmSubmit}
            >
              Save &amp; Submit
            </Button>
          </>
        }
      />
      {savedDraft !== undefined && save.error !== null && (
        <div className="alert warning" role="status">
          Draft {savedDraft.batchNo} was saved but not submitted. Correct the voucher and submit
          again: this updates the same draft.
        </div>
      )}
      <ErrorAlert error={save.error} />
      {checked && hasProblems && (
        <div className="alert danger" role="alert">
          Correct the highlighted lines before saving.
        </div>
      )}
      <SubmitConfirmation
        open={confirming}
        values={values}
        busy={save.isPending}
        onConfirm={() => run('submit')}
        onClose={() => setConfirming(false)}
      />
      <Card title="Voucher header">
        <JournalHeaderFields
          value={values.header}
          onChange={(header) => setValues((v) => ({ ...v, header }))}
        />
      </Card>
      <Card title="Lines">
        <JournalLinesEditor
          lines={values.lines}
          problems={checked ? problems : undefined}
          onChange={(lines) => setValues((v) => ({ ...v, lines }))}
        />
      </Card>
    </div>
  );
}

/** Manual journal voucher entry: new voucher, or edit of a draft / rejected voucher. */
export default function JournalEntryPage() {
  const { id } = useParams();
  const editingId = id === undefined ? undefined : Number(id);
  const { company, branches } = useWorkspace();
  const defaultBranch = useDefaultBranchId();
  const existing = useQuery({
    queryKey: ['journal', editingId],
    queryFn: () => glApi.journal(editingId ?? 0),
    enabled: editingId !== undefined,
  });

  if (editingId !== undefined) {
    if (existing.data === undefined) {
      return existing.error ? (
        <ErrorAlert error={existing.error} />
      ) : (
        <span className="spinner" aria-label="Loading" />
      );
    }
    return (
      <JournalForm key={editingId} initial={journalValues(existing.data)} editingId={editingId} />
    );
  }
  if (branches.length === 0) {
    return <span className="spinner" aria-label="Loading" />;
  }
  return (
    <JournalForm
      key="new"
      initial={newJournalValues(defaultBranch, company?.baseCurrency ?? 'PHP')}
    />
  );
}
