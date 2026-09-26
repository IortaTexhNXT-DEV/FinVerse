import { describe, expect, it } from 'vitest';
import type { MatchDetail, ScreeningMatch } from './api';
import {
  MATCH_TABS,
  comparisonRows,
  entryChanged,
  falsePositiveErrors,
  falsePositiveRequest,
  isOpen,
  matchedText,
  scoreError,
  scoreText,
} from './matchLogic';

function match(patch: Partial<ScreeningMatch> = {}): ScreeningMatch {
  return {
    id: 1,
    runId: 2,
    clientId: 3,
    clientCode: 'PR-2026-000123',
    clientName: 'Dela Cruz, Juan',
    entryId: 4,
    entryVersion: 1,
    entryName: 'Juan de la Cruz',
    sourceCode: 'AML_ADVISORY',
    listType: 'SANCTION',
    subjectType: 'INDIVIDUAL',
    score: 0.95,
    algorithm: 'PHONETIC',
    matchedFields: ['NAME', 'BIRTH_DATE'],
    caseThreshold: true,
    status: 'POTENTIAL',
    createdAt: '2026-09-26T01:30:00Z',
    ...patch,
  };
}

function detail(entryVersion?: number): MatchDetail {
  return {
    match: match(),
    client: {
      name: 'Dela Cruz, Juan',
      reference: 'PR-2026-000123',
      type: 'INDIVIDUAL',
      status: 'PROSPECT',
      birthDate: '1970-02-15',
      nationality: 'FILIPINO',
      ids: '123-456-789-000',
      aliases: [],
    },
    entry:
      entryVersion === undefined
        ? null
        : {
            name: 'Juan de la Cruz',
            reference: 'AML-900001',
            type: 'INDIVIDUAL',
            status: 'ACTIVE',
            birthDate: '1970-02-16',
            nationality: 'filipino',
            aliases: ['Juanito Dela Cruz'],
            list: 'AML_ADVISORY / SANCTION',
            entryVersion,
          },
  };
}

describe('screening match logic', () => {
  it('formats scores and matched fields', () => {
    expect(scoreText(0.95)).toBe('0.9500 (95%)');
    expect(matchedText(match())).toBe('Phonetic · Name, Birth Date');
    expect(matchedText(match({ matchedFields: [] }))).toBe('Phonetic');
    expect(MATCH_TABS.map((t) => t.id)).toEqual([
      'POTENTIAL',
      'TRUE_MATCH',
      'FALSE_POSITIVE',
      'ALL',
    ]);
  });

  it('validates the score filters', () => {
    expect(scoreError('')).toBeUndefined();
    expect(scoreError('0.85')).toBeUndefined();
    expect(scoreError('1.2')).toBe('Enter a score from 0 to 1');
    expect(scoreError('abc')).toBe('Enter a score from 0 to 1');
  });

  it('only undecided matches outside a case can be decided', () => {
    expect(isOpen(match())).toBe(true);
    expect(isOpen(match({ caseId: 9 }))).toBe(false);
    expect(isOpen(match({ status: 'FALSE_POSITIVE' }))).toBe(false);
  });

  it('requires a justification and evidence for a false positive', () => {
    const form = { justification: ' ', riskRating: '', removeWatchlistTag: false };
    expect(falsePositiveErrors(form, 0)).toEqual({
      justification: 'Enter the justification and attach the evidence',
      evidence: 'Attach at least one evidence document',
    });
    expect(falsePositiveErrors({ ...form, justification: 'x'.repeat(2001) }, 1)).toEqual({
      justification: 'The justification is limited to 2000 characters',
    });
    expect(falsePositiveErrors({ ...form, justification: 'Different person' }, 1)).toEqual({});
  });

  it('builds the false-positive request with the optional profile correction', () => {
    expect(
      falsePositiveRequest({
        justification: ' Different ',
        riskRating: '',
        removeWatchlistTag: false,
      }),
    ).toEqual({ justification: 'Different', riskRating: undefined, removeTags: undefined });
    expect(
      falsePositiveRequest({
        justification: 'Different',
        riskRating: 'STANDARD',
        removeWatchlistTag: true,
      }),
    ).toEqual({
      justification: 'Different',
      riskRating: 'STANDARD',
      removeTags: ['WATCHLIST_REVIEW'],
    });
  });

  it('compares the client and the entry side by side', () => {
    const rows = comparisonRows(detail(2));
    const byLabel = Object.fromEntries(rows.map((r) => [r.label, r]));
    expect(byLabel['Birth Date']).toMatchObject({
      client: '15-02-1970',
      entry: '16-02-1970',
      same: false,
    });
    expect(byLabel.Nationality?.same).toBe(true);
    expect(byLabel.Aliases?.entry).toBe('Juanito Dela Cruz');
    expect(byLabel['TIN / ID Numbers']?.entry).toBe('—');
    expect(entryChanged(detail(2))).toBe(true);
    expect(entryChanged(detail(1))).toBe(false);
    const noEntry = comparisonRows(detail());
    expect(noEntry.find((r) => r.label === 'Name')?.entry).toBe('—');
    expect(noEntry.find((r) => r.label === 'Birth Date')?.same).toBeUndefined();
    expect(entryChanged(detail())).toBe(false);
  });
});
