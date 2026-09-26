import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { assetsApi } from '@/api/assets';
import type { FixedAsset } from '@/api/assets';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { today } from '@/utils/format';
import { disposalResult } from './assetMath';
import { NumberInput, SelectInput, TextInput } from './FormControls';
import { useAssetLookups } from './useAssetLookups';

export type AssetAction = 'DISPOSE' | 'TRANSFER';

interface Props {
  asset: FixedAsset;
  action: AssetAction;
  onClose: () => void;
}

interface ActionForm {
  date: string;
  proceeds?: number;
  bankAccount?: string;
  toBranchId?: number;
  location?: string;
  custodian?: string;
  reference?: string;
  remarks?: string;
}

/**
 * Gain or loss a disposal would post. The server preview applies the reversal of depreciation
 * already charged for the disposal month; the current net book value is used until it arrives.
 */
function useDisposalPreview(asset: FixedAsset, date: string, proceeds: number, enabled: boolean) {
  const preview = useQuery({
    queryKey: ['asset-disposal-preview', asset.id, date, proceeds],
    queryFn: () => assetsApi.disposalPreview(asset.id, date, proceeds),
    enabled: enabled && date !== '',
  });
  return {
    result: preview.data?.gainOrLoss ?? disposalResult(proceeds, asset.netBookValue),
    reversed: preview.data?.depreciationReversed ?? 0,
    netBookValue: preview.data?.netBookValue ?? asset.netBookValue,
  };
}

function gainLossLabel(result: number): string {
  const amount = Math.abs(result).toLocaleString('en-PH', { minimumFractionDigits: 2 });
  return result >= 0 ? `Gain ${amount}` : `Loss ${amount}`;
}

/** Disposal (sale or scrap, with gain or loss) or inter-branch transfer of an asset. */
export function AssetActionModal({ asset, action, onClose }: Readonly<Props>) {
  const lookups = useAssetLookups();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ActionForm>({ date: today(), proceeds: 0 });
  const set = (patch: Partial<ActionForm>) => setForm({ ...form, ...patch });
  const dispose = action === 'DISPOSE';
  const preview = useDisposalPreview(asset, form.date, form.proceeds ?? 0, dispose);
  const submit = useMutation({
    mutationFn: () =>
      dispose
        ? assetsApi.dispose(asset.id, {
            disposalDate: form.date,
            proceeds: form.proceeds ?? 0,
            bankAccount: form.bankAccount,
            reference: form.reference,
            remarks: form.remarks,
          })
        : assetsApi.transfer(asset.id, {
            toBranchId: form.toBranchId ?? 0,
            transferDate: form.date,
            location: form.location,
            custodian: form.custodian,
            remarks: form.remarks,
          }),
    onSuccess: async (m) => {
      await queryClient.invalidateQueries({ queryKey: ['fixed-assets'] });
      toast.success(
        `${asset.tagNo} ${dispose ? 'disposed' : 'transferred'} – journal ${m.batchNo ?? ''}`,
      );
      onClose();
    },
  });

  return (
    <Modal
      title={`${dispose ? 'Dispose of' : 'Transfer'} ${asset.tagNo}`}
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={submit.isPending} onClick={() => submit.mutate()}>
          {dispose ? 'Post disposal' : 'Post transfer'}
        </Button>
      }
    >
      <ErrorAlert error={submit.error} />
      <p className="muted">
        {asset.description} – net book value <Amount value={asset.netBookValue} />
      </p>
      {dispose && preview.reversed > 0 && (
        <p className="muted">
          Depreciation of <Amount value={preview.reversed} /> already charged for the disposal month
          or later will be reversed; the gain or loss is measured against a net book value of{' '}
          <Amount value={preview.netBookValue} />.
        </p>
      )}
      <div className="form-grid">
        <TextInput
          label={dispose ? 'Disposal date' : 'Transfer date'}
          type="date"
          required
          value={form.date}
          onChange={(date) => set({ date })}
        />
        {dispose ? (
          <>
            <NumberInput
              label="Sale proceeds"
              required
              hint={gainLossLabel(preview.result)}
              value={form.proceeds}
              onChange={(proceeds) => set({ proceeds })}
            />
            <SelectInput
              label="Bank account"
              blank="Select"
              value={form.bankAccount}
              options={lookups.bankAccounts.map((a) => ({
                value: a.code,
                label: `${a.code} – ${a.name}`,
              }))}
              onChange={(bankAccount) => set({ bankAccount })}
            />
            <TextInput
              label="Reference"
              value={form.reference}
              onChange={(reference) => set({ reference })}
            />
          </>
        ) : (
          <>
            <SelectInput
              label="To branch"
              required
              blank="Select"
              value={form.toBranchId}
              options={lookups.branches
                .filter((b) => b.id !== asset.branchId)
                .map((b) => ({ value: String(b.id), label: b.name }))}
              onChange={(v) => set({ toBranchId: Number(v) })}
            />
            <TextInput
              label="New location"
              value={form.location}
              onChange={(location) => set({ location })}
            />
            <TextInput
              label="New custodian"
              value={form.custodian}
              onChange={(custodian) => set({ custodian })}
            />
          </>
        )}
        <TextInput label="Remarks" value={form.remarks} onChange={(remarks) => set({ remarks })} />
      </div>
    </Modal>
  );
}
