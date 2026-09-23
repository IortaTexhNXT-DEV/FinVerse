import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save, Send } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { glApi } from '@/api/gl';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useWorkspace } from '@/context/workspaceContext';
import { JournalHeaderFields } from './JournalHeaderFields';
import { JournalLinesEditor } from './JournalLinesEditor';
import { journalValues, newJournalValues, toJournalInput } from './journalForm';
import type { JournalFormValues } from './journalForm';
import { totals } from './journalMath';

type SaveMode = 'draft' | 'submit';

function JournalForm({
  initial,
  editingId,
}: Readonly<{ initial: JournalFormValues; editingId?: number }>) {
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { company } = useWorkspace();
  const [values, setValues] = useState(initial);
  const [mode, setMode] = useState<SaveMode>('draft');

  const save = useMutation({
    mutationFn: async (saveMode: SaveMode) => {
      setMode(saveMode);
      const body = toJournalInput(company?.id ?? 0, values);
      const saved =
        editingId === undefined
          ? await glApi.createJournal(body)
          : await glApi.updateJournal(editingId, body);
      return saveMode === 'submit' ? glApi.submitJournal(saved.id) : saved;
    },
    onSuccess: async (journal) => {
      await queryClient.invalidateQueries({ queryKey: ['journals'] });
      await queryClient.invalidateQueries({ queryKey: ['journal', journal.id] });
      toast.success(`Journal ${journal.batchNo} saved (${journal.status.replace('_', ' ')})`);
      await navigate(`/gl/journals/${journal.id}`);
    },
  });
  const canSubmit = totals(values.lines).balanced && values.header.narration.trim() !== '';

  return (
    <div className="stack">
      <PageHeader
        section="General Ledger"
        title={editingId === undefined ? 'New Journal Voucher' : 'Edit Journal Voucher'}
        description="Enter a balanced voucher. Drafts can be saved incomplete; submission sends it to an authorizer."
        actions={
          <>
            <Button
              variant="secondary"
              icon={<Save size={16} />}
              busy={save.isPending && mode === 'draft'}
              onClick={() => save.mutate('draft')}
            >
              Save draft
            </Button>
            <Button
              variant="accent"
              icon={<Send size={16} />}
              busy={save.isPending && mode === 'submit'}
              disabled={!canSubmit}
              onClick={() => save.mutate('submit')}
            >
              Save &amp; submit
            </Button>
          </>
        }
      />
      <ErrorAlert error={save.error} />
      <Card title="Voucher header">
        <JournalHeaderFields
          value={values.header}
          onChange={(header) => setValues((v) => ({ ...v, header }))}
        />
      </Card>
      <Card title="Lines">
        <JournalLinesEditor
          lines={values.lines}
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
  const defaultBranch = branches.find((b) => b.headOffice) ?? branches[0];
  return (
    <JournalForm
      key="new"
      initial={newJournalValues(defaultBranch?.id ?? 0, company?.baseCurrency ?? 'PHP')}
    />
  );
}
