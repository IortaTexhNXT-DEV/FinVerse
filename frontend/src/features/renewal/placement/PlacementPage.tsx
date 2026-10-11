import { useMutation, useQueryClient } from '@tanstack/react-query';
import { FileOutput, Send, ShieldCheck, Undo2 } from 'lucide-react';
import { renewalPlacementApi } from '@/api/renewalPlacement';
import type { SendSummary } from '@/api/renewalPlacement';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { CandidateList } from '../common/CandidateList';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import { useBatchAction } from '../common/useBatchAction';
import { useListDialogs } from '../common/useListDialogs';
import { ReturnPlacementDialog, SendAdviceDialog, SendPlacementDialog } from './PlacementDialogs';
import '../renewal.css';

type Open = 'send' | 'advice' | 'return';

function summaryText(s: SendSummary): string {
  const failed = Object.entries(s.failed);
  const head = `${String(s.submitted.length)} of ${String(s.selected)} account(s) sent`;
  return failed.length === 0
    ? head
    : head + '; not sent: ' + failed.map(([ref, why]) => `${ref} (${why})`).join(', ');
}

/**
 * Submitted for Placement (FRRN.029 to FRRN.032): the renewal accounts submitted for placement in
 * the user's scope with search, filters, sort and export; Generate Placement makes one slip per
 * insurer per account and the placement file per insurer, Send Placement sends them by MFT or CCM,
 * Send Insurance Advice sends the advice of mortgaged accounts, and an account submitted or rejected
 * can be returned to Marketing with a reason.
 */
export default function PlacementPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { open, refs, close, done, show } = useListDialogs<Open>();
  const generate = useBatchAction<string[]>('Generate Placement', 'generated', (r) =>
    renewalPlacementApi.generate(companyId, r),
  );
  const back = useBatchAction<{ reasonCode: string; remarks: string }>(
    'Return to Marketing',
    'returned',
    (body) => renewalPlacementApi.returnToMarketing(companyId, { refs, ...body }),
    done,
  );
  const send = useMutation({
    mutationFn: (body: { recipients: Record<string, string[]>; cc: string[] }) =>
      renewalPlacementApi.send(companyId, { refs, ...body }),
    onSuccess: async (s) => {
      done();
      toast.success(summaryText(s));
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  const advice = useMutation({
    mutationFn: (body: { to: string[]; cc: string[] }) =>
      renewalPlacementApi.sendAdvices(companyId, { refs, ...body }),
    onSuccess: (messages) => {
      done();
      toast.success(`Insurance Advice submitted to CCM (${messages.join(', ')})`);
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Submitted for Placement"
        description="Renewal accounts posted and submitted for placement and booking."
      />
      <CandidateList
        base={{ stage: 'FOR_PLACEMENT_BOOKING' }}
        emptyMessage="No renewal account is submitted for placement"
        actions={(selected, selection) => {
          const none = selected.length === 0;
          return (
            can('RNW_PROCESS') && (
              <span className="rnw-actions">
                <Button
                  icon={<FileOutput size={16} />}
                  disabled={none}
                  busy={generate.mutation.isPending}
                  onClick={() => generate.mutation.mutate(selected, { onSuccess: selection.clear })}
                >
                  Generate Placement
                </Button>
                <Button
                  variant="secondary"
                  icon={<Send size={16} />}
                  disabled={none}
                  onClick={() => show('send', selected, selection.clear)}
                >
                  Send Placement
                </Button>
                <Button
                  variant="secondary"
                  icon={<ShieldCheck size={16} />}
                  disabled={none}
                  onClick={() => show('advice', selected, selection.clear)}
                >
                  Send Insurance Advice
                </Button>
                <Button
                  variant="secondary"
                  icon={<Undo2 size={16} />}
                  disabled={none}
                  onClick={() => show('return', selected, selection.clear)}
                >
                  Return
                </Button>
              </span>
            )
          );
        }}
      />
      {open === 'send' && (
        <SendPlacementDialog
          refs={refs}
          busy={send.isPending}
          error={send.error}
          onClose={close}
          onConfirm={(body) => send.mutate(body)}
        />
      )}
      {open === 'advice' && (
        <SendAdviceDialog
          refs={refs}
          busy={advice.isPending}
          error={advice.error}
          onClose={close}
          onConfirm={(body) => advice.mutate(body)}
        />
      )}
      {open === 'return' && (
        <ReturnPlacementDialog
          refs={refs}
          busy={back.mutation.isPending}
          error={back.mutation.error}
          onClose={close}
          onConfirm={(body) => back.mutation.mutate(body)}
        />
      )}
      {generate.dialog}
      {back.dialog}
    </div>
  );
}
