import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { cutoverApi } from '@/api/migrationCutover';
import type { CutoverPlan, PlanKind } from '@/api/migrationCutover';
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
import { formatDate } from '@/utils/format';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION, migLabel } from '../common/migrationCodes';
import { PlanPanel } from './PlanPanel';
import '../migration.css';

const KINDS: { value: PlanKind; label: string }[] = [
  { value: 'MOCK', label: 'Mock run' },
  { value: 'DRESS_REHEARSAL', label: 'Dress rehearsal' },
  { value: 'PRODUCTION', label: 'Production cut-over' },
];

/**
 * Cutover (DATA_MIGRATION_DESIGN 17.1-17.5): the mock runs, the dress rehearsal and the production
 * cut-over, each with its runbook planned from the go-live date, the go / no-go criteria and the
 * decision of the go / no-go board.
 */
export default function CutoverPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [params, setParams] = useSearchParams();
  const [creating, setCreating] = useState(false);
  const selected = params.get('plan') ?? undefined;
  const plans = useQuery({
    queryKey: ['migration', 'cutover', companyId],
    queryFn: () => cutoverApi.plans(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Cutover"
        description="Mock runs, dress rehearsal and the production cut-over with their runbook and go / no-go."
        actions={
          can('MIG_CUTOVER_MANAGE') && (
            <Button variant="primary" icon={<Plus size={14} />} onClick={() => setCreating(true)}>
              New Plan
            </Button>
          )
        }
      />
      <Card flush>
        <ErrorAlert error={plans.error} onRetry={() => void plans.refetch()} />
        <DataTable<CutoverPlan>
          loading={plans.isLoading}
          rows={plans.data ?? []}
          rowKey={(p) => p.planNo}
          selectedKey={selected}
          onRowClick={(p) => setParams({ plan: p.planNo })}
          emptyMessage="No cutover plan yet"
          columns={[
            {
              key: 'plan',
              header: 'Plan',
              render: (p) => <CellStack main={p.name} sub={p.planNo} />,
            },
            {
              key: 'kind',
              header: 'Kind',
              render: (p) =>
                migLabel(p.kind) + (p.mockNo === undefined ? '' : ` ${String(p.mockNo)}`),
            },
            { key: 'env', header: 'Environment', render: (p) => p.environment },
            {
              key: 'golive',
              header: 'Go-live',
              kind: 'date',
              render: (p) => formatDate(p.goLiveDate),
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (p) => <MigStatus status={p.status} />,
            },
          ]}
        />
      </Card>
      {selected !== undefined && <PlanPanel planNo={selected} onClose={() => setParams({})} />}
      {creating && <CreatePlanDialog onClose={() => setCreating(false)} />}
    </div>
  );
}

function CreatePlanDialog({ onClose }: Readonly<{ onClose: () => void }>) {
  const companyId = useCompanyId();
  const client = useQueryClient();
  const [name, setName] = useState('');
  const [kind, setKind] = useState<PlanKind>('MOCK');
  const [mockNo, setMockNo] = useState('');
  const [environment, setEnvironment] = useState('SIT');
  const [goLiveDate, setGoLiveDate] = useState('');
  const create = useMutation({
    mutationFn: () =>
      cutoverApi.create(companyId, {
        name,
        kind,
        mockNo: kind === 'MOCK' && mockNo !== '' ? Number(mockNo) : undefined,
        environment,
        goLiveDate,
      }),
    onSuccess: async () => {
      await client.invalidateQueries({ queryKey: ['migration', 'cutover'] });
      onClose();
    },
  });
  return (
    <Modal
      title="New Cutover Plan"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={name.trim() === '' || goLiveDate === '' || environment.trim() === ''}
            busy={create.isPending}
            onClick={() => create.mutate()}
          >
            Create
          </Button>
        </>
      }
    >
      <ErrorAlert error={create.error} />
      <p className="muted">
        The runbook tasks and the twelve go / no-go criteria are created from the go-live date.
      </p>
      <div className="form-grid">
        <Field label="Name" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={name}
              maxLength={120}
              onChange={(e) => setName(e.target.value)}
            />
          )}
        </Field>
        <Field label="Kind" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={kind}
              onChange={(e) => setKind(e.target.value as PlanKind)}
            >
              {KINDS.map((k) => (
                <option key={k.value} value={k.value}>
                  {k.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        {kind === 'MOCK' && (
          <Field label="Mock No.">
            {(id) => (
              <input
                id={id}
                type="number"
                min={1}
                className="input"
                value={mockNo}
                onChange={(e) => setMockNo(e.target.value)}
              />
            )}
          </Field>
        )}
        <Field label="Environment" required hint="SIT, UAT, PERF or PROD">
          {(id) => (
            <input
              id={id}
              className="input"
              value={environment}
              maxLength={20}
              onChange={(e) => setEnvironment(e.target.value)}
            />
          )}
        </Field>
        <Field label="Go-live Date" required>
          {(id) => (
            <input
              id={id}
              type="date"
              className="input"
              value={goLiveDate}
              onChange={(e) => setGoLiveDate(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
