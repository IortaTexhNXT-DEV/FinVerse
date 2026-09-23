import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { assetsApi } from '@/api/assets';
import type { FixedAssetInput } from '@/api/assets';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { monthlyDepreciation, validateAsset } from './assetMath';
import { NumberInput, SelectInput, TextInput } from './FormControls';
import { codeOptions } from './options';
import { useAssetLookups } from './useAssetLookups';

export type AssetForm = Partial<FixedAssetInput> & { id?: number };

interface Props {
  initial: AssetForm;
  onClose: () => void;
}

/** Register (or edit, while pending) a fixed asset; saving sends it for capitalization. */
export function AssetFormModal({ initial, onClose }: Readonly<Props>) {
  const lookups = useAssetLookups();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<AssetForm>(initial);
  const [touched, setTouched] = useState(false);
  const set = (patch: AssetForm) => setForm({ ...form, ...patch });
  const category = lookups.categories.find((c) => c.id === form.categoryId);
  const errors = touched ? validateAsset(form) : {};
  const save = useMutation({
    mutationFn: (f: AssetForm) => {
      const body = { ...f, companyId: lookups.companyId } as FixedAssetInput;
      return f.id === undefined ? assetsApi.create(body) : assetsApi.update(f.id, body);
    },
    onSuccess: async (a) => {
      await queryClient.invalidateQueries({ queryKey: ['fixed-assets'] });
      toast.success(`Asset ${a.tagNo} saved – pending capitalization`);
      onClose();
    },
  });
  const submit = () => {
    setTouched(true);
    if (Object.keys(validateAsset(form)).length === 0) {
      save.mutate(form);
    }
  };
  const editing = form.id !== undefined;
  const monthly =
    category === undefined
      ? undefined
      : monthlyDepreciation(
          form.acquisitionCost ?? 0,
          category.residualPercent,
          category.depreciationMethod,
          form.usefulLifeMonths ?? category.usefulLifeMonths,
        );

  return (
    <Modal
      title={editing ? `Edit ${form.tagNo ?? ''}` : 'Register fixed asset'}
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={save.isPending} onClick={submit}>
          Save for capitalization
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <TextInput
          label="Tag number"
          required
          upper
          disabled={editing}
          error={errors.tagNo}
          value={form.tagNo}
          onChange={(tagNo) => set({ tagNo })}
        />
        <TextInput
          label="Description"
          required
          error={errors.description}
          value={form.description}
          onChange={(description) => set({ description })}
        />
        <SelectInput
          label="Category"
          required
          blank="Select"
          disabled={editing}
          error={errors.categoryId}
          value={form.categoryId}
          options={lookups.activeCategories.map((c) => ({
            value: String(c.id),
            label: `${c.code} – ${c.name}`,
          }))}
          onChange={(v) => set({ categoryId: Number(v) })}
        />
        <SelectInput
          label="Branch"
          required
          disabled={editing}
          value={form.branchId}
          options={lookups.branches.map((b) => ({ value: String(b.id), label: b.name }))}
          onChange={(v) => set({ branchId: Number(v) })}
        />
        <SelectInput
          label="Cost centre"
          required
          blank="Select"
          error={errors.costCenter}
          value={form.costCenter}
          options={codeOptions(lookups.costCenters)}
          onChange={(costCenter) => set({ costCenter })}
        />
        <SelectInput
          label="Supplier"
          blank="None"
          value={form.supplierCode}
          options={codeOptions(lookups.suppliers)}
          onChange={(v) => set({ supplierCode: v || undefined })}
        />
        <TextInput
          label="Acquisition date"
          type="date"
          required
          disabled={editing}
          error={errors.acquisitionDate}
          value={form.acquisitionDate}
          onChange={(acquisitionDate) => set({ acquisitionDate })}
        />
        <NumberInput
          label="Acquisition cost"
          required
          disabled={editing}
          error={errors.acquisitionCost}
          hint={
            monthly === undefined ? undefined : `First month depreciation ${monthly.toFixed(2)}`
          }
          value={form.acquisitionCost}
          onChange={(acquisitionCost) => set({ acquisitionCost })}
        />
        <TextInput
          label="Location"
          value={form.location}
          onChange={(location) => set({ location })}
        />
        <TextInput
          label="Custodian"
          value={form.custodian}
          onChange={(custodian) => set({ custodian })}
        />
        {form.takeOn !== true && (
          <SelectInput
            label="Settlement account"
            required
            blank="Select"
            hint="Supplier payable or bank credited on capitalization"
            error={errors.settlementAccount}
            value={form.settlementAccount}
            options={lookups.postableAccounts.map((a) => ({
              value: a.code,
              label: `${a.code} – ${a.name}`,
            }))}
            onChange={(v) => set({ settlementAccount: v || undefined })}
          />
        )}
        <TextInput
          label={form.takeOn === true ? 'Take-on date' : 'Capitalization date'}
          type="date"
          disabled={editing}
          hint={form.takeOn === true ? undefined : 'Defaults to the acquisition date'}
          error={errors.capitalizationDate}
          value={form.capitalizationDate}
          onChange={(capitalizationDate) => set({ capitalizationDate })}
        />
      </div>
      <label className="checkbox">
        <input
          type="checkbox"
          disabled={editing}
          checked={form.takeOn ?? false}
          onChange={(e) => set({ takeOn: e.target.checked })}
        />
        Existing asset taken on from a previous register (opening balance, depreciation up to the
        take-on date is computed)
      </label>
    </Modal>
  );
}
