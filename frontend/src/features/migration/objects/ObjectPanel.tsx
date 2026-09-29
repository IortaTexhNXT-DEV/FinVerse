import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { migrationApi } from '@/api/migration';
import type { DataObject, Decision } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { ActionConfirm } from '../common/ActionConfirm';
import type { MigAction } from '../common/ActionConfirm';
import { MigStatus } from '../common/MigStatus';
import { migLabel, yesNo } from '../common/migrationCodes';
import { UserName } from '@/components/ui/UserName';
import { BRAND } from '@/branding';

const HISTORY: Column<Decision>[] = [
  { key: 'no', header: 'Decision', kind: 'code', render: (d) => d.decisionNo },
  { key: 'class', header: 'Class', render: (d) => migLabel(d.proposedClass) },
  { key: 'by', header: 'Submitted by', render: (d) => <UserName login={d.submittedBy} /> },
  {
    key: 'at',
    header: 'Submitted',
    kind: 'datetime',
    render: (d) => formatDateTime(d.submittedAt),
  },
  {
    key: 'decidedBy',
    header: 'Decided by',
    render: (d) => <UserName login={d.decidedBy} empty="" />,
  },
  { key: 'reason', header: 'Return reason', render: (d) => d.returnReason ?? '' },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (d) => <MigStatus status={d.status} />,
  },
];

function ObjectDetails({ object: o }: Readonly<{ object: DataObject }>) {
  return (
    <DefinitionGrid
      columns={2}
      items={[
        { label: 'Category', value: migLabel(o.category) },
        { label: 'Sources', value: o.sourceSystems.join(', ') },
        { label: `Target in ${BRAND.product}`, value: o.target, wide: true },
        { label: 'Proposed class', value: migLabel(o.proposedClass) },
        { label: 'Decided class', value: migLabel(o.decidedClass) },
        { label: 'Condition', value: o.conditionText, wide: true },
        { label: 'Day-1 need', value: yesNo(o.day1Need, o.day1Note) },
        { label: 'Compliance need', value: yesNo(o.complianceNeed, o.complianceNote) },
        { label: 'Archive option', value: yesNo(o.archivalOption, o.archivalNote) },
        { label: 'Data trust', value: migLabel(o.dataTrust) },
        {
          label: 'Business owner',
          value: o.businessOwner ? <UserName login={o.businessOwner} /> : o.ownerTitle,
        },
        {
          label: 'Data steward',
          value: o.dataSteward ? <UserName login={o.dataSteward} /> : o.stewardTitle,
        },
        { label: 'Depends on', value: o.dependsOn.join(', ') },
        { label: 'Load order', value: String(o.loadOrder) },
        { label: 'Rationale', value: o.rationale, wide: true },
      ]}
    />
  );
}

function submitAction(companyId: number, o: DataObject): MigAction {
  return {
    title: `Submit ${o.code} for decision`,
    record: o.code,
    effect: `The proposed class ${migLabel(o.proposedClass)} goes to the business owner for approval.`,
    confirmLabel: 'Submit for Decision',
    done: 'Submitted to the business owner',
    run: () => migrationApi.submitDecision(companyId, o.code, o.proposedClass === 'CONDITIONAL'),
  };
}

function decisionActions(code: string, d: Decision): { label: string; action: MigAction }[] {
  return [
    {
      label: 'Approve',
      action: {
        title: `Approve decision ${d.decisionNo}`,
        record: d.decisionNo,
        effect: `${code} is decided as ${migLabel(d.proposedClass)} (gate G1).`,
        confirmLabel: 'Approve',
        reason: 'optional',
        done: 'Decision approved',
        run: (reason) => migrationApi.approveDecision(d.decisionNo, reason),
      },
    },
    {
      label: 'Return',
      action: {
        title: `Return decision ${d.decisionNo}`,
        record: d.decisionNo,
        effect: 'The proposal goes back to the Data Migration Lead.',
        confirmLabel: 'Return',
        reason: 'required',
        done: 'Decision returned',
        run: (reason) => migrationApi.returnDecision(d.decisionNo, reason),
      },
    },
  ];
}

function pendingActions(
  code: string,
  history: Decision[] | undefined,
  user: string | undefined,
): { label: string; action: MigAction }[] {
  const pending = history?.find((d) => d.status === 'FOR_DECISION');
  return pending === undefined || pending.submittedBy === user
    ? []
    : decisionActions(code, pending);
}

/**
 * An object of the register: the four criteria, the rationale, the decision history and the
 * decision actions (submit to the business owner, approve or return).
 */
export function ObjectPanel({
  code,
  canManage,
  canDecide,
  onClose,
}: Readonly<{ code: string; canManage: boolean; canDecide: boolean; onClose: () => void }>) {
  const companyId = useCompanyId();
  const { user } = useAuth();
  const [action, setAction] = useState<MigAction>();
  const object = useQuery({
    queryKey: ['migration', 'object', code],
    queryFn: () => migrationApi.object(code),
  });
  const history = useQuery({
    queryKey: ['migration', 'decisions', code],
    queryFn: () => migrationApi.decisions(code),
  });
  const o = object.data;
  const decide = canDecide ? pendingActions(code, history.data, user?.username) : [];
  const maySubmit = canManage && o !== undefined && o.status !== 'FOR_DECISION';
  return (
    <Card
      title={o ? `${o.code} ${o.name}` : code}
      actions={
        <span className="mig-actions">
          {maySubmit && (
            <Button
              variant="primary"
              size="sm"
              onClick={() => setAction(submitAction(companyId, o))}
            >
              Submit for Decision
            </Button>
          )}
          {decide.map((d) => (
            <Button
              key={d.label}
              variant={d.label === 'Approve' ? 'primary' : 'secondary'}
              size="sm"
              onClick={() => setAction(d.action)}
            >
              {d.label}
            </Button>
          ))}
          <Button variant="ghost" size="sm" onClick={onClose}>
            Close
          </Button>
        </span>
      }
    >
      <ErrorAlert error={object.error ?? history.error} />
      {o !== undefined && <ObjectDetails object={o} />}
      <h3>Decision history</h3>
      <DataTable<Decision>
        loading={history.isLoading}
        rows={history.data ?? []}
        rowKey={(d) => d.decisionNo}
        emptyMessage="No decision submitted yet"
        columns={HISTORY}
      />
      <ActionConfirm action={action} onClose={() => setAction(undefined)} />
    </Card>
  );
}
