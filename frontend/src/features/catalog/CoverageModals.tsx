import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import { productCatalogApi } from '@/api/productCatalog';
import type { Clause, ClauseInput, Coverage, CoverageInput } from '@/api/productCatalog';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { clauseErrors, coverageErrors } from './coverageForm';

function useLines() {
  const lines = useQuery({ queryKey: ['catalog', 'lines'], queryFn: catalogApi.lines });
  return (lines.data ?? []).map((l) => ({ value: l.code, label: l.name }));
}

function Footer({
  busy,
  onCancel,
  onSave,
}: Readonly<{ busy: boolean; onCancel: () => void; onSave: () => void }>) {
  return (
    <>
      <Button variant="secondary" onClick={onCancel}>
        Cancel
      </Button>
      <Button variant="accent" busy={busy} onClick={onSave}>
        Save for Authorization
      </Button>
    </>
  );
}

/** New or changed coverage / peril (PMADD01), pending authorization. */
export function CoverageEditorModal({
  initial,
  onClose,
}: Readonly<{ initial?: Coverage; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const lines = useLines();
  const [form, setForm] = useState<CoverageInput>(
    initial ?? { lineCode: '', code: '', name: '', kind: '', basic: false, sortOrder: 100 },
  );
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = (patch: Partial<CoverageInput>) => setForm((f) => ({ ...f, ...patch }));
  const save = useMutation({
    mutationFn: () =>
      initial
        ? productCatalogApi.updateCoverage(initial.id, form)
        : productCatalogApi.createCoverage(form),
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: ['catalog'] });
      toast.success(`Coverage ${c.code} saved – pending authorization`);
      onClose();
    },
  });
  const submit = () => {
    const found = coverageErrors(form);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      save.mutate();
    }
  };
  return (
    <Modal
      open
      title={initial ? `Coverage ${initial.code}` : 'New Coverage'}
      onClose={onClose}
      footer={<Footer busy={save.isPending} onCancel={onClose} onSave={submit} />}
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <SelectInput
          label="Product line"
          required
          disabled={initial !== undefined}
          value={form.lineCode}
          options={lines}
          blank="Select the line"
          error={errors.lineCode}
          onChange={(lineCode) => set({ lineCode })}
        />
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
        <Field label="Kind" required error={errors.kind}>
          {(id) => (
            <LovSelect
              id={id}
              type="COVERAGE_KIND"
              value={form.kind}
              onChange={(kind) => set({ kind })}
            />
          )}
        </Field>
        <NumberInput
          label="Display order"
          step="1"
          value={form.sortOrder}
          onChange={(sortOrder) => set({ sortOrder: sortOrder ?? 0 })}
        />
        <label className="checkbox" style={{ alignSelf: 'end' }}>
          <input
            type="checkbox"
            checked={form.basic}
            onChange={(e) => set({ basic: e.target.checked })}
          />
          Basic coverage
        </label>
      </div>
    </Modal>
  );
}

/** New or changed clause of the library (PMADD02), pending authorization. */
export function ClauseEditorModal({
  initial,
  onClose,
}: Readonly<{ initial?: Clause; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const lines = useLines();
  const [form, setForm] = useState<ClauseInput>(
    initial ?? { code: '', kind: '', lineCode: '', title: '', wording: '', effectiveFrom: '' },
  );
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = (patch: Partial<ClauseInput>) => setForm((f) => ({ ...f, ...patch }));
  const save = useMutation({
    mutationFn: () => {
      const input = {
        ...form,
        lineCode: form.lineCode === '' ? undefined : form.lineCode,
        effectiveTo: form.effectiveTo === '' ? undefined : form.effectiveTo,
      };
      return initial
        ? productCatalogApi.updateClause(initial.id, input)
        : productCatalogApi.createClause(input);
    },
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: ['catalog'] });
      toast.success(`Clause ${c.code} saved – pending authorization`);
      onClose();
    },
  });
  const submit = () => {
    const found = clauseErrors(form);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      save.mutate();
    }
  };
  return (
    <Modal
      open
      title={initial ? `Clause ${initial.code}` : 'New Clause'}
      onClose={onClose}
      footer={<Footer busy={save.isPending} onCancel={onClose} onSave={submit} />}
    >
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
        <Field label="Kind" required error={errors.kind}>
          {(id) => (
            <LovSelect
              id={id}
              type="CLAUSE_KIND"
              value={form.kind}
              onChange={(kind) => set({ kind })}
            />
          )}
        </Field>
        <SelectInput
          label="Product line"
          value={form.lineCode ?? ''}
          options={lines}
          blank="Every line"
          onChange={(lineCode) => set({ lineCode })}
        />
        <TextInput
          label="Title"
          required
          value={form.title}
          error={errors.title}
          onChange={(title) => set({ title })}
        />
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
      </div>
      <Field label="Wording" required error={errors.wording}>
        {(id) => (
          <textarea
            id={id}
            className="input"
            rows={6}
            value={form.wording}
            onChange={(e) => set({ wording: e.target.value })}
          />
        )}
      </Field>
    </Modal>
  );
}
