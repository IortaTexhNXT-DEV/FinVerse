import { RotateCw } from 'lucide-react';
import { ApiError } from '@/api/client';
import { businessText } from '@/utils/businessText';
import { fieldErrorLines } from '@/utils/fieldErrors';

/** Moves focus to the form control named after a field of a validation error, if on the page. */
function focusField(field: string): void {
  const name =
    field
      .replace(/\[\d+\].*$/, '')
      .split('.')
      .pop() ?? field;
  const control = document.querySelector<HTMLElement>(
    `[name="${CSS.escape(field)}"], [name="${CSS.escape(name)}"]`,
  );
  control?.focus();
}

/** The field errors of a validation failure, each linked to its field. */
function FieldErrorList({ errors }: Readonly<{ errors: Record<string, string> }>) {
  const fields = Object.keys(errors);
  const lines = fieldErrorLines(errors);
  if (lines.length === 0) {
    return null;
  }
  return (
    <ul className="field-errors">
      {lines.map((line, i) => {
        const field = fields[i];
        return (
          <li key={line}>
            {field === undefined ? (
              line
            ) : (
              <button type="button" className="link-button" onClick={() => focusField(field)}>
                {line}
              </button>
            )}
          </li>
        );
      })}
    </ul>
  );
}

/** Error code and correlation id for support, and Retry for network and server failures. */
function SupportLine({ error, onRetry }: Readonly<{ error: ApiError; onRetry?: () => void }>) {
  return (
    <>
      <div className="alert-reference muted">
        Reference: {error.code}
        {error.reference === undefined ? '' : ` · ${error.reference}`}
      </div>
      {error.retryable && onRetry !== undefined && (
        <button type="button" className="btn btn-secondary btn-sm" onClick={onRetry}>
          <RotateCw size={14} aria-hidden="true" /> Retry
        </button>
      )}
    </>
  );
}

/**
 * Renders an error from the API (or any Error) in a consistent banner: the business message, the
 * field errors of a VALIDATION_FAILED response one per line (each linked to its field), and the
 * reference support needs (error code and correlation id). Network and server failures offer
 * Retry. No stack traces, raw JSON or HTTP codes are shown.
 */
export function ErrorAlert({ error, onRetry }: Readonly<{ error: unknown; onRetry?: () => void }>) {
  if (error === null || error === undefined) {
    return null;
  }
  if (!(error instanceof ApiError)) {
    const message = error instanceof Error ? error.message : 'The request could not be completed.';
    return (
      <div className="alert danger" role="alert">
        <strong>{businessText(message)}</strong>
      </div>
    );
  }
  return (
    <div className={error.retryable ? 'alert warning' : 'alert danger'} role="alert">
      <strong>{businessText(error.message)}</strong>
      <FieldErrorList errors={error.fieldErrors} />
      <SupportLine error={error} onRetry={onRetry} />
    </div>
  );
}
