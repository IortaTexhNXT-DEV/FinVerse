import { describe, expect, it } from 'vitest';
import { reasonText } from './validationOutcome';

const labels: Record<string, string> = { INCOMPLETE_DETAILS: 'Incomplete or incorrect details' };
const labelOf = (code: string) => labels[code] ?? null;

describe('return reason text', () => {
  it('reads the reason of the list by its label, then the comment', () => {
    expect(reasonText('INCOMPLETE_DETAILS: The rate differs.', labelOf)).toBe(
      'Incomplete or incorrect details: The rate differs.',
    );
    expect(reasonText('INCOMPLETE_DETAILS', labelOf)).toBe('Incomplete or incorrect details');
  });

  it('keeps a free-text reason as it was written', () => {
    expect(reasonText('Returned for walkthrough B', labelOf)).toBe('Returned for walkthrough B');
    expect(reasonText(undefined, labelOf)).toBe('');
  });
});
