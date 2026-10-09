import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { DIRECTORY_STATUS_LABELS, EVENT_STATUS_LABELS, identityApi } from '@/api/identity';
import type { DirectoryAccount, IdentityEventType } from '@/api/identity';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Notice } from '@/components/ui/Notice';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { EMPTY_ACCOUNT, SIMULATED_EVENTS } from './identitySync';
import { SimulatorAccountDialog } from './SimulatorAccountDialog';

/**
 * The Enterprise SSO simulator of SIT and UAT: the accounts of the simulated directory and the
 * events UIDM-ISC would send for them, applied as the real interface applies them.
 */
export function SimulatorTab() {
  const toast = useToast();
  const client = useQueryClient();
  const [editing, setEditing] = useState<{ account: DirectoryAccount; isNew: boolean } | null>(
    null,
  );
  const accounts = useQuery({
    queryKey: ['identity-simulator'],
    queryFn: identityApi.simulatorAccounts,
  });
  const refresh = () => {
    void client.invalidateQueries({ queryKey: ['identity-simulator'] });
    void client.invalidateQueries({ queryKey: ['identity-events'] });
  };
  const send = useMutation({
    mutationFn: ({ windowsId, type }: { windowsId: string; type: IdentityEventType }) =>
      identityApi.sendSimulatorEvent(windowsId, type),
    onSuccess: (event) => {
      const outcome = `${EVENT_STATUS_LABELS[event.status]}: ${event.message ?? ''}`;
      if (event.status === 'APPLIED' || event.status === 'NO_CHANGE') {
        toast.success(outcome);
      } else {
        toast.warning(outcome);
      }
      refresh();
    },
    onError: (error) => toast.error(error.message),
  });
  return (
    <div className="stack">
      <Notice tone="info">
        SIT and UAT only. These accounts stand for the Enterprise SSO platform until it is
        connected; the sign-in page of the simulator and the events below use them.
      </Notice>
      <ErrorAlert error={accounts.error} />
      <Card
        flush
        title="Enterprise SSO accounts"
        actions={
          <Button
            variant="accent"
            icon={<Plus size={16} />}
            onClick={() => setEditing({ account: EMPTY_ACCOUNT, isNew: true })}
          >
            New Account
          </Button>
        }
      >
        <DataTable<DirectoryAccount>
          loading={accounts.isLoading}
          rows={accounts.data ?? []}
          rowKey={(a) => a.windowsId}
          columns={[
            { key: 'w', header: 'Windows ID', render: (a) => <strong>{a.windowsId}</strong> },
            { key: 'u', header: 'User ID', render: (a) => a.userId },
            { key: 'n', header: 'Display Name', render: (a) => a.displayName ?? '' },
            { key: 'e', header: 'AD E-mail', render: (a) => a.email },
            { key: 'g', header: 'Unit / Segment', render: (a) => a.unitSegment ?? '' },
            { key: 'l', header: 'Location', render: (a) => a.location ?? '' },
            {
              key: 's',
              header: 'AD Status',
              kind: 'status',
              render: (a) => (
                <StatusBadge
                  status={a.status ?? 'ACTIVE'}
                  label={DIRECTORY_STATUS_LABELS[a.status ?? 'ACTIVE']}
                />
              ),
            },
            {
              key: 'a',
              header: 'Actions',
              render: (a) => (
                <RowActionMenu
                  label={a.windowsId}
                  actions={[
                    { label: 'Change', onSelect: () => setEditing({ account: a, isNew: false }) },
                    ...SIMULATED_EVENTS.map((e) => ({
                      label: e.label,
                      onSelect: () => send.mutate({ windowsId: a.windowsId, type: e.type }),
                    })),
                  ]}
                />
              ),
            },
          ]}
        />
      </Card>
      {editing !== null && (
        <SimulatorAccountDialog
          account={editing.account}
          isNew={editing.isNew}
          onClose={() => setEditing(null)}
          onSaved={refresh}
        />
      )}
    </div>
  );
}
