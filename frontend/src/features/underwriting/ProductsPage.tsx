import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { underwritingApi } from '@/api/underwriting';
import type { Product, ProductInput } from '@/api/underwriting';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { CheckboxField, NumberField, SelectField, TextField } from './FormFields';
import { useUwLookups } from './useUwLookups';
import { awaitsOtherChecker } from '@/utils/makerChecker';

type ProductForm = ProductInput & { id?: number };

const UPR = [
  { value: 'DAYS_365', label: '1/365 daily pro-rata' },
  { value: 'TWENTY_FOURTHS', label: '1/24 monthly' },
  { value: 'EIGHTHS', label: '1/8 quarterly' },
];

const RATE_FIELDS: { key: keyof ProductInput; label: string }[] = [
  { key: 'defaultCommissionRate', label: 'Default commission %' },
  { key: 'dstRate', label: 'DST %' },
  { key: 'vatRate', label: 'VAT %' },
  { key: 'lgtRate', label: 'LGT %' },
  { key: 'fstRate', label: 'FST % (fire)' },
  { key: 'premiumTaxRate', label: 'Premium tax %' },
  { key: 'policyFee', label: 'Policy fee' },
];

function blank(companyId: number): ProductForm {
  return {
    companyId,
    code: '',
    name: '',
    businessLine: '',
    defaultCommissionRate: 15,
    uprBasis: 'DAYS_365',
    dstRate: 12.5,
    vatRate: 12,
    lgtRate: 0.75,
    fstRate: 0,
    premiumTaxRate: 0,
    policyFee: 250,
    openCoverAllowed: false,
  };
}

/** Product master: lines of business, commission, earning basis and Philippine premium taxes. */
export default function ProductsPage() {
  const lookups = useUwLookups();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ProductForm | null>(null);
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['uw-products'] });

  const save = useMutation({
    mutationFn: ({ id, ...body }: ProductForm) =>
      id === undefined
        ? underwritingApi.createProduct(body)
        : underwritingApi.updateProduct(id, body),
    onSuccess: async (p) => {
      await refresh();
      setForm(null);
      toast.success(`Product ${p.code} saved – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: (id: number) => underwritingApi.authorizeProduct(id),
    onSuccess: async (p) => {
      await refresh();
      toast.success(`Product ${p.code} authorized`);
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Underwriting"
        title="Products"
        description="Classes of business with commission, UPR basis and tax rates (DST, VAT, LGT, FST, premium tax). Changes need authorization."
        actions={
          can('POLICY_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => setForm(blank(lookups.companyId))}
            >
              New product
            </Button>
          )
        }
      />
      <ErrorAlert error={authorize.error} />
      <Card flush>
        <DataTable<Product>
          rows={lookups.products}
          rowKey={(p) => p.id}
          onRowClick={can('POLICY_MAINTAIN') ? (p) => setForm(p) : undefined}
          caption="Products"
          columns={[
            { key: 'c', header: 'Code', render: (p) => <strong>{p.code}</strong> },
            { key: 'n', header: 'Name', render: (p) => p.name },
            { key: 'l', header: 'Class', render: (p) => p.businessLine },
            { key: 'cm', header: 'Comm. %', numeric: true, render: (p) => p.defaultCommissionRate },
            {
              key: 't',
              header: 'DST / VAT / LGT / FST %',
              render: (p) =>
                [p.dstRate, p.vatRate, p.lgtRate, p.fstRate].map((r) => String(r)).join(' / '),
            },
            {
              key: 'f',
              header: 'Policy fee',
              numeric: true,
              render: (p) => <Amount value={p.policyFee} />,
            },
            { key: 'o', header: 'Open cover', render: (p) => (p.openCoverAllowed ? 'Yes' : '') },
            { key: 's', header: 'Status', render: (p) => <StatusBadge status={p.recordStatus} /> },
            {
              key: 'a',
              header: 'Actions',
              render: (p) =>
                awaitsOtherChecker(p, user?.username) &&
                can('POLICY_AUTHORIZE') && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={(e) => {
                      e.stopPropagation();
                      authorize.mutate(p.id);
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
        title={form?.id === undefined ? 'New product' : `Edit product ${form.code}`}
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
              onChange={(v) => setForm({ ...form, code: v.toUpperCase() })}
            />
            <TextField
              label="Name"
              required
              value={form.name}
              onChange={(v) => setForm({ ...form, name: v })}
            />
            <SelectField
              label="Class (line of business)"
              required
              value={form.businessLine}
              emptyLabel="Select class"
              options={lookups.businessLines.map((d) => ({ value: d.code, label: d.name }))}
              onChange={(v) => setForm({ ...form, businessLine: v })}
            />
            <SelectField
              label="UPR basis"
              required
              value={form.uprBasis}
              options={UPR}
              onChange={(v) => setForm({ ...form, uprBasis: v as ProductInput['uprBasis'] })}
            />
            {RATE_FIELDS.map((f) => (
              <NumberField
                key={f.key}
                label={f.label}
                required
                value={form[f.key] as number}
                onChange={(v) => setForm({ ...form, [f.key]: v ?? 0 })}
              />
            ))}
            <CheckboxField
              label="Marine open covers allowed"
              checked={form.openCoverAllowed}
              onChange={(v) => setForm({ ...form, openCoverAllowed: v })}
            />
          </div>
        )}
      </Modal>
    </div>
  );
}
