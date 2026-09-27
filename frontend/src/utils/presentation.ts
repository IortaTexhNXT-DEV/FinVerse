import type { ReactNode } from 'react';
import { extensionOf } from './files';

/** Whether a displayed value counts as empty: null, undefined, '' or false (shown as a dash). */
export function isEmptyValue(value: ReactNode): boolean {
  return value === null || value === undefined || value === '' || value === false;
}

const FORMAT_TOKENS = new Set([
  'dd',
  'mm',
  'mmm',
  'yyyy',
  'hh',
  'max',
  'maximum',
  'min',
  'minimum',
  'mb',
  'kb',
  'pdf',
  'xlsx',
  'xls',
  'ods',
  'csv',
  'txt',
  'zip',
  'json',
  'xml',
  'digit',
  'digits',
  'character',
  'characters',
  'chars',
  'format',
  'decimal',
  'decimals',
  'e.g.',
]);

/**
 * Whether a field hint is a short format hint ("dd-MMM-yyyy", "Max 10 MB, PDF", "2 decimals"),
 * kept visible under the field; any other guidance moves to the label's info tooltip.
 */
export function isFormatHint(hint: string): boolean {
  if (hint.length > 40) {
    return false;
  }
  if (/^\d/.test(hint) || hint.includes('%')) {
    return true;
  }
  return hint
    .toLowerCase()
    .split(/[^a-z.]+/)
    .some((token) => FORMAT_TOKENS.has(token) || /^\.[a-z]{3,4}$/.test(token));
}

const MIME_NAMES: Record<string, string> = {
  'application/pdf': 'PDF',
  'text/csv': 'CSV',
  'text/plain': 'TXT',
};

/** "XLSX, ODS or CSV" from ".xlsx,.ods,.csv"; empty when any type is accepted. */
export function acceptedTypesText(accept: string | undefined): string {
  if (!accept) {
    return '';
  }
  const names = accept
    .split(',')
    .map((a) => a.trim())
    .filter((a) => a !== '')
    .map((a) => {
      if (a.startsWith('.')) {
        return a.slice(1).toUpperCase();
      }
      if (a.startsWith('image/')) {
        return 'Images';
      }
      return MIME_NAMES[a] ?? a;
    })
    .filter((a, i, all) => all.indexOf(a) === i);
  if (names.length <= 1) {
    return names.join('');
  }
  return `${names.slice(0, -1).join(', ')} or ${names[names.length - 1] ?? ''}`;
}

/** Whether a file matches an accept list (extensions, MIME types or "image/*"). */
export function acceptsFile(file: { name: string; type: string }, accept?: string): boolean {
  if (!accept) {
    return true;
  }
  const ext = `.${extensionOf(file.name)}`;
  return accept
    .split(',')
    .map((a) => a.trim().toLowerCase())
    .some(
      (a) =>
        a === ext ||
        a === file.type ||
        (a.endsWith('/*') && file.type.startsWith(a.slice(0, -1))) ||
        (a === 'text/csv' && ext === '.csv'),
    );
}
