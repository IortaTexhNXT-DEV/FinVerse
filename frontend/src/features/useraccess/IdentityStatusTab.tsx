import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import { userAccessApi } from '@/api/userAccess';
import type { IdentityStatus } from '@/api/userAccess';
import type { Definition } from '@/components/ui/DefinitionGrid';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';

function connectionItems(s: IdentityStatus | undefined, directoryName?: string): Definition[] {
  return [
    { label: 'Directory', value: directoryName },
    { label: 'Last call received', value: formatDateTime(s?.lastCall) || 'None yet' },
    { label: 'Last successful event', value: formatDateTime(s?.lastSuccess) || 'None yet' },
    { label: 'Errors in the last 24 hours', value: s?.errorsLast24Hours ?? 0 },
    { label: 'Refused events to review', value: s?.refusedOpen ?? 0 },
    { label: 'Mismatches to resolve', value: s?.mismatchesOpen ?? 0 },
  ];
}

function lastSyncItems(s: IdentityStatus | undefined): Definition[] {
  const status = s?.lastSyncStatus;
  return [
    { label: 'Started', value: formatDateTime(s?.lastSyncAt) || 'Never run' },
    { label: 'Result', value: status === undefined ? '' : <StatusBadge status={status} /> },
    { label: 'Details', value: s?.lastSyncMessage, wide: true },
  ];
}

/**
 * The state of the connection with UIDM-ISC: when it last called, when an event last applied, the
 * errors of the last 24 hours and the open work; Synchronise Now compares every user with the
 * directory at once and shows the result.
 */
export function IdentityStatusTab({ directoryName }: Readonly<{ directoryName?: string }>) {
  const toast = useToast();
  const queries = useQueryClient();
  const status = useQuery({
    queryKey: ['identity', 'status'],
    queryFn: userAccessApi.identityStatus,
  });
  const sync = useMutation({
    mutationFn: userAccessApi.synchroniseNow,
    onSuccess: async (run) => {
      toast.success(run.message ?? 'Synchronisation finished');
      await queries.invalidateQueries({ queryKey: ['identity'] });
    },
  });
  const s = status.data;
  return (
    <div className="stack">
      <ErrorAlert error={status.error ?? sync.error} title="Synchronisation not completed" />
      <Card
        title="Connection with UIDM-ISC"
        actions={
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
        <DefinitionGrid columns={2} items={connectionItems(s, directoryName)} />
      </Card>
      <Card title="Last synchronisation">
        <DefinitionGrid columns={2} items={lastSyncItems(s)} />
      </Card>
    </div>
  );
}
