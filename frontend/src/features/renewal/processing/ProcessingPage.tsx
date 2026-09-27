import { Undo2, UserCheck, UserPlus } from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { CandidateList } from '../common/CandidateList';
import type { QuickFilter } from '../common/CandidateList';
import { PoAssignDialog, ReturnToMarketingDialog } from '../common/MoreDialogs';
import { PROCESSING_TABS, RENEWAL_SECTION, tabOf } from '../common/renewalCodes';
import { useBatchAction } from '../common/useBatchAction';
import { useListDialogs } from '../common/useListDialogs';
import '../renewal.css';

type Open = 'assign' | 'return';

/**
 * Processing Worklist (FR-RN-061-065): the posted renewals for the Renewal processing team. The
 * lead assigns a Processing Officer (or an officer takes a renewal), the officer creates the
 * renewal account, sends the insurer batch, records the insurer's reply and returns a renewal to
 * Marketing with a reason when it cannot proceed.
 */
export default function ProcessingPage() {
  const companyId = useCompanyId();
  const { can, user } = useAuth();
  const [params] = useSearchParams();
  const { open, refs, close, done, show } = useListDialogs<Open>();
  const assign = useBatchAction<string | undefined>(
    'Assign Processing Officer',
    'assigned',
    (po) => renewalApi.assignPo(companyId, refs, po),
    done,
  );
  const takeMine = useBatchAction<string[]>('Assign to Me', 'assigned to you', (r) =>
    renewalApi.assignPo(companyId, r),
  );
  const back = useBatchAction<{ toLeader: boolean; reasonCode: string; remarks: string }>(
    'Return to Marketing',
    'returned',
    (input) => renewalApi.returnToMarketing(companyId, refs, input),
    done,
  );
  const quick: QuickFilter[] = [
    { id: 'mine', label: 'Assigned to me', filters: { assignedPo: user?.username } },
    { id: 'due30', label: 'Due in 30 days', filters: { dueWithin: 30 } },
  ];
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Processing Worklist"
        description="Posted renewals: renewal accounts, insurer batches and replies."
      />
      <CandidateList
        tabs={PROCESSING_TABS}
        initialTab={tabOf(PROCESSING_TABS, params.get('tab'))}
        quickFilters={quick}
        actions={(selected, selection) => {
          const none = selected.length === 0;
          return (
            <span className="rnw-actions">
              {can('RNW_PROCESS_ASSIGN') && (
                <>
                  <Button
                    icon={<UserPlus size={16} />}
                    disabled={none}
                    onClick={() => show('assign', selected, selection.clear)}
                  >
                    Assign PO
                  </Button>
                  <Button
                    variant="secondary"
                    icon={<UserCheck size={16} />}
                    disabled={none}
                    busy={takeMine.mutation.isPending}
                    onClick={() =>
                      takeMine.mutation.mutate(selected, { onSuccess: selection.clear })
                    }
                  >
                    Assign to Me
                  </Button>
                </>
              )}
              {can('RNW_PROCESS') && (
                <Button
                  variant="secondary"
                  icon={<Undo2 size={16} />}
                  disabled={none}
                  onClick={() => show('return', selected, selection.clear)}
                >
                  Return to Marketing
                </Button>
              )}
            </span>
          );
        }}
      />
      {open === 'assign' && (
        <PoAssignDialog
          count={refs.length}
          busy={assign.mutation.isPending}
          error={assign.mutation.error}
          onClose={close}
          onConfirm={(po) => assign.mutation.mutate(po)}
        />
      )}
      {open === 'return' && (
        <ReturnToMarketingDialog
          count={refs.length}
          busy={back.mutation.isPending}
          error={back.mutation.error}
          onClose={close}
          onConfirm={(input) => back.mutation.mutate(input)}
        />
      )}
      {assign.dialog}
      {takeMine.dialog}
      {back.dialog}
    </div>
  );
}
