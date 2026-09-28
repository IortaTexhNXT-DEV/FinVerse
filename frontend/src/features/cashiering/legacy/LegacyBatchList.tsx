import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { LegacyBatch } from '@/api/legacyBatches';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import type { BatchScreen } from './batchScreens';
import { approvers } from './batchScreens';
import { UserName } from '@/components/ui/UserName';
import { useDisplayName } from '@/components/ui/useDisplayName';

/** The batches of one legacy batch screen, newest first, with the way to open a new one. */
export function LegacyBatchList({ screen }: Readonly<{ screen: BatchScreen }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const name = useDisplayName();
  const navigate = useNavigate();
  const [creating, setCreating] = useState(false);
  const list = useQuery({
    queryKey: ['legacy-batches', screen.id, companyId],
    queryFn: () => screen.api.list(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section={screen.section}
        title={screen.title}
        description={screen.description}
        actions={
          can(screen.request) && (
            <Button variant="primary" icon={<Plus size={14} />} onClick={() => setCreating(true)}>
              New Batch
            </Button>
          )
        }
      />
      <Card flush>
        <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
        <DataTable<LegacyBatch>
          loading={list.isLoading}
          rows={list.data ?? []}
          rowKey={(b) => b.batchNo}
          onRowClick={(b) => void navigate(`${screen.detailBase}/${b.batchNo}`)}
          emptyMessage="No batch yet"
          columns={[
            {
              key: 'batch',
              header: 'Batch',
              render: (b) => <CellStack main={b.batchNo} sub={b.reason} />,
            },
            {
              key: 'lines',
              header: 'Lines',
              numeric: true,
              render: (b) => String(b.lineCount),
            },
            {
              key: 'total',
              header: 'Total',
              kind: 'amount',
              render: (b) => <Amount value={b.total} />,
            },
            {
              key: 'people',
              header: 'Requested / Approved',
              render: (b) => (
                <CellStack main={<UserName login={b.createdBy} />} sub={approvers(b, name)} />
              ),
            },
            {
              key: 'posted',
              header: 'Posted',
              render: (b) =>
                b.executedAt ? (
                  <CellStack
                    main={`${String(b.postedCount)} posted, ${String(b.failedCount)} refused`}
                    sub={formatDateTime(b.executedAt)}
                  />
                ) : (
                  ''
                ),
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (b) => <StatusBadge status={b.status} />,
            },
          ]}
        />
      </Card>
      {creating && (
        <CreateDialog
          screen={screen}
          onClose={() => setCreating(false)}
          onCreated={(b) => void navigate(`${screen.detailBase}/${b.batchNo}`)}
        />
      )}
    </div>
  );
}

function CreateDialog({
  screen,
  onClose,
  onCreated,
}: Readonly<{
  screen: BatchScreen;
  onClose: () => void;
  onCreated: (b: LegacyBatch) => void;
}>) {
  const companyId = useCompanyId();
  const client = useQueryClient();
  const [reason, setReason] = useState('');
  const [currency, setCurrency] = useState('PHP');
  const create = useMutation({
    mutationFn: () => screen.api.create(companyId, reason.trim(), currency),
    onSuccess: async (b) => {
      await client.invalidateQueries({ queryKey: ['legacy-batches'] });
      onCreated(b);
    },
  });
  return (
    <Modal
      title={`New ${screen.title} Batch`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={reason.trim() === ''}
            busy={create.isPending}
            onClick={() => create.mutate()}
          >
            Create
          </Button>
        </>
      }
    >
      <ErrorAlert error={create.error} />
      <div className="form-grid">
        <Field label="Reason" required>
          {(id) => (
            <textarea
              id={id}
              className="input"
              maxLength={500}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
        {screen.askCurrency && (
          <Field label="Currency" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={currency}
                onChange={(e) => setCurrency(e.target.value)}
              >
                <option value="PHP">PHP</option>
                <option value="USD">USD</option>
              </select>
            )}
          </Field>
        )}
      </div>
    </Modal>
  );
}
