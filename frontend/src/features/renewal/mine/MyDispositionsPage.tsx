import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { renewalApi } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { TransferDialog } from '../common/ActionDialogs';
import { CandidateList } from '../common/CandidateList';
import type { QuickFilter } from '../common/CandidateList';
import { EXPIRY_TABS, RENEWAL_SECTION } from '../common/renewalCodes';
import { useBatchAction } from '../common/useBatchAction';
import { useListDialogs } from '../common/useListDialogs';
import '../renewal.css';

const TABS = [
  { id: 'ALL' as const, label: 'All' },
  ...EXPIRY_TABS.filter(
    (t) => !['ALL', 'EXTRACTED', 'UNASSIGNED', 'TRANSFER_PENDING'].includes(t.id),
  ),
];

const QUICK: QuickFilter[] = [
  { id: 'open', label: 'To disposition', filters: { stage: 'FOR_DISPOSITION' } },
  { id: 'returned', label: 'Returned to me', filters: { returned: true } },
  { id: 'due30', label: 'Due in 30 days', filters: { dueWithin: 30 } },
  { id: 'nrns', label: 'NRNS', filters: { nrns: true } },
];

/**
 * My Dispositions (FR-RN-040-047, 060): the renewals assigned to the officer. Open a renewal to view
 * its account history and set the disposition; Push sends the dispositioned renewals to the Team
 * Leader; Transfer moves one to another unit. The dispositioned file is uploaded by Processing on
 * the Processing Worklist.
 */
export default function MyDispositionsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { open, refs, close, done, show } = useListDialogs<'transfer'>();
  const push = useBatchAction<string[]>('Push', 'pushed', (r) => renewalApi.push(companyId, r));
  const transfer = useMutation({
    mutationFn: (input: { toUnit: string; reasonCode: string; remarks: string }) =>
      renewalApi.requestTransfer(companyId, {
        renewalRef: refs[0] ?? '',
        toUnit: input.toUnit,
        reasonCode: input.reasonCode || undefined,
        remarks: input.remarks,
      }),
    onSuccess: async () => {
      done();
      toast.success('Transfer requested');
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="My Dispositions"
        description="Renewals assigned to you for a disposition."
      />
      <CandidateList
        tabs={TABS}
        initialTab="ALL"
        base={{ mine: true }}
        quickFilters={QUICK}
        emptyMessage="No renewals assigned to you"
        actions={(selected, selection) => (
          <span className="rnw-actions">
            <Button
              icon={<Send size={16} />}
              disabled={selected.length === 0}
              busy={push.mutation.isPending}
              onClick={() => push.mutation.mutate(selected, { onSuccess: selection.clear })}
            >
              Push
            </Button>
            {can('RNW_ASSIGN') && (
              <Button
                variant="secondary"
                disabled={selected.length !== 1}
                title="Select one renewal"
                onClick={() => show('transfer', selected, selection.clear)}
              >
                Transfer
              </Button>
            )}
          </span>
        )}
      />
      {open === 'transfer' && (
        <TransferDialog
          busy={transfer.isPending}
          error={transfer.error}
          onClose={close}
          onConfirm={(toUnit, reasonCode, remarks) =>
            transfer.mutate({ toUnit, reasonCode, remarks })
          }
        />
      )}
      {push.dialog}
    </div>
  );
}
