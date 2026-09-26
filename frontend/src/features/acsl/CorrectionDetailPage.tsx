import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Banknote, BookOpen, CalendarDays, FileText, Layers, User } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import type { WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { RecordSummary } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { CORRECTION_LABELS, correctionActions, totals, optionalText } from './acsl';
import type { CorrectionAction } from './acsl';
import { acslApi, CORRECTION_ENTITY } from './api';
import type { Correction } from './api';
import { CorrectionLinesCard } from './CorrectionLinesCard';
import { FormDialog } from './FormDialog';
import type { DialogField } from './FormDialog';
import { OriginalLinesCard } from './OriginalLinesCard';

function fieldsOf(action: CorrectionAction): DialogField[] {
  return action === 'assign'
    ? [{ key: 'username', label: 'Preparer (User ID)', required: true }]
    : [{ key: 'comment', label: 'Comment', multiline: true }];
}

function perform(c: Correction, action: CorrectionAction, v: Record<string, string>) {
  const comment = optionalText(v.comment);
  switch (action) {
    case 'assign':
      return acslApi.assignCorrection(c.id, (v.username ?? '').trim());
    case 'submit':
      return acslApi.submitCorrection(c.id, comment);
    case 'endorse':
      return acslApi.endorseCorrection(c.id, comment);
    default:
      return acslApi.approveCorrection(c.id, comment);
  }
}

/** Business buttons of a correction (ACSL 2.7-2.15): assign, submit, endorse, approve and post. */
function CorrectionActions({ c, actions }: Readonly<{ c: Correction; actions: WorkAction[] }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState<CorrectionAction | null>(null);
  const act = useMutation({
    mutationFn: ({ a, v }: { a: CorrectionAction; v: Record<string, string> }) => perform(c, a, v),
    onSuccess: async (_r, { a }) => {
      setOpen(null);
      await queryClient.invalidateQueries({ queryKey: ['acsl'] });
      await queryClient.invalidateQueries({ queryKey: workflowKey(CORRECTION_ENTITY, c.id) });
      toast.success(`${CORRECTION_LABELS[a]}: done`);
    },
  });
  const balanced = totals(c.lines).balanced;
  return (
    <>
      {correctionActions(actions.map((a) => a.action)).map((a) => (
        <Button
          key={a}
          variant={a === 'approve' ? 'accent' : 'primary'}
          disabled={a === 'submit' && !balanced}
          title={a === 'submit' && !balanced ? 'Save balanced lines first' : undefined}
          onClick={() => setOpen(a)}
        >
          {CORRECTION_LABELS[a]}
        </Button>
      ))}
      {open && (
        <FormDialog
          title={`${CORRECTION_LABELS[open]} · ${c.correctionNo}`}
          confirmLabel={CORRECTION_LABELS[open]}
          fields={fieldsOf(open)}
          busy={act.isPending}
          error={act.error}
          onConfirm={(v) => act.mutate({ a: open, v })}
          onClose={() => setOpen(null)}
        />
      )}
    </>
  );
}

function facts(c: Correction): Fact[] {
  return [
    { icon: Layers, label: 'Kind', value: humanize(c.kind) },
    { icon: FileText, label: 'Invoice', value: c.invoiceNo ?? '—' },
    { icon: BookOpen, label: 'Journal Corrected', value: c.originalBatchNo ?? '—' },
    { icon: Banknote, label: 'Total', value: `${c.currency} ${formatAmount(c.totalDebit)}` },
    { icon: User, label: 'Raised By', value: c.createdBy },
    { icon: CalendarDays, label: 'Raised', value: formatDate(c.createdAt) },
  ];
}

/**
 * One correction entry (ACSL 2.7-2.15, 2.9.1): the posted lines of the invoice family to pick the
 * wrong one from, the correction lines with their balance, and the review and approval that post
 * the linked journal, the open items and the ledger movement.
 */
export default function CorrectionDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const found = useQuery({
    queryKey: ['acsl', 'correction', id],
    queryFn: () => acslApi.getCorrection(id),
  });
  if (found.data === undefined) {
    return found.error ? (
      <ErrorAlert error={found.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const c = found.data;
  const editable = c.stage === 'DRAFT' && can('ACSL_PROCESS');
  return (
    <div className="stack">
      <PageHeader
        backTo="/acsl/corrections"
        section="Finance · ACSL"
        title={c.correctionNo}
        description={c.description}
        actions={
          c.caseId && (
            <Link className="btn btn-secondary" to={`/acsl/cases/${String(c.caseId)}`}>
              Open the Case
            </Link>
          )
        }
      />
      <RecordSummary
        title={c.description}
        chips={
          <>
            <ReferenceChip label="Correction" value={c.correctionNo} />
            {c.journalBatchNo && <ReferenceChip label="Journal" value={c.journalBatchNo} />}
            {c.rootInvoiceNo && <ReferenceChip label="Root Invoice" value={c.rootInvoiceNo} />}
            <StatusBadge status={c.stage} />
          </>
        }
        facts={facts(c)}
      />
      {c.returnComment && c.stage === 'DRAFT' && (
        <div className="alert warning" role="status">
          Returned: {c.returnComment}
        </div>
      )}
      {c.stage === 'POSTED' && (
        <div className="alert success" role="status">
          Posted as {c.journalBatchNo ?? '—'} with {String(c.openItems)} open item(s) and{' '}
          {String(c.ledgerMovements)} ledger movement(s).
        </div>
      )}
      <WorkflowPanel
        entityType={CORRECTION_ENTITY}
        entityId={c.id}
        onChanged={() => void queryClient.invalidateQueries({ queryKey: ['acsl'] })}
        renderBusinessActions={(actions) => <CorrectionActions c={c} actions={actions} />}
      />
      {c.invoiceNo && <OriginalLinesCard correction={c} editable={editable} />}
      <CorrectionLinesCard correction={c} editable={editable} />
    </div>
  );
}
