import { useId } from 'react';
import type { ReactNode } from 'react';

interface FieldProps {
  label: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: (id: string) => ReactNode;
}

/** Labelled form field; passes a generated id to the control for label association. */
export function Field({ label, required = false, error, hint, children }: Readonly<FieldProps>) {
  const id = useId();
  return (
    <div className="field">
      <label htmlFor={id} className={required ? 'required' : undefined}>
        {label}
      </label>
      {children(id)}
      {hint !== undefined && error === undefined && <span className="muted">{hint}</span>}
      {error !== undefined && (
        <span className="field-error" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
