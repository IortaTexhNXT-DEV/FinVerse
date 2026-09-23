import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { taxApi } from '@/api/tax';
import type {
  FilingFrequency,
  TaxAuthority,
  TaxForm,
  TaxFormRequest,
  WorksheetKind,
} from '@/api/tax';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { humanize, today } from '@/utils/format';
import { AuthorizeButton, SelectField, TextField } from './MasterControls';

const AUTHORITIES: readonly TaxAuthority[] = ['BIR', 'LGU', 'BFP'];
const FREQUENCIES: readonly FilingFrequency[] = [
  'MONTHLY',
  'QUARTERLY',
  'MONTHLY_EXCEPT_QUARTER_END',
  'ANNUAL',
];
const WORKSHEETS: readonly WorksheetKind[] = [
  'VAT',
  'EWT',
  'DST',
  'PREMIUM_TAX',
  'LGT',
  'FST',
  'NONE',
];

type Form = Partial<TaxFormRequest> & { id?: number };

/** Forms of the filing calendar: authority, frequency, due rule and accounts cleared. */
export function TaxFormsPanel() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Form | null>(null);
  const forms = useQuery({
    queryKey: ['tax-forms', companyId],
    queryFn: () => taxApi.forms(companyId),
    enabled: companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['tax-forms'] });
  const save = useMutation({
    mutationFn: (f: Form) => {
      const body = { ...f, companyId } as TaxFormRequest;
      return f.id === undefined ? taxApi.createForm(body) : taxApi.updateForm(f.id, body);
    },
    onSuccess: async (f) => {
      await refresh();
      setForm(null);
      toast.success(`Form ${f.code} saved – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: taxApi.authorizeForm,
    onSuccess: async (f) => {
      await refresh();
      toast.success(`Form ${f.code} authorized`);
    },
  });
  const set = (patch: Partial<Form>) => setForm((f) => ({ ...f, ...patch }));

  return (
    <div className="stack">
      {can('TAX_MANAGE') && (
        <div className="row">
          <Button
            variant="accent"
            icon={<Plus size={16} />}
            onClick={() =>
              setForm({
                authority: 'BIR',
                frequency: 'MONTHLY',
                worksheet: 'NONE',
                dueMonthsAfter: 1,
                dueDay: 10,
                trackFiling: false,
                effectiveFrom: today(),
              })
            }
          >
            New form
          </Button>
        </div>
      )}
      <ErrorAlert error={forms.error ?? authorize.error} />
      <Card flush>
        <DataTable<TaxForm>
          rows={forms.data ?? []}
          loading={forms.isLoading}
          rowKey={(f) => f.id}
          caption="Tax forms"
          onRowClick={can('TAX_MANAGE') ? (f) => setForm(f) : undefined}
          columns={[
            { key: 'c', header: 'Form', render: (f) => <strong>{f.code}</strong> },
            { key: 'n', header: 'Name', render: (f) => f.name },
            { key: 'a', header: 'Authority', render: (f) => f.authority },
            { key: 'q', header: 'Frequency', render: (f) => humanize(f.frequency) },
            { key: 'w', header: 'Worksheet', render: (f) => f.worksheet },
            {
              key: 'd',
              header: 'Due',
              render: (f) => `Day ${f.dueDay}, ${f.dueMonthsAfter} month(s) after period`,
            },
            { key: 'p', header: 'Payable', render: (f) => f.payableAccountCode ?? '' },
            { key: 't', header: 'Tracked', render: (f) => (f.trackFiling ? 'Yes' : 'Reminder') },
            { key: 's', header: 'Status', render: (f) => <StatusBadge status={f.recordStatus} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (f) =>
                f.recordStatus === 'PENDING_AUTHORIZATION' && can('MASTER_AUTHORIZE') ? (
                  <AuthorizeButton
                    busy={authorize.isPending && authorize.variables === f.id}
                    onClick={() => authorize.mutate(f.id)}
                  />
                ) : null,
            },
          ]}
        />
      </Card>
      <Modal
        title={form?.id === undefined ? 'New form' : `Edit ${form.code ?? ''}`}
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
          <div className="form-grid">
            <TextField
              label="Form code"
              required
              disabled={form.id !== undefined}
              value={form.code}
              onChange={(v) => set({ code: v.toUpperCase() })}
            />
            <TextField label="Name" required value={form.name} onChange={(v) => set({ name: v })} />
            <SelectField
              label="Authority"
              required
              value={form.authority}
              options={AUTHORITIES}
              onChange={(v) => set({ authority: v })}
            />
            <SelectField
              label="Frequency"
              required
              value={form.frequency}
              options={FREQUENCIES}
              onChange={(v) => set({ frequency: v })}
            />
            <SelectField
              label="Worksheet"
              required
              value={form.worksheet}
              options={WORKSHEETS}
              onChange={(v) => set({ worksheet: v })}
            />
            <TextField
              label="Months after period end"
              type="number"
              value={form.dueMonthsAfter}
              onChange={(v) => set({ dueMonthsAfter: Number(v) })}
            />
            <TextField
              label="Due day (31 = month end)"
              type="number"
              value={form.dueDay}
              onChange={(v) => set({ dueDay: Number(v) })}
            />
            <TextField
              label="Tax payable account"
              value={form.payableAccountCode}
              onChange={(v) => set({ payableAccountCode: v })}
            />
            <TextField
              label="Credit account (input VAT)"
              value={form.creditAccountCode}
              onChange={(v) => set({ creditAccountCode: v })}
            />
            <TextField
              label="Tracked from"
              type="date"
              required
              value={form.effectiveFrom}
              onChange={(v) => set({ effectiveFrom: v })}
            />
            <label className="row">
              <input
                type="checkbox"
                checked={form.trackFiling === true}
                onChange={(e) => set({ trackFiling: e.target.checked })}
              />
              Returns and alerts managed in FinVerse
            </label>
          </div>
        )}
      </Modal>
    </div>
  );
}
