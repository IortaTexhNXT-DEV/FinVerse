import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '@/api/catalog';
import { lovApi } from '@/api/lov';
import type { RequestScope, RequestType } from '@/api/productmaint';
import { ClientPicker } from '@/components/broking/ClientPicker';
import { LovSelect } from '@/components/broking/LovSelect';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { REQUEST_TYPES, negotiationOptional } from './packageRequest';
import type { RequestForm } from './packageRequest';

interface HeaderProps {
  form: RequestForm;
  set: (patch: Partial<RequestForm>) => void;
  errors: Record<string, string>;
}

const SCOPES: { value: RequestScope; label: string }[] = [
  { value: 'GENERIC', label: 'Generic programme' },
  { value: 'CLIENT_SPECIFIC', label: 'Client-specific package' },
];

function SegmentChoices({ form, set }: Readonly<Omit<HeaderProps, 'errors'>>) {
  const segments = useQuery({
    queryKey: ['lov', 'MARKET_SEGMENT'],
    queryFn: () => lovApi.options('MARKET_SEGMENT'),
    staleTime: 5 * 60_000,
  });
  const toggle = (code: string) =>
    set({
      marketSegments: form.marketSegments.includes(code)
        ? form.marketSegments.filter((s) => s !== code)
        : [...form.marketSegments, code],
    });
  return (
    <fieldset className="field">
      <legend>Market segments</legend>
      <div className="insurer-choices">
        {(segments.data ?? []).map((s) => (
          <label key={s.code} className="checkbox">
            <input
              type="checkbox"
              checked={form.marketSegments.includes(s.code)}
              onChange={() => toggle(s.code)}
            />
            {s.label}
          </label>
        ))}
      </div>
    </fieldset>
  );
}

/** Line, cover type and target product of the request (PMADD01 hierarchy, BRPM.011). */
function ProductFields({ form, set, errors }: Readonly<HeaderProps>) {
  const saved = form.id !== undefined;
  const lines = useQuery({ queryKey: ['catalog', 'lines'], queryFn: catalogApi.lines });
  const coverTypes = useQuery({
    queryKey: ['catalog', 'cover-types'],
    queryFn: catalogApi.coverTypes,
  });
  const products = useQuery({
    queryKey: ['catalog', 'products', { line: form.lineCode, packaged: true }],
    queryFn: () => catalogApi.products({ line: form.lineCode, packaged: true }),
    enabled: form.lineCode !== '' && form.type !== 'NEW',
  });
  return (
    <>
      <SelectInput
        label="Product line"
        required
        blank="Select"
        error={errors.lineCode}
        value={form.lineCode}
        options={(lines.data ?? []).map((l) => ({ value: l.code, label: l.name }))}
        onChange={(lineCode) => set({ lineCode, coverTypeCode: '', productCode: '' })}
      />
      <SelectInput
        label="Cover type / subtype"
        required={form.type === 'NEW'}
        blank="Select"
        error={errors.coverTypeCode}
        value={form.coverTypeCode}
        options={(coverTypes.data ?? [])
          .filter((c) => c.lineCode === form.lineCode)
          .map((c) => ({ value: c.code, label: c.name }))}
        onChange={(coverTypeCode) => set({ coverTypeCode })}
      />
      {form.type !== 'NEW' && (
        <SelectInput
          label="Package product"
          required
          blank="Select"
          disabled={saved}
          error={errors.productCode}
          value={form.productCode}
          hint="Its current terms can be loaded into the requested terms below."
          options={(products.data ?? []).map((p) => ({
            value: p.code,
            label: `${p.code} – ${p.name}`,
          }))}
          onChange={(productCode) => set({ productCode })}
        />
      )}
    </>
  );
}

/**
 * Header of the Package Request Form (BRPM.008): type and scope, the package or programme name,
 * the client of a client-specific package (CRM look-up), line, cover type and product, market
 * segments, reason and whether insurers are approached.
 */
export function RequestHeaderCard({ form, set, errors }: Readonly<HeaderProps>) {
  const saved = form.id !== undefined;
  return (
    <Card title="Request">
      <div className="form-grid">
        <SelectInput
          label="Request type"
          required
          disabled={saved}
          value={form.type}
          options={REQUEST_TYPES}
          onChange={(v) => {
            const type = v as RequestType;
            set({ type, productCode: '', negotiationRequired: type !== 'RETIRE' });
          }}
        />
        <SelectInput
          label="Scope"
          required
          value={form.scope}
          options={SCOPES}
          onChange={(v) => set({ scope: v as RequestScope })}
        />
        <TextInput
          label="Package / programme name"
          required
          error={errors.title}
          value={form.title}
          onChange={(title) => set({ title })}
        />
        {form.scope === 'CLIENT_SPECIFIC' && (
          <Field label="Client or prospect" required error={errors.clientId}>
            {(id) => (
              <ClientPicker
                id={id}
                value={form.clientId}
                onChange={(c) => set({ clientId: c?.id })}
              />
            )}
          </Field>
        )}
        <ProductFields form={form} set={set} errors={errors} />
        <Field label="Reason" required error={errors.reason}>
          {(id) => (
            <LovSelect
              id={id}
              type="PKG_REQUEST_REASON"
              value={form.reason}
              onChange={(reason) => set({ reason })}
            />
          )}
        </Field>
        <TextInput
          label="Comment on the reason"
          value={form.reasonNote}
          onChange={(reasonNote) => set({ reasonNote })}
        />
        {negotiationOptional(form.type) && (
          <label className="checkbox">
            <input
              type="checkbox"
              checked={form.negotiationRequired}
              onChange={(e) => set({ negotiationRequired: e.target.checked })}
            />
            Negotiate with the insurers
          </label>
        )}
      </div>
      <SegmentChoices form={form} set={set} />
    </Card>
  );
}
