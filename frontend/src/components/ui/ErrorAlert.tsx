import { ApiError } from '@/api/client';

/** Renders an error from the API (or any Error) in a consistent alert. */
export function ErrorAlert({ error }: Readonly<{ error: unknown }>) {
  if (error === null || error === undefined) {
    return null;
  }
  const message = error instanceof Error ? error.message : 'Unexpected error';
  const code = error instanceof ApiError ? error.code : undefined;
  return (
    <div className="alert danger" role="alert">
      <strong>{message}</strong>
      {code !== undefined && <div className="muted">Reference: {code}</div>}
    </div>
  );
}
