import { humanizeField } from '@/utils/fieldErrors';
import { Notice } from './Notice';

/** Focuses the first control marked invalid on the page (the field of the first error). */
function focusFirstInvalid(): void {
  document.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
}

interface FormErrorSummaryProps {
  /** The field errors of the form, by field name. */
  errors: Readonly<Record<string, string | undefined>>;
  /** Readable labels of the fields, when they differ from the field name. */
  labels?: Readonly<Record<string, string>>;
  title?: string;
}

/**
 * The error summary at the top of a long form: one error notice listing each field to correct,
 * while every error also stays inline under its field. Renders nothing without errors.
 */
export function FormErrorSummary({
  errors,
  labels = {},
  title = 'Correct the highlighted fields',
}: Readonly<FormErrorSummaryProps>) {
  const lines = Object.entries(errors)
    .filter((entry): entry is [string, string] => entry[1] !== undefined && entry[1] !== '')
    .map(([field, message]) => `${labels[field] ?? humanizeField(field)}: ${message}`);
  if (lines.length === 0) {
    return null;
  }
  return (
    <Notice
      tone="error"
      title={title}
      items={lines}
      actions={
        <button type="button" className="link-button" onClick={focusFirstInvalid}>
          Go to First Field
        </button>
      }
    />
  );
}
