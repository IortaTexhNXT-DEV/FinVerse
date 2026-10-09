import type { ContactChange } from '@/api/csf';
import { useLovLabel } from '@/components/broking/useLabels';
import { CellStack } from '@/components/ui/CellStack';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';
import { CSF_LOV } from '../csfCodes';

const SYNC_LABELS: Record<string, string> = {
  NOT_REQUIRED: 'Not Required',
  NOT_CONFIGURED: 'Not Configured',
  QUEUED: 'Queued',
  SENT: 'Sent',
  FAILED: 'Failed',
};

/** Columns of contact changes, shared by the Contact History tab and the Contact Changes list. */
export function useChangeColumns(withClient: boolean): Column<ContactChange>[] {
  const reason = useLovLabel(CSF_LOV.reason);
  const channel = useLovLabel(CSF_LOV.channel);
  const columns: Column<ContactChange>[] = [
    { key: 'no', header: 'Change No.', kind: 'code', render: (c) => c.changeNo },
    { key: 'at', header: 'Date and Time', kind: 'datetime', render: (c) => formatDateTime(c.at) },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (c) => (
        <StatusBadge
          status={c.status}
          tone={c.status === 'REFUSED' ? 'danger' : undefined}
          label={c.status === 'REFERRED' ? 'Referred' : undefined}
        />
      ),
    },
    {
      key: 'verification',
      header: 'Verification',
      defaultHidden: true,
      render: (c) =>
        c.verificationResult ? (
          <CellStack
            main={c.verificationResult === 'PASSED' ? 'Passed' : 'Failed'}
            sub={`${String(c.verificationMatches ?? 0)} checks matched`}
          />
        ) : (
          ''
        ),
    },
    {
      key: 'reason',
      header: 'Reason',
      render: (c) => (
        <CellStack main={c.reasonCode ? reason(c.reasonCode) : ''} sub={channel(c.channel)} />
      ),
    },
    {
      key: 'agent',
      header: 'Agent',
      defaultHidden: true,
      render: (c) => <UserName login={c.agent} />,
    },
    {
      key: 'sync',
      header: 'Legacy Sync',
      kind: 'status',
      render: (c) =>
        c.handoffStatus ? (
          <StatusBadge
            status={c.handoffStatus}
            label={c.handoffStatus === 'OPEN' ? 'With Fulfilment' : 'Done'}
          />
        ) : (
          <StatusBadge status={c.syncStatus} label={SYNC_LABELS[c.syncStatus] ?? c.syncStatus} />
        ),
    },
  ];
  if (withClient) {
    columns.splice(2, 0, {
      key: 'client',
      header: 'Client',
      render: (c) => <CellStack main={c.clientName} sub={c.clientCode} />,
    });
  }
  return columns;
}
