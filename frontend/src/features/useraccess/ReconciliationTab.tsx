import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Play } from 'lucide-react';
import { useState } from 'react';
import { userAccessApi } from '@/api/userAccess';
import type { ReconciliationMismatch } from '@/api/userAccess';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { FilterToggle } from '@/components/ui/FilterToggle';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { MISMATCH_LABELS } from './labels';
import { RemarkDialog } from './RemarkDialog';

/**
 * The differences the reconciliation found between the users and the UIDM-ISC directory. UIDM-ISC
 * corrects the user with an event; the mismatch is then resolved here with a remark. Run Now
 * compares every user at once.
 */
export function ReconciliationTab() {
  const toast = useToast();
  const queries = useQueryClient();
  const [all, setAll] = useState(false);
  const [resolving, setResolving] = useState<ReconciliationMismatch | null>(null);
  const mismatches = useQuery({
    queryKey: ['identity', 'mismatches', all],
    queryFn: () => userAccessApi.mismatches(!all),
  });
  const run = useMutation({
    mutationFn: userAccessApi.synchroniseNow,
    onSuccess: async (r) => {
      toast.success(r.message ?? 'Reconciliation finished');
      await queries.invalidateQueries({ queryKey: ['identity'] });
    },
  });
  const resolve = useMutation({
    mutationFn: ({ id, remark }: { id: number; remark: string }) =>
      userAccessApi.resolveMismatch(id, remark),
    onSuccess: async () => {
      toast.success('Mismatch resolved');
      setResolving(null);
      await queries.invalidateQueries({ queryKey: ['identity'] });
    },
  });
  return (
    <div className="stack">
      <div className="row" style={{ gap: 'var(--space-2)', justifyContent: 'space-between' }}>
        <FilterToggle label="Show resolved" checked={all} onChange={setAll} />
        <Button
          variant="accent"
          icon={<Play size={16} />}
          busy={run.isPending}
          onClick={() => run.mutate()}
        >
          Run Now
        </Button>
      </div>
      <ErrorAlert error={mismatches.error ?? run.error} />
      <Card flush>
        <DataTable<ReconciliationMismatch>
          caption="Reconciliation mismatches"
          loading={mismatches.isLoading}
          rows={mismatches.data ?? []}
          rowKey={(m) => m.id}
          emptyMessage={all ? 'No mismatch was found' : 'No mismatch waits to be resolved'}
          columns={[
            { key: 'd', header: 'Run Date', kind: 'date', render: (m) => formatDate(m.runDate) },
            { key: 'u', header: 'User ID', kind: 'code', render: (m) => m.username },
            { key: 'w', header: 'Windows ID', render: (m) => m.windowsId ?? '' },
            { key: 'k', header: 'Mismatch', render: (m) => MISMATCH_LABELS[m.kind] },
            { key: 't', header: 'Details', render: (m) => m.detail },
            {
              key: 'r',
              header: 'Resolved',
              render: (m) =>
                m.resolvedBy === undefined ? (
                  ''
                ) : (
                  <span>
                    <UserName login={m.resolvedBy} /> · {formatDateTime(m.resolvedAt)} ·{' '}
                    {m.resolutionRemark}
                  </span>
                ),
            },
            {
              key: 'a',
              header: 'Actions',
              kind: 'actions',
              render: (m) =>
                m.resolvedAt === undefined ? (
                  <RowActionMenu
                    label={`Mismatch of ${m.username}`}
                    actions={[{ label: 'Resolve', onSelect: () => setResolving(m) }]}
                  />
                ) : (
                  ''
                ),
            },
          ]}
        />
      </Card>
      {resolving !== null && (
        <RemarkDialog
          title={`Resolve the mismatch of ${resolving.username}`}
          record={resolving.detail}
          effect="The mismatch leaves the list of mismatches to resolve; the remark is kept."
          confirmLabel="Resolve"
          busy={resolve.isPending}
          error={resolve.error}
          onConfirm={(remark) => resolve.mutate({ id: resolving.id, remark })}
          onClose={() => setResolving(null)}
        />
      )}
    </div>
  );
}
