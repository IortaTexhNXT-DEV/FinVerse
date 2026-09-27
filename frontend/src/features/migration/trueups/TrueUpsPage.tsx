import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { migrationApi } from '@/api/migration';
import { migrationGlApi } from '@/api/migrationGl';
import type { TrueUp } from '@/api/migrationGl';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { ActionConfirm } from '../common/ActionConfirm';
import type { MigAction } from '../common/ActionConfirm';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION } from '../common/migrationCodes';
import { trueUpActions } from './trueUpActions';
import '../migration.css';

const NUMBERS = [
  { value: '1', label: '1 - after the legacy year-end close' },
  { value: '2', label: '2 - interim' },
  { value: '3', label: '3 - interim' },
  { value: 'F', label: 'F - final, after the audited statements' },
];

/**
 * Opening-Balance Adjustments (DATA_MIGRATION_DESIGN 17.7): the FY2027 true-ups of the year-end
 * go-live. The Comptrollership GL lead prepares an adjustment on a validated adjustment batch and
 * the legacy trial balance of the same version; the Head of Comptrollership approves it; the load
 * of the batch posts the opening-balance adjustment journals and the open-item adjustments; the
 * adjustment is then reconciled against the legacy trial balances and signed.
 */
export default function TrueUpsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [preparing, setPreparing] = useState(false);
  const [action, setAction] = useState<MigAction>();
  const list = useQuery({
    queryKey: ['migration', 'trueups', companyId],
    queryFn: () => migrationGlApi.trueups(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Opening-Balance Adjustments"
        description="Year-end adjustments of the legacy books after go-live, posted into the opening period."
        actions={
          can('MIG_TRUEUP_PREPARE') && (
            <Button variant="primary" icon={<Plus size={14} />} onClick={() => setPreparing(true)}>
              Prepare Adjustment
            </Button>
          )
        }
      />
      <Card flush>
        <ErrorAlert error={list.error} onRetry={() => void list.refetch()} />
        <DataTable<TrueUp>
          loading={list.isLoading}
          rows={list.data ?? []}
          rowKey={(t) => t.reference}
          emptyMessage="No opening-balance adjustment yet"
          columns={[
            {
              key: 'ref',
              header: 'Adjustment',
              render: (t) => <CellStack main={t.reference} sub={`As of ${formatDate(t.asOf)}`} />,
            },
            {
              key: 'batches',
              header: 'Batches',
              render: (t) => (
                <CellStack
                  main={t.batchNo ?? ''}
                  sub={t.tbBatchNo ? `Trial balance ${t.tbBatchNo}` : ''}
                />
              ),
            },
            {
              key: 'posted',
              header: 'Posted',
              render: (t) =>
                t.postedAt === undefined ? (
                  ''
                ) : (
                  <CellStack
                    main={`${String(t.journalsPosted)} journal(s), ${String(t.itemsAdjusted)} item(s)`}
                    sub={formatDateTime(t.postedAt)}
                  />
                ),
            },
            {
              key: 'people',
              header: 'Prepared / Approved / Signed',
              render: (t) => (
                <CellStack
                  main={t.preparedBy}
                  sub={[t.approvedBy, t.signedBy].filter(Boolean).join(' / ')}
                />
              ),
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (t) => <MigStatus status={t.status} />,
            },
            {
              key: 'actions',
              header: 'Actions',
              render: (t) => (
                <span className="mig-actions">
                  {trueUpActions(t, can).map((a) => (
                    <Button
                      key={a.title}
                      variant="secondary"
                      size="sm"
                      onClick={() => setAction(a)}
                    >
                      {a.confirmLabel}
                    </Button>
                  ))}
                </span>
              ),
            },
          ]}
        />
      </Card>
      {preparing && <PrepareDialog onClose={() => setPreparing(false)} />}
      <ActionConfirm action={action} onClose={() => setAction(undefined)} />
    </div>
  );
}

function PrepareDialog({ onClose }: Readonly<{ onClose: () => void }>) {
  const companyId = useCompanyId();
  const client = useQueryClient();
  const [trueupNo, setTrueupNo] = useState('1');
  const [asOf, setAsOf] = useState('');
  const [batchNo, setBatchNo] = useState('');
  const [tbBatchNo, setTbBatchNo] = useState('');
  const batches = useQuery({
    queryKey: ['migration', 'batches', companyId, 'trueup'],
    queryFn: () => migrationApi.batches(companyId, 0, 200),
  });
  const of = (object: string) =>
    (batches.data?.content ?? []).filter((b) => b.objectCode === object);
  const prepare = useMutation({
    mutationFn: () =>
      migrationGlApi.prepare(companyId, {
        trueupNo,
        asOf,
        batchNo,
        tbBatchNo: tbBatchNo === '' ? undefined : tbBatchNo,
      }),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ['migration'] });
      onClose();
    },
  });
  return (
    <Modal
      title="Prepare Adjustment"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={asOf === '' || batchNo === ''}
            busy={prepare.isPending}
            onClick={() => prepare.mutate()}
          >
            Prepare
          </Button>
        </>
      }
    >
      <ErrorAlert error={prepare.error ?? batches.error} />
      <div className="form-grid">
        <Field label="Adjustment" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={trueupNo}
              onChange={(e) => setTrueupNo(e.target.value)}
            >
              {NUMBERS.map((n) => (
                <option key={n.value} value={n.value}>
                  {n.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Legacy Trial Balance As Of" required>
          {(id) => (
            <input
              id={id}
              type="date"
              className="input"
              value={asOf}
              onChange={(e) => setAsOf(e.target.value)}
            />
          )}
        </Field>
        <Field
          label="Adjustment Batch"
          required
          hint="A validated batch of the adjustment journals"
        >
          {(id) => (
            <select
              id={id}
              className="select"
              value={batchNo}
              onChange={(e) => setBatchNo(e.target.value)}
            >
              <option value="">Choose a batch</option>
              {of('G03').map((b) => (
                <option key={b.batchNo} value={b.batchNo}>
                  {b.batchNo} ({b.status})
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field
          label="Legacy Trial Balance Batch"
          hint="The trial balance of the same version, for the reconciliation"
        >
          {(id) => (
            <select
              id={id}
              className="select"
              value={tbBatchNo}
              onChange={(e) => setTbBatchNo(e.target.value)}
            >
              <option value="">None</option>
              {of('G01').map((b) => (
                <option key={b.batchNo} value={b.batchNo}>
                  {b.batchNo} ({b.status})
                </option>
              ))}
            </select>
          )}
        </Field>
      </div>
    </Modal>
  );
}
