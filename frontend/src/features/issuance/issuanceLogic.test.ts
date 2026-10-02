import { describe, expect, it } from 'vitest';
import type { Review } from '@/api/issuance';
import {
  addresses,
  issuanceTiles,
  matchText,
  numbersError,
  proposedNumbers,
  extractionNotes,
} from './issuanceLogic';

const review = (termYears: number, extracted: string[], onAccount: string[] = []): Review => ({
  epolicy: {
    id: 1,
    accountId: 1,
    arn: 'ARN-1',
    attachmentId: 1,
    fileName: 'e.pdf',
    matchMethod: 'MANUAL',
    status: 'REVIEW',
    extractedPolicyNumbers: extracted,
    policyNumbers: [],
    dispatchCount: 0,
    createdAt: '2026-09-24T00:00:00Z',
    createdBy: 'proc',
  },
  account: {
    accountId: 1,
    arn: 'ARN-1',
    clientName: 'Client',
    status: 'PLACED',
    productCode: 'PAR01',
    termYears,
    policyNumbers: onAccount,
  },
  differences: [],
});

describe('issuance logic', () => {
  it('builds one tile per tab and flags the review queue', () => {
    expect(issuanceTiles(undefined).map((t) => t.value)).toEqual([0, 0, 0, 0]);
    const tiles = issuanceTiles({
      awaitingPolicy: 1,
      toReview: 2,
      readyToDispatch: 3,
      adviceToGenerate: 4,
    });
    expect(tiles.map((t) => t.value)).toEqual([1, 2, 3, 4]);
    expect(tiles.filter((t) => t.alert).map((t) => t.tab)).toEqual(['REVIEW']);
  });

  it('proposes one policy number per policy year', () => {
    expect(proposedNumbers(review(1, ['P-1', 'P-2']))).toEqual(['P-1']);
    expect(proposedNumbers(review(3, ['P-1']))).toEqual(['P-1', '', '']);
    expect(proposedNumbers(review(2, [], ['A', 'B']))).toEqual(['A', 'B']);
    expect(proposedNumbers(review(0, []))).toEqual(['']);
  });

  it('validates the policy numbers and splits addresses', () => {
    expect(numbersError(['A', ' '])).toMatch(/one policy number/);
    expect(numbersError(['A', 'A'])).toMatch(/different/);
    expect(numbersError(['A', 'B'])).toBeUndefined();
    expect(addresses('a@b.ph; c@d.ph ,e@f.ph')).toEqual(['a@b.ph', 'c@d.ph', 'e@f.ph']);
    expect(addresses('  ')).toEqual([]);
  });
});

describe('how an e-policy was matched', () => {
  it('says how the e-policy was matched in words', () => {
    expect(matchText('MANUAL')).toBe('the account chosen at upload');
    expect(matchText('POLICY_NUMBER')).toBe('the policy number');
    expect(matchText(undefined)).toBe('—');
  });
});

describe('extractionNotes', () => {
  it('writes each note of an extraction as a sentence', () => {
    expect(extractionNotes('premium not found; period end not found')).toEqual([
      'Premium not found.',
      'Period end not found.',
    ]);
  });
});
