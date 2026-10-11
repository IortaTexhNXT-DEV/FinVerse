import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { glPlatformApi } from './glPlatformApi';
import type { CoaNumbering } from './glPlatformApi';
import { numberingExample, numberingProblems } from './chartRules';
import type { NumberingForm } from './chartRules';

const EMPTY: NumberingForm = { parentCode: '', separator: '.', width: 2, active: true };

/**
 * Numbering schemes of parent accounts (FRBS 2.3.2): a child created with a blank code gets the
 * parent code, the separator and the next sequence of the chosen width.
 */
export function NumberingPanel() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<NumberingForm | null>(null);
  const [checked, setChecked] = useState(false);
  const query = useQuery({
    queryKey: ['coa-numbering', companyId],
    queryFn: () => glPlatformApi.numbering(companyId),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: (f: NumberingForm) => glPlatformApi.saveNumbering({ companyId, ...f }),
    onSuccess: async (n) => {
      setForm(null);
      await queryClient.invalidateQueries({ queryKey: ['coa-numbering'] });
      toast.success(`Numbering under ${n.parentCode} saved: first child ${n.example}`);
    },
  });
  const problems = form === null ? {} : numberingProblems(form);
  const submit = () => {
    setChecked(true);
    if (form !== null && Object.keys(problems).length === 0) {
      save.mutate(form);
    }
  };
  const edit = (n?: CoaNumbering) => {
    setChecked(false);
    save.reset();
    setForm(n === undefined ? EMPTY : { ...n });
  };

  return (
    <Card
      title="Numbering schemes"
      flush
      actions={
        can('MASTER_MAINTAIN') && (
          <Button variant="accent" size="sm" icon={<Plus size={14} />} onClick={() => edit()}>
            Add Scheme
          </Button>
        )
      }
    >
      <ErrorAlert error={query.error} />
      <DataTable<CoaNumbering>
        loading={query.isLoading}
        rows={query.data ?? []}
        rowKey={(n) => n.id}
        caption="Numbering schemes"
        emptyMessage="No numbering scheme: account codes are entered by hand"
        onRowClick={can('MASTER_MAINTAIN') ? edit : undefined}
        columns={[
          {
            key: 'parent',
            header: 'Parent Account',
            render: (n) => <strong>{n.parentCode}</strong>,
          },
          { key: 'sep', header: 'Separator', render: (n) => n.separator || '(none)' },
          { key: 'width', header: 'Digits', numeric: true, render: (n) => n.width },
          { key: 'ex', header: 'First Child', render: (n) => n.example },
          {
            key: 'st',
            header: 'Status',
            render: (n) => <StatusBadge status={n.active ? 'ACTIVE' : 'INACTIVE'} />,
          },
        ]}
      />
      <Modal
        title="Numbering scheme"
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setForm(null)}>
              Cancel
            </Button>
            <Button variant="accent" busy={save.isPending} onClick={submit}>
              Save Scheme
            </Button>
          </>
        }
      >
        {form !== null && (
          <div className="stack">
            <ErrorAlert error={save.error} />
            <Field
              label="Parent account"
              required
              error={checked ? problems.parentCode : undefined}
            >
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={form.parentCode}
                  onChange={(e) => setForm({ ...form, parentCode: e.target.value.toUpperCase() })}
                />
              )}
            </Field>
            <div className="form-grid">
              <Field label="Separator">
                {(id) => (
                  <select
                    id={id}
                    className="select"
                    value={form.separator}
                    onChange={(e) => setForm({ ...form, separator: e.target.value })}
                  >
                    <option value="">None</option>
                    <option value=".">Dot (.)</option>
                    <option value="-">Dash (-)</option>
                  </select>
                )}
              </Field>
              <Field label="Digits" required error={checked ? problems.width : undefined}>
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    type="number"
                    min={1}
                    max={6}
                    value={form.width}
                    onChange={(e) => setForm({ ...form, width: Number(e.target.value) })}
                  />
                )}
              </Field>
              <Field label="Proposes codes">
                {(id) => (
                  <select
                    id={id}
                    className="select"
                    value={form.active ? 'Y' : 'N'}
                    onChange={(e) => setForm({ ...form, active: e.target.value === 'Y' })}
                  >
                    <option value="Y">Yes</option>
                    <option value="N">No</option>
                  </select>
                )}
              </Field>
            </div>
            <p className="muted">First child: {numberingExample(form)}</p>
          </div>
        )}
      </Modal>
    </Card>
  );
}
