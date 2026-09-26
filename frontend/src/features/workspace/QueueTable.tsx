import { Hand, UserPlus } from 'lucide-react';
import type { MouseEvent } from 'react';
import { Link } from 'react-router-dom';
import { WORKFLOW_NAMES } from '@/api/workflow';
import type { WorkItem } from '@/api/workflow';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';
import { ageText } from './age';

interface QueueTableProps {
  items: WorkItem[];
  loading: boolean;
  canAssign: boolean;
  claiming: boolean;
  onOpen: (item: WorkItem) => void;
  onClaim: (item: WorkItem) => void;
  onAssign: (item: WorkItem) => void;
}

/** Runs a row button without also opening the row. */
const only = (action: () => void) => (e: MouseEvent) => {
  e.stopPropagation();
  action();
};

/** Work items with reference, stage, origin, age, due time, assignee and claim / assign. */
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
            <span>
              <StatusBadge status={i.stageCode} />{' '}
              <span className="muted">{WORKFLOW_NAMES[i.workflowCode]}</span>
            </span>
          ),
        },
        { key: 'unit', header: 'From', render: (i) => i.originatingUnit ?? i.createdBy },
        { key: 'age', header: 'In Stage', render: (i) => ageText(i.stageEnteredAt) },
        { key: 'due', header: 'Due', render: (i) => <DueCell item={i} /> },
        {
          key: 'assignee',
          header: 'Assignee',
          render: (i) => i.assignee ?? <span className="muted">Unassigned</span>,
        },
        {
          key: 'actions',
          header: '',
          render: (i) => (
            <span className="row">
              {!i.assignee && (
                <Button
                  size="sm"
                  variant="secondary"
                  icon={<Hand size={14} />}
                  busy={p.claiming}
                  onClick={only(() => p.onClaim(i))}
                >
                  Claim
                </Button>
              )}
              {p.canAssign && (
                <Button
                  size="sm"
                  variant="ghost"
                  icon={<UserPlus size={14} />}
                  onClick={only(() => p.onAssign(i))}
                >
                  Assign
                </Button>
              )}
            </span>
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
