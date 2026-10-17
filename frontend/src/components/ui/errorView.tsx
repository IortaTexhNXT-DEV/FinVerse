import type { ReactNode } from 'react';
import { ApiError, NETWORK_STATUS } from '@/api/client';
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

/** The field errors of a validation failure, each linked to its field (the error summary). */
function fieldItems(errors: Record<string, string>): ReactNode[] {
  const fields = Object.keys(errors);
  return fieldErrorLines(errors).map((line, i) => {
    const field = fields[i];
    return field === undefined ? (
      line
    ) : (
      <button key={line} type="button" className="link-button" onClick={() => focusField(field)}>
        {line}
      </button>
    );
  });
}

const UUID = /\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b/gi;
const SUPPORT_SENTENCE = / ?Quote reference [^ ]* to support\.?/gi;
const TECHNICAL_CODE = /\b[A-Z][A-Z0-9]+(?:_[A-Z0-9]+)+\b(?:-\S+)?/g;

/**
 * A message as the business reads it: requirement references, identifiers, technical error codes
 * and "quote reference" sentences removed.
 */
export function plainMessage(message: string): string {
  return businessText(
    message.replace(SUPPORT_SENTENCE, '').replace(UUID, '').replace(TECHNICAL_CODE, ''),
  )
    .replace(/ {2,}/g, ' ')
    .replace(/ ([.,;:])/g, '$1')
    .trim();
}

/** The parts of a business message: the lead and, for "Complete: a; b; c", one item each. */
export function messageParts(message: string): { lead: string; items: string[] } {
  const colon = message.indexOf(': ');
  if (colon > 0) {
    const items = message
      .slice(colon + 2)
      .split(/;\s*/)
      .map((i) => i.trim().replace(/\.$/, ''))
      .filter((i) => i !== '');
    if (items.length > 1 || message.slice(colon + 2).includes(';')) {
      return { lead: message.slice(0, colon), items };
    }
  }
  return { lead: message, items: [] };
}

/** What an error notice shows. */
export interface ErrorView {
  title: string;
  text?: string;
  items: ReactNode[];
  /** Support reference: unexpected system errors only. */
  reference?: string;
  retryable: boolean;
}

/** An unexpected failure: a friendly message and, for support, the reference behind Details. */
function systemFailure(error: ApiError, title: string | undefined): ErrorView {
  if (error.status === NETWORK_STATUS) {
    return {
      title: title ?? 'The system could not be reached',
      text: 'Check your connection and try again.',
      items: [],
      retryable: true,
    };
  }
  return {
    title: title ?? 'The system could not complete the request',
    text: 'Try again. If it continues, contact support with the reference in Details.',
    items: [],
    reference: error.reference,
    retryable: true,
  };
}

/** A business message: bold title, then the message and its items. */
function businessView(message: string, title: string | undefined): ErrorView {
  const { lead, items } = messageParts(plainMessage(message));
  if (title !== undefined) {
    return { title, text: lead, items, retryable: false };
  }
  return { title: lead, items, retryable: false };
}

/**
 * What to show for an error: business-rule refusals and validation failures show their business
 * message only (no code or reference); network and server failures show a friendly message with
 * the support reference behind Details.
 */
export function errorView(error: unknown, title?: string): ErrorView {
  if (!(error instanceof ApiError)) {
    const message = error instanceof Error ? error.message : '';
    return businessView(message || 'The request could not be completed.', title);
  }
  if (error.retryable) {
    return systemFailure(error, title);
  }
  const fields = fieldItems(error.fieldErrors);
  if (error.code === 'VALIDATION_FAILED' || fields.length > 0) {
    return {
      title: title ?? 'Check the highlighted fields',
      text: fields.length > 0 ? undefined : plainMessage(error.message),
      items: fields,
      retryable: false,
    };
  }
  return businessView(error.message, title);
}
