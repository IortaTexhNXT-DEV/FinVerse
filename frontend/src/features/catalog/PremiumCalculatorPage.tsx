import { useMutation, useQuery } from '@tanstack/react-query';
import { Calculator, Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import type { PeriodBasis, RatingMethod } from '@/api/catalog';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { calcProblems, EMPTY_ITEM, newCalcForm, toRatingInput } from './calculatorForm';
import type { CalcForm, CalcItem } from './calculatorForm';
import { CalculatorResult } from './CalculatorResult';

const BASES = [
  { value: 'ANNUAL', label: 'Annual' },
  { value: 'PRO_RATA', label: 'Pro-rata (days)' },
  { value: 'SHORT_PERIOD', label: 'Short period (table)' },
];

function useCalculatorLookups(companyId: number, insurerCode: string) {
  const products = useQuery({
    queryKey: ['catalog', 'products', { activeOnly: true }],
    queryFn: () => catalogApi.products({ activeOnly: true }),
  });
  const lines = useQuery({ queryKey: ['catalog', 'lines'], queryFn: catalogApi.lines });
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
  });
  const insurerId = insurers.data?.find((i) => i.partyCode === insurerCode)?.id;
  const insurer = useQuery({
    queryKey: ['catalog', 'insurer', insurerId],
    queryFn: () => catalogApi.insurer(insurerId ?? 0),
    enabled: insurerId !== undefined,
  });
  return { products, lines, insurers, branches: insurer.data?.branches ?? [] };
}

function ItemRow({
  item,
  motor,
  onChange,
  onRemove,
}: Readonly<{
  item: CalcItem;
  motor: boolean;
  onChange: (patch: Partial<CalcItem>) => void;
  onRemove?: () => void;
}>) {
  return (
    <div className="form-grid">
      <TextInput label="Item" value={item.label} onChange={(label) => onChange({ label })} />
      <TextInput
        label="Sum insured"
        required
        value={item.sumInsured}
        onChange={(sumInsured) => onChange({ sumInsured })}
      />
      {motor ? (
        <>
          <TextInput
            label="Excess BI limit"
            value={item.biLimit}
            onChange={(biLimit) => onChange({ biLimit })}
          />
          <TextInput
            label="PD limit"
            value={item.pdLimit}
            onChange={(pdLimit) => onChange({ pdLimit })}
          />
        </>
      ) : (
        <TextInput
          label="Rate %"
          hint="Blank = product default"
          value={item.ratePercent}
          onChange={(ratePercent) => onChange({ ratePercent })}
        />
      )}
      {onRemove && (
        <Button
          variant="ghost"
          size="sm"
          icon={<Trash2 size={14} />}
          onClick={onRemove}
          style={{ alignSelf: 'end' }}
        >
          Remove
        </Button>
      )}
    </div>
  );
}

/**
 * Interactive premium calculator (BRNB.007): rates a product for an insurer branch, annual,
 * pro-rata or short period, for a new account or an endorsement over the remaining term, and
 * shows every charge (DST, premium tax, VAT, FST, LGT) and the commission.
 */
export default function PremiumCalculatorPage() {
  const companyId = useCompanyId();
  const [form, setForm] = useState<CalcForm>(newCalcForm);
  const [problems, setProblems] = useState<string[]>([]);
  const { products, lines, insurers, branches } = useCalculatorLookups(companyId, form.insurerCode);
  const product = products.data?.find((p) => p.code === form.productCode);
  const method: RatingMethod =
    lines.data?.find((l) => l.code === product?.lineCode)?.ratingMethod ?? 'GENERIC';
  const set = (patch: Partial<CalcForm>) => setForm((f) => ({ ...f, ...patch }));
  const setItem = (index: number, patch: Partial<CalcItem>) =>
    set({ items: form.items.map((it, i) => (i === index ? { ...it, ...patch } : it)) });
  const quote = useMutation({
    mutationFn: () => catalogApi.quote(toRatingInput(form, companyId, method)),
  });
  const rate = () => {
    const found = calcProblems(form);
    setProblems(found);
    if (found.length === 0) {
      quote.mutate();
    }
  };

  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Premium Calculator"
        description="Rate a product with the charges in force: for a quotation, a new account or an endorsement over the remaining term."
      />
      <Card title="Cover">
        <div className="form-grid">
          <SelectInput
            label="Product"
            required
            blank="Select"
            value={form.productCode}
            options={(products.data ?? []).map((p) => ({
              value: p.code,
              label: `${p.code} – ${p.name}`,
            }))}
            onChange={(productCode) => set({ productCode })}
          />
          <SelectInput
            label="Insurer"
            blank="Product default commission"
            value={form.insurerCode}
            options={(insurers.data ?? []).map((i) => ({ value: i.partyCode, label: i.name }))}
            onChange={(insurerCode) => set({ insurerCode, branchCode: '' })}
          />
          <SelectInput
            label="Insurer branch (LGT)"
            blank="No LGT"
            value={form.branchCode}
            options={branches.map((b) => ({ value: b.code, label: `${b.name} (${b.lgtRate}%)` }))}
            onChange={(branchCode) => set({ branchCode })}
          />
          <SelectInput
            label="Period basis"
            value={form.basis}
            options={BASES}
            onChange={(v) => set({ basis: v as PeriodBasis })}
          />
          <TextInput
            label="From"
            type="date"
            value={form.periodFrom}
            onChange={(periodFrom) => set({ periodFrom })}
          />
          <TextInput
            label="To"
            type="date"
            value={form.periodTo}
            onChange={(periodTo) => set({ periodTo })}
          />
          <TextInput
            label="Commission % override"
            value={form.commissionRate}
            onChange={(commissionRate) => set({ commissionRate })}
          />
          <label className="checkbox" style={{ alignSelf: 'end' }}>
            <input
              type="checkbox"
              checked={form.endorsement}
              onChange={(e) => set({ endorsement: e.target.checked })}
            />
            Endorsement (remaining term, may return premium)
          </label>
          {method === 'MOTOR' && product?.multiYearAllowed === true && (
            <label className="checkbox" style={{ alignSelf: 'end' }}>
              <input
                type="checkbox"
                checked={form.multiYear}
                onChange={(e) => set({ multiYear: e.target.checked })}
              />
              Multi-year own damage factor
            </label>
          )}
        </div>
      </Card>
      <Card
        title="Items"
        actions={
          <Button
            size="sm"
            variant="secondary"
            icon={<Plus size={14} />}
            onClick={() => set({ items: [...form.items, { ...EMPTY_ITEM }] })}
          >
            Add Item
          </Button>
        }
      >
        <div className="stack">
          {form.items.map((item, index) => (
            <ItemRow
              key={index}
              item={item}
              motor={method === 'MOTOR'}
              onChange={(patch) => setItem(index, patch)}
              onRemove={
                form.items.length > 1
                  ? () => set({ items: form.items.filter((_, i) => i !== index) })
                  : undefined
              }
            />
          ))}
        </div>
        {problems.length > 0 && (
          <div className="alert warning" role="alert">
            {problems.join(' ')}
          </div>
        )}
        <ErrorAlert error={quote.error} />
        <div className="row">
          <Button
            variant="accent"
            icon={<Calculator size={16} />}
            busy={quote.isPending}
            onClick={rate}
          >
            Calculate
          </Button>
        </div>
      </Card>
      {quote.data && <CalculatorResult result={quote.data} />}
    </div>
  );
}
