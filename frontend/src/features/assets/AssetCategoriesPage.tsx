import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { assetsApi } from '@/api/assets';
import type { AssetCategory, AssetCategoryInput } from '@/api/assets';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import { monthlyDepreciation } from './assetMath';
import { NumberInput, SelectInput, TextInput } from './FormControls';
import { enumOptions } from './options';
import { useAssetLookups } from './useAssetLookups';
import { awaitsOtherChecker } from '@/utils/makerChecker';

type CategoryForm = Partial<AssetCategoryInput> & { id?: number };

const METHODS = enumOptions(['STRAIGHT_LINE', 'DECLINING_BALANCE']);
const SAMPLE_COST = 100_000;

/** Fixed asset categories: GL accounts and default depreciation policy (maker-checker). */
export default function AssetCategoriesPage() {
  const { companyId, categories, postableAccounts } = useAssetLookups();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<CategoryForm | null>(null);
  const accounts = postableAccounts.map((a) => ({ value: a.code, label: `${a.code} – ${a.name}` }));

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['asset-categories'] });
  const save = useMutation({
    mutationFn: (f: CategoryForm) => {
      const body = { ...f, companyId } as AssetCategoryInput;
      return f.id === undefined
        ? assetsApi.createCategory(body)
        : assetsApi.updateCategory(f.id, body);
    },
    onSuccess: async (c) => {
      await refresh();
      setForm(null);
      toast.success(`Category ${c.code} saved – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: (id: number) => assetsApi.authorizeCategory(id),
    onSuccess: async (c) => {
      await refresh();
      toast.success(`Category ${c.code} authorized`);
    },
  });
  const set = (patch: CategoryForm) => form && setForm({ ...form, ...patch });

  return (
    <div className="stack">
      <PageHeader
        section="Assets & Investments"
        title="Asset Categories"
        description="GL accounts and default depreciation policy of each class of property and equipment."
        actions={
          can('MASTER_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() =>
                setForm({
                  depreciationMethod: 'STRAIGHT_LINE',
                  usefulLifeMonths: 60,
                  residualPercent: 0,
                })
              }
            >
              New category
            </Button>
          )
        }
      />
      <ErrorAlert error={authorize.error} />
      <Card flush>
        <DataTable<AssetCategory>
          rows={categories}
          rowKey={(c) => c.id}
          onRowClick={can('MASTER_MAINTAIN') ? (c) => setForm(c) : undefined}
          columns={[
            { key: 'c', header: 'Code', render: (c) => <strong>{c.code}</strong> },
            { key: 'n', header: 'Name', render: (c) => c.name },
            { key: 'a', header: 'Asset', render: (c) => c.assetAccount },
            { key: 'ad', header: 'Accum. depr.', render: (c) => c.accumulatedDepreciationAccount },
            { key: 'e', header: 'Expense', render: (c) => c.depreciationExpenseAccount },
            { key: 'm', header: 'Method', render: (c) => humanize(c.depreciationMethod) },
            { key: 'l', header: 'Life (months)', numeric: true, render: (c) => c.usefulLifeMonths },
            { key: 'r', header: 'Residual %', numeric: true, render: (c) => c.residualPercent },
            { key: 's', header: 'Status', render: (c) => <StatusBadge status={c.recordStatus} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (c) =>
                awaitsOtherChecker(c, user?.username) &&
                can('MASTER_AUTHORIZE') && (
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
        title={form?.id === undefined ? 'New asset category' : `Edit ${form.code ?? ''}`}
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
            <TextInput
              label="Code"
              required
              upper
              disabled={form.id !== undefined}
              value={form.code}
              onChange={(code) => set({ code })}
            />
            <TextInput label="Name" required value={form.name} onChange={(name) => set({ name })} />
            <SelectInput
              label="Asset (cost) account"
              required
              blank="Select"
              value={form.assetAccount}
              options={accounts}
              onChange={(assetAccount) => set({ assetAccount })}
            />
            <SelectInput
              label="Accumulated depreciation account"
              required
              blank="Select"
              value={form.accumulatedDepreciationAccount}
              options={accounts}
              onChange={(v) => set({ accumulatedDepreciationAccount: v })}
            />
            <SelectInput
              label="Depreciation expense account"
              required
              blank="Select"
              value={form.depreciationExpenseAccount}
              options={accounts}
              onChange={(v) => set({ depreciationExpenseAccount: v })}
            />
            <SelectInput
              label="Method"
              required
              value={form.depreciationMethod}
              options={METHODS}
              onChange={(v) =>
                set({ depreciationMethod: v as AssetCategory['depreciationMethod'] })
              }
            />
            <NumberInput
              label="Useful life (months)"
              required
              step="1"
              value={form.usefulLifeMonths}
              onChange={(usefulLifeMonths) => set({ usefulLifeMonths })}
              hint={`First month on ${SAMPLE_COST.toLocaleString()}: ${monthlyDepreciation(
                SAMPLE_COST,
                form.residualPercent ?? 0,
                form.depreciationMethod ?? 'STRAIGHT_LINE',
                form.usefulLifeMonths ?? 0,
              ).toFixed(2)}`}
            />
            <NumberInput
              label="Residual value %"
              required
              value={form.residualPercent}
              onChange={(residualPercent) => set({ residualPercent })}
            />
          </div>
        )}
      </Modal>
    </div>
  );
}
