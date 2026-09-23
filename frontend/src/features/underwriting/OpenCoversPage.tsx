import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { underwritingApi } from '@/api/underwriting';
import type { OpenCover, OpenCoverInput } from '@/api/underwriting';
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
import { useWorkspace } from '@/context/workspaceContext';
import { formatDate, today } from '@/utils/format';
import { DateField, NumberField, SelectField, TextField } from './FormFields';
import { oneYearFrom } from './premiumMath';
import { useUwLookups } from './useUwLookups';

function blank(companyId: number, branchId: number): OpenCoverInput {
  return {
    companyId,
    branchId,
    productId: 0,
    customerCode: '',
    insuredName: '',
    periodFrom: today(),
    periodTo: oneYearFrom(today()),
    currency: 'PHP',
    limitPerShipment: 0,
    annualLimit: 0,
    rate: 0.25,
  };
}

/** Marine open covers (master cargo policies) under which shipments are declared. */
export default function OpenCoversPage() {
  const lookups = useUwLookups();
  const { branchId } = useWorkspace();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<OpenCoverInput | null>(null);
  const companyId = lookups.companyId;
  const query = useQuery({
    queryKey: ['open-covers', companyId],
    queryFn: () => underwritingApi.openCovers(companyId),
    enabled: companyId > 0,
  });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['open-covers'] });
  const create = useMutation({
    mutationFn: (body: OpenCoverInput) => underwritingApi.createOpenCover(body),
    onSuccess: async (c) => {
      await refresh();
      setForm(null);
      toast.success(`Open cover ${c.openCoverNo} created – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: (id: number) => underwritingApi.authorizeOpenCover(id),
    onSuccess: async (c) => {
      await refresh();
      toast.success(`Open cover ${c.openCoverNo} authorized`);
    },
  });
  const marineProducts = lookups.activeProducts.filter((p) => p.openCoverAllowed);
  const set = (patch: Partial<OpenCoverInput>) => form && setForm({ ...form, ...patch });

  return (
    <div className="stack">
      <PageHeader
        section="Underwriting"
        title="Open covers"
        description="Marine cargo open covers with limits per shipment and per year; open one to declare shipments (certificates)."
        actions={
          can('POLICY_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => setForm(blank(companyId, branchId ?? lookups.branches[0]?.id ?? 0))}
            >
              New open cover
            </Button>
          )
        }
      />
      <ErrorAlert error={query.error ?? authorize.error} />
      <Card flush>
        <DataTable<OpenCover>
          loading={query.isLoading}
          rows={query.data ?? []}
          rowKey={(c) => c.id}
          onRowClick={(c) => void navigate(`/underwriting/open-covers/${String(c.id)}`)}
          caption="Open covers"
          columns={[
            { key: 'no', header: 'Open cover', render: (c) => <strong>{c.openCoverNo}</strong> },
            { key: 'ins', header: 'Insured', render: (c) => c.insuredName },
            {
              key: 'per',
              header: 'Period',
              render: (c) => `${formatDate(c.periodFrom)} – ${formatDate(c.periodTo)}`,
            },
            { key: 'ccy', header: 'Ccy', render: (c) => c.currency },
            {
              key: 'lim',
              header: 'Per shipment',
              numeric: true,
              render: (c) => <Amount value={c.limitPerShipment} />,
            },
            {
              key: 'ann',
              header: 'Annual limit',
              numeric: true,
              render: (c) => <Amount value={c.annualLimit} />,
            },
            { key: 'rate', header: 'Rate %', numeric: true, render: (c) => c.rate },
            { key: 'st', header: 'Status', render: (c) => <StatusBadge status={c.recordStatus} /> },
            {
              key: 'a',
              header: 'Actions',
              render: (c) =>
                c.recordStatus === 'PENDING_AUTHORIZATION' &&
                can('POLICY_AUTHORIZE') && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={(e) => {
                      e.stopPropagation();
                      authorize.mutate(c.id);
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
        title="New open cover"
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <Button
            variant="accent"
            busy={create.isPending}
            onClick={() => form && create.mutate(form)}
          >
            Save for authorization
          </Button>
        }
      >
        <ErrorAlert error={create.error} />
        {form !== null && (
          <div className="form-grid">
            <SelectField
              label="Marine product"
              required
              value={form.productId > 0 ? String(form.productId) : ''}
              emptyLabel="Select product"
              options={marineProducts.map((p) => ({ value: String(p.id), label: p.name }))}
              onChange={(v) => set({ productId: Number(v) })}
            />
            <SelectField
              label="Client"
              required
              value={form.customerCode}
              emptyLabel="Select client"
              options={lookups.clients.map((c) => ({ value: c.code, label: c.name }))}
              onChange={(v) =>
                set({
                  customerCode: v,
                  insuredName: lookups.clients.find((c) => c.code === v)?.name ?? '',
                })
              }
            />
            <TextField
              label="Insured"
              required
              value={form.insuredName}
              onChange={(v) => set({ insuredName: v })}
            />
            <DateField
              label="Period from"
              required
              value={form.periodFrom}
              onChange={(v) => set({ periodFrom: v })}
            />
            <DateField
              label="Period to"
              required
              value={form.periodTo}
              onChange={(v) => set({ periodTo: v })}
            />
            <SelectField
              label="Currency"
              required
              value={form.currency}
              options={lookups.currencies.map((c) => ({ value: c.code, label: c.code }))}
              onChange={(v) => set({ currency: v })}
            />
            <NumberField
              label="Limit per shipment"
              required
              value={form.limitPerShipment}
              onChange={(v) => set({ limitPerShipment: v ?? 0 })}
            />
            <NumberField
              label="Annual limit"
              required
              value={form.annualLimit}
              onChange={(v) => set({ annualLimit: v ?? 0 })}
            />
            <NumberField
              label="Rate %"
              required
              value={form.rate}
              onChange={(v) => set({ rate: v ?? 0 })}
            />
            <TextField
              label="Goods covered"
              value={form.cargoDescription}
              onChange={(v) => set({ cargoDescription: v })}
            />
          </div>
        )}
      </Modal>
    </div>
  );
}
