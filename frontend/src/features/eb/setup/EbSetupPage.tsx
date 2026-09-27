import { useQuery } from '@tanstack/react-query';
import { Ban, CheckCircle2, Pencil, Plus } from 'lucide-react';
import { useState } from 'react';
import type { ReactNode } from 'react';
import type { RequiredDocument, ThresholdRule } from '@/api/ebMarket';
import { ebServiceApi } from '@/api/ebService';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { EB_SECTION } from '../EbPlaceholder';
import '../eb.css';
import { EbLov } from '../common/EbLabels';
import { EB_LOV, ebLabel } from '../common/ebCodes';
import { useEbMutation } from '../common/useEbMutation';
import { RequiredDocumentDialog, ThresholdRuleDialog } from './SetupDialogs';

type Action = 'authorize' | 'deactivate';

interface Maintained {
  id: number;
  recordStatus: string;
  maker: string;
}

/** The row actions of a maintained record: edit, authorize (not by its maker), deactivate. */
function RowActions<T extends Maintained>({
  row,
  name,
  me,
  onEdit,
  onAct,
}: Readonly<{
  row: T;
  name: string;
  me: string;
  onEdit: (r: T) => void;
  onAct: (id: number, a: Action) => void;
}>) {
  return (
    <span className="eb-actions">
      {row.recordStatus !== 'INACTIVE' && (
        <Button
          variant="ghost"
          size="sm"
          aria-label={`Edit ${name}`}
          icon={<Pencil size={14} />}
          onClick={() => onEdit(row)}
        />
      )}
      {row.recordStatus === 'PENDING_AUTHORIZATION' && row.maker !== me && (
        <Button
          variant="ghost"
          size="sm"
          aria-label={`Authorize ${name}`}
          icon={<CheckCircle2 size={14} />}
          onClick={() => onAct(row.id, 'authorize')}
        />
      )}
      {row.recordStatus === 'ACTIVE' && (
        <Button
          variant="ghost"
          size="sm"
          aria-label={`Deactivate ${name}`}
          icon={<Ban size={14} />}
          onClick={() => onAct(row.id, 'deactivate')}
        />
      )}
    </span>
  );
}

const status = (r: Maintained) => (
  <CellStack main={<StatusBadge status={r.recordStatus} />} sub={<UserName login={r.maker} />} />
);

function ruleColumns(actions: (r: ThresholdRule) => ReactNode): Column<ThresholdRule>[] {
  return [
    {
      key: 'line',
      header: 'Benefit Line',
      render: (r) =>
        r.benefitLine ? <EbLov type={EB_LOV.benefitLine} code={r.benefitLine} /> : 'All lines',
    },
    { key: 'measure', header: 'Measure', render: (r) => ebLabel(r.measure) },
    { key: 'amount', header: 'Above', kind: 'amount', render: (r) => <Amount value={r.amount} /> },
    {
      key: 'period',
      header: 'Effective',
      render: (r) => `${formatDate(r.effectiveFrom)} – ${formatDate(r.effectiveTo) || 'open'}`,
    },
    { key: 'desc', header: 'Description', render: (r) => r.description ?? '' },
    { key: 'status', header: 'Status', kind: 'status', render: status },
    { key: 'actions', header: '', width: '112px', render: actions },
  ];
}

function docColumns(actions: (r: RequiredDocument) => ReactNode): Column<RequiredDocument>[] {
  return [
    {
      key: 'process',
      header: 'Process',
      render: (d) => <EbLov type={EB_LOV.processType} code={d.processType} />,
    },
    {
      key: 'line',
      header: 'Benefit Line',
      render: (d) =>
        d.benefitLine ? <EbLov type={EB_LOV.benefitLine} code={d.benefitLine} /> : 'All lines',
    },
    {
      key: 'type',
      header: 'Document',
      render: (d) => <EbLov type={EB_LOV.documentType} code={d.documentType} />,
    },
    { key: 'mandatory', header: 'Mandatory', render: (d) => (d.mandatory ? 'Yes' : 'Optional') },
    { key: 'status', header: 'Status', kind: 'status', render: status },
    { key: 'actions', header: '', width: '112px', render: actions },
  ];
}

type Editing = { kind: 'rule'; rule?: ThresholdRule } | { kind: 'doc'; doc?: RequiredDocument };

/**
 * EB Setup (design 10.1): the value threshold rules that send a comparative to BDOI Management and
 * the required documents per process and benefit line. Each change waits for another user's
 * authorization; records are deactivated, never deleted.
 */
export default function EbSetupPage() {
  const companyId = useCompanyId();
  const { user } = useAuth();
  const me = user?.username ?? '';
  const [editing, setEditing] = useState<Editing>();
  const rules = useQuery({
    queryKey: ['eb', 'setup', 'rules'],
    queryFn: () => ebServiceApi.thresholdRules(companyId),
  });
  const docs = useQuery({
    queryKey: ['eb', 'setup', 'docs'],
    queryFn: () => ebServiceApi.requiredDocuments(companyId),
  });
  const ruleAct = useEbMutation(
    (c, v: { id: number; action: Action }) => ebServiceApi.thresholdRuleAction(c, v.id, v.action),
    (r: ThresholdRule) =>
      `Threshold rule ${r.recordStatus === 'ACTIVE' ? 'authorized' : 'deactivated'}`,
    () => undefined,
  );
  const docAct = useEbMutation(
    (c, v: { id: number; action: Action }) =>
      ebServiceApi.requiredDocumentAction(c, v.id, v.action),
    (d: RequiredDocument) =>
      `Required document ${d.recordStatus === 'ACTIVE' ? 'authorized' : 'deactivated'}`,
    () => undefined,
  );
  const close = () => setEditing(undefined);
  const addButton = (label: string, next: Editing) => (
    <Button
      variant="secondary"
      size="sm"
      icon={<Plus size={14} />}
      onClick={() => setEditing(next)}
    >
      {label}
    </Button>
  );
  return (
    <div className="stack">
      <PageHeader
        section={EB_SECTION}
        title="EB Setup"
        description="Value threshold rules and required documents; each change waits for another user's authorization."
      />
      <Card title="Value Threshold Rules" actions={addButton('Add Rule', { kind: 'rule' })}>
        <ErrorAlert error={rules.error ?? ruleAct.error} onRetry={() => void rules.refetch()} />
        <DataTable<ThresholdRule>
          loading={rules.isLoading}
          rows={rules.data ?? []}
          rowKey={(r) => r.id}
          columns={ruleColumns((r) => (
            <RowActions
              row={r}
              name={`rule ${ebLabel(r.measure)}`}
              me={me}
              onEdit={(x) => setEditing({ kind: 'rule', rule: x })}
              onAct={(id, action) => ruleAct.mutate({ id, action })}
            />
          ))}
          emptyMessage="No threshold rule"
        />
      </Card>
      <Card title="Required Documents" actions={addButton('Add Document', { kind: 'doc' })}>
        <ErrorAlert error={docs.error ?? docAct.error} onRetry={() => void docs.refetch()} />
        <DataTable<RequiredDocument>
          loading={docs.isLoading}
          rows={docs.data ?? []}
          rowKey={(d) => d.id}
          columns={docColumns((d) => (
            <RowActions
              row={d}
              name={`${ebLabel(d.processType)} ${ebLabel(d.documentType)}`}
              me={me}
              onEdit={(x) => setEditing({ kind: 'doc', doc: x })}
              onAct={(id, action) => docAct.mutate({ id, action })}
            />
          ))}
          emptyMessage="No required document"
        />
      </Card>
      {editing?.kind === 'rule' && <ThresholdRuleDialog rule={editing.rule} onClose={close} />}
      {editing?.kind === 'doc' && <RequiredDocumentDialog doc={editing.doc} onClose={close} />}
    </div>
  );
}
