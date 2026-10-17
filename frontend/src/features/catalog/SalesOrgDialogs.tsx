import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Pencil } from 'lucide-react';
import { useState, useSyncExternalStore } from 'react';
import { catalogApi } from '@/api/catalog';
import type { SalesLevel, SalesOrganisation } from '@/api/catalog';
import { userDirectory } from '@/api/users';
import { Button } from '@/components/ui/Button';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import type { Definition } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useDisplayName } from '@/components/ui/useDisplayName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { countOf, formatDateTime } from '@/utils/format';
import { LEVEL_LABEL, unitsOf } from './salesTree';
import type { AddForm, SalesNode } from './salesTree';

const SALES = ['catalog', 'sales'] as const;

const PARENT_LEVEL: Record<SalesLevel, SalesLevel | undefined> = {
  REGION: undefined,
  DEPARTMENT: 'REGION',
  TEAM: 'DEPARTMENT',
};

const LEVEL_OPTIONS = (['REGION', 'DEPARTMENT', 'TEAM'] as const).map((l) => ({
  value: l,
  label: LEVEL_LABEL[l],
}));

/** Parent units offered for a level, active or pending only, as "code – name". */
function parentOptions(org: SalesOrganisation | undefined, level: SalesLevel | undefined) {
  return level
    ? unitsOf(org, level)
        .filter((u) => u.recordStatus !== 'INACTIVE')
        .map((u) => ({ value: u.code, label: `${u.code} – ${u.name}` }))
    : [];
}

/** Users of the directory by name and role (never the login id). */
function useUserOptions() {
  const snapshot = useSyncExternalStore(userDirectory.subscribe, userDirectory.snapshot);
  return Array.from(snapshot.values())
    .map((e) => ({
      value: e.username,
      label: e.roleName ? `${e.displayName} (${e.roleName})` : e.displayName,
    }))
    .sort((a, b) => a.label.localeCompare(b.label));
}

/** New unit, new sub-unit of a unit, or an account officer placed in or moved to a team. */
export function AddModal({
  org,
  initial,
  onClose,
}: Readonly<{ org: SalesOrganisation | undefined; initial: AddForm; onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const users = useUserOptions();
  const nameOf = useDisplayName();
  const [form, setForm] = useState(initial);
  const set = (patch: Partial<AddForm>) => setForm((f) => ({ ...f, ...patch }));
  const save = useMutation({
    mutationFn: async () => {
      if (form.mode === 'officer') {
        await catalogApi.assignOfficer({
          companyId,
          teamCode: form.parentCode,
          username: form.username,
        });
        return;
      }
      await catalogApi.createSalesUnit({
        companyId,
        level: form.level,
        code: form.code,
        name: form.name,
        parentCode: form.parentCode || undefined,
        costCenter: form.costCenter || undefined,
      });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: SALES });
      toast.success('Saved – pending authorization');
      onClose();
    },
  });
  const parentLevel = form.mode === 'officer' ? 'TEAM' : PARENT_LEVEL[form.level];
  let title = `New ${LEVEL_LABEL[form.level]}`;
  if (form.mode === 'officer') {
    title = form.reassign ? `Reassign ${nameOf(form.username)}` : 'Assign Account Officer';
  }
  return (
    <Modal
      title={title}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={save.isPending}>
            Cancel
          </Button>
          <Button variant="accent" busy={save.isPending} onClick={() => save.mutate()}>
            Save for Authorization
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        {form.mode === 'unit' && (
          <>
            <SelectInput
              label="Level"
              value={form.level}
              options={LEVEL_OPTIONS}
              onChange={(v) => set({ level: v as SalesLevel, parentCode: '' })}
            />
            <TextInput
              label="Code"
              required
              upper
              value={form.code}
              onChange={(code) => set({ code })}
            />
            <TextInput label="Name" required value={form.name} onChange={(name) => set({ name })} />
            <TextInput
              label="Cost Center"
              upper
              hint="Blank = inherited from the parent"
              value={form.costCenter}
              onChange={(costCenter) => set({ costCenter })}
            />
          </>
        )}
        {parentLevel && (
          <SelectInput
            label={form.mode === 'officer' ? 'Team' : `Parent ${LEVEL_LABEL[parentLevel]}`}
            required
            blank="Select"
            value={form.parentCode}
            options={parentOptions(org, parentLevel)}
            onChange={(parentCode) => set({ parentCode })}
          />
        )}
        {form.mode === 'officer' && (
          <SelectInput
            label="Account Officer"
            required
            blank="Select"
            disabled={form.reassign}
            value={form.username}
            options={users}
            onChange={(username) => set({ username })}
          />
        )}
      </div>
    </Modal>
  );
}

/** Who created, last changed and authorized a record, as "name on date time". */
function byOn(login: string | undefined, at: string | undefined) {
  return (
    login && (
      <>
        <UserName login={login} /> on {formatDateTime(at)}
      </>
    )
  );
}

/** The facts of a unit: place in the tree, cost center, head, counts, status and its audit. */
function unitDefinitions(node: SalesNode, org: SalesOrganisation | undefined): Definition[] {
  const u = node.unit;
  const parent = org?.units.find((p) => p.code === u.parentCode);
  let costCenter = u.costCenter ?? '';
  if (!u.costCenter && node.costCenter) {
    costCenter = `${node.costCenter} (inherited from ${node.costCenterFrom ?? ''})`;
  }
  const activeOfficers = node.officers.filter((o) => o.recordStatus !== 'INACTIVE').length;
  const activeSubUnits = node.children.filter((c) => c.unit.recordStatus !== 'INACTIVE').length;
  const team = u.level === 'TEAM';
  return [
    { label: 'Code', value: u.code },
    { label: 'Name', value: u.name },
    { label: 'Level', value: LEVEL_LABEL[u.level] },
    { label: 'Parent Unit', value: parent ? `${parent.code} – ${parent.name}` : '' },
    { label: 'Cost Center', value: costCenter },
    { label: 'Unit Head', value: u.headUsername && <UserName login={u.headUsername} /> },
    { label: 'Sub-units', value: team ? '' : countOf(activeSubUnits, 'active sub-unit') },
    { label: 'Account Officers', value: team ? countOf(activeOfficers, 'active officer') : '' },
    { label: 'Status', value: <StatusBadge status={u.recordStatus} /> },
    { label: 'Status Reason', value: u.statusReason },
    { label: 'Created', value: byOn(u.createdBy, u.createdAt) },
    { label: 'Last Changed', value: byOn(u.maker, u.lastChangedAt) },
    { label: 'Authorized', value: byOn(u.authorizedBy, u.authorizedAt) },
  ];
}

/** Unit details with the audit of the record, and the edit form (name, parent, cost center). */
export function UnitDetailModal({
  node,
  org,
  canEdit,
  onClose,
}: Readonly<{
  node: SalesNode;
  org: SalesOrganisation | undefined;
  canEdit: boolean;
  onClose: () => void;
}>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const u = node.unit;
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(u.name);
  const [parentCode, setParentCode] = useState(u.parentCode ?? '');
  const [costCenter, setCostCenter] = useState(u.costCenter ?? '');
  const save = useMutation({
    mutationFn: () =>
      catalogApi.updateSalesUnit(u.id, {
        companyId,
        level: u.level,
        code: u.code,
        name,
        parentCode: parentCode || undefined,
        costCenter: costCenter || undefined,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: SALES });
      toast.success(`${u.code} saved – pending authorization`);
      onClose();
    },
  });
  const parentLevel = PARENT_LEVEL[u.level];
  const footer = editing ? (
    <>
      <Button variant="secondary" onClick={() => setEditing(false)} disabled={save.isPending}>
        Cancel
      </Button>
      <Button variant="accent" busy={save.isPending} onClick={() => save.mutate()}>
        Save for Authorization
      </Button>
    </>
  ) : (
    <>
      <Button variant="secondary" onClick={onClose}>
        Close
      </Button>
      {canEdit && u.recordStatus !== 'INACTIVE' && (
        <Button variant="accent" icon={<Pencil size={16} />} onClick={() => setEditing(true)}>
          Edit
        </Button>
      )}
    </>
  );
  return (
    <Modal title={`${LEVEL_LABEL[u.level]} ${u.code}`} open onClose={onClose} footer={footer}>
      <ErrorAlert error={save.error} />
      {editing ? (
        <div className="form-grid">
          <TextInput label="Name" required value={name} onChange={setName} />
          {parentLevel && (
            <SelectInput
              label={`Parent ${LEVEL_LABEL[parentLevel]}`}
              required
              blank="Select"
              value={parentCode}
              options={parentOptions(org, parentLevel)}
              onChange={setParentCode}
            />
          )}
          <TextInput
            label="Cost Center"
            upper
            hint="Blank = inherited from the parent"
            value={costCenter}
            onChange={setCostCenter}
          />
        </div>
      ) : (
        <DefinitionGrid label="Unit details" items={unitDefinitions(node, org)} />
      )}
    </Modal>
  );
}
