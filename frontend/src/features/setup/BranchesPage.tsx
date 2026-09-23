import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { organizationApi } from '@/api/organization';
import type { Branch } from '@/api/types';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useWorkspace } from '@/context/workspaceContext';
import { formatDate, today } from '@/utils/format';

type BranchForm = Partial<Branch>;

const TEXT_FIELDS: { key: keyof Branch; label: string; required?: boolean }[] = [
  { key: 'code', label: 'Branch code', required: true },
  { key: 'name', label: 'Branch name', required: true },
  { key: 'region', label: 'Region' },
  { key: 'address', label: 'Address' },
  { key: 'managerName', label: 'Branch manager' },
  { key: 'contactPhone', label: 'Phone' },
  { key: 'contactEmail', label: 'Email' },
  { key: 'weeklyHolidays', label: 'Weekly holidays (ISO days, e.g. 6,7)' },
];

/** Office Master maintenance: branches with maker-checker authorization. */
export default function BranchesPage() {
  const { company, branches } = useWorkspace();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<BranchForm | null>(null);

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['branches'] });
  const save = useMutation({
    mutationFn: (b: BranchForm) =>
      b.id === undefined
        ? organizationApi.createBranch({ ...b, companyId: company?.id })
        : organizationApi.updateBranch(b.id, b),
    onSuccess: async (b) => {
      await refresh();
      setForm(null);
      toast.success(`Branch ${b.code} saved – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: (id: number) => organizationApi.authorizeBranch(id),
    onSuccess: async (b) => {
      await refresh();
      toast.success(`Branch ${b.code} authorized`);
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Setup"
        title="Branches"
        description="Branches, offices and customer centres. New and changed branches must be authorized before use."
        actions={
          can('MASTER_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() =>
                setForm({ openingDate: today(), headOffice: false, forexAuthorized: false })
              }
            >
              New branch
            </Button>
          )
        }
      />
      <ErrorAlert error={authorize.error} />
      <Card flush>
        <DataTable<Branch>
          rows={branches}
          rowKey={(b) => b.id}
          onRowClick={can('MASTER_MAINTAIN') ? (b) => setForm(b) : undefined}
          columns={[
            { key: 'c', header: 'Code', render: (b) => <strong>{b.code}</strong> },
            { key: 'n', header: 'Name', render: (b) => b.name },
            { key: 'r', header: 'Region', render: (b) => b.region ?? '' },
            { key: 'o', header: 'Opened', render: (b) => formatDate(b.openingDate) },
            { key: 'h', header: 'Head office', render: (b) => (b.headOffice ? 'Yes' : '') },
            { key: 'f', header: 'Forex', render: (b) => (b.forexAuthorized ? 'Authorized' : '') },
            { key: 's', header: 'Status', render: (b) => <StatusBadge status={b.recordStatus} /> },
            {
              key: 'a',
              header: 'Actions',
              render: (b) =>
                b.recordStatus === 'PENDING_AUTHORIZATION' &&
                can('MASTER_AUTHORIZE') && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={(e) => {
                      e.stopPropagation();
                      authorize.mutate(b.id);
                    }}
                  >
                    Authorize
                  </Button>
                ),
            },
          ]}
        />
      </Card>
      <Modal
        title={form?.id === undefined ? 'New branch' : `Edit branch ${form.code ?? ''}`}
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <Button variant="accent" busy={save.isPending} onClick={() => form && save.mutate(form)}>
            Save for authorization
          </Button>
        }
      >
        <ErrorAlert error={save.error} />
        {form !== null && (
          <div className="stack">
            <div className="form-grid">
              {TEXT_FIELDS.map((f) => (
                <Field key={f.key} label={f.label} required={f.required}>
                  {(id) => (
                    <input
                      id={id}
                      className="input"
                      disabled={f.key === 'code' && form.id !== undefined}
                      value={String(form[f.key] ?? '')}
                      onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                    />
                  )}
                </Field>
              ))}
              <Field label="Date of opening" required>
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    type="date"
                    disabled={form.id !== undefined}
                    value={form.openingDate ?? ''}
                    onChange={(e) => setForm({ ...form, openingDate: e.target.value })}
                  />
                )}
              </Field>
            </div>
            <div className="row">
              <label className="checkbox">
                <input
                  type="checkbox"
                  checked={form.headOffice ?? false}
                  onChange={(e) => setForm({ ...form, headOffice: e.target.checked })}
                />
                Head office
              </label>
              <label className="checkbox">
                <input
                  type="checkbox"
                  checked={form.forexAuthorized ?? false}
                  onChange={(e) => setForm({ ...form, forexAuthorized: e.target.checked })}
                />
                Forex authorized
              </label>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
