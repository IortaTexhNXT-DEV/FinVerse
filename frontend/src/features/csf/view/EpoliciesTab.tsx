import { useQuery } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { useState } from 'react';
import { csfApi } from '@/api/csf';
import type { EpolicyRow } from '@/api/csf';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import { ResendDialog } from '../dialogs/ResendDialog';
import { TAB_UNAVAILABLE } from './AccountsTab';

const COLUMNS: Column<EpolicyRow>[] = [
  {
    key: 'policy',
    header: 'Policy No.',
    kind: 'code',
    render: (e) => <CellStack main={e.policyNumbers.join(', ')} sub={e.arn} />,
  },
  { key: 'file', header: 'File', render: (e) => e.fileName },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (e) => <StatusBadge status={e.status} />,
  },
  {
    key: 'received',
    header: 'Received',
    kind: 'datetime',
    render: (e) => formatDateTime(e.receivedAt),
  },
  {
    key: 'sent',
    header: 'Last Sent',
    render: (e) =>
      e.lastSentAt ? <CellStack main={formatDateTime(e.lastSentAt)} sub={e.lastSentTo} /> : '',
  },
  { key: 'count', header: 'Times Sent', kind: 'center', render: (e) => String(e.dispatchCount) },
];

/**
 * The E-policies tab (FR-CSF-031; CSF-EM09): the e-policies of the client's accounts; a confirmed
 * e-policy is resent through the dispatch service of Issuance, encrypted with the password apart.
 */
export function EpoliciesTab({
  companyId,
  clientId,
}: Readonly<{ companyId: number; clientId: number }>) {
  const { can } = useAuth();
  const [resending, setResending] = useState<EpolicyRow | null>(null);
  const list = useQuery({
    queryKey: ['csf', 'epolicies', companyId, clientId],
    queryFn: () => csfApi.epolicies(companyId, clientId),
  });
  const actions: Column<EpolicyRow> = {
    key: 'actions',
    header: '',
    render: (e) =>
      e.resendable ? (
        <Button
          size="sm"
          variant="ghost"
          icon={<Send size={14} aria-hidden="true" />}
          onClick={() => setResending(e)}
        >
          Resend
        </Button>
      ) : (
        ''
      ),
  };
  return (
    <Card flush>
      <ErrorAlert error={list.error} title={TAB_UNAVAILABLE} onRetry={() => void list.refetch()} />
      <DataTable
        columns={can('CSF_RESEND') ? [...COLUMNS, actions] : COLUMNS}
        rows={list.data ?? []}
        rowKey={(e) => e.id}
        loading={list.isLoading}
        caption="E-policies"
      />
      {resending !== null && (
        <ResendDialog
          companyId={companyId}
          clientId={clientId}
          kind="EPOLICY"
          documentId={resending.id}
          onClose={() => setResending(null)}
        />
      )}
    </Card>
  );
}
