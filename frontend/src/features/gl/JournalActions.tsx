import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Copy, Pencil, RotateCcw, Send, XCircle } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { glApi } from '@/api/gl';
import type { Journal } from '@/api/gl';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import { PostingConfirmation } from './PostingConfirmation';
import type { PostingStep } from './PostingConfirmation';

type Dialog = 'reject' | 'reverse' | PostingStep | null;

const RUN: Record<PostingStep, (id: number) => Promise<Journal>> = {
  submit: glApi.submitJournal,
  approve: glApi.approveJournal,
};

interface Allowed {
  edit: boolean;
  authorize: boolean;
  reverse: boolean;
  copy: boolean;
}

function allowedActions(
  j: Journal,
  username: string | undefined,
  can: (p: string) => boolean,
): Allowed {
  const isMaker = username === j.createdBy;
  return {
    edit: (j.status === 'DRAFT' || j.status === 'REJECTED') && isMaker,
    authorize:
      j.status === 'PENDING_APPROVAL' && can('JOURNAL_AUTHORIZE') && username !== j.submittedBy,
    reverse: j.status === 'POSTED' && can('JOURNAL_REVERSE'),
    copy: can('JOURNAL_CREATE'),
  };
}

function postingStep(dialog: Dialog): PostingStep | null {
  return dialog === 'submit' || dialog === 'approve' ? dialog : null;
}

/**
 * Workflow actions permitted for a journal's status and the user's role: edit/submit/cancel for
 * the maker, authorize/reject for a different checker, reverse for posted journals, copy.
 */
export function JournalActions({ journal }: Readonly<{ journal: Journal }>) {
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { user, can } = useAuth();
  const [dialog, setDialog] = useState<Dialog>(null);
  const [reason, setReason] = useState('');
  const [reversalDate, setReversalDate] = useState(today());
  const id = journal.id;
  const allowed = allowedActions(journal, user?.username, can);

  const action = useMutation({
    mutationFn: (run: () => Promise<Journal>) => run(),
    onSuccess: async (result) => {
      setDialog(null);
      setReason('');
      await queryClient.invalidateQueries({ queryKey: ['journals'] });
      await queryClient.invalidateQueries({ queryKey: ['journal'] });
      toast.success(`${result.batchNo}: ${result.status.replace('_', ' ')}`);
      if (result.id !== id) {
        await navigate(`/gl/journals/${result.id}`);
      }
    },
  });
  const confirm = () =>
    action.mutate(() =>
      dialog === 'reject'
        ? glApi.rejectJournal(id, reason)
        : glApi.reverseJournal(id, reversalDate, reason),
    );

  return (
    <>
      {allowed.edit && (
        <>
          <Button
            variant="secondary"
            icon={<Pencil size={16} />}
            onClick={() => void navigate(`/gl/journals/${id}/edit`)}
          >
            Edit
          </Button>
          <Button
            variant="accent"
            icon={<Send size={16} />}
            busy={action.isPending}
            onClick={() => setDialog('submit')}
          >
            Submit
          </Button>
          <Button variant="ghost" onClick={() => action.mutate(() => glApi.cancelJournal(id))}>
            Cancel Voucher
          </Button>
        </>
      )}
      {allowed.authorize && (
        <>
          <Button
            variant="accent"
            icon={<CheckCircle2 size={16} />}
            busy={action.isPending}
            onClick={() => setDialog('approve')}
          >
            Authorize &amp; Post
          </Button>
          <Button variant="danger" icon={<XCircle size={16} />} onClick={() => setDialog('reject')}>
            Return to Maker
          </Button>
        </>
      )}
      {allowed.reverse && (
        <Button
          variant="secondary"
          icon={<RotateCcw size={16} />}
          onClick={() => setDialog('reverse')}
        >
          Reverse
        </Button>
      )}
      {allowed.copy && (
        <Button
          variant="ghost"
          icon={<Copy size={16} />}
          onClick={() => action.mutate(() => glApi.copyJournal(id, today()))}
        >
          Copy
        </Button>
      )}
      {action.error !== null && dialog === null && <ErrorAlert error={action.error} />}
      <PostingConfirmation
        journal={journal}
        step={postingStep(dialog)}
        busy={action.isPending}
        error={action.error}
        onConfirm={(step) => action.mutate(() => RUN[step](id))}
        onClose={() => setDialog(null)}
      />
      <Modal
        title={dialog === 'reject' ? 'Return journal to its maker' : 'Reverse journal'}
        open={dialog === 'reject' || dialog === 'reverse'}
        onClose={() => setDialog(null)}
        footer={
          <Button
            variant={dialog === 'reject' ? 'danger' : 'accent'}
            disabled={reason.trim() === ''}
            busy={action.isPending}
            onClick={confirm}
          >
            Confirm
          </Button>
        }
      >
        <div className="stack">
          <ErrorAlert error={action.error} />
          {dialog === 'reverse' && (
            <Field label="Reversal value date" required>
              {(fid) => (
                <input
                  id={fid}
                  className="input"
                  type="date"
                  value={reversalDate}
                  onChange={(e) => setReversalDate(e.target.value)}
                />
              )}
            </Field>
          )}
          <Field label="Reason / remarks" required>
            {(fid) => (
              <textarea
                id={fid}
                className="textarea"
                maxLength={200}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            )}
          </Field>
        </div>
      </Modal>
    </>
  );
}
