import { noErrors, refErrors } from './locationLogic';

describe('insurer location reference rules', () => {
  it('asks for every field with the FRS message for the reference', () => {
    const errors = refErrors(' ', '', '', ' ');
    expect(errors.reference).toBe('Enter the insurer location reference');
    expect(errors.insurer).toBe('Select the insurer');
    expect(noErrors(errors)).toBe(false);
    expect(noErrors(refErrors('ARN-2026-940002', '1', 'INS-MGIC', 'A-LOC-0091'))).toBe(true);
  });
});
