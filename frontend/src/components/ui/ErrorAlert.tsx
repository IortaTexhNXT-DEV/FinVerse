import { RotateCw } from 'lucide-react';
import { errorView } from './errorView';
import { Notice } from './Notice';

interface ErrorAlertProps {
  error: unknown;
  /** Retry for network and server failures. */
  onRetry?: () => void;
  /** Bold short title naming what failed ("Cannot submit for ManCom sign-off"). */
  title?: string;
}

/**
 * An error in the notice standard: the bold title, the business message with one bullet per
 * missing item or field (each field linked to its control), and, for unexpected system errors
 * only, the support reference behind Details and Retry. No stack traces, raw JSON, HTTP codes,
 * technical error codes or identifiers are shown.
 */
export function ErrorAlert({ error, onRetry, title }: Readonly<ErrorAlertProps>) {
  if (error === null || error === undefined) {
    return null;
  }
  const view = errorView(error, title);
  return (
    <Notice
      tone="error"
      title={view.title}
      items={view.items}
      reference={view.reference}
      actions={
        view.retryable && onRetry !== undefined ? (
          <button type="button" className="btn btn-secondary btn-sm" onClick={onRetry}>
            <RotateCw size={14} aria-hidden="true" /> Retry
          </button>
        ) : undefined
      }
    >
      {view.text}
    </Notice>
  );
}
