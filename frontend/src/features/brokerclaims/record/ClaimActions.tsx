import { useMutation, useQueryClient } from '@tanstack/react-query';
import { KeyRound, Mail, RefreshCw, Layers } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { LossAdviceDialog } from '../insurer/LossAdviceDialog';
import { ClaimStatusActions } from '../status/ClaimStatusActions';
import type { Claim } from './api';
import { claimApi } from './api';
import { AuthorizeDialog } from './ClaimDialogs';
import { authorizationBlock } from './recordLogic';

type Dialog = 'authorize' | 'advice';

/**
 * Page actions of a claim (BRCLM.001/016/039, FR-CM-014/015/016/024): Generate Authorization Code
 * (disabled while the premium is not paid), Send Loss Advice, Refresh Cover Data and Use Latest
 * Version when a newer cover version exists, and the status actions of wave CL1-B (Change Status,
 * Set Settlement, Override Follow-up Date, Assign Adjuster, Reopen).
 */
export function ClaimActions({ claim, companyId }: Readonly<{ claim: Claim; companyId: number }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<Dialog>();
  const refreshClaim = () =>
    queryClient.invalidateQueries({ queryKey: ['broker-claims', 'claim', claim.id] });
  const authorize = useMutation({
    mutationFn: (evidence?: number) => claimApi.authorize(companyId, claim.id, evidence),
    onSuccess: async (c) => {
      toast.success(`Authorization code ${c.premium.authorizationCode ?? ''} generated`);
      setDialog(undefined);
      await refreshClaim();
    },
  });
  const refreshCover = useMutation({
    mutationFn: () => claimApi.refreshCover(companyId, claim.id),
    onSuccess: async (changes) => {
      toast.success(
        changes.length === 0
          ? 'Cover data is current'
          : `Cover data refreshed: ${changes.join('; ')}`,
      );
      await refreshClaim();
    },
  });
  const latest = useMutation({
    mutationFn: () => claimApi.useLatestVersion(companyId, claim.id),
    onSuccess: async (c) => {
      toast.success(`The claim now uses ${c.cover.versionLabel}`);
      await refreshClaim();
    },
  });
  const open = claim.progress.phase !== 'CLOSED';
  const block = authorizationBlock(
    claim.premium.live.status,
    claim.premium.dpPolicy,
    claim.premium.authorizationCode !== undefined,
  );
  return (
    <>
      <ClaimStatusActions claimId={claim.id} companyId={companyId} />
      {open && can('BCL_AUTHORIZE') && (
        <Button
          variant="accent"
          icon={<KeyRound size={16} />}
          disabled={block !== undefined}
          title={block}
          onClick={() => setDialog('authorize')}
        >
          Generate Authorization Code
        </Button>
      )}
      {can('BCL_RECORD') && (
        <Button variant="secondary" icon={<Mail size={16} />} onClick={() => setDialog('advice')}>
          Send Loss Advice
        </Button>
      )}
      {open && can('BCL_RECORD') && (
        <Button
          variant="secondary"
          icon={<RefreshCw size={16} />}
          busy={refreshCover.isPending}
          onClick={() => refreshCover.mutate()}
        >
          Refresh Cover Data
        </Button>
      )}
      {open && can('BCL_RECORD') && claim.flags.newerCoverVersion && (
        <Button
          variant="secondary"
          icon={<Layers size={16} />}
          busy={latest.isPending}
          onClick={() => latest.mutate()}
        >
          Use Latest Version
        </Button>
      )}
      <ErrorAlert error={refreshCover.error ?? latest.error} />
      {dialog === 'authorize' && (
        <AuthorizeDialog
          claim={claim}
          busy={authorize.isPending}
          error={authorize.error}
          onClose={() => setDialog(undefined)}
          onConfirm={(evidence) => authorize.mutate(evidence)}
        />
      )}
      {dialog === 'advice' && (
        <LossAdviceDialog
          companyId={companyId}
          claimId={claim.id}
          onClose={() => setDialog(undefined)}
          onSent={() => {
            setDialog(undefined);
            void queryClient.invalidateQueries({ queryKey: ['messages'] });
            void queryClient.invalidateQueries({ queryKey: ['attachments'] });
          }}
        />
      )}
    </>
  );
}
