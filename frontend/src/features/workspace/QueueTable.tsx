import { Hand, UserPlus } from 'lucide-react';
import { Link } from 'react-router-dom';
import { WORKFLOW_NAMES } from '@/api/workflow';
import type { WorkItem } from '@/api/workflow';
import { useLovLabelOrNull, useSalesUnitName } from '@/components/broking/useLabels';
import { DataTable } from '@/components/ui/DataTable';
import { RowActions } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
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
 * Work items with reference, stage, origin, age, due time, assignee, and Claim / Assign in the
 * row action menu (screen standard: one menu button at the end of the row).
 */
export function QueueTable(p: Readonly<QueueTableProps>) {
  return (
    <DataTable<WorkItem>
      loading={p.loading}
      rows={p.items}
      rowKey={(i) => i.id}
      onRowClick={p.onOpen}
      emptyMessage="Nothing waiting here."
      columns={[
        {
          key: 'ref',
          header: 'Reference',
          render: (i) => (i.link ? <Link to={i.link}>{i.reference}</Link> : i.reference),
        },
        { key: 'title', header: 'Description', render: (i) => i.title },
        {
          key: 'stage',
          header: 'Stage',
          render: (i) => (
            <span className="cell-stack">
              <span>
                <StatusBadge status={i.stageCode} />
              </span>
              <span className="muted">{WORKFLOW_NAMES[i.workflowCode]}</span>
            </span>
          ),
        },
        {
          key: 'unit',
          header: 'From',
          render: (i) =>
            i.originatingUnit ? (
              <OriginUnit code={i.originatingUnit} />
            ) : (
              <UserName login={i.createdBy} />
            ),
        },
        { key: 'age', header: 'In Stage', render: (i) => ageText(i.stageEnteredAt) },
        { key: 'due', header: 'Due', render: (i) => <DueCell item={i} /> },
        {
          key: 'assignee',
          header: 'Assignee',
          render: (i) => <UserName login={i.assignee} empty="Unassigned" />,
        },
        {
          key: 'actions',
          header: '',
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

function DueCell({ item }: Readonly<{ item: WorkItem }>) {
  if (!item.dueAt) {
    return <span className="muted">—</span>;
  }
  return <span className={item.overdue ? 'text-danger' : ''}>{formatDateTime(item.dueAt)}</span>;
}

/**
 * The unit a work item comes from, by its name: the label of a market segment (Corporate
 * Banking), else the name of a sales unit (team, department or region); never the code.
 */
function OriginUnit({ code }: Readonly<{ code: string }>) {
  const segment = useLovLabelOrNull('MARKET_SEGMENT');
  const unit = useSalesUnitName();
  return <>{segment(code) ?? unit(code)}</>;
}
