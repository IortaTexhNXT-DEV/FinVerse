import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { supportAdminApi } from '@/api/supportAdmin';
import type { CacheInfo } from '@/api/supportAdmin';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { durationText } from './supportText';

/** Which caches the dialog clears: one by name, or all of them. */
type Clearing = { name: string } | 'ALL';

/**
 * Caches (Administration): the reference-data caches of the platform with their time to live and
 * store. After a change made outside the application (runbook), the System Administrator clears
 * one cache or all of them; the next read loads the data again.
 */
export default function CachesPage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [clearing, setClearing] = useState<Clearing | null>(null);
  const caches = useQuery({ queryKey: ['admin-caches'], queryFn: supportAdminApi.caches });
  const clear = useMutation({
    mutationFn: (c: Clearing) =>
      c === 'ALL' ? supportAdminApi.clearAllCaches() : supportAdminApi.clearCache(c.name),
    onSuccess: async (_r, c) => {
      setClearing(null);
      await queryClient.invalidateQueries({ queryKey: ['admin-caches'] });
      toast.success(c === 'ALL' ? 'Every cache cleared' : `Cache ${c.name} cleared`);
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Caches"
        description="Reference data kept in memory for fast reads. Clear a cache after a change made outside the application."
        actions={
          <Button variant="secondary" onClick={() => setClearing('ALL')}>
            Clear All Caches
          </Button>
        }
      />
      <Card flush>
        <ErrorAlert error={caches.error} />
        <DataTable<CacheInfo>
          callout="caches"
          loading={caches.isLoading}
          rows={caches.data ?? []}
          rowKey={(c) => c.name}
          columns={[
            { key: 'n', header: 'Cache', render: (c) => <strong>{c.name}</strong> },
            { key: 't', header: 'Kept For', render: (c) => durationText(c.ttl) },
            {
              key: 's',
              header: 'Store',
              kind: 'status',
              render: (c) => (
                <StatusBadge
                  status={c.store}
                  label={c.store === 'REDIS' ? 'Shared (Redis)' : 'This Instance'}
                  tone="info"
                />
              ),
            },
            {
              key: 'i',
              header: 'Cleared When These Change',
              render: (c) => (c.invalidatedBy.length === 0 ? '—' : c.invalidatedBy.join(', ')),
            },
            {
              key: 'r',
              header: 'Read Only Outside Changes',
              render: (c) => (c.readOnlyTransactionsOnly ? 'Yes' : 'No'),
            },
            {
              key: 'a',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (c) => (
                <RowActionMenu
                  label={c.name}
                  actions={[
                    { label: 'Clear Cache', onSelect: () => setClearing({ name: c.name }) },
                  ]}
                />
              ),
            },
          ]}
        />
      </Card>
      {clearing !== null && (
        <ConfirmDialog
          title={clearing === 'ALL' ? 'Clear All Caches' : 'Clear Cache'}
          record={clearing === 'ALL' ? 'Every cache' : clearing.name}
          effect="The cached data is dropped on every instance; the next read loads it from the database again. Screens may be slower for a short time."
          confirmLabel={clearing === 'ALL' ? 'Clear All Caches' : 'Clear Cache'}
          busy={clear.isPending}
          error={clear.error}
          onConfirm={() => clear.mutate(clearing)}
          onClose={() => setClearing(null)}
        />
      )}
    </div>
  );
}
