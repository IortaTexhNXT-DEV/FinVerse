import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import type { BranchInput, CommissionInput } from '@/api/catalog';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { today } from '@/utils/format';

interface Props {
  insurerId: number;
  onClose: () => void;
}

function useSaved(onClose: () => void, message: string) {
  const toast = useToast();
  const queryClient = useQueryClient();
  return async () => {
    await queryClient.invalidateQueries({ queryKey: ['catalog'] });
    toast.success(message);
    onClose();
  };
}

/** Adds a branch of an insurer with its local government tax (LGT) rate. */
export function BranchModal({ insurerId, onClose }: Readonly<Props>) {
  const [form, setForm] = useState<Partial<BranchInput>>({ lgtRate: 0 });
  const saved = useSaved(onClose, 'Branch saved – pending authorization');
  const save = useMutation({
    mutationFn: () =>
      catalogApi.createBranch(insurerId, { ...form, lgtRate: form.lgtRate ?? 0 } as BranchInput),
    onSuccess: saved,
  });
  const set = (patch: Partial<BranchInput>) => setForm((f) => ({ ...f, ...patch }));
  return (
    <Modal
      title="New branch"
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={save.isPending} onClick={() => save.mutate()}>
          Save for authorization
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <TextInput
          label="Code"
          required
          upper
          value={form.code}
          onChange={(code) => set({ code })}
        />
        <TextInput label="Name" required value={form.name} onChange={(name) => set({ name })} />
        <TextInput label="City" value={form.city} onChange={(city) => set({ city })} />
        <NumberInput
          label="LGT %"
          required
          step="0.0001"
          hint="Local government tax on premium"
          value={form.lgtRate}
          onChange={(lgtRate) => set({ lgtRate })}
        />
        <TextInput
          label="Placement e-mail"
          value={form.placementEmail}
          onChange={(placementEmail) => set({ placementEmail })}
        />
      </div>
    </Modal>
  );
}

/**
 * Adds a commission rate of an insurer, for all its products or one product, from a date
 * (effective-dated; the rate in force on the account's period start applies).
 */
export function CommissionModal({ insurerId, onClose }: Readonly<Props>) {
  const [form, setForm] = useState<Partial<CommissionInput>>({ effectiveFrom: today() });
  const products = useQuery({
    queryKey: ['catalog', 'products', { activeOnly: true }],
    queryFn: () => catalogApi.products({ activeOnly: true }),
  });
  const saved = useSaved(onClose, 'Commission rate saved – pending authorization');
  const save = useMutation({
    mutationFn: () =>
      catalogApi.createCommission(insurerId, {
        ...form,
        productCode: form.productCode === '' ? undefined : form.productCode,
        effectiveTo: form.effectiveTo === '' ? undefined : form.effectiveTo,
      } as CommissionInput),
    onSuccess: saved,
  });
  const set = (patch: Partial<CommissionInput>) => setForm((f) => ({ ...f, ...patch }));
  return (
    <Modal
      title="New commission rate"
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={save.isPending} onClick={() => save.mutate()}>
          Save for authorization
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <SelectInput
          label="Product"
          blank="All products of the insurer"
          value={form.productCode}
          options={(products.data ?? []).map((p) => ({
            value: p.code,
            label: `${p.code} – ${p.name}`,
          }))}
          onChange={(productCode) => set({ productCode })}
        />
        <NumberInput
          label="Commission %"
          required
          value={form.rate}
          onChange={(rate) => set({ rate })}
        />
        <TextInput
          label="Effective from"
          type="date"
          required
          value={form.effectiveFrom}
          onChange={(effectiveFrom) => set({ effectiveFrom })}
        />
        <TextInput
          label="Effective to"
          type="date"
          value={form.effectiveTo}
          onChange={(effectiveTo) => set({ effectiveTo })}
        />
      </div>
    </Modal>
  );
}
