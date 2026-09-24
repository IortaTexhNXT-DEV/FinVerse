import { inForce, instructionErrors } from './instructionRules';

describe('special instructions', () => {
  it('requires type, text and ordered dates', () => {
    expect(instructionErrors({ type: '', text: ' ', effectiveFrom: '' })).toEqual({
      type: 'Select the instruction type',
      text: 'Enter the instruction',
      effectiveFrom: 'Enter the first day',
    });
    expect(
      instructionErrors({
        type: 'BILLING',
        text: 'x',
        effectiveFrom: '2026-02-01',
        effectiveTo: '2026-01-31',
      }),
    ).toEqual({ effectiveTo: 'The end date must not be before the start date' });
    expect(
      instructionErrors({
        type: 'BILLING',
        text: 'x',
        effectiveFrom: '2026-02-01',
        effectiveTo: '',
      }),
    ).toEqual({});
  });

  it('knows when an instruction is in force', () => {
    const i = { active: true, effectiveFrom: '2026-01-01', effectiveTo: '2026-01-31' };
    expect(inForce(i, '2026-01-15')).toBe(true);
    expect(inForce(i, '2026-02-01')).toBe(false);
    expect(inForce({ ...i, effectiveTo: undefined }, '2030-01-01')).toBe(true);
    expect(inForce({ ...i, active: false }, '2026-01-15')).toBe(false);
  });
});
