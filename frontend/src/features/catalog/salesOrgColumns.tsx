import { useAuth } from '@/auth/authContext';
import type { Column } from '@/components/ui/DataTable';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import type { RowAction } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { countOf, formatDate } from '@/utils/format';
import { awaitsOtherChecker } from '@/utils/makerChecker';
import { CostCenterCell, UnitCell } from './SalesOrgCells';
import { BLANK_FORM, CHILD_LEVEL, LEVEL_LABEL } from './salesTree';
import type { AddForm, OrgRow, ReasonAction, SalesNode } from './salesTree';

/** Columns, cells and row actions of the sales organisation tree table. */

export interface RowHandlers {
  view: (node: SalesNode) => void;
  add: (form: AddForm) => void;
  reason: (action: ReasonAction) => void;
  authorize: (row: OrgRow) => void;
}

interface ActionContext {
  handlers: RowHandlers;
  maintain: boolean;
  /** The user may authorize the record (permission and four eyes). */
  mayAuthorize: (record: { recordStatus: string; maker?: string }) => boolean;
}

function officerActions(
  row: Extract<OrgRow, { kind: 'officer' }>,
  { handlers, maintain, mayAuthorize }: ActionContext,
): RowAction[] {
  const o = row.officer;
  const actions: RowAction[] = [];
  if (mayAuthorize(o)) {
    actions.push({ label: 'Authorize', onSelect: () => handlers.authorize(row) });
  }
  if (!maintain) {
    return actions;
  }
  const removed = o.recordStatus === 'INACTIVE';
  actions.push({
    label: removed ? 'Assign to a Team' : 'Reassign',
    onSelect: () =>
      handlers.add({ ...BLANK_FORM, mode: 'officer', reassign: true, username: o.username }),
  });
  if (!removed) {
    actions.push({
      label: 'Remove from Team',
      danger: true,
      onSelect: () => handlers.reason({ kind: 'remove', officer: o }),
    });
  }
  return actions;
}

function unitActions(
  row: Extract<OrgRow, { kind: 'unit' }>,
  { handlers, maintain, mayAuthorize }: ActionContext,
): RowAction[] {
  const node = row.node;
  const u = node.unit;
  const inactive = u.recordStatus === 'INACTIVE';
  const actions: RowAction[] = [
    { label: maintain && !inactive ? 'View / Edit' : 'View', onSelect: () => handlers.view(node) },
  ];
  if (mayAuthorize(u)) {
    actions.push({ label: 'Authorize', onSelect: () => handlers.authorize(row) });
  }
  if (!maintain) {
    return actions;
  }
  if (inactive) {
    actions.push({
      label: 'Reactivate',
      onSelect: () => handlers.reason({ kind: 'reactivate', node }),
    });
    return actions;
  }
  const childLevel = CHILD_LEVEL[u.level];
  if (childLevel) {
    actions.push({
      label: childLevel === 'TEAM' ? 'Add Team' : 'Add Department',
      onSelect: () => handlers.add({ ...BLANK_FORM, level: childLevel, parentCode: u.code }),
    });
  } else {
    actions.push({
      label: 'Assign Officer',
      onSelect: () => handlers.add({ ...BLANK_FORM, mode: 'officer', parentCode: u.code }),
    });
  }
  actions.push({
    label: 'Deactivate',
    danger: true,
    onSelect: () => handlers.reason({ kind: 'deactivate', node }),
  });
  return actions;
}

/** The actions of a row the user may take, in the row menu. */
export function useRowActions(handlers: RowHandlers) {
  const { can, user } = useAuth();
  const context: ActionContext = {
    handlers,
    maintain: can('MASTER_MAINTAIN'),
    mayAuthorize: (record) => can('MASTER_AUTHORIZE') && awaitsOtherChecker(record, user?.username),
  };
  return (row: OrgRow): RowAction[] =>
    row.kind === 'officer' ? officerActions(row, context) : unitActions(row, context);
}

/** Status of the record of a row. */
export const statusOf = (r: OrgRow) =>
  r.kind === 'officer' ? r.officer.recordStatus : r.node.unit.recordStatus;

/** Whether a unit row has a cost centre to show (its own or inherited). */
const hasCostCenter = (r: OrgRow) => r.kind === 'unit' && Boolean(r.node.costCenter);

export function orgColumns(actionsOf: (row: OrgRow) => RowAction[]): Column<OrgRow>[] {
  return [
    { key: 'unit', header: 'Unit', width: '32%', render: (r) => <UnitCell row={r} /> },
    {
      key: 'level',
      header: 'Level',
      width: '130px',
      render: (r) => (r.kind === 'officer' ? 'Account Officer' : LEVEL_LABEL[r.node.unit.level]),
    },
    {
      key: 'costCenter',
      header: 'Cost Center',
      width: '150px',
      render: (r) => (hasCostCenter(r) ? <CostCenterCell row={r} /> : ''),
    },
    {
      key: 'officers',
      header: 'Account Officers',
      width: '120px',
      render: (r) =>
        r.kind === 'unit' && r.node.unit.level === 'TEAM' ? String(r.activeOfficers) : '',
    },
    {
      key: 'since',
      header: 'Assigned Since',
      width: '140px',
      render: (r) =>
        r.kind === 'officer' ? (
          <span className="nowrap">{formatDate(r.officer.assignedSince)}</span>
        ) : (
          ''
        ),
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      width: '150px',
      render: (r) => <StatusBadge status={statusOf(r)} />,
    },
    {
      key: 'actions',
      header: <span className="visually-hidden">Actions</span>,
      width: '64px',
      render: (r) => (
        <RowActionMenu
          label={r.kind === 'officer' ? r.officer.username : r.node.unit.code}
          actions={actionsOf(r)}
        />
      ),
    },
  ];
}

export function summaryText(counts: {
  regions: number;
  departments: number;
  teams: number;
  officers: number;
}) {
  return [
    countOf(counts.regions, 'region'),
    countOf(counts.departments, 'department'),
    countOf(counts.teams, 'team'),
    countOf(counts.officers, 'officer'),
  ].join(' · ');
}
