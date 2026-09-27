import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { migrationApi } from '@/api/migration';
import type { Batch, Resubmission } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { ActionConfirm } from '../common/ActionConfirm';
import type { MigAction } from '../common/ActionConfirm';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION, migLabel, percent } from '../common/migrationCodes';
import '../migration.css';

type Tab = 'batches' | 'resubmissions';

/**
 * Batches (DATA_MIGRATION_DESIGN sections 10 and 11): every load batch with its status and counts,
 * planning a batch from staged extracts, and the resubmissions of corrected rejected rows waiting
 * for the checker. Open a batch for its steps, issues, reconciliation and gates.
 */
export default function BatchesPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const { can, user } = useAuth();
  const [tab, setTab] = useState<Tab>('batches');
  const [page, setPage] = useState(0);
  const [planning, setPlanning] = useState(false);
  const [action, setAction] = useState<MigAction>();
  const batches = useQuery({
    queryKey: ['migration', 'batches', companyId, page],
    queryFn: () => migrationApi.batches(companyId, page),
    enabled: companyId > 0,
  });
  const resubmissions = useQuery({
    queryKey: ['migration', 'resubmissions', companyId],
    queryFn: () => migrationApi.resubmissions(companyId),
    enabled: tab === 'resubmissions' && companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Batches"
        description="Load batches from validation to acceptance, with rerun and rollback."
        actions={
          can('MIG_LOAD_RUN') && (
            <Button variant="primary" icon={<Plus size={16} />} onClick={() => setPlanning(true)}>
              Plan Batch
            </Button>
          )
        }
      />
      <Tabs<Tab>
        tabs={[
          { id: 'batches', label: 'Batches' },
          { id: 'resubmissions', label: 'Resubmissions' },
        ]}
        active={tab}
        onChange={setTab}
      />
      {tab === 'batches' && (
        <Card flush>
          <ErrorAlert error={batches.error} onRetry={() => void batches.refetch()} />
          <DataTable<Batch>
            loading={batches.isLoading}
            rows={batches.data?.content ?? []}
            rowKey={(b) => b.batchNo}
            onRowClick={(b) => void navigate(`/migration/batches/${b.batchNo}`)}
            emptyMessage="No batch planned yet"
            columns={[
              {
                key: 'batch',
                header: 'Batch',
                kind: 'code',
                render: (b) => (
                  <CellStack
                    main={b.batchNo}
                    sub={b.parentBatchNo ? `Rerun of ${b.parentBatchNo}` : migLabel(b.mode)}
                  />
                ),
              },
              { key: 'object', header: 'Object', kind: 'code', render: (b) => b.objectCode },
              { key: 'env', header: 'Environment', render: (b) => migLabel(b.environmentClass) },
              { key: 'staged', header: 'Staged', kind: 'center', render: (b) => b.counts.staged },
              { key: 'loaded', header: 'Loaded', kind: 'center', render: (b) => b.counts.loaded },
              {
                key: 'rejected',
                header: 'Rejected',
                kind: 'center',
                render: (b) => b.counts.rejected + b.counts.invalid,
              },
              {
                key: 'rate',
                header: 'Error rate',
                kind: 'center',
                render: (b) => percent(b.errorRate),
              },
              {
                key: 'created',
                header: 'Planned',
                render: (b) => <CellStack main={formatDateTime(b.createdAt)} sub={b.createdBy} />,
              },
              {
                key: 'status',
                header: 'Status',
                kind: 'status',
                render: (b) => <MigStatus status={b.status} />,
              },
            ]}
          />
          <PageFooter data={batches.data} noun="batches" onPage={setPage} />
        </Card>
      )}
      {tab === 'resubmissions' && (
        <Card flush>
          <ErrorAlert error={resubmissions.error} />
          <DataTable<Resubmission>
            loading={resubmissions.isLoading}
            rows={resubmissions.data ?? []}
            rowKey={(r) => r.resubmissionNo}
            emptyMessage="No resubmission of corrected rows"
            columns={[
              { key: 'no', header: 'Resubmission', kind: 'code', render: (r) => r.resubmissionNo },
              { key: 'object', header: 'Object', kind: 'code', render: (r) => r.objectCode },
              {
                key: 'rows',
                header: 'Corrected rows',
                kind: 'center',
                render: (r) => r.correctedRows,
              },
              {
                key: 'prepared',
                header: 'Prepared',
                render: (r) => <CellStack main={formatDateTime(r.preparedAt)} sub={r.preparedBy} />,
              },
              { key: 'decided', header: 'Decided by', render: (r) => r.decidedBy ?? '' },
              { key: 'note', header: 'Note', render: (r) => r.decisionNote ?? '' },
              {
                key: 'status',
                header: 'Status',
                kind: 'status',
                render: (r) => <MigStatus status={r.status} />,
              },
              {
                key: 'actions',
                header: '',
                render: (r) =>
                  can('MIG_RESUBMIT_APPROVE') &&
                  r.status === 'PREPARED' &&
                  r.preparedBy !== user?.username ? (
                    <span className="mig-actions">
                      <Button
                        variant="primary"
                        size="sm"
                        onClick={() =>
                          setAction({
                            title: `Approve ${r.resubmissionNo}`,
                            record: r.resubmissionNo,
                            effect:
                              'A rerun batch of the corrected rows is created for validation and load.',
                            confirmLabel: 'Approve',
                            reason: 'optional',
                            done: 'Resubmission approved',
                            run: (note) =>
                              migrationApi.decideResubmission(r.resubmissionNo, true, note),
                          })
                        }
                      >
                        Approve
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() =>
                          setAction({
                            title: `Return ${r.resubmissionNo}`,
                            record: r.resubmissionNo,
                            effect: 'The corrected rows go back to the maker.',
                            confirmLabel: 'Return',
                            reason: 'required',
                            done: 'Resubmission returned',
                            run: (note) =>
                              migrationApi.decideResubmission(r.resubmissionNo, false, note),
                          })
                        }
                      >
                        Return
                      </Button>
                    </span>
                  ) : null,
              },
            ]}
          />
        </Card>
      )}
      {planning && <PlanDialog onClose={() => setPlanning(false)} />}
      <ActionConfirm action={action} onClose={() => setAction(undefined)} />
    </div>
  );
}

function PlanDialog({ onClose }: Readonly<{ onClose: () => void }>) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const client = useQueryClient();
  const [objectCode, setObjectCode] = useState('');
  const [extractNos, setExtractNos] = useState('');
  const objects = useQuery({
    queryKey: ['migration', 'objects'],
    queryFn: () => migrationApi.objects(),
  });
  const plan = useMutation({
    mutationFn: () =>
      migrationApi.plan(
        companyId,
        objectCode,
        extractNos
          .split(/[\s,]+/)
          .map((s) => s.trim())
          .filter((s) => s !== ''),
      ),
    onSuccess: async (b) => {
      await client.invalidateQueries({ queryKey: ['migration'] });
      onClose();
      void navigate(`/migration/batches/${b.batchNo}`);
    },
  });
  const loadable = (objects.data ?? []).filter((o) =>
    ['MIGRATE', 'CARRY_FORWARD', 'CONDITIONAL'].includes(o.decidedClass ?? ''),
  );
  return (
    <Modal
      title="Plan Batch"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={objectCode === ''}
            busy={plan.isPending}
            onClick={() => plan.mutate()}
          >
            Plan
          </Button>
        </>
      }
    >
      <ErrorAlert error={plan.error ?? objects.error} />
      <div className="form-grid">
        <Field label="Object" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={objectCode}
              onChange={(e) => setObjectCode(e.target.value)}
            >
              <option value="">Choose a decided object</option>
              {loadable.map((o) => (
                <option key={o.code} value={o.code}>
                  {o.code} {o.name}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field
          label="Extracts"
          hint="Extract numbers separated by commas; leave empty for the latest staged extract of each layout"
        >
          {(id) => (
            <input
              id={id}
              className="input"
              value={extractNos}
              onChange={(e) => setExtractNos(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
