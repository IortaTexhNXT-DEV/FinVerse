import { useQuery } from '@tanstack/react-query';
import { Fragment } from 'react';
import type { ReactNode } from 'react';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest, AccessRequestEvent, ApproverStep, UserAccess } from '@/api/nbadmin';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useLovLabel } from '@/components/broking/useLabels';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { PermissionChanges } from '@/components/broking/PermissionPicker';
import { isGroupProfile, roleChanges } from './accessRequest';
import { userStatus } from './accessUsers';
import { accessStatusLabel } from './accessStages';
import { RequestedDataAccess } from './DataAccessFields';
import { UserName } from '@/components/ui/UserName';

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

/** Names of group profiles and of list values, for the rows of a request. */
interface Names {
  profile: (code: string) => string;
  businessUnit: (code: string | null | undefined) => string;
  userLevel: (code: string | null | undefined) => string;
}

function profileList(codes: string[], names: Names): string {
  return codes.map(names.profile).join(', ');
}

/** A group profile the request adds to or removes from the user. */
interface ProfileChange {
  code: string;
  name: string;
  change: 'Added' | 'Removed';
}

const PROFILE_CHANGE_COLUMNS: Column<ProfileChange>[] = [
  { key: 'n', header: 'Group Profile', render: (c) => c.name },
  {
    key: 'c',
    header: 'Change',
    render: (c) => (
      <StatusBadge status={c.change === 'Added' ? 'ADDED' : 'REMOVED'} label={c.change} />
    ),
  },
];

function profileChanges(added: string[], removed: string[], names: Names): ProfileChange[] {
  return [
    ...added.map((code) => ({ code, name: names.profile(code), change: 'Added' as const })),
    ...removed.map((code) => ({ code, name: names.profile(code), change: 'Removed' as const })),
  ];
}

function amount(value: number | null | undefined): string | undefined {
  return value === undefined || value === null ? undefined : formatAmount(value);
}

function label(
  of: (code: string | null | undefined) => string,
  code: string | undefined,
): string | undefined {
  return code ? of(code) : undefined;
}

function userRows(r: AccessRequest, user: UserAccess | undefined, names: Names): Row[] {
  const d = r.details;
  const rows: Row[] = [
    { attribute: 'Full name', current: user?.fullName, requested: r.fullName },
    { attribute: 'E-mail', current: user?.email, requested: r.email },
    { attribute: 'Windows ID', current: user?.windowsId, requested: d.windowsId },
    {
      attribute: 'Business unit',
      current: label(names.businessUnit, user?.businessUnitCode),
      requested: label(names.businessUnit, d.businessUnitCode),
    },
    {
      attribute: 'User level',
      current: label(names.userLevel, user?.userLevel),
      requested: label(names.userLevel, d.userLevel),
    },
    {
      attribute: 'Authorisation limit',
      current: amount(user?.authorizationLimit),
      requested: amount(d.authorizationLimit),
    },
  ];
  if (r.roleCodes.length > 0) {
    rows.push({
      attribute: 'Group profiles',
      current: user === undefined ? undefined : profileList(user.roleCodes, names),
      requested: profileList(r.roleCodes, names),
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

function profileRows(r: AccessRequest, names: Names): Row[] {
  const d = r.details;
  return [
    {
      attribute: 'Group profile',
      requested: r.type === 'CREATE_ROLE' ? r.roleCode : names.profile(r.roleCode ?? ''),
    },
    { attribute: 'Name', requested: d.roleName },
    { attribute: 'Description', requested: d.roleDescription },
    { attribute: 'Privilege level', requested: d.privilegeLevel && humanize(d.privilegeLevel) },
  ].filter((row) => row.requested !== undefined && row.requested !== '');
}

function extraFacts(
  r: AccessRequest,
  members: string[],
  reason: (code: string | null | undefined) => string,
): [string, string][] {
  const d = r.details;
  const facts: [string, string][] = [['Remarks', r.justification ?? '—']];
  if (d.effectiveFrom) {
    facts.push(['Effective date', formatDate(d.effectiveFrom)]);
  }
  if (d.reasonCode) {
    facts.push(['Reason', reason(d.reasonCode)]);
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
  const roles = useQuery({ queryKey: ['nbadmin', 'roles'], queryFn: nbadminApi.roles });
  const businessUnit = useLovLabel('UAM_BUSINESS_UNIT');
  const userLevel = useLovLabel('UAM_USER_LEVEL');
  const reason = useLovLabel('UAM_DEACTIVATION_REASON');
  const names: Names = {
    profile: (code) => roles.data?.find((x) => x.code === code)?.name ?? code,
    businessUnit,
    userLevel,
  };
  const group = isGroupProfile(r.type);
  const user = users.data?.find((u) => u.username === r.username);
  const { added, removed } = roleChanges(user?.roleCodes ?? [], r.roleCodes);
  const changes = group ? [] : profileChanges(added, removed, names);
  const members = (users.data ?? [])
    .filter((u) => r.roleCode !== undefined && u.roleCodes.includes(r.roleCode))
    .map((u) => u.username);
  return (
    <Card>
      <div className="stack">
        <DataTable<Row>
          caption="Current and requested values"
          columns={COMPARE}
          rows={group ? profileRows(r, names) : userRows(r, user, names)}
          rowKey={(row) => row.attribute}
        />
        {changes.length > 0 && (
          <h3 style={{ margin: 0, fontSize: 'var(--font-size-md, 15px)' }}>
            Group Profiles Added and Removed
          </h3>
        )}
        {changes.length > 0 && (
          <DataTable<ProfileChange>
            caption="Group profiles added and removed"
            columns={PROFILE_CHANGE_COLUMNS}
            rows={changes}
            rowKey={(c) => `${c.change}-${c.code}`}
          />
        )}
        {group && (r.permissionsAdded.length > 0 || r.permissionsRemoved.length > 0) && (
          <section aria-label="Permissions added and removed">
            <h3 style={{ margin: '0 0 var(--space-2)', fontSize: 'var(--font-size-md, 15px)' }}>
              Permissions Added and Removed
            </h3>
            <PermissionChanges current={r.permissionsRemoved} selected={r.permissionsAdded} />
          </section>
        )}
        {!group && <RequestedDataAccess text={r.details.dataScope} />}
        <dl className="detail-list">
          {extraFacts(r, members, reason).map(([label, value]) => (
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
  { key: 'a', header: 'Approver', render: (a) => <UserName login={a.approver} /> },
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
  {
    key: 's',
    header: 'Status',
    render: (e) => <StatusBadge full status={e.toStatus} label={accessStatusLabel(e.toStatus)} />,
  },
  { key: 'u', header: 'By', render: (e) => <UserName login={e.actor} /> },
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
