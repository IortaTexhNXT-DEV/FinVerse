import type { RecurringTemplate } from '@/api/journalAutomation';
import {
  describeSchedule,
  newTemplate,
  templateForm,
  templateProblems,
  toTemplateInput,
} from './recurringModel';

describe('recurring template model', () => {
  it('starts a new accrual template with two empty lines', () => {
    const t = newTemplate(3, 'PHP');
    expect(t.journalType).toBe('ACCRUAL');
    expect(t.dayOfMonth).toBe(31);
    expect(t.lines).toHaveLength(2);
    expect(templateProblems(t)).toEqual([
      'Enter a template name.',
      'Enter a narration.',
      'Debits and credits must be equal and greater than zero.',
    ]);
  });

  it('builds the request, dropping blank lines and optional fields', () => {
    const form = {
      ...newTemplate(3, 'PHP'),
      name: '  Rent  ',
      narration: 'Monthly rent',
      endDate: '',
      reference: ' ',
      lines: [
        { accountCode: '5603', side: 'DEBIT' as const, amount: 100 },
        { accountCode: '1111', side: 'CREDIT' as const, amount: 100 },
        { accountCode: '', side: 'DEBIT' as const, amount: 0 },
      ],
    };
    expect(templateProblems(form)).toEqual([]);
    const input = toTemplateInput(7, form);
    expect(input.companyId).toBe(7);
    expect(input.name).toBe('Rent');
    expect(input.endDate).toBeUndefined();
    expect(input.reference).toBeUndefined();
    expect(input.lines).toHaveLength(2);
    expect(templateProblems({ ...form, dayOfMonth: 40 })).toContain(
      'Day of month must be between 1 and 31.',
    );
  });

  it('reports non-positive lines instead of silently dropping them', () => {
    const form = {
      ...newTemplate(3, 'PHP'),
      name: 'Rent',
      narration: 'Rent',
      lines: [
        { accountCode: '5603', side: 'DEBIT' as const, amount: -100 },
        { accountCode: '1111', side: 'CREDIT' as const, amount: -100 },
      ],
    };
    expect(templateProblems(form)).toEqual([
      'Line 1: Amount must be greater than zero.',
      'Line 2: Amount must be greater than zero.',
      'Debits and credits must be equal and greater than zero.',
    ]);
    expect(toTemplateInput(1, form).lines).toHaveLength(2);
  });

  it('round-trips an existing template and describes schedules', () => {
    const template: RecurringTemplate = {
      id: 9,
      companyId: 1,
      branchId: 2,
      name: 'Rent',
      journalType: 'MANUAL',
      currency: 'PHP',
      narration: 'Rent',
      frequency: 'QUARTERLY',
      dayOfMonth: 15,
      startDate: '2026-01-01',
      autoReverse: false,
      autoSubmit: true,
      active: true,
      createdBy: 'accountant',
      lines: [],
    };
    const form = templateForm(template);
    expect(form.id).toBe(9);
    expect(form.reference).toBe('');
    expect(form.endDate).toBe('');
    expect(describeSchedule('QUARTERLY', 15)).toBe('Every quarter on day 15');
    expect(describeSchedule('MONTHLY', 31)).toBe('Every month on the last day');
    expect(describeSchedule('ANNUALLY', 1)).toBe('Every year on day 1');
  });
});
