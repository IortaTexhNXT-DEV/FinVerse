import type { Frequency, RecurringTemplate, RecurringTemplateInput } from '@/api/journalAutomation';
import { emptyLine, isBlankLine, lineProblems, totals } from '@/features/gl/journalMath';
import { today } from '@/utils/format';

export const FREQUENCIES: Frequency[] = ['MONTHLY', 'QUARTERLY', 'ANNUALLY'];

/** Form state of a recurring template (the id is set when editing). */
export interface TemplateForm extends Omit<RecurringTemplateInput, 'companyId'> {
  id?: number;
}

/** Values for a new template. */
export function newTemplate(branchId: number, currency: string): TemplateForm {
  return {
    branchId,
    name: '',
    journalType: 'ACCRUAL',
    currency,
    narration: '',
    reference: '',
    frequency: 'MONTHLY',
    dayOfMonth: 31,
    startDate: today(),
    endDate: '',
    autoReverse: true,
    autoSubmit: false,
    lines: [emptyLine('DEBIT'), emptyLine('CREDIT')],
  };
}

/** Form values of an existing template. */
export function templateForm(t: RecurringTemplate): TemplateForm {
  return {
    id: t.id,
    branchId: t.branchId,
    name: t.name,
    journalType: t.journalType,
    currency: t.currency,
    narration: t.narration,
    reference: t.reference ?? '',
    frequency: t.frequency,
    dayOfMonth: t.dayOfMonth,
    startDate: t.startDate,
    endDate: t.endDate ?? '',
    autoReverse: t.autoReverse,
    autoSubmit: t.autoSubmit,
    lines: t.lines,
  };
}

function blankToUndefined(value: string | undefined): string | undefined {
  return value === undefined || value.trim() === '' ? undefined : value;
}

/** Request body: blank placeholder lines dropped, blank optional fields omitted. */
export function toTemplateInput(companyId: number, f: TemplateForm): RecurringTemplateInput {
  return {
    companyId,
    branchId: f.branchId,
    name: f.name.trim(),
    journalType: f.journalType,
    currency: f.currency,
    narration: f.narration,
    reference: blankToUndefined(f.reference),
    frequency: f.frequency,
    dayOfMonth: f.dayOfMonth,
    startDate: f.startDate,
    endDate: blankToUndefined(f.endDate),
    autoReverse: f.autoReverse,
    autoSubmit: f.autoSubmit,
    lines: f.lines.filter((l) => !isBlankLine(l)),
  };
}

/** Problems that prevent saving (the server re-validates accounts and balance). */
export function templateProblems(f: TemplateForm): string[] {
  const problems: string[] = [];
  if (f.name.trim() === '') {
    problems.push('Enter a template name.');
  }
  if (f.narration.trim() === '') {
    problems.push('Enter a narration.');
  }
  if (f.dayOfMonth < 1 || f.dayOfMonth > 31) {
    problems.push('Day of month must be between 1 and 31.');
  }
  Object.entries(lineProblems(f.lines)).forEach(([index, problem]) => {
    problems.push(`Line ${Number(index) + 1}: ${problem.message}.`);
  });
  const t = totals(f.lines.filter((l) => !isBlankLine(l)));
  if (!t.balanced) {
    problems.push('Debits and credits must be equal and greater than zero.');
  }
  return problems;
}

/** Plain-language schedule, e.g. "Monthly on day 31 (month end)". */
export function describeSchedule(frequency: Frequency, dayOfMonth: number): string {
  const day = dayOfMonth >= 31 ? 'the last day' : `day ${dayOfMonth}`;
  const label: Record<Frequency, string> = {
    MONTHLY: 'Every month',
    QUARTERLY: 'Every quarter',
    ANNUALLY: 'Every year',
  };
  return `${label[frequency]} on ${day}`;
}
