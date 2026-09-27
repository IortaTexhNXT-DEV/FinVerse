import { CheckCheck, Undo2 } from 'lucide-react';
import { renewalApi } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { AssignDialog, ReasonDialog } from '../common/ActionDialogs';
import { CandidateList } from '../common/CandidateList';
import { OverrideDialog } from '../common/MoreDialogs';
import { RENEWAL_SECTION, RNW_LOV } from '../common/renewalCodes';
import type { RenewalTabDef } from '../common/renewalCodes';
import { useBatchAction } from '../common/useBatchAction';
import { useListDialogs } from '../common/useListDialogs';
import '../renewal.css';

const TABS: RenewalTabDef[] = [
  { id: 'REVIEW', label: 'For Review' },
  { id: 'EXCEPTIONS', label: 'Exceptions' },
  { id: 'UNASSIGNED', label: 'Unassigned' },
  { id: 'NB_PATH', label: 'Quotation / Proposal' },
  { id: 'ALL', label: 'All' },
];

type Open = 'return' | 'post' | 'override' | 'assign';

/**
 * TL Review (FR-RN-050, 051): the Team Leader reviews the dispositioned renewals of the
 * unit, returns them to the officer with a reason, posts them to Processing (straight-through
 * renewals move on without review) and overrides a check, the classification, the disposition or
 * the outstanding balance with a reason.
 */
export default function ReviewPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const { open, refs, close, done, show } = useListDialogs<Open>();
  const back = useBatchAction<{ reason: string; remarks: string }>(
    'Return to Account Officer',
    'returned',
    ({ reason, remarks }) => renewalApi.returnToAo(companyId, refs, reason, remarks),
    done,
  );
  const post = useBatchAction<null>('Post', 'posted', () => renewalApi.post(companyId, refs), done);
  const override = useBatchAction<{
    kind: string;
    target?: string;
    reasonCode: string;
    remarks: string;
  }>('Override', 'overridden', (input) => renewalApi.override(companyId, refs, input), done);
  const assign = useBatchAction<{ ao: string; reason: string }>(
    'Re-assign',
    'assigned',
    ({ ao, reason }) => renewalApi.assign(companyId, refs, ao, reason || undefined),
    done,
  );
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="TL Review"
        description="Dispositions of your unit waiting for review, and the exceptions."
      />
      <CandidateList
        tabs={TABS}
        actions={(selected, selection) => {
          const none = selected.length === 0;
          return (
            <span className="rnw-actions">
              <Button
                icon={<CheckCheck size={16} />}
                disabled={none}
                onClick={() => show('post', selected, selection.clear)}
              >
                Post
              </Button>
              <Button
                variant="secondary"
                icon={<Undo2 size={16} />}
                disabled={none}
                onClick={() => show('return', selected, selection.clear)}
              >
                Return
              </Button>
              {can('RNW_OVERRIDE') && (
                <Button
                  variant="secondary"
                  disabled={none}
                  onClick={() => show('override', selected, selection.clear)}
                >
                  Override
                </Button>
              )}
              {can('RNW_ASSIGN') && (
                <Button
                  variant="ghost"
                  disabled={none}
                  onClick={() => show('assign', selected, selection.clear)}
                >
                  Re-assign
                </Button>
              )}
            </span>
          );
        }}
      />
      {open === 'post' && (
        <ConfirmDialog
          title="Post to Processing"
          effect={`${String(refs.length)} renewal(s) move to Processing; renewals not for renewal go to the closing letters.`}
          confirmLabel="Post"
          busy={post.mutation.isPending}
          error={post.mutation.error}
          onClose={close}
          onConfirm={() => post.mutation.mutate(null)}
        />
      )}
      {open === 'return' && (
        <ReasonDialog
          title="Return to Account Officer"
          lov={RNW_LOV.returnReason}
          confirmLabel="Return"
          count={refs.length}
          busy={back.mutation.isPending}
          error={back.mutation.error}
          onClose={close}
          onConfirm={(reason, remarks) => back.mutation.mutate({ reason, remarks })}
        />
      )}
      {open === 'override' && (
        <OverrideDialog
          count={refs.length}
          busy={override.mutation.isPending}
          error={override.mutation.error}
          onClose={close}
          onConfirm={(input) => override.mutation.mutate(input)}
        />
      )}
      {open === 'assign' && (
        <AssignDialog
          count={refs.length}
          busy={assign.mutation.isPending}
          error={assign.mutation.error}
          onClose={close}
          onConfirm={(ao, reason) => assign.mutation.mutate({ ao, reason })}
        />
      )}
      {back.dialog}
      {post.dialog}
      {override.dialog}
      {assign.dialog}
    </div>
  );
}
