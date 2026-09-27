import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, User } from 'lucide-react';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import type { SalesLevel, SalesOrganisation } from '@/api/catalog';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { enumOptions } from '@/features/assets/options';
import { RecordActions } from './RecordActions';
import { salesTree, unitsOf } from './salesTree';
import type { SalesNode } from './salesTree';

const REFRESH = [['catalog', 'sales']] as const;
const PARENT_LEVEL: Record<SalesLevel, SalesLevel | undefined> = {
  REGION: undefined,
  DEPARTMENT: 'REGION',
  TEAM: 'DEPARTMENT',
};

function Node({ node }: Readonly<{ node: SalesNode }>) {
  const u = node.unit;
  return (
    <li>
      <div className="row">
        <strong>{u.code}</strong> {u.name}
        <span className="muted">
          {u.level.toLowerCase()} · cost center {node.costCenter ?? '—'}
          {u.costCenter ? '' : ' (inherited)'}
        </span>
        {u.recordStatus !== 'ACTIVE' && <StatusBadge status={u.recordStatus} />}
        <RecordActions kind="SALES_UNIT" record={u} refresh={REFRESH} />
      </div>
      {node.officers.length > 0 && (
        <ul>
          {node.officers.map((o) => (
            <li key={o.id} className="row">
              <User size={12} aria-hidden="true" /> {o.username}
              {o.recordStatus !== 'ACTIVE' && <StatusBadge status={o.recordStatus} />}
              <RecordActions kind="SALES_OFFICER" record={o} refresh={REFRESH} />
            </li>
          ))}
        </ul>
      )}
      {node.children.length > 0 && (
        <ul>
          {node.children.map((c) => (
            <Node key={c.unit.id} node={c} />
          ))}
        </ul>
      )}
    </li>
  );
}

interface AddForm {
  mode: 'unit' | 'officer';
  level: SalesLevel;
  code: string;
  name: string;
  parentCode: string;
  costCenter: string;
  username: string;
}

function AddModal({
  org,
  initial,
  onClose,
}: Readonly<{ org: SalesOrganisation | undefined; initial: AddForm; onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
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
      await queryClient.invalidateQueries({ queryKey: ['catalog', 'sales'] });
      toast.success('Saved – pending authorization');
      onClose();
    },
  });
  const parentLevel = form.mode === 'officer' ? 'TEAM' : PARENT_LEVEL[form.level];
  const parents = parentLevel
    ? unitsOf(org, parentLevel).map((u) => ({ value: u.code, label: `${u.code} – ${u.name}` }))
    : [];
  return (
    <Modal
      title={form.mode === 'officer' ? 'Assign account officer' : 'New sales unit'}
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={save.isPending} onClick={() => save.mutate()}>
          Save for Authorization
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        {form.mode === 'unit' && (
          <>
            <SelectInput
              label="Level"
              value={form.level}
              options={enumOptions(['REGION', 'DEPARTMENT', 'TEAM'])}
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
              label="Cost center"
              upper
              hint="Blank = inherited from the parent"
              value={form.costCenter}
              onChange={(costCenter) => set({ costCenter })}
            />
          </>
        )}
        {parentLevel && (
          <SelectInput
            label={form.mode === 'officer' ? 'Team' : 'Parent'}
            required
            blank="Select"
            value={form.parentCode}
            options={parents}
            onChange={(parentCode) => set({ parentCode })}
          />
        )}
        {form.mode === 'officer' && (
          <TextInput
            label="User name"
            required
            value={form.username}
            onChange={(username) => set({ username })}
          />
        )}
      </div>
    </Modal>
  );
}

const BLANK: AddForm = {
  mode: 'unit',
  level: 'REGION',
  code: '',
  name: '',
  parentCode: '',
  costCenter: '',
  username: '',
};

/**
 * Sales organisation (BRNB.010): regions, departments and teams with their cost centers, and
 * the account officers of each team. Accounts are stamped with the creator's units and cost
 * center when they are created.
 */
export default function SalesOrganisationPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [adding, setAdding] = useState<AddForm | null>(null);
  const org = useQuery({
    queryKey: ['catalog', 'sales', companyId],
    queryFn: () => catalogApi.salesOrganisation(companyId),
  });
  const tree = org.data ? salesTree(org.data) : [];
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Sales Organisation"
        description="Regions, departments and teams, their cost centers and the account officers of each team."
        actions={
          can('MASTER_MAINTAIN') && (
            <>
              <Button
                variant="secondary"
                icon={<User size={16} />}
                onClick={() => setAdding({ ...BLANK, mode: 'officer' })}
              >
                Assign Officer
              </Button>
              <Button variant="accent" icon={<Plus size={16} />} onClick={() => setAdding(BLANK)}>
                New Unit
              </Button>
            </>
          )
        }
      />
      <ErrorAlert error={org.error} />
      <Card>
        {org.isLoading && <span className="spinner" aria-label="Loading" />}
        {!org.isLoading && tree.length === 0 && <p className="muted">No sales unit defined yet.</p>}
        <ul className="stack">
          {tree.map((n) => (
            <Node key={n.unit.id} node={n} />
          ))}
        </ul>
      </Card>
      {adding && <AddModal org={org.data} initial={adding} onClose={() => setAdding(null)} />}
    </div>
  );
}
