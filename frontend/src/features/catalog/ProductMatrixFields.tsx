import { useQuery } from '@tanstack/react-query';
import { pmRoutingApi } from '@/api/pmRouting';
import { LovSelect } from '@/components/broking/LovSelect';
import { Combobox } from '@/components/ui/Combobox';
import { Field } from '@/components/ui/Field';
import { useDisplayName } from '@/components/ui/useDisplayName';
import { NumberInput, TextInput } from '@/features/assets/FormControls';
import type { ProductForm } from './productForm';

/**
 * The Product Matrix fields of a product (BDOI FRS FRPM.003.02 and Annex A): Policy Type,
 * Insured's Name, Incentive Eligible with the incentive amount or commission rate, and the
 * approver who is told of the record.
 */
export function ProductMatrixFields({
  form,
  set,
}: Readonly<{ form: ProductForm; set: (patch: Partial<ProductForm>) => void }>) {
  const name = useDisplayName();
  const approvers = useQuery({
    queryKey: ['product-approvers'],
    queryFn: () => pmRoutingApi.productApprovers(),
    staleTime: 10 * 60_000,
  });
  return (
    <fieldset className="stack" style={{ border: 0, padding: 0 }}>
      <legend className="muted">Product Matrix</legend>
      <div className="form-grid">
        <Field label="Policy Type">
          {(id) => (
            <LovSelect
              id={id}
              type="PKG_POLICY_TYPE"
              value={form.policyType ?? ''}
              onChange={(policyType) => set({ policyType })}
              placeholder="Select"
            />
          )}
        </Field>
        <TextInput
          label="Insured's Name"
          hint="Client-specific packages"
          value={form.insuredName ?? ''}
          onChange={(insuredName) => set({ insuredName })}
        />
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.incentiveEligible === true}
            onChange={(e) => set({ incentiveEligible: e.target.checked })}
          />
          Incentive Eligible
        </label>
        {form.incentiveEligible === true && (
          <>
            <NumberInput
              label="Incentive Amount"
              value={form.incentiveAmount}
              onChange={(incentiveAmount) => set({ incentiveAmount })}
            />
            <NumberInput
              label="Incentive Commission Rate (%)"
              value={form.incentiveRate}
              onChange={(incentiveRate) => set({ incentiveRate })}
            />
          </>
        )}
        <Field label="Approver" hint="Told when the record is saved for approval">
          {(id) => (
            <Combobox
              id={id}
              emptyLabel="Any approver"
              value={form.approver ?? ''}
              options={(approvers.data ?? []).map((u) => ({ value: u, label: name(u) }))}
              onChange={(approver) => set({ approver: approver || undefined })}
            />
          )}
        </Field>
      </div>
    </fieldset>
  );
}
