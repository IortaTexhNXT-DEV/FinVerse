import { useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Upload, XCircle } from 'lucide-react';
import { useState } from 'react';
import type { ProgrammeView } from '@/api/eb';
import { ebServiceApi } from '@/api/ebService';
import type { Member, RosterVersion } from '@/api/ebMarket';
import { useAuth } from '@/auth/authContext';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { ActionButton } from '../common/ActionButton';
import { useEbMutation } from '../common/useEbMutation';

type Acting = { mode: 'upload' } | { mode: 'accept' | 'reject'; version: RosterVersion };

/** Upload Master List: the client's census for a policy year, staged for review. */
function UploadDialog({
  programme,
  onClose,
}: Readonly<{ programme: ProgrammeView; onClose: () => void }>) {
  const queryClient = useQueryClient();
  const current = programme.cycles.find((c) => c.id === programme.currentCycleId);
  const [year, setYear] = useState(current ? String(current.policyYear) : '');
  return (
    <Modal open title="Upload Master List" onClose={onClose}>
      <BulkUploadWizard
        handler="EB_MASTERLIST"
        parameters={{ programmeId: String(programme.id), policyYear: year }}
        parametersReady={/^\d{4}$/.test(year)}
        onCommitted={() => void queryClient.invalidateQueries({ queryKey: ['eb'] })}
        parameterFields={
          <Field label="Policy Year" required>
            {(id) => (
              <input
                id={id}
                className="input"
                inputMode="numeric"
                value={year}
                onChange={(e) => setYear(e.target.value)}
              />
            )}
          </Field>
        }
      />
    </Modal>
  );
}

function versionColumns(market: boolean, onAct: (a: Acting) => void): Column<RosterVersion>[] {
  return [
    {
      key: 'version',
      header: 'Roster',
      render: (v) => (
        <CellStack
          main={`${String(v.policyYear)} – version ${String(v.versionNo)}`}
          sub={v.sourceRef}
        />
      ),
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (v) => (
        <StatusBadge status={v.status} tone={v.status === 'ACCEPTED' ? 'success' : undefined} />
      ),
    },
    { key: 'count', header: 'Members', kind: 'center', render: (v) => v.headcount },
    {
      key: 'loaded',
      header: 'Loaded',
      render: (v) => (
        <CellStack main={formatDateTime(v.loadedAt)} sub={<UserName login={v.loadedBy} />} />
      ),
    },
    { key: 'reason', header: 'Reason', render: (v) => v.rejectReason ?? '' },
    {
      key: 'actions',
      header: '',
      width: '96px',
      render: (v) =>
        market &&
        v.status === 'STAGED' && (
          <span className="eb-actions">
            <Button
              variant="ghost"
              size="sm"
              aria-label={`Accept roster ${String(v.versionNo)}`}
              icon={<CheckCircle2 size={14} />}
              onClick={() => onAct({ mode: 'accept', version: v })}
            />
            <Button
              variant="ghost"
              size="sm"
              aria-label={`Reject roster ${String(v.versionNo)}`}
              icon={<XCircle size={14} />}
              onClick={() => onAct({ mode: 'reject', version: v })}
            />
          </span>
        ),
    },
  ];
}

const MEMBER_COLUMNS: Column<Member>[] = [
  { key: 'no', header: 'Employee No.', kind: 'code', render: (m) => m.employeeNo },
  { key: 'name', header: 'Name', render: (m) => `${m.lastName}, ${m.firstName}` },
  { key: 'birth', header: 'Birth Date', kind: 'date', render: (m) => formatDate(m.birthDate) },
  { key: 'plan', header: 'Plan', kind: 'code', render: (m) => m.planCode },
  { key: 'deps', header: 'Dependants', kind: 'center', render: (m) => m.dependants },
  { key: 'from', header: 'Effective', kind: 'date', render: (m) => formatDate(m.effectiveFrom) },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (m) => <StatusBadge status={m.status} />,
  },
];

/** The members of a roster version, searchable by employee number or name. */
function MembersCard({ version }: Readonly<{ version: RosterVersion }>) {
  const companyId = useCompanyId();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const members = useQuery({
    queryKey: ['eb', 'members', version.id, q, page],
    queryFn: () => ebServiceApi.members(companyId, version.id, q, page),
  });
  const differences = useQuery({
    queryKey: ['eb', 'roster-differences', version.id],
    queryFn: () => ebServiceApi.differences(companyId, version.id),
    enabled: version.status === 'STAGED',
  });
  const d = differences.data;
  return (
    <Card
      flush
      title={`Members – ${String(version.policyYear)} version ${String(version.versionNo)}`}
    >
      {d && (
        <div className="card-body">
          <DefinitionGrid
            columns={2}
            items={[
              {
                label: 'Headcount',
                value: `${String(d.headcount)} (now ${String(d.currentHeadcount)})`,
              },
              { label: 'Added', value: String(d.added) },
              { label: 'Removed', value: String(d.removed) },
              { label: 'Plan Changes', value: String(d.planChanges) },
            ]}
          />
        </div>
      )}
      <WorklistToolbar
        placeholder="Search employee no. or name"
        onSearch={(text) => {
          setQ(text);
          setPage(0);
        }}
      />
      <ErrorAlert error={members.error} onRetry={() => void members.refetch()} />
      <DataTable<Member>
        loading={members.isLoading}
        rows={members.data?.content ?? []}
        rowKey={(m) => m.id}
        columns={MEMBER_COLUMNS}
        emptyMessage="No member found"
      />
      <PageFooter data={members.data} noun="members" onPage={setPage} />
    </Card>
  );
}

/** The roster shown: the one selected, else the staged one, else the accepted one. */
function shownVersion(rows: RosterVersion[], selected: number | undefined) {
  return (
    rows.find((v) => v.id === selected) ??
    rows.find((v) => v.status === 'STAGED') ??
    rows.find((v) => v.status === 'ACCEPTED')
  );
}

/**
 * Members tab: the rosters of the programme per policy year, uploaded from the client's master
 * list and staged until the AO accepts them, with the differences from the accepted roster; the
 * members of the selected roster.
 */
export function MembersTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [acting, setActing] = useState<Acting>();
  const [selected, setSelected] = useState<number>();
  const close = () => setActing(undefined);
  const versions = useQuery({
    queryKey: ['eb', 'roster', programme.id],
    queryFn: () => ebServiceApi.roster(companyId, programme.id),
  });
  const accept = useEbMutation(
    (c, id: number) => ebServiceApi.acceptRoster(c, id),
    (v: RosterVersion) => `Roster ${String(v.policyYear)} version ${String(v.versionNo)} accepted`,
    close,
  );
  const reject = useEbMutation(
    (c, v: { id: number; reason: string }) => ebServiceApi.rejectRoster(c, v.id, v.reason),
    'Roster rejected',
    close,
  );
  const market = can('EB_MARKET');
  const rows = versions.data ?? [];
  const shown = shownVersion(rows, selected);
  const label = (v: RosterVersion) => `${String(v.policyYear)} version ${String(v.versionNo)}`;
  return (
    <div className="stack">
      <Card
        title="Rosters"
        actions={
          <ActionButton
            shown={market}
            icon={<Upload size={14} />}
            label="Upload Master List"
            onClick={() => setActing({ mode: 'upload' })}
          />
        }
      >
        <ErrorAlert error={versions.error} onRetry={() => void versions.refetch()} />
        <DataTable<RosterVersion>
          loading={versions.isLoading}
          rows={rows}
          rowKey={(v) => v.id}
          columns={versionColumns(market, setActing)}
          selectedKey={shown?.id}
          onRowClick={(v) => setSelected(v.id)}
          emptyMessage="No master list uploaded"
        />
      </Card>
      {shown && <MembersCard version={shown} />}
      {acting?.mode === 'upload' && <UploadDialog programme={programme} onClose={close} />}
      {acting?.mode === 'accept' && (
        <ConfirmDialog
          title="Accept Roster"
          record={label(acting.version)}
          effect="The roster becomes the members of the policy year; the earlier accepted roster is superseded."
          confirmLabel="Accept"
          busy={accept.isPending}
          error={accept.error}
          onConfirm={() => accept.mutate(acting.version.id)}
          onClose={close}
        />
      )}
      {acting?.mode === 'reject' && (
        <ConfirmDialog
          title="Reject Roster"
          record={label(acting.version)}
          effect="The staged roster is set aside; upload a corrected master list."
          confirmLabel="Reject"
          reason="required"
          destructive
          busy={reject.isPending}
          error={reject.error}
          onConfirm={(reason) => reject.mutate({ id: acting.version.id, reason })}
          onClose={close}
        />
      )}
    </div>
  );
}
