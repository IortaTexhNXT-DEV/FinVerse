import { Info } from 'lucide-react';
import { useId } from 'react';
import type { ReactNode } from 'react';
import { isFormatHint } from '@/utils/presentation';

interface FieldProps {
  label: string;
  required?: boolean;
  error?: string;
  /**
   * Guidance for the field. A short format hint ("dd-MMM-yyyy", "Max 10 MB, PDF") shows under the
   * field; any other guidance moves to an info icon tooltip on the label (no explanatory helper
   * text under fields on business screens).
   */
  hint?: string;
  children: (id: string) => ReactNode;
}

/** Info icon with a tooltip: guidance kept off the screen, available on hover and focus. */
export function InfoTip({ text }: Readonly<{ text: string }>) {
  return (
    <button type="button" className="info-tip" aria-label={text} title={text}>
      <Info size={14} aria-hidden="true" />
    </button>
  );
}

/**
 * Labelled form field; passes a generated id to the control for label association. The error
 * shows inline under the field in the error colour.
 */
export function Field({ label, required = false, error, hint, children }: Readonly<FieldProps>) {
  const id = useId();
  const format = hint !== undefined && isFormatHint(hint);
  return (
    <div className="field">
      <span className="label-row">
        <label htmlFor={id} className={required ? 'required' : undefined}>
          {label}
        </label>
        {hint !== undefined && !format && <InfoTip text={hint} />}
      </span>
      {children(id)}
      {format && error === undefined && <span className="field-format">{hint}</span>}
      {error !== undefined && (
        <span className="field-error" role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
