import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import type { FieldTarget, ProductClass, RuleScope } from '@/api/catalog';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { enumOptions } from '@/features/assets/options';

export type RuleKind = 'fields' | 'documents' | 'tsu';

const SCOPES = enumOptions(['ALL', 'LINE', 'PRODUCT']);
const TARGETS = [
  { value: 'ACCOUNT', label: 'Account' },
  { value: 'ITEM', label: 'Risk item' },
];
const CLASSES = enumOptions(['ANY', 'PACKAGE', 'NON_PACKAGE']);

interface RuleForm {
  scope: RuleScope;
  scopeCode: string;
  target: FieldTarget;
  fieldKey: string;
  label: string;
  documentType: string;
  required: boolean;
  sortOrder?: number;
  code: string;
  description: string;
  productClass: ProductClass;
  lineCode: string;
  minFleetUnits?: number;
  minLocations?: number;
  tsiAbove?: number;
  priority?: number;
}

interface Props {
  kind: RuleKind;
  /** Pre-set scope, e.g. a product's own rules. */
  scope?: { scope: RuleScope; scopeCode: string };
  onClose: () => void;
}

function send(kind: RuleKind, f: RuleForm): Promise<unknown> {
  const scopeCode = f.scope === 'ALL' ? '*' : f.scopeCode;
  if (kind === 'fields') {
    return catalogApi.createFieldRule({
      scope: f.scope,
      scopeCode,
      target: f.target,
      fieldKey: f.fieldKey,
      label: f.label,
      required: f.required,
      sortOrder: f.sortOrder ?? 100,
    });
  }
  if (kind === 'documents') {
    return catalogApi.createDocumentRule({
      scope: f.scope,
      scopeCode,
      documentType: f.documentType,
      required: f.required,
    });
  }
  return catalogApi.createTsuRule({
    code: f.code,
    description: f.description,
    productClass: f.productClass,
    lineCode: f.lineCode || undefined,
    minFleetUnits: f.minFleetUnits,
    minLocations: f.minLocations,
    tsiAbove: f.tsiAbove,
    priority: f.priority ?? 100,
  });
}

function ScopeFields({
  form,
  set,
}: Readonly<{ form: RuleForm; set: (p: Partial<RuleForm>) => void }>) {
  return (
    <>
      <SelectInput
        label="Applies to"
        required
        value={form.scope}
        options={SCOPES}
        onChange={(v) => set({ scope: v as RuleScope })}
      />
      {form.scope !== 'ALL' && (
        <TextInput
          label={form.scope === 'LINE' ? 'Line code' : 'Product code'}
          required
          upper
          value={form.scopeCode}
          onChange={(scopeCode) => set({ scopeCode })}
        />
      )}
    </>
  );
}

function TsuFields({
  form,
  set,
}: Readonly<{ form: RuleForm; set: (p: Partial<RuleForm>) => void }>) {
  return (
    <>
      <TextInput label="Code" required upper value={form.code} onChange={(code) => set({ code })} />
      <TextInput
        label="Description"
        required
        value={form.description}
        onChange={(description) => set({ description })}
      />
      <SelectInput
        label="Products"
        value={form.productClass}
        options={CLASSES}
        onChange={(v) => set({ productClass: v as ProductClass })}
      />
      <TextInput
        label="Line code"
        upper
        value={form.lineCode}
        onChange={(lineCode) => set({ lineCode })}
      />
      <NumberInput
        label="Vehicles at least"
        step="1"
        value={form.minFleetUnits}
        onChange={(minFleetUnits) => set({ minFleetUnits })}
      />
      <NumberInput
        label="Locations at least"
        step="1"
        value={form.minLocations}
        onChange={(minLocations) => set({ minLocations })}
      />
      <NumberInput
        label="TSI above"
        value={form.tsiAbove}
        onChange={(tsiAbove) => set({ tsiAbove })}
      />
      <NumberInput
        label="Priority"
        step="1"
        hint="Lower numbers are evaluated first"
        value={form.priority}
        onChange={(priority) => set({ priority })}
      />
    </>
  );
}

/** Adds a field, document or TSU routing rule (maker-checker). */
export function RuleEditorModal({ kind, scope, onClose }: Readonly<Props>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<RuleForm>({
    scope: scope?.scope ?? 'ALL',
    scopeCode: scope?.scopeCode ?? '',
    target: 'ACCOUNT',
    fieldKey: '',
    label: '',
    documentType: '',
    required: true,
    code: '',
    description: '',
    productClass: 'ANY',
    lineCode: '',
  });
  const set = (patch: Partial<RuleForm>) => setForm((f) => ({ ...f, ...patch }));
  const save = useMutation({
    mutationFn: () => send(kind, form),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['catalog'] });
      toast.success('Rule saved – pending authorization');
      onClose();
    },
  });
  return (
    <Modal
      title="New rule"
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
        {kind !== 'tsu' && <ScopeFields form={form} set={set} />}
        {kind === 'fields' && (
          <>
            <SelectInput
              label="Level"
              value={form.target}
              options={TARGETS}
              onChange={(v) => set({ target: v as FieldTarget })}
            />
            <TextInput
              label="Field key"
              required
              hint="e.g. plateNo|conductionSticker (one of)"
              value={form.fieldKey}
              onChange={(fieldKey) => set({ fieldKey })}
            />
            <TextInput
              label="Label"
              required
              value={form.label}
              onChange={(label) => set({ label })}
            />
          </>
        )}
        {kind === 'documents' && (
          <Field label="Document type" required>
            {(id) => (
              <LovSelect
                id={id}
                type="DOCUMENT_TYPE"
                value={form.documentType}
                onChange={(documentType) => set({ documentType })}
              />
            )}
          </Field>
        )}
        {kind === 'tsu' && <TsuFields form={form} set={set} />}
        {kind !== 'tsu' && (
          <label className="checkbox" style={{ alignSelf: 'end' }}>
            <input
              type="checkbox"
              checked={form.required}
              onChange={(e) => set({ required: e.target.checked })}
            />
            Mandatory
          </label>
        )}
      </div>
    </Modal>
  );
}
