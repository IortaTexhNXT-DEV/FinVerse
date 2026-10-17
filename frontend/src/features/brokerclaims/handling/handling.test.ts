import { describe, expect, it } from 'vitest';
import { followUpReady, missingDocuments, toFollowUpInput } from './api';

describe('claim handling', () => {
  it('builds a follow-up from the dialog, leaving blank fields out', () => {
    const input = toFollowUpInput({
      type: 'APPOINT_ASSESSOR',
      actionTaken: 'Called the adjuster',
      nextPlanSummary: ' ',
      adjusterEvaluation: '42000',
    });
    expect(input.nextPlanSummary).toBeUndefined();
    expect(input.adjusterEvaluation).toBe(42000);
    expect(followUpReady(input)).toBe(true);
  });

  it('needs a type, the action taken and no negative amount', () => {
    expect(followUpReady({ type: 'X', actionTaken: '' })).toBe(false);
    expect(followUpReady({ type: '', actionTaken: 'Done' })).toBe(false);
    expect(followUpReady({ type: 'X', actionTaken: 'Done', insurerDeductible: -1 })).toBe(false);
  });

  it('lists the required documents not uploaded', () => {
    expect(
      missingDocuments([
        { documentType: 'A', label: 'Police report', required: true, uploaded: false },
        { documentType: 'B', label: 'Photos', required: false, uploaded: false },
        { documentType: 'C', label: 'License', required: true, uploaded: true },
      ]),
    ).toEqual(['Police report']);
  });
});
