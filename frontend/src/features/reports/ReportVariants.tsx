import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { nbReportsApi } from '@/api/nbReports';
import type { ReportVariant } from '@/api/nbReports';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';

interface ReportVariantsProps {
  code: string;
  /** Current parameter values (saved as the variant). */
  values: () => Record<string, string>;
  /** Applies the saved values of a variant to the form. */
  onApply: (values: Record<string, string>) => void;
}

function SaveDialog({
  code,
  values,
  onClose,
}: Readonly<{ code: string; values: Record<string, string>; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [shared, setShared] = useState(false);
  const [checked, setChecked] = useState(false);
  const save = useMutation({
    mutationFn: () =>
      nbReportsApi.saveVariant({ reportCode: code, name: name.trim(), parameters: values, shared }),
    onSuccess: async (v) => {
      await queryClient.invalidateQueries({ queryKey: ['report-variants', code] });
      toast.success(`Variant "${v.name}" saved`);
      onClose();
    },
  });
  const missing = name.trim() === '';
  return (
    <Modal
      open
      title="Save Report Variant"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={save.isPending}
            onClick={() => {
              setChecked(true);
              if (!missing) {
                save.mutate();
              }
            }}
          >
            Save Variant
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <Field
          label="Variant Name"
          required
          error={checked && missing ? 'Enter a name' : undefined}
          hint="Saving under an existing name of yours replaces it."
        >
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={80}
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          )}
        </Field>
        <label className="checkbox">
          <input type="checkbox" checked={shared} onChange={(e) => setShared(e.target.checked)} />
          Share with every user who may run this report
        </label>
      </div>
    </Modal>
  );
}

/**
 * Saved report variants (BRNB.057): choose a saved set of parameters, save the current ones under
 * a name (optionally shared) or delete one of your own. The ad-hoc report builder is parked (Q40).
 */
export function ReportVariants({ code, values, onApply }: Readonly<ReportVariantsProps>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState('');
  const [saving, setSaving] = useState<Record<string, string> | null>(null);
  const variants = useQuery({
    queryKey: ['report-variants', code],
    queryFn: () => nbReportsApi.variants(code),
  });
  const remove = useMutation({
    mutationFn: (v: ReportVariant) => nbReportsApi.deleteVariant(v.id),
    onSuccess: async () => {
      setSelected('');
      await queryClient.invalidateQueries({ queryKey: ['report-variants', code] });
      toast.success('Variant deleted');
    },
  });
  const current = variants.data?.find((v) => String(v.id) === selected);
  return (
    <div className="report-variants">
      <Field label="Saved Variant">
        {(id) => (
          <select
            id={id}
            className="select"
            value={selected}
            onChange={(e) => {
              setSelected(e.target.value);
              const v = variants.data?.find((x) => String(x.id) === e.target.value);
              if (v !== undefined) {
                onApply(v.parameters);
              }
            }}
          >
            <option value="">— None —</option>
            {(variants.data ?? []).map((v) => (
              <option key={v.id} value={v.id}>
                {v.name}
                {v.mine ? '' : ` (shared by ${v.owner})`}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Button variant="secondary" icon={<Save size={16} />} onClick={() => setSaving(values())}>
        Save Variant
      </Button>
      {current?.mine === true && (
        <Button
          variant="ghost"
          icon={<Trash2 size={16} />}
          busy={remove.isPending}
          onClick={() => remove.mutate(current)}
        >
          Delete Variant
        </Button>
      )}
      <ErrorAlert error={variants.error ?? remove.error} />
      {saving !== null && (
        <SaveDialog code={code} values={saving} onClose={() => setSaving(null)} />
      )}
    </div>
  );
}
