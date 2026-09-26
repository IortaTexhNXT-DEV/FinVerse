import { Info } from 'lucide-react';
import { useId } from 'react';
import type { ReactNode } from 'react';

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

const FORMAT_WORDS =
  /\b(dd|mm|yyyy|MMM|HH|max(imum)?|min(imum)?|MB|KB|PDF|XLSX?|CSV|TXT|ZIP|JSON|XML|digits?|characters?|chars|format|e\.g\.|%|decimals?)\b|^\d|\.[a-z]{3,4}\b/i;

/** Whether a hint is a short format hint kept visible under the field. */
export function isFormatHint(hint: string): boolean {
  return hint.length <= 40 && FORMAT_WORDS.test(hint);
}

/** Info icon with a tooltip: guidance moved off the screen, still available on hover and focus. */
export function InfoTip({ text }: Readonly<{ text: string }>) {
  return (
    <span className="info-tip" role="img" aria-label={text} title={text} tabIndex={0}>
      <Info size={14} aria-hidden="true" />
    </span>
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
