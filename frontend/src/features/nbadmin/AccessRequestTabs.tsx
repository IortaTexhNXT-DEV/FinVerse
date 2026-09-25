import { useQuery } from '@tanstack/react-query';
import { Fragment } from 'react';
import type { ReactNode } from 'react';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest, AccessRequestEvent, ApproverStep, UserAccess } from '@/api/nbadmin';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { isGroupProfile, roleChanges, userStatus } from './accessRequest';

interface Row {
  attribute: string;
  current?: ReactNode;
  requested?: ReactNode;
}

const COMPARE: Column<Row>[] = [
  { key: 'a', header: 'Attribute', render: (r) => r.attribute },
  { key: 'c', header: 'Current', render: (r) => r.current ?? '—' },
  { key: 'r', header: 'Requested', render: (r) => r.requested ?? '—' },
];

function rolesRequested(requested: string[], added: string[], removed: string[]): string {
  if (added.length + removed.length === 0) {
    return requested.join(', ');
  }
  const change = `added: ${added.join(', ') || '—'}; removed: ${removed.join(', ') || '—'}`;
  return `${requested.join(', ')} (${change})`;
}

function userRows(r: AccessRequest, user: UserAccess | undefined): Row[] {
  const d = r.details;
  const { added, removed } = roleChanges(user?.roleCodes ?? [], r.roleCodes);
  const rows: Row[] = [
    { attribute: 'Full name', current: user?.fullName, requested: r.fullName },
    { attribute: 'E-mail', current: user?.email, requested: r.email },
    { attribute: 'Windows ID', current: user?.windowsId, requested: d.windowsId },
    { attribute: 'Business unit', current: user?.businessUnitCode, requested: d.businessUnitCode },
    { attribute: 'User level', current: user?.userLevel, requested: d.userLevel },
  ];
  if (r.roleCodes.length > 0) {
    rows.push({
      attribute: 'Group profiles',
      current: user?.roleCodes.join(', '),
      requested: rolesRequested(r.roleCodes, added, removed),
    });
  }
  if (user !== undefined) {
    rows.push({
      attribute: 'Status',
      current: <StatusBadge status={userStatus(user)} />,
    });
  }
  return rows.filter((row) => row.requested !== undefined || row.attribute === 'Status');
}

function profileRows(r: AccessRequest): Row[] {
  const d = r.details;
  return [
    { attribute: 'Group profile', requested: r.roleCode },
    { attribute: 'Name', requested: d.roleName },
    { attribute: 'Description', requested: d.roleDescription },
    { attribute: 'Privilege level', requested: d.privilegeLevel && humanize(d.privilegeLevel) },
    { attribute: 'Permissions added', requested: r.permissionsAdded.join(', ') || undefined },
    { attribute: 'Permissions removed', requested: r.permissionsRemoved.join(', ') || undefined },
  ].filter((row) => row.requested !== undefined && row.requested !== '');
}

function extraFacts(r: AccessRequest, members: string[]): [string, string][] {
  const d = r.details;
  const facts: [string, string][] = [['Remarks', r.justification ?? '—']];
  if (d.effectiveFrom) {
    facts.push(['Effective date', formatDate(d.effectiveFrom)]);
  }
  if (d.reasonCode) {
    facts.push(['Reason', humanize(d.reasonCode)]);
  }
  if (d.partyCode) {
    facts.push(['Party', `${humanize(d.partyKind ?? '')} ${d.partyCode} · ${d.portalRole ?? ''}`]);
  }
  if (r.type === 'DEACTIVATE_ROLE') {
    facts.push(['Members', members.join(', ') || 'None']);
  }
  if (r.lifecycle.applyError) {
    facts.push(['Could not be applied', r.lifecycle.applyError]);
  }
  return facts;
}

/** Current and requested values of a request (FR-UA-030). */
export function RequestDetails({ request: r }: Readonly<{ request: AccessRequest }>) {
  const users = useQuery({ queryKey: ['nbadmin', 'users'], queryFn: nbadminApi.users });
  const group = isGroupProfile(r.type);
  const user = users.data?.find((u) => u.username === r.username);
  const members = (users.data ?? [])
    .filter((u) => r.roleCode !== undefined && u.roleCodes.includes(r.roleCode))
    .map((u) => u.username);
  return (
    <Card>
      <div className="stack">
        <DataTable<Row>
          caption="Current and requested values"
          columns={COMPARE}
          rows={group ? profileRows(r) : userRows(r, user)}
          rowKey={(row) => row.attribute}
        />
        <dl className="detail-list">
          {extraFacts(r, members).map(([label, value]) => (
            <Fragment key={label}>
              <dt>{label}</dt>
              <dd>{value}</dd>
            </Fragment>
          ))}
        </dl>
      </div>
    </Card>
  );
}

const APPROVER_COLUMNS: Column<ApproverStep>[] = [
  { key: 's', header: 'Order', numeric: true, render: (a) => a.sequence },
  { key: 'a', header: 'Approver', render: (a) => a.approver },
  { key: 'd', header: 'Decision', render: (a) => <StatusBadge status={a.decision} /> },
  { key: 'r', header: 'Remarks', render: (a) => a.remarks ?? '' },
  { key: 't', header: 'Decided', render: (a) => formatDateTime(a.decidedAt) },
];

/** The approvers of a request in order with their decisions (FR-UA-044). */
export function RequestApprovers({ request: r }: Readonly<{ request: AccessRequest }>) {
  return (
    <Card>
      <DataTable<ApproverStep>
        caption="Approvers"
        columns={APPROVER_COLUMNS}
        rows={r.lifecycle.approvers}
        rowKey={(a) => a.sequence}
        emptyMessage={
          r.status === 'DRAFT'
            ? 'The approvers are chosen on submission.'
            : 'Any holder of the approval right decides this request.'
        }
      />
    </Card>
  );
}

const HISTORY_COLUMNS: Column<AccessRequestEvent>[] = [
  { key: 't', header: 'Date', render: (e) => formatDateTime(e.occurredAt) },
  { key: 'a', header: 'Action', render: (e) => humanize(e.action) },
  { key: 's', header: 'Status', render: (e) => <StatusBadge status={e.toStatus} /> },
  { key: 'u', header: 'By', render: (e) => e.actor },
  { key: 'r', header: 'Remarks', render: (e) => e.remarks ?? '' },
];

/** Every event of a request with its remarks (BRD 1.008; History tab). */
export function RequestHistory({ requestId }: Readonly<{ requestId: number }>) {
  const history = useQuery({
    queryKey: ['nbadmin', 'history', requestId],
    queryFn: () => nbadminApi.history(requestId),
  });
  return (
    <Card>
      <DataTable<AccessRequestEvent>
        caption="History"
        loading={history.isLoading}
        columns={HISTORY_COLUMNS}
        rows={history.data ?? []}
        rowKey={(e) => e.id}
      />
    </Card>
  );
}
