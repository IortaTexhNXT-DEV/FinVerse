import { RotateCw } from 'lucide-react';
import { ApiError } from '@/api/client';
import { businessText } from '@/utils/businessText';
import { fieldErrorLines } from '@/utils/fieldErrors';

/** Moves focus to the form control named after a field of a validation error, if on the page. */
function focusField(field: string): void {
  const name = field.replace(/\[\d+\].*$/, '').split('.').pop() ?? field;
  const control = document.querySelector<HTMLElement>(`[name="${CSS.escape(field)}"], [name="${CSS.escape(name)}"]`);
  control?.focus();
}

/**
 * Renders an error from the API (or any Error) in a consistent banner: the business message, the
 * field errors of a VALIDATION_FAILED response one per line (each linked to its field when the
 * field is on the page), and the reference support needs (error code and correlation id). Network
 * and server failures offer Retry. No stack traces, raw JSON or HTTP codes are shown.
 */
export function ErrorAlert({
  error,
  onRetry,
}: Readonly<{ error: unknown; onRetry?: () => void }>) {
  if (error === null || error === undefined) {
    return null;
  }
  const api = error instanceof ApiError ? error : undefined;
  const message = businessText(
    error instanceof Error ? error.message : 'The request could not be completed.',
  );
  const fields = api ? Object.keys(api.fieldErrors) : [];
  const lines = api ? fieldErrorLines(api.fieldErrors) : [];
  return (
    <div className={api?.retryable ? 'alert warning' : 'alert danger'} role="alert">
      <strong>{message}</strong>
      {lines.length > 0 && (
        <ul className="field-errors">
          {lines.map((line, i) => {
            const field = fields[i];
            return (
              <li key={line}>
                {field !== undefined && typeof document !== 'undefined' ? (
                  <button type="button" className="link-button" onClick={() => focusField(field)}>
                    {line}
                  </button>
                ) : (
                  line
                )}
              </li>
            );
          })}
        </ul>
      )}
      {api !== undefined && (
        <div className="alert-reference muted">
          Reference: {api.code}
          {api.reference !== undefined && ` · ${api.reference}`}
        </div>
      )}
      {api?.retryable && onRetry !== undefined && (
        <button type="button" className="btn btn-secondary btn-sm" onClick={onRetry}>
          <RotateCw size={14} aria-hidden="true" /> Retry
        </button>
      )}
    </div>
  );
}
