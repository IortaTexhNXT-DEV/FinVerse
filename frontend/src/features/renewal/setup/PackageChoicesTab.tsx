import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import type { PackageChoiceView } from '@/api/renewal';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { TextDialog } from '../common/ActionDialogs';

interface Pending {
  choice: PackageChoiceView;
  renewalRef: string;
  clientName: string;
}

/**
 * Package choices waiting for the checker (DMQ36): a second member of the Renewal processing team
 * approves or rejects the product version proposed for a migrated policy.
 */
export function PackageChoicesTab() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [deciding, setDeciding] = useState<{ row: Pending; approve: boolean }>();
  const rows = useQuery({
    queryKey: ['renewal', 'setup', 'choices'],
    queryFn: renewalApi.pendingChoices,
  });
  const decide = useMutation({
    mutationFn: ({ id, approve, remarks }: { id: number; approve: boolean; remarks: string }) =>
      renewalApi.decideChoice(id, approve, remarks || undefined),
    onSuccess: async (c) => {
      setDeciding(undefined);
      toast.success(`Package choice ${c.status.toLowerCase()}`);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  return (
    <Card title="Package choices to approve" flush>
      <ErrorAlert error={rows.error} />
      <DataTable<Pending>
        loading={rows.isLoading}
        rows={rows.data ?? []}
        rowKey={(r) => r.choice.id}
        emptyMessage="No package choices waiting"
        columns={[
          {
            key: 'ref',
            header: 'Renewal',
            kind: 'code',
            render: (r) => (
              <Link to={`/renewal/candidates/${encodeURIComponent(r.renewalRef)}`}>
                {r.renewalRef}
              </Link>
            ),
          },
          { key: 'client', header: 'Client', render: (r) => r.clientName },
          { key: 'legacy', header: 'Legacy Package', render: (r) => r.choice.legacyPackage },
          {
            key: 'target',
            header: 'Proposed',
            render: (r) => `${r.choice.productCode} v${String(r.choice.productVersionNo)}`,
          },
          { key: 'reason', header: 'Reason', render: (r) => r.choice.reason },
          {
            key: 'maker',
            header: 'Proposed by',
            render: (r) => (
              <>
                <UserName login={r.choice.maker} /> {formatDateTime(r.choice.createdAt)}
              </>
            ),
          },
          {
            key: 'act',
            header: '',
            render: (r) => (
              <span className="rnw-actions">
                <Button size="sm" onClick={() => setDeciding({ row: r, approve: true })}>
                  Approve
                </Button>
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setDeciding({ row: r, approve: false })}
                >
                  Reject
                </Button>
              </span>
            ),
          },
        ]}
      />
      {deciding !== undefined && (
        <TextDialog
          title={`${deciding.approve ? 'Approve' : 'Reject'} package choice of ${deciding.row.renewalRef}`}
          label="Remarks"
          confirmLabel={deciding.approve ? 'Approve' : 'Reject'}
          required={!deciding.approve}
          busy={decide.isPending}
          error={decide.error}
          onClose={() => setDeciding(undefined)}
          onConfirm={(remarks) =>
            decide.mutate({ id: deciding.row.choice.id, approve: deciding.approve, remarks })
          }
        />
      )}
    </Card>
  );
}
