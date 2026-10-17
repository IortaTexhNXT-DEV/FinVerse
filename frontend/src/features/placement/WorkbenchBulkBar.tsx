import { useMutation } from '@tanstack/react-query';
import { BookCheck, Ban, FileOutput, RotateCcw, Send } from 'lucide-react';
import { useState } from 'react';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { placementApi } from '@/api/placement';
import type { ItemResult, WorkbenchRow, WorkbenchTab } from '@/api/placement';
import type { ActionNote } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { ItemResultsDialog } from '@/components/broking/ItemResultsDialog';
import type { RowSelection } from '@/components/broking/rowSelection';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { GenerateSlipsDialog } from './GenerateSlipsDialog';
import { actionsOf, bookingLink, selectionFor } from './placementLogic';
import type { BulkAction } from './placementLogic';

interface BulkBarProps {
  companyId: number;
  tab: WorkbenchTab;
  rows: WorkbenchRow[];
  selection: RowSelection;
  onChanged: () => void;
}

const BUTTONS: Record<BulkAction, { label: string; icon: ReactNode; permissions: string[] }> = {
  PLACE: {
    label: 'For Placement',
    icon: <FileOutput size={16} />,
    permissions: ['PLACEMENT_MANAGE'],
  },
  SEND: { label: 'Send Slips', icon: <Send size={16} />, permissions: ['PLACEMENT_MANAGE'] },
  BOOK: { label: 'For Booking', icon: <BookCheck size={16} />, permissions: ['BOOKING_PROCESS'] },
  CANCEL: { label: 'Cancel Placement', icon: <Ban size={16} />, permissions: ['PLACEMENT_MANAGE'] },
  REACTIVATE: {
    label: 'Reactivate',
    icon: <RotateCcw size={16} />,
    permissions: ['PLACEMENT_MANAGE', 'ACCOUNT_MAINTAIN'],
  },
};

type NoteAction = 'CANCEL' | 'REACTIVATE';

/**
 * Bulk actions of the Placement Workbench, on the right of the toolbar and enabled by the row
 * selection: For Placement (generate slips), Send Slips, For Booking, Cancel Placement
 * (BRNB.062) and Reactivate (BRD 2.1.16). Each account is processed separately and the outcome is
 * listed.
 */
export function WorkbenchBulkBar({
  companyId,
  tab,
  rows,
  selection,
  onChanged,
}: Readonly<BulkBarProps>) {
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [placing, setPlacing] = useState<string[] | null>(null);
  const [noting, setNoting] = useState<{ action: NoteAction; arns: string[] } | null>(null);
  const [results, setResults] = useState<{ title: string; items: ItemResult[] } | null>(null);
  const finish = (title: string, items: ItemResult[]) => {
    setNoting(null);
    selection.clear();
    setResults({ title, items });
    onChanged();
  };
  const send = useMutation({
    mutationFn: (arns: string[]) => placementApi.sendSlips(companyId, arns),
    onSuccess: (items) => finish('Send Slips', items),
  });
  const note = useMutation({
    mutationFn: ({ action, arns, n }: { action: NoteAction; arns: string[]; n: ActionNote }) =>
      action === 'CANCEL'
        ? placementApi.cancel(arns, n.reasonCode ?? '', n.comment)
        : placementApi.reactivate(arns, n.comment),
    onSuccess: (items, { action }) => finish(BUTTONS[action].label, items),
  });
  const run = (action: BulkAction, arns: string[]) => {
    if (action === 'BOOK') {
      void navigate(bookingLink(arns));
    } else if (action === 'SEND') {
      send.mutate(arns);
    } else if (action === 'PLACE') {
      setPlacing(arns);
    } else {
      setNoting({ action, arns });
    }
  };
  const actions = actionsOf(tab).filter((a) => BUTTONS[a].permissions.some((p) => can(p)));
  return (
    <>
      <ErrorAlert error={send.error} />
      {actions.map((a) => {
        const { arns, enabled } = selectionFor(a, rows, selection.keys);
        return (
          <Button
            key={a}
            variant={a === 'PLACE' || a === 'BOOK' ? 'primary' : 'secondary'}
            icon={BUTTONS[a].icon}
            disabled={!enabled}
            busy={a === 'SEND' && send.isPending}
            onClick={() => run(a, arns)}
          >
            {BUTTONS[a].label}
          </Button>
        );
      })}
      {placing && (
        <GenerateSlipsDialog
          companyId={companyId}
          arns={placing}
          onClose={() => setPlacing(null)}
          onDone={(slips) => {
            setPlacing(null);
            selection.clear();
            toast.success(
              `${slips.length} placement slip(s) generated: ${slips.map((s) => s.displayNo).join(', ')}`,
            );
            onChanged();
          }}
        />
      )}
      {noting && (
        <ActionDialog
          title={`${BUTTONS[noting.action].label} (${noting.arns.length})`}
          reasonLov={noting.action === 'CANCEL' ? 'CANCELLATION_REASON' : undefined}
          confirmLabel={BUTTONS[noting.action].label}
          busy={note.isPending}
          error={note.error}
          onClose={() => setNoting(null)}
          onConfirm={(n) => note.mutate({ ...noting, n })}
        />
      )}
      {results && (
        <ItemResultsDialog
          title={results.title}
          results={results.items.map((r) => ({ reference: r.arn, ok: r.ok, message: r.message }))}
          onClose={() => setResults(null)}
        />
      )}
    </>
  );
}
