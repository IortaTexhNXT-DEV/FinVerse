import { Hand, UserPlus } from 'lucide-react';
import { Link } from 'react-router-dom';
import { workflowRecordType } from '@/api/workflow';
import type { WorkItem } from '@/api/workflow';
import { useLovLabelOrNull, useSalesUnitNameOrNull } from '@/components/broking/useLabels';
import { DataTable } from '@/components/ui/DataTable';
import { DueDate } from '@/components/ui/DueDate';
import { RowActions } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ageText } from './age';
import { UserName } from '@/components/ui/UserName';

interface QueueTableProps {
  items: WorkItem[];
  loading: boolean;
  canAssign: boolean;
  claiming: boolean;
  onOpen: (item: WorkItem) => void;
  onClaim: (item: WorkItem) => void;
  onAssign: (item: WorkItem) => void;
}

/**
 * Work items with reference, record type, description, stage, origin, age, due time, assignee,
 * and Claim / Assign in the row action menu (screen standard: one menu button at the end of the
 * row). The record type has its own column, never a word beside the stage pill.
 */
export function QueueTable(p: Readonly<QueueTableProps>) {
  return (
    <DataTable<WorkItem>
      loading={p.loading}
      rows={p.items}
      rowKey={(i) => i.id}
      onRowClick={p.onOpen}
      emptyMessage="Nothing waiting here."
      callout="work-queue"
      columns={[
        {
          key: 'ref',
          header: 'Reference',
          kind: 'code',
          render: (i) => (i.link ? <Link to={i.link}>{i.reference}</Link> : i.reference),
        },
        {
          key: 'type',
          header: 'Type',
          width: '160px',
          truncate: true,
          render: (i) => workflowRecordType(i.workflowCode),
        },
        { key: 'title', header: 'Description', render: (i) => i.title },
        {
          key: 'stage',
          header: 'Stage',
          kind: 'status',
          render: (i) => <StatusBadge status={i.stageCode} />,
        },
        {
          key: 'unit',
          header: 'From',
          width: '180px',
          truncate: true,
          render: (i) =>
            i.originatingUnit ? (
              <OriginUnit code={i.originatingUnit} />
            ) : (
              <UserName login={i.createdBy} truncate />
            ),
        },
        {
          key: 'age',
          header: 'In Stage',
          kind: 'amount',
          width: '96px',
          render: (i) => ageText(i.stageEnteredAt),
        },
        {
          key: 'due',
          header: 'Due',
          kind: 'datetime',
          render: (i) => <DueDate at={i.dueAt} overdue={i.overdue} />,
        },
        {
          key: 'assignee',
          header: 'Assignee',
          width: '180px',
          truncate: true,
          render: (i) => <UserName login={i.assignee} empty="Unassigned" truncate />,
        },
        {
          key: 'actions',
          header: '',
          kind: 'actions',
          render: (i) => (
            <RowActions
              record={i.reference}
              actions={[
                {
                  label: 'Claim',
                  icon: <Hand size={14} />,
                  hidden: Boolean(i.assignee),
                  disabledReason: p.claiming ? 'A claim is in progress' : undefined,
                  onSelect: () => p.onClaim(i),
                },
                {
                  label: 'Assign',
                  icon: <UserPlus size={14} />,
                  hidden: !p.canAssign,
                  onSelect: () => p.onAssign(i),
                },
              ]}
            />
          ),
        },
      ]}
    />
  );
}

/**
 * The unit a work item comes from, by its name: the label of a market segment (Corporate
 * Banking), of an employee benefits team (Voluntary), else the name of a sales unit (team,
 * department or region). A code without a name is shown muted, never as a value.
 */
function OriginUnit({ code }: Readonly<{ code: string }>) {
  const segment = useLovLabelOrNull('MARKET_SEGMENT');
  const ebTeam = useLovLabelOrNull('EB_TEAM');
  const unit = useSalesUnitNameOrNull();
  const name = segment(code) ?? ebTeam(code) ?? unit(code);
  if (name === null) {
    return <span className="muted">{code}</span>;
  }
  return (
    <span className="truncate" title={name}>
      {name}
    </span>
  );
}
