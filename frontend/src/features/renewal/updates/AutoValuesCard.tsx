import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { RefreshCw } from 'lucide-react';
import type { ReactNode } from 'react';
import { renewalUpdatesApi } from '@/api/renewalUpdates';
import type { AutoValueRow } from '@/api/renewalUpdates';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';

const DATE_FIELDS = new Set(['New Inception', 'New Expiry']);
const TEXT_FIELDS = new Set(['Risk Code']);

function cell(row: AutoValueRow, value: string | null): ReactNode {
  if (value === null) return <span />;
  if (DATE_FIELDS.has(row.field)) return <span>{formatDate(value)}</span>;
  if (TEXT_FIELDS.has(row.field)) return <span>{value}</span>;
  return <Amount value={value} />;
}

/**
 * The CBG Motor automatic values (FRRN.009.01): each field with its expiring value, the automatic
 * renewal value, the value of the renewal account and the difference (the financial impact).
 */
export function AutoValuesCard({ renewalRef }: Readonly<{ renewalRef: string }>) {
  const companyId = useCompanyId();
  const rows = useQuery({
    queryKey: ['renewal', 'auto-update', companyId, renewalRef],
    queryFn: () => renewalUpdatesApi.autoUpdate(companyId, renewalRef),
  });
  if (!rows.isLoading && (rows.data ?? []).length === 0 && !rows.error) return null;
  return (
    <Card title="Automatic renewal values" flush>
      <ErrorAlert error={rows.error} />
      <DataTable<AutoValueRow>
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => r.field}
        emptyMessage="No automatic values"
        columns={[
          { key: 'field', header: 'Field', render: (r) => r.field },
          { key: 'exp', header: 'Expiring', kind: 'amount', render: (r) => cell(r, r.expiring) },
          { key: 'ren', header: 'Renewal', kind: 'amount', render: (r) => cell(r, r.renewal) },
          {
            key: 'acc',
            header: 'Renewal account',
            kind: 'amount',
            render: (r) => cell(r, r.account),
          },
          {
            key: 'diff',
            header: 'Difference',
            kind: 'amount',
            render: (r) => (r.difference === null ? '' : <Amount value={r.difference} />),
          },
        ]}
      />
    </Card>
  );
}

/** Refresh Endorsements (FRRN.004.06): re-reads the endorsements and re-runs the checks. */
export function RefreshEndorsementsButton({ renewalRef }: Readonly<{ renewalRef: string }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const refresh = useMutation({
    mutationFn: () => renewalUpdatesApi.refreshEndorsements(companyId, renewalRef),
    onSuccess: async (rows) => {
      toast.success(
        rows.length === 0
          ? 'Endorsements refreshed: none in the term'
          : `Endorsements refreshed: ${String(rows.length)} in the term`,
      );
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
    onError: () => toast.error('The endorsements could not be refreshed'),
  });
  if (!(can('RNW_DISPOSE') || can('RNW_PROCESS') || can('RNW_REVIEW'))) return null;
  return (
    <Button
      size="sm"
      variant="secondary"
      icon={<RefreshCw size={14} />}
      busy={refresh.isPending}
      onClick={() => refresh.mutate()}
    >
      Refresh Endorsements
    </Button>
  );
}
