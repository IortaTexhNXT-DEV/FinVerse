import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import type { PaymentGate, TsuInvolvement } from '@/api/catalog';
import { lovApi } from '@/api/lov';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { enumOptions } from '@/features/assets/options';
import { productProblems, toProductInput, toggleSegment } from './productForm';
import type { ProductForm } from './productForm';

const GATES = enumOptions(['PAID', 'CLIENT_CONFIRMATION']);
const TSU = enumOptions(['BY_RULES', 'ALWAYS', 'NEVER']);

type Flag =
  | 'packaged'
  | 'fleetCapable'
  | 'mortgageApplicable'
  | 'directPaymentEligible'
  | 'multiYearAllowed'
  | 'ffyEligible';

const FLAGS: { key: Flag; label: string }[] = [
  { key: 'packaged', label: 'Package product (fixed cover)' },
  { key: 'fleetCapable', label: 'Fleet (several vehicles)' },
  { key: 'mortgageApplicable', label: 'Mortgage / loan related' },
  { key: 'directPaymentEligible', label: 'Direct payment to insurer allowed' },
  { key: 'multiYearAllowed', label: 'Multi-year term allowed' },
  { key: 'ffyEligible', label: 'Free First Year eligible' },
];

interface Props {
  initial: ProductForm;
  onClose: () => void;
}

/** Create or change a product (maker-checker: saved changes wait for authorization). */
export function ProductEditorModal({ initial, onClose }: Readonly<Props>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState(initial);
  const [problems, setProblems] = useState<string[]>([]);
  const lines = useQuery({ queryKey: ['catalog', 'lines'], queryFn: catalogApi.lines });
  const covers = useQuery({ queryKey: ['catalog', 'cover-types'], queryFn: catalogApi.coverTypes });
  const segments = useQuery({
    queryKey: ['lov', 'MARKET_SEGMENT'],
    queryFn: () => lovApi.options('MARKET_SEGMENT'),
  });
  const set = (patch: Partial<ProductForm>) => setForm((f) => ({ ...f, ...patch }));
  const save = useMutation({
    mutationFn: () =>
      form.existing
        ? catalogApi.updateProduct(form.code, toProductInput(form))
        : catalogApi.createProduct(toProductInput(form)),
    onSuccess: async (p) => {
      await queryClient.invalidateQueries({ queryKey: ['catalog'] });
      toast.success(`Product ${p.code} saved – pending authorization`);
      onClose();
    },
  });
  const submit = () => {
    const found = productProblems(form);
    setProblems(found);
    if (found.length === 0) {
      save.mutate();
    }
  };
  const coverOptions = (covers.data ?? [])
    .filter((c) => c.lineCode === form.lineCode)
    .map((c) => ({ value: c.code, label: c.name }));

  return (
    <Modal
      title={form.existing ? `Edit product ${form.code}` : 'New product'}
      open
      onClose={onClose}
      footer={
        <Button variant="accent" busy={save.isPending} onClick={submit}>
          Save for authorization
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      {problems.length > 0 && (
        <div className="alert warning" role="alert">
          {problems.join(' ')}
        </div>
      )}
      <div className="form-grid">
        <TextInput
          label="Code"
          required
          upper
          disabled={form.existing}
          value={form.code}
          onChange={(code) => set({ code })}
        />
        <TextInput label="Name" required value={form.name} onChange={(name) => set({ name })} />
        <SelectInput
          label="Product line"
          required
          blank="Select"
          value={form.lineCode}
          options={(lines.data ?? []).map((l) => ({ value: l.code, label: l.name }))}
          onChange={(lineCode) => set({ lineCode, coverTypeCode: undefined })}
        />
        <SelectInput
          label="Cover type"
          blank="None"
          value={form.coverTypeCode}
          options={coverOptions}
          onChange={(coverTypeCode) => set({ coverTypeCode })}
        />
        <SelectInput
          label="Placement after"
          required
          value={form.paymentGate}
          options={GATES}
          onChange={(v) => set({ paymentGate: v as PaymentGate })}
        />
        <SelectInput
          label="TSU review"
          value={form.tsuInvolvement}
          options={TSU}
          onChange={(v) => set({ tsuInvolvement: v as TsuInvolvement })}
        />
        <NumberInput
          label="Default rate %"
          step="0.0001"
          value={form.defaultRate}
          onChange={(defaultRate) => set({ defaultRate })}
        />
        <NumberInput
          label="Default commission %"
          required
          value={form.defaultCommissionRate}
          onChange={(defaultCommissionRate) => set({ defaultCommissionRate })}
        />
        <NumberInput
          label="Minimum premium"
          required
          value={form.minimumPremium}
          onChange={(minimumPremium) => set({ minimumPremium })}
        />
        <NumberInput
          label="Package TSI limit"
          hint="Above it the account goes to TSU"
          value={form.maxSumInsured}
          onChange={(maxSumInsured) => set({ maxSumInsured })}
        />
        <NumberInput
          label="Maximum term (years)"
          step="1"
          disabled={!form.multiYearAllowed}
          value={form.maxTermYears}
          onChange={(maxTermYears) => set({ maxTermYears })}
        />
      </div>
      <fieldset className="stack" style={{ border: 0, padding: 0 }}>
        <legend className="muted">Features</legend>
        <div className="form-grid">
          {FLAGS.map((f) => (
            <label key={f.key} className="checkbox">
              <input
                type="checkbox"
                checked={form[f.key]}
                onChange={(e) => set({ [f.key]: e.target.checked })}
              />
              {f.label}
            </label>
          ))}
        </div>
      </fieldset>
      <fieldset className="stack" style={{ border: 0, padding: 0 }}>
        <legend className="muted">Market segments (none = all)</legend>
        <div className="form-grid">
          {(segments.data ?? []).map((s) => (
            <label key={s.code} className="checkbox">
              <input
                type="checkbox"
                checked={form.marketSegments.includes(s.code)}
                onChange={() => set({ marketSegments: toggleSegment(form.marketSegments, s.code) })}
              />
              {s.label}
            </label>
          ))}
        </div>
      </fieldset>
    </Modal>
  );
}
