import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Trash2 } from 'lucide-react';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import { productCatalogApi } from '@/api/productCatalog';
import type { IncentiveCriteria, IncentiveScope, IncentiveValueBasis } from '@/api/productCatalog';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import {
  incentiveErrors,
  incentiveFormOf,
  newIncentiveForm,
  toIncentiveInput,
} from './incentiveForm';
import type { IncentiveForm } from './incentiveForm';

const BASES = [
  { value: 'RATE', label: 'Rate (%)' },
  { value: 'FIXED_AMOUNT', label: 'Fixed amount' },
  { value: 'RULE', label: 'Rule (parameters)' },
];

const withSegment = (scope: IncentiveScope, segment: string): IncentiveScope => ({
  ...scope,
  marketSegment: segment === '' ? undefined : segment,
});

const segmentOf = (products: IncentiveScope[], code: string, segment: string) =>
  products.map((p) => (p.productCode === code ? withSegment(p, segment) : p));

/** The products-matrix entries of a criterion: active products only, optional segment. */
function ProductMatrix({
  products,
  error,
  onChange,
}: Readonly<{
  products: IncentiveScope[];
  error?: string;
  onChange: (p: IncentiveScope[]) => void;
}>) {
  const [adding, setAdding] = useState('');
  const sellable = useQuery({
    queryKey: ['catalog', 'products', { activeOnly: true }],
    queryFn: () => catalogApi.products({ activeOnly: true }),
  });
  const options = (sellable.data ?? []).filter(
    (p) => !products.some((s) => s.productCode === p.code),
  );
  return (
    <Field label="Products matrix" required error={error}>
      {(id) => (
        <div className="stack">
          <div className="row">
            <select
              id={id}
              className="select"
              value={adding}
              onChange={(e) => setAdding(e.target.value)}
            >
              <option value="">Select an active product</option>
              {options.map((p) => (
                <option key={p.code} value={p.code}>
                  {p.code} – {p.name}
                </option>
              ))}
            </select>
            <Button
              size="sm"
              variant="secondary"
              disabled={adding === ''}
              onClick={() => {
                onChange([...products, { productCode: adding }]);
                setAdding('');
              }}
            >
              Add Product
            </Button>
          </div>
          {products.map((s) => (
            <div className="row" key={s.productCode}>
              <strong>{s.productCode}</strong>
              <Field label={`Segment for ${s.productCode}`}>
                {(segmentId) => (
                  <LovSelect
                    id={segmentId}
                    type="MARKET_SEGMENT"
                    placeholder="Any segment"
                    value={s.marketSegment ?? ''}
                    onChange={(marketSegment) =>
                      onChange(segmentOf(products, s.productCode, marketSegment))
                    }
                  />
                )}
              </Field>
              <Button
                size="sm"
                variant="ghost"
                aria-label={`Remove ${s.productCode}`}
                icon={<Trash2 size={14} />}
                onClick={() => onChange(products.filter((p) => p.productCode !== s.productCode))}
              />
            </div>
          ))}
        </div>
      )}
    </Field>
  );
}

/**
 * New criterion, change of a pending one, or amendment of an active one (a successor row from a
 * later date; PMADD07). Everything waits for authorization.
 */
export function IncentiveEditorModal({
  initial,
  onClose,
}: Readonly<{ initial?: IncentiveCriteria; onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const amending = initial?.recordStatus === 'ACTIVE';
  const [form, setForm] = useState<IncentiveForm>(() => {
    if (!initial) {
      return newIncentiveForm();
    }
    const f = incentiveFormOf(initial);
    return amending ? { ...f, effectiveFrom: '', effectiveTo: '' } : f;
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = (patch: Partial<IncentiveForm>) => setForm((f) => ({ ...f, ...patch }));
  const save = useMutation({
    mutationFn: () => {
      const input = toIncentiveInput(form, companyId);
      return initial
        ? productCatalogApi.updateIncentive(initial.id, input)
        : productCatalogApi.createIncentive(input);
    },
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: ['catalog', 'incentives'] });
      toast.success(`Criterion ${c.code} saved – pending authorization`);
      onClose();
    },
  });
  const submit = () => {
    const found = incentiveErrors(form, amending, initial?.effectiveFrom);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      save.mutate();
    }
  };
  let title = 'New Incentive Criterion';
  if (initial) {
    title = amending ? `Amend ${initial.code}` : `Change ${initial.code}`;
  }
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={save.isPending} onClick={submit}>
            Save for Authorization
          </Button>
        </>
      }
    >
      {amending && (
        <p className="muted">
          The active criterion ends the day before the amendment starts, once the amendment is
          authorized; its history is kept.
        </p>
      )}
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <TextInput
          label="Code"
          required
          upper
          disabled={initial !== undefined}
          value={form.code}
          error={errors.code}
          onChange={(code) => set({ code })}
        />
        <TextInput
          label="Name"
          required
          value={form.name}
          error={errors.name}
          onChange={(name) => set({ name })}
        />
        <Field label="Incentive type" required error={errors.incentiveType}>
          {(id) => (
            <LovSelect
              id={id}
              type="INCENTIVE_TYPE"
              value={form.incentiveType}
              onChange={(incentiveType) => set({ incentiveType })}
            />
          )}
        </Field>
        <SelectInput
          label="Value basis"
          required
          value={form.valueBasis}
          options={BASES}
          onChange={(v) => set({ valueBasis: v as IncentiveValueBasis })}
        />
        {form.valueBasis !== 'RULE' && (
          <TextInput
            label="Value"
            required
            value={form.value}
            error={errors.value}
            onChange={(value) => set({ value })}
          />
        )}
        <TextInput
          label="Effective from"
          type="date"
          required
          value={form.effectiveFrom}
          error={errors.effectiveFrom}
          onChange={(effectiveFrom) => set({ effectiveFrom })}
        />
        <TextInput
          label="Effective to"
          type="date"
          value={form.effectiveTo}
          error={errors.effectiveTo}
          onChange={(effectiveTo) => set({ effectiveTo })}
        />
        <TextInput
          label="Rule parameters (JSON)"
          hint='e.g. {"minimumPremium": 5000}'
          value={form.ruleParams}
          error={errors.ruleParams}
          onChange={(ruleParams) => set({ ruleParams })}
        />
        <TextInput
          label="Description"
          value={form.description}
          onChange={(description) => set({ description })}
        />
      </div>
      <ProductMatrix
        products={form.products}
        error={errors.products}
        onChange={(products) => set({ products })}
      />
    </Modal>
  );
}
