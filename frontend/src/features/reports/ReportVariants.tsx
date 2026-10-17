import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save, Trash2 } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { nbReportsApi } from '@/api/nbReports';
import type { ReportVariant } from '@/api/nbReports';
import { displayNameOf } from '@/api/users';
import { Button } from '@/components/ui/Button';
import { ConfirmButton } from '@/components/ui/ConfirmButton';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { variantGroups } from './variantGroups';

interface ReportVariantsProps {
  code: string;
  /** Title of the report, named in the save dialog. */
  title: string;
  /** Current parameter values (saved as the variant). */
  values: () => Record<string, string>;
  /** Applies the saved values of a variant to the form (keywords not yet resolved). */
  onApply: (values: Record<string, string>) => void;
}

function SaveDialog({
  code,
  title,
  values,
  onClose,
}: Readonly<{
  code: string;
  title: string;
  values: Record<string, string>;
  onClose: () => void;
}>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [shared, setShared] = useState(false);
  const [defaultVariant, setDefaultVariant] = useState(false);
  const [checked, setChecked] = useState(false);
  const save = useMutation({
    mutationFn: () =>
      nbReportsApi.saveVariant({
        reportCode: code,
        name: name.trim(),
        parameters: values,
        shared,
        defaultVariant,
      }),
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
      size="md"
      title={`Save Variant of ${title}`}
      helper="A variant keeps the current parameters under a name, to run the report with them again."
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
        <ErrorAlert error={save.error} title="Cannot save the variant" />
        <Field
          label="Variant Name"
          required
          error={checked && missing ? 'Enter the name of the variant.' : undefined}
          hint="Saving under a name you already use replaces that variant."
        >
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={80}
              value={name}
              placeholder="e.g. My Branch – Overdue Only"
              onChange={(e) => setName(e.target.value)}
            />
          )}
        </Field>
        <fieldset className="variant-options">
          <legend>Who sees it</legend>
          <label className="checkbox">
            <input
              type="radio"
              name="variant-visibility"
              checked={!shared}
              onChange={() => setShared(false)}
            />
            Only me
          </label>
          <label className="checkbox">
            <input
              type="radio"
              name="variant-visibility"
              checked={shared}
              onChange={() => setShared(true)}
            />
            Every user who may run this report
          </label>
        </fieldset>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={defaultVariant}
            onChange={(e) => setDefaultVariant(e.target.checked)}
          />
          Open the report with this variant
        </label>
      </div>
    </Modal>
  );
}

/** One option of the variant list: its name, and who shared it when it is not the user's. */
function optionText(v: ReportVariant): string {
  if (v.standard || v.mine) {
    return v.defaultVariant && v.mine ? `${v.name} (default)` : v.name;
  }
  return `${v.name} (shared by ${displayNameOf(v.owner)})`;
}

/**
 * Report variants (BRNB.057): the standard variants of the report (configuration, for everyone)
 * and the user's own and shared ones. Choosing one fills the parameters; the current parameters
 * are saved as a variant (private or shared, optionally the user's default); the user deletes
 * only his own variants. The report opens with the user's default variant, else the first standard
 * one.
 */
export function ReportVariants({ code, title, values, onApply }: Readonly<ReportVariantsProps>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [selected, setSelected] = useState('');
  const [saving, setSaving] = useState<Record<string, string> | null>(null);
  const applied = useRef<string | null>(null);
  const variants = useQuery({
    queryKey: ['report-variants', code],
    queryFn: () => nbReportsApi.variants(code),
  });
  const groups = variantGroups(variants.data ?? []);
  useEffect(() => {
    // The user's default variant, else the first standard variant of the report.
    const preferred =
      variants.data?.find((v) => v.mine && !v.standard && v.defaultVariant) ??
      variants.data?.find((v) => v.standard);
    if (preferred !== undefined && applied.current !== code) {
      applied.current = code;
      setSelected(String(preferred.id));
      onApply(preferred.parameters);
    }
  }, [code, variants.data, onApply]);
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
      <Field label="Variant">
        {(id) => (
          <select
            id={id}
            className="select"
            value={selected}
            disabled={variants.isLoading}
            onChange={(e) => {
              setSelected(e.target.value);
              const v = variants.data?.find((x) => String(x.id) === e.target.value);
              if (v !== undefined) {
                onApply(v.parameters);
              }
            }}
          >
            <option value="">{variants.isLoading ? 'Loading…' : 'Choose a variant'}</option>
            {groups.sections.map((section) => (
              <optgroup key={section.label} label={section.label}>
                {section.variants.map((v) => (
                  <option key={v.id} value={v.id}>
                    {optionText(v)}
                  </option>
                ))}
              </optgroup>
            ))}
          </select>
        )}
      </Field>
      <div className="report-variant-actions">
        <Button variant="secondary" icon={<Save size={16} />} onClick={() => setSaving(values())}>
          Save Variant
        </Button>
        {current?.mine === true && !current.standard && (
          <ConfirmButton
            variant="ghost"
            icon={<Trash2 size={16} />}
            busy={remove.isPending}
            confirm={{
              title: `Delete Variant ${current.name}`,
              effect:
                'Your saved variant of the report is deleted; the report itself is unchanged.',
              destructive: true,
            }}
            onConfirm={() => remove.mutateAsync(current)}
          >
            Delete Variant
          </ConfirmButton>
        )}
      </div>
      {!variants.isLoading && groups.mine.length === 0 && (
        <p className="report-variant-hint muted">
          Save the current parameters as a variant to reuse them.
        </p>
      )}
      <ErrorAlert error={variants.error ?? remove.error} />
      {saving !== null && (
        <SaveDialog code={code} title={title} values={saving} onClose={() => setSaving(null)} />
      )}
    </div>
  );
}
