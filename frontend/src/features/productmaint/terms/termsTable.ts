import type { ClientResponseInput, OptionColumn, TermsAnswer, TermsTable } from '@/api/pmTerms';
import type { Tone } from '@/components/ui/statusTones';

/** Name of a column: the insurer and the option number. */
export function optionHeader(column: OptionColumn): string {
  return `${column.insurerName} – Option ${column.optionNo}`;
}

/** Tone of an insurer response. */
export function answerTone(answer: TermsAnswer | null): Tone {
  switch (answer) {
    case 'APPROVED':
      return 'success';
    case 'NOT_COVERED':
      return 'danger';
    case 'OTHERS':
      return 'warning';
    default:
      return 'neutral';
  }
}

/** Name of an insurer response; Others shows the insurer's own wording. */
export function answerName(column: OptionColumn, names: Record<string, string>): string {
  if (column.answer === null) {
    return 'Awaiting response';
  }
  const name = names[column.answer] ?? column.answer;
  return column.answer === 'OTHERS' && column.otherAnswer ? `${name}: ${column.otherAnswer}` : name;
}

/** The insurers that gave terms (responded and not Not Covered), each once. */
export function selectableInsurers(table: TermsTable): { code: string; name: string }[] {
  const seen = new Map<string, string>();
  for (const c of table.columns) {
    if (c.answer !== null && c.answer !== 'NOT_COVERED' && !seen.has(c.insurerCode)) {
      seen.set(c.insurerCode, c.insurerName);
    }
  }
  return [...seen].map(([code, name]) => ({ code, name }));
}

/** The Final Terms that changed, with blank values as empty text. */
export function changedTerms(
  saved: Record<string, string | null>,
  edited: Record<string, string>,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [key, value] of Object.entries(edited)) {
    if ((saved[key] ?? '') !== value) {
      out[key] = value;
    }
  }
  return out;
}

export interface OptionForm {
  answer: TermsAnswer | '';
  otherAnswer: string;
  values: Record<string, string>;
}

/** What is missing in a quotation option. */
export function optionProblems(
  form: OptionForm,
): Partial<Record<'answer' | 'otherAnswer', string>> {
  const problems: Partial<Record<'answer' | 'otherAnswer', string>> = {};
  if (form.answer === '') {
    problems.answer = 'Choose the insurer response';
  }
  if (form.answer === 'OTHERS' && form.otherAnswer.trim() === '') {
    problems.otherAnswer = "Enter the insurer's response";
  }
  return problems;
}

/** What is missing in a client response. */
export function clientResponseProblems(
  input: ClientResponseInput,
  today: string,
): Partial<Record<'remarks' | 'responseDate', string>> {
  const problems: Partial<Record<'remarks' | 'responseDate', string>> = {};
  if (input.response !== 'ACCEPTED' && !input.remarks?.trim()) {
    problems.remarks = "Enter the client's remarks";
  }
  if (input.responseDate && input.responseDate > today) {
    problems.responseDate = 'The response date cannot be in the future';
  }
  return problems;
}
