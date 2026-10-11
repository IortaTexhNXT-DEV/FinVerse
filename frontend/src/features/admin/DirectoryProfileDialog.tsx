import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import {
  DIRECTORY_STATUS_LABELS,
  EVENT_STATUS_LABELS,
  EVENT_TYPE_LABELS,
  identityApi,
} from '@/api/identity';
import type { DirectoryProfile } from '@/api/identity';
import { Button } from '@/components/ui/Button';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';

function rows(p: DirectoryProfile) {
  return [
    { label: 'Windows ID', value: p.windowsId },
    { label: 'AD e-mail', value: p.adEmail },
    { label: 'AD group', value: p.adGroup },
    { label: 'AD status', value: p.adStatus && DIRECTORY_STATUS_LABELS[p.adStatus] },
    { label: 'AD sync date / time', value: formatDateTime(p.adSyncAt) },
    { label: 'First name', value: p.firstName },
    { label: 'Last name', value: p.lastName },
    { label: 'Display name', value: p.displayName },
    { label: "Team leader's name", value: p.teamLeaderName },
    { label: "Team head's name", value: p.teamHeadName },
    { label: "Section head's name", value: p.sectionHeadName },
    { label: "Unit head's name", value: p.unitHeadName },
    { label: 'Unit / segment', value: p.unitSegment },
    { label: 'Department', value: p.department },
    { label: 'Location', value: p.location },
    { label: 'UIDM request no.', value: p.uidmRequestNo },
    {
      label: 'Last event',
      value:
        p.lastEventType &&
        `${EVENT_TYPE_LABELS[p.lastEventType]} from ${String(p.lastEventSource)}, ${formatDateTime(p.lastEventAt)}`,
    },
  ];
}

/**
 * The directory details of a user from the Enterprise SSO platform and the latest synchronisation,
 * with "Synchronise Now" (BDOI FRS FRUM.002.01 and FRUM.002.02).
 */
export function DirectoryProfileDialog({
  username,
  onClose,
}: Readonly<{ username: string | null; onClose: () => void }>) {
  return username === null ? null : <ProfileDialog username={username} onClose={onClose} />;
}

function ProfileDialog({ username, onClose }: Readonly<{ username: string; onClose: () => void }>) {
  const toast = useToast();
  const client = useQueryClient();
  const profile = useQuery({
    queryKey: ['identity-profile', username],
    queryFn: () => identityApi.profile(username),
  });
  const sync = useMutation({
    mutationFn: () => identityApi.synchronise(username),
    onSuccess: (event) => {
      toast.success(`Synchronisation: ${EVENT_STATUS_LABELS[event.status]}`);
      void client.invalidateQueries({ queryKey: ['identity-profile', username] });
      void client.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (error) => toast.error(error.message),
  });
  const p = profile.data;
  return (
    <Modal
      title={`Directory Details of ${username}`}
      open
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          icon={<RefreshCw size={16} />}
          busy={sync.isPending}
          onClick={() => sync.mutate()}
        >
          Synchronise Now
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={profile.error} />
        {p === undefined && !profile.isLoading && (
          <Notice tone="info">
            This user has not been synchronised from the Enterprise SSO platform yet.
          </Notice>
        )}
        {p !== undefined && (
          <>
            <p>
              Latest synchronisation:{' '}
              <StatusBadge
                status={p.syncStatus === 'SYNCED' ? 'COMPLETED' : 'FAILED'}
                label={p.syncStatus === 'SYNCED' ? 'Synchronised' : 'Failed'}
              />{' '}
              {formatDateTime(p.updatedAt ?? p.adSyncAt)}
            </p>
            {p.syncMessage && <Notice tone="error">{p.syncMessage}</Notice>}
            <DefinitionGrid items={rows(p)} />
          </>
        )}
      </div>
    </Modal>
  );
}
