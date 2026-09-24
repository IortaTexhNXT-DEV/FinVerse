import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Save, Send } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { glApi } from '@/api/gl';
import type { Journal } from '@/api/gl';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useDefaultBranchId, useWorkspace } from '@/context/workspaceContext';
import { JournalHeaderFields } from './JournalHeaderFields';
import { JournalLinesEditor } from './JournalLinesEditor';
import { journalValues, newJournalValues, toJournalInput } from './journalForm';
import type { JournalFormValues } from './journalForm';
import { lineProblems, totals } from './journalMath';
import { useJournalSave } from './useJournalSave';
import type { SaveMode } from './useJournalSave';

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

  const { save, draftId, savedDraft } = useJournalSave(editingId, async (journal: Journal) => {
    await queryClient.invalidateQueries({ queryKey: ['journal', journal.id] });
    toast.success(`Journal ${journal.batchNo} saved (${journal.status.replace('_', ' ')})`);
    await navigate(`/gl/journals/${journal.id}`);
  });
  const mode = save.variables?.mode;
  const problems = lineProblems(values.lines);
  const hasProblems = Object.keys(problems).length > 0;
  const canSubmit = totals(values.lines).balanced && values.header.narration.trim() !== '';
  const run = (saveMode: SaveMode) => {
    setChecked(true);
    if (!hasProblems) {
      save.mutate({ mode: saveMode, body: toJournalInput(company?.id ?? 0, values) });
    }
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
              onClick={() => run('submit')}
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
