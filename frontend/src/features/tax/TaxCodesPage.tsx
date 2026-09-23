import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { taxApi } from '@/api/tax';
import type { PayeeClass, TaxCode, TaxCodeRequest, TaxType } from '@/api/tax';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { AuthorizeButton, SelectField, TextField } from './MasterControls';
import { TaxFormsPanel } from './TaxFormsPanel';

const TAX_TYPES: readonly TaxType[] = [
  'VAT_OUTPUT',
  'VAT_INPUT',
  'VAT_ZERO_RATED',
  'VAT_EXEMPT',
  'PREMIUM_TAX',
  'DST',
  'LGT',
  'FST',
  'EWT',
];
const PAYEE_CLASSES: readonly PayeeClass[] = ['INDIVIDUAL', 'CORPORATE'];
const TABS = [
  { id: 'codes', label: 'Tax codes / ATC' },
  { id: 'forms', label: 'Filing calendar forms' },
] as const;

type Form = Partial<TaxCodeRequest> & { id?: number };

/** Tax codes and ATCs (rates and GL accounts) and the forms of the filing calendar. */
export default function TaxCodesPage() {
  const [tab, setTab] = useState<(typeof TABS)[number]['id']>('codes');
  return (
    <div className="stack">
      <PageHeader
        section="Tax & Statutory"
        title="Tax Codes & Forms"
        description="VAT, premium taxes and withholding ATCs with rates and GL accounts; BIR, LGU and BFP forms with due-date rules. Changes need authorization."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'codes' ? <TaxCodesPanel /> : <TaxFormsPanel />}
    </div>
  );
}

function TaxCodesPanel() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Form | null>(null);
  const codes = useQuery({
    queryKey: ['tax-codes', companyId],
    queryFn: () => taxApi.codes(companyId),
    enabled: companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['tax-codes'] });
  const save = useMutation({
    mutationFn: (f: Form) => {
      const body = { ...f, companyId } as TaxCodeRequest;
      return f.id === undefined ? taxApi.createCode(body) : taxApi.updateCode(f.id, body);
    },
    onSuccess: async (c) => {
      await refresh();
      setForm(null);
      toast.success(`Tax code ${c.code} saved – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: taxApi.authorizeCode,
    onSuccess: async (c) => {
      await refresh();
      toast.success(`Tax code ${c.code} authorized`);
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
            onClick={() => setForm({ taxType: 'EWT', rate: 0, effectiveFrom: today() })}
          >
            New tax code
          </Button>
        </div>
      )}
      <ErrorAlert error={codes.error ?? authorize.error} />
      <Card flush>
        <DataTable<TaxCode>
          rows={codes.data ?? []}
          loading={codes.isLoading}
          rowKey={(c) => c.id}
          caption="Tax codes"
          onRowClick={can('TAX_MANAGE') ? (c) => setForm(c) : undefined}
          columns={[
            { key: 'c', header: 'Code', render: (c) => <strong>{c.code}</strong> },
            { key: 'n', header: 'Name', render: (c) => c.name },
            { key: 't', header: 'Type', render: (c) => c.taxType },
            { key: 'p', header: 'Payee', render: (c) => c.payeeClass ?? '' },
            { key: 'r', header: 'Rate %', numeric: true, render: (c) => c.rate },
            { key: 'g', header: 'GL', render: (c) => c.glAccountCode },
            { key: 's', header: 'Status', render: (c) => <StatusBadge status={c.recordStatus} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (c) =>
                c.recordStatus === 'PENDING_AUTHORIZATION' && can('MASTER_AUTHORIZE') ? (
                  <AuthorizeButton
                    busy={authorize.isPending && authorize.variables === c.id}
                    onClick={() => authorize.mutate(c.id)}
                  />
                ) : null,
            },
          ]}
        />
      </Card>
      <Modal
        title={form?.id === undefined ? 'New tax code' : `Edit ${form.code ?? ''}`}
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
              label="Code"
              required
              disabled={form.id !== undefined}
              value={form.code}
              onChange={(v) => set({ code: v.toUpperCase() })}
            />
            <TextField label="Name" required value={form.name} onChange={(v) => set({ name: v })} />
            <SelectField
              label="Tax type"
              required
              value={form.taxType}
              options={TAX_TYPES}
              onChange={(v) => set({ taxType: v })}
            />
            <TextField
              label="ATC"
              value={form.atc}
              onChange={(v) => set({ atc: v.toUpperCase() })}
            />
            <SelectField
              label="Payee class"
              value={form.payeeClass}
              options={PAYEE_CLASSES}
              allowEmpty
              onChange={(v) => set({ payeeClass: v })}
            />
            <TextField
              label="Rate %"
              type="number"
              required
              value={form.rate}
              onChange={(v) => set({ rate: Number(v) })}
            />
            <TextField
              label="GL account"
              required
              value={form.glAccountCode}
              onChange={(v) => set({ glAccountCode: v })}
            />
            <TextField
              label="Nature of income (2307 / QAP)"
              value={form.incomeNature}
              onChange={(v) => set({ incomeNature: v })}
            />
            <TextField
              label="Effective from"
              type="date"
              required
              value={form.effectiveFrom}
              onChange={(v) => set({ effectiveFrom: v })}
            />
            <TextField
              label="Effective to"
              type="date"
              value={form.effectiveTo}
              onChange={(v) => set({ effectiveTo: v === '' ? undefined : v })}
            />
          </div>
        )}
      </Modal>
    </div>
  );
}
