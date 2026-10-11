import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { InsurerSelect } from '../common/InsurerSelect';

/** A field of a setup form. */
export interface FormField {
  key: string;
  label: string;
  kind: 'text' | 'number' | 'lov' | 'insurer' | 'bool' | 'choice';
  lov?: string;
  choices?: Record<string, string>;
  required?: boolean;
}

type Values = Record<string, string | boolean>;

function initial(fields: readonly FormField[]): Values {
  return Object.fromEntries(fields.map((f) => [f.key, f.kind === 'bool' ? false : '']));
}

function toBody(fields: readonly FormField[], values: Values): Record<string, unknown> {
  return Object.fromEntries(
    fields.map((f) => {
      const v = values[f.key];
      if (f.kind === 'bool') {
        return [f.key, v === true];
      }
      if (v === '') {
        return [f.key, null];
      }
      return [f.key, f.kind === 'number' ? Number(v) : v];
    }),
  );
}

/** A new setup record, saved pending approval by a checker. */
export function RowForm({
  title,
  fields,
  save,
  onSaved,
}: Readonly<{
  title: string;
  fields: readonly FormField[];
  save: (body: Record<string, unknown>) => Promise<unknown>;
  onSaved: () => void;
}>) {
  const toast = useToast();
  const [values, setValues] = useState<Values>(() => initial(fields));
  const [touched, setTouched] = useState(false);
  const missing = fields.filter((f) => f.required === true && values[f.key] === '');
  const submit = useMutation({
    mutationFn: () => save(toBody(fields, values)),
    onSuccess: () => {
      toast.success('Saved: waiting for approval');
      setValues(initial(fields));
      setTouched(false);
      onSaved();
    },
  });
  const set = (key: string, v: string | boolean) => setValues((cur) => ({ ...cur, [key]: v }));
  return (
    <Card title={title}>
      <ErrorAlert error={submit.error} />
      <div className="form-grid">
        {fields.map((f) => (
          <Field
            key={f.key}
            label={f.label}
            required={f.required}
            error={
              touched && missing.includes(f) ? `Enter the ${f.label.toLowerCase()}` : undefined
            }
          >
            {(id) => {
              const v = values[f.key];
              const text = typeof v === 'string' ? v : '';
              switch (f.kind) {
                case 'lov':
                  return (
                    <LovSelect
                      id={id}
                      type={f.lov ?? ''}
                      value={text}
                      placeholder="Any"
                      onChange={(c) => set(f.key, c)}
                    />
                  );
                case 'insurer':
                  return <InsurerSelect id={id} value={text} onChange={(c) => set(f.key, c)} />;
                case 'bool':
                  return (
                    <input
                      id={id}
                      type="checkbox"
                      checked={v === true}
                      onChange={(e) => set(f.key, e.target.checked)}
                    />
                  );
                case 'choice':
                  return (
                    <select
                      id={id}
                      className="select"
                      value={text}
                      onChange={(e) => set(f.key, e.target.value)}
                    >
                      <option value="">Select…</option>
                      {Object.entries(f.choices ?? {}).map(([code, label]) => (
                        <option key={code} value={code}>
                          {label}
                        </option>
                      ))}
                    </select>
                  );
                default:
                  return (
                    <input
                      id={id}
                      className="input"
                      inputMode={f.kind === 'number' ? 'decimal' : undefined}
                      value={text}
                      onChange={(e) => set(f.key, e.target.value)}
                      onBlur={() => setTouched(true)}
                    />
                  );
              }
            }}
          </Field>
        ))}
      </div>
      <div className="form-actions">
        <Button
          disabled={submit.isPending}
          onClick={() => {
            setTouched(true);
            if (missing.length === 0) {
              submit.mutate();
            }
          }}
        >
          Save
        </Button>
      </div>
    </Card>
  );
}
