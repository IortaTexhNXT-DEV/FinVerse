import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Plus, Trash2, Upload } from 'lucide-react';
import { useState } from 'react';
import { migrationApi } from '@/api/migration';
import type { MapEntry, MapEntryInput, MapVersion } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { ActionConfirm } from '../common/ActionConfirm';
import type { MigAction } from '../common/ActionConfirm';
import { MigStatus } from '../common/MigStatus';
import { useDownload } from '../common/useDownload';
import { EntryDialog } from './EntryDialog';
import { ACTION_LABEL, EMPTY } from './entryCodes';
import { UserName } from '@/components/ui/UserName';
import { BRAND } from '@/branding';

const VERSION_COLUMNS: Column<MapVersion>[] = [
  { key: 'v', header: 'Version', kind: 'center', render: (v) => `v${String(v.versionNo)}` },
  { key: 'comment', header: 'Comment', render: (v) => v.comment ?? '' },
  {
    key: 'by',
    header: 'Prepared by',
    render: (v) => <UserName login={v.submittedBy ?? v.createdBy} />,
  },
  {
    key: 'approvedBy',
    header: 'Approved by',
    render: (v) => <UserName login={v.approvedBy} />,
  },
  {
    key: 'approvedAt',
    header: 'Approved',
    kind: 'datetime',
    render: (v) => formatDateTime(v.approvedAt),
  },
  { key: 'return', header: 'Return reason', render: (v) => v.returnReason ?? '' },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (v) => <MigStatus status={v.status} />,
  },
];

function entryColumns(onDelete: ((e: MapEntry) => void) | undefined): Column<MapEntry>[] {
  const cols: Column<MapEntry>[] = [
    { key: 'source', header: 'Source', render: (e) => e.sourceSystem },
    { key: 'code', header: 'Legacy code', kind: 'code', render: (e) => e.legacyCode },
    { key: 'desc', header: 'Legacy description', render: (e) => e.legacyDescription ?? '' },
    {
      key: 'qualifier',
      header: 'Condition',
      render: (e) => (e.qualifier ? e.qualifier + ' = ' + (e.qualifierValue ?? '') : ''),
    },
    { key: 'action', header: 'Action', render: (e) => ACTION_LABEL[e.action] },
    {
      key: 'target',
      header: `${BRAND.product} value`,
      kind: 'code',
      render: (e) => e.targetCode ?? '',
    },
    { key: 'remarks', header: 'Remarks', render: (e) => e.remarks ?? '' },
  ];
  if (onDelete !== undefined) {
    cols.push({
      key: 'delete',
      header: '',
      render: (e) => (
        <Button
          variant="ghost"
          size="sm"
          aria-label={`Delete entry ${e.legacyCode}`}
          icon={<Trash2 size={14} />}
          onClick={(ev) => {
            ev.stopPropagation();
            onDelete(e);
          }}
        />
      ),
    });
  }
  return cols;
}

function versionActions(
  setCode: string,
  version: MapVersion,
  mayEdit: boolean,
  mayApprove: boolean,
): { label: string; primary: boolean; action: MigAction }[] {
  const name = `v${String(version.versionNo)} of ${setCode}`;
  const out: { label: string; primary: boolean; action: MigAction }[] = [];
  if (mayEdit) {
    out.push({
      label: 'Submit',
      primary: true,
      action: {
        title: `Submit ${name}`,
        effect: 'The version goes to the business owner for approval.',
        confirmLabel: 'Submit',
        done: 'Version submitted',
        run: () => migrationApi.submitVersion(version.id),
      },
    });
  }
  if (mayApprove) {
    out.push(
      {
        label: 'Approve',
        primary: true,
        action: {
          title: `Approve ${name}`,
          effect:
            'The version replaces the approved one for the next batches; values marked Create are created through the reference-data load.',
          confirmLabel: 'Approve',
          reason: 'optional',
          done: 'Version approved',
          run: (comment) => migrationApi.approveVersion(version.id, comment),
        },
      },
      {
        label: 'Return',
        primary: false,
        action: {
          title: `Return ${name}`,
          effect: 'The version goes back to the Data Steward as a draft.',
          confirmLabel: 'Return',
          reason: 'required',
          done: 'Version returned',
          run: (reason) => migrationApi.returnVersion(version.id, reason),
        },
      },
    );
  }
  return out;
}

function version0(list: MapVersion[], versionId: number | undefined): MapVersion | undefined {
  const id = versionId ?? list[0]?.id;
  return list.find((v) => v.id === id);
}

function versionState(
  list: MapVersion[],
  versionId: number | undefined,
  canEdit: boolean,
  canApprove: boolean,
) {
  const version = version0(list, versionId);
  return {
    currentId: version?.id,
    version,
    draft: canEdit && version?.status === 'DRAFT',
    hasOpen: list.some((v) => v.status === 'DRAFT' || v.status === 'SUBMITTED'),
    mayApprove: canApprove && version?.status === 'SUBMITTED',
  };
}

function SetActions({
  setCode,
  version,
  canCreate,
  onAction,
  onImported,
  onClose,
}: Readonly<{
  setCode: string;
  version: MapVersion | undefined;
  canCreate: boolean;
  onAction: (a: MigAction) => void;
  onImported: (v: MapVersion) => void;
  onClose: () => void;
}>) {
  const companyId = useCompanyId();
  const client = useQueryClient();
  const download = useDownload();
  const importing = useMutation({
    mutationFn: (file: File) => migrationApi.importDraft(companyId, setCode, file),
    onSuccess: async (v) => {
      onImported(v);
      await client.invalidateQueries({ queryKey: ['migration'] });
    },
  });
  return (
    <span className="mig-actions">
      <ErrorAlert error={importing.error ?? download.error} />
      {canCreate && (
        <>
          <Button
            variant="primary"
            size="sm"
            icon={<Plus size={14} />}
            onClick={() =>
              onAction({
                title: `New version of ${setCode}`,
                effect: 'A draft is created from the approved version, ready for your changes.',
                confirmLabel: 'Create Draft',
                reason: 'optional',
                done: 'Draft created',
                run: (comment) => migrationApi.createDraft(companyId, setCode, true, comment),
              })
            }
          >
            New Version
          </Button>
          <label className="btn btn-secondary btn-sm">
            <Upload size={14} /> Import Excel
            <input
              type="file"
              accept=".xlsx"
              hidden
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) {
                  importing.mutate(file);
                }
              }}
            />
          </label>
        </>
      )}
      {version !== undefined && (
        <Button
          variant="secondary"
          size="sm"
          icon={<Download size={14} />}
          busy={download.busy}
          onClick={() => download.run(() => migrationApi.exportVersion(version.id))}
        >
          Export
        </Button>
      )}
      <Button variant="ghost" size="sm" onClick={onClose}>
        Close
      </Button>
    </span>
  );
}

function VersionEntries({
  version,
  draft,
  buttons,
  onAction,
}: Readonly<{
  version: MapVersion;
  draft: boolean;
  buttons: { label: string; primary: boolean; action: MigAction }[];
  onAction: (a: MigAction) => void;
}>) {
  const client = useQueryClient();
  const [editing, setEditing] = useState<{ id?: number; entry: MapEntryInput }>();
  const entries = useQuery({
    queryKey: ['migration', 'maps', 'entries', version.id],
    queryFn: () => migrationApi.mapEntries(version.id),
  });
  const removing = useMutation({
    mutationFn: (entryId: number) => migrationApi.deleteEntry(version.id, entryId),
    onSuccess: () => client.invalidateQueries({ queryKey: ['migration'] }),
  });
  return (
    <>
      <div className="mig-actions">
        <h3>Entries of v{version.versionNo}</h3>
        <div className="spacer" />
        {draft && (
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={14} />}
            onClick={() => setEditing({ entry: EMPTY })}
          >
            Add Entry
          </Button>
        )}
        {buttons.map((b) => (
          <Button
            key={b.label}
            variant={b.primary ? 'primary' : 'secondary'}
            size="sm"
            onClick={() => onAction(b.action)}
          >
            {b.label}
          </Button>
        ))}
      </div>
      <ErrorAlert error={entries.error ?? removing.error} />
      <DataTable<MapEntry>
        loading={entries.isLoading}
        rows={entries.data ?? []}
        rowKey={(e) => e.id}
        onRowClick={draft ? (e) => setEditing({ id: e.id, entry: e }) : undefined}
        emptyMessage="No entry in this version"
        columns={entryColumns(draft ? (e) => removing.mutate(e.id) : undefined)}
      />
      {editing !== undefined && (
        <EntryDialog
          versionId={version.id}
          entryId={editing.id}
          initial={editing.entry}
          onClose={() => setEditing(undefined)}
        />
      )}
    </>
  );
}

/** A code map set: its versions and the entries of the chosen version with the version actions. */
export function MapSetPanel({
  setCode,
  canEdit,
  canApprove,
  onClose,
}: Readonly<{ setCode: string; canEdit: boolean; canApprove: boolean; onClose: () => void }>) {
  const { user } = useAuth();
  const [versionId, setVersionId] = useState<number>();
  const [action, setAction] = useState<MigAction>();
  const versions = useQuery({
    queryKey: ['migration', 'maps', setCode, 'versions'],
    queryFn: () => migrationApi.mapVersions(setCode),
  });
  const list = versions.data ?? [];
  const { currentId, version, draft, hasOpen, mayApprove } = versionState(
    list,
    versionId,
    canEdit,
    canApprove && version0(list, versionId)?.submittedBy !== user?.username,
  );
  return (
    <Card
      title={`Code map ${setCode}`}
      actions={
        <SetActions
          setCode={setCode}
          version={version}
          canCreate={canEdit && !hasOpen}
          onAction={setAction}
          onImported={(v) => setVersionId(v.id)}
          onClose={onClose}
        />
      }
    >
      <ErrorAlert error={versions.error} />
      <DataTable<MapVersion>
        loading={versions.isLoading}
        rows={list}
        rowKey={(v) => v.id}
        selectedKey={currentId}
        onRowClick={(v) => setVersionId(v.id)}
        emptyMessage="No version yet"
        columns={VERSION_COLUMNS}
      />
      {version !== undefined && (
        <VersionEntries
          version={version}
          draft={draft}
          buttons={versionActions(setCode, version, draft, mayApprove)}
          onAction={setAction}
        />
      )}
      <ActionConfirm action={action} onClose={() => setAction(undefined)} />
    </Card>
  );
}
