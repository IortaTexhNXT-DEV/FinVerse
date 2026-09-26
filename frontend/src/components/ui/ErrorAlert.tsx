import { ApiError } from '@/api/client';
import { fieldErrorLines } from '@/utils/fieldErrors';

/**
 * Renders an error from the API (or any Error) in a consistent alert. Field errors of a
 * VALIDATION_FAILED response are listed one per line ("Line 1 amount: must be greater than 0").
 */
export function ErrorAlert({ error }: Readonly<{ error: unknown }>) {
  if (error === null || error === undefined) {
    return null;
  }
  const message = error instanceof Error ? error.message : 'Unexpected error';
  const code = error instanceof ApiError ? error.code : undefined;
  const fields = error instanceof ApiError ? fieldErrorLines(error.fieldErrors) : [];
  return (
    <div className="alert danger" role="alert">
      <strong>{message}</strong>
      {fields.length > 0 && (
        <ul className="field-errors">
          {fields.map((line) => (
            <li key={line}>{line}</li>
          ))}
        </ul>
      )}
      {code !== undefined && <div className="muted">Reference: {code}</div>}
    </div>
  );
}
