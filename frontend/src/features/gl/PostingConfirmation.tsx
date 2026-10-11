import { useQuery } from '@tanstack/react-query';
import type { Journal } from '@/api/gl';
import { Amount } from '@/components/ui/Amount';
import { formatDate } from '@/utils/format';
import { ConfirmPostingDialog } from './ConfirmPostingDialog';
import { glPlatformApi } from './glPlatformApi';

/** Workflow step that asks for a confirmation (FRBS 2.5.10). */
export type PostingStep = 'submit' | 'approve';

const TEXT: Record<PostingStep, { title: string; intro: string; confirm: string }> = {
  submit: {
    title: 'Submit journal for posting',
    intro: 'The journal goes to an authorizer and can no longer be edited unless it is returned.',
    confirm: 'Submit',
  },
  approve: {
    title: 'Authorize and post journal',
    intro:
      'Posting updates the general ledger and the sub-ledger at once and cannot be undone; corrections are made by reversal.',
    confirm: 'Authorize & Post',
  },
};

interface Props {
  journal: Journal;
  step: PostingStep | null;
  busy: boolean;
  error: unknown;
  onConfirm: (step: PostingStep) => void;
  onClose: () => void;
}

/**
 * Confirmation of a journal's details before submission or posting, with the negative balance
 * warnings of its accounts (FRBS 2.5.5, 2.5.10, 2.8.3, 2.8.4).
 */
export function PostingConfirmation({
  journal,
  step,
  busy,
  error,
  onConfirm,
  onClose,
}: Readonly<Props>) {
  const warnings = useQuery({
    queryKey: ['journal-warnings', journal.id, step],
    queryFn: () => glPlatformApi.warnings(journal.id),
    enabled: step !== null,
  });
  const text = TEXT[step ?? 'submit'];
  return (
    <ConfirmPostingDialog
      open={step !== null}
      title={text.title}
      intro={text.intro}
      facts={[
        { label: 'Batch no.', value: journal.batchNo },
        { label: 'Value date', value: formatDate(journal.valueDate) },
        { label: 'Lines', value: journal.lines.length },
        { label: 'Total debit', value: <Amount value={journal.totalDebit} /> },
        { label: 'Total credit', value: <Amount value={journal.totalCredit} /> },
      ]}
      warnings={warnings.data}
      confirmLabel={text.confirm}
      busy={busy}
      error={error}
      onConfirm={() => step !== null && onConfirm(step)}
      onClose={onClose}
    />
  );
}
