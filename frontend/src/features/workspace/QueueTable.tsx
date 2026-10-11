import { Hand, UserPlus } from 'lucide-react';
import { Link } from 'react-router-dom';
import { workflowRecordType } from '@/api/workflow';
import type { WorkItem } from '@/api/workflow';
import {
  useLineNameOrNull,
  useLovLabelOrNull,
  useSalesUnitNameOrNull,
} from '@/components/broking/useLabels';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { CellStack } from '@/components/ui/CellStack';
import { DueDate } from '@/components/ui/DueDate';
import { RowActions } from '@/components/ui/RowActions';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { ageText } from './age';
import { UserName } from '@/components/ui/UserName';
import { formatMoney, splitTrailingAmount } from '@/utils/wording';

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
 * Work items with reference, record type, description (the name, one line, with the unit it comes
 * from under it) and its amount in a right-aligned column of its own ("PHP 2,500.00"), stage, due
 * time with the time in stage under it, assignee, and
 * Claim / Assign in the row action menu (screen standard: one menu button at the end of the
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
          width: '112px',
          render: (i) => workflowRecordType(i.workflowCode),
        },
        {
          key: 'title',
          header: 'Description / From',
          width: '260px',
          render: (i) => (
            <CellStack
              main={<Description text={splitTrailingAmount(i.title).text} />}
              sub={
                i.originatingUnit ? (
                  <OriginUnit code={i.originatingUnit} />
                ) : (
                  <UserName login={i.createdBy} />
                )
              }
            />
          ),
        },
        ...amountColumn(p.items),
        {
          key: 'stage',
          header: 'Stage',
          kind: 'status',
          render: (i) => <StatusBadge status={i.stageCode} workflow={i.workflowCode} />,
        },
        {
          key: 'due',
          header: 'Due / In Stage',
          kind: 'datetime',
          render: (i) => (
            <CellStack
              main={<DueDate at={i.dueAt} overdue={i.overdue} />}
              sub={<span className="num">{ageText(i.stageEnteredAt)} in stage</span>}
            />
          ),
        },
        {
          key: 'assignee',
          header: 'Assignee',
          width: '128px',
          render: (i) => <UserName login={i.assignee} empty="Unassigned" />,
        },
        {
          key: 'actions',
          header: 'Actions',
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
 * Banking), of an employee benefits team (Voluntary), the name of a sales unit (team, department
 * or region), else of a product line (a package request comes from its line). A code without a
 * name is shown muted, never as a value.
 */
function OriginUnit({ code }: Readonly<{ code: string }>) {
  const segment = useLovLabelOrNull('MARKET_SEGMENT');
  const ebTeam = useLovLabelOrNull('EB_TEAM');
  const unit = useSalesUnitNameOrNull();
  const line = useLineNameOrNull();
  const name = segment(code) ?? ebTeam(code) ?? unit(code) ?? line(code);
  if (name === null) {
    return <span className="muted">{code}</span>;
  }
  return (
    <span className="truncate" title={name}>
      {name}
    </span>
  );
}

/** The description of a work item, wrapped in its column (never cut), the full text in the tooltip. */
function Description({ text }: Readonly<{ text: string }>) {
  return (
    <span className="queue-description" title={text}>
      {text}
    </span>
  );
}

/**
 * The Amount column, only when a work item of the list has an amount in its description (a
 * payment, a voucher); a list without amounts has no column of dashes.
 */
function amountColumn(items: readonly WorkItem[]): Column<WorkItem>[] {
  const amountOf = (i: WorkItem) => splitTrailingAmount(i.title);
  if (!items.some((i) => amountOf(i).amount !== undefined)) {
    return [];
  }
  return [
    {
      key: 'amount',
      header: 'Amount',
      kind: 'amount',
      width: '136px',
      render: (i) => {
        const { currency, amount } = amountOf(i);
        return amount === undefined ? '' : formatMoney(currency, amount);
      },
    },
  ];
}
