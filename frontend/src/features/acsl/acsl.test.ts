import { describe, expect, it, vi } from 'vitest';
import {
  CASE_TABS,
  assigneeField,
  caseRequestFacts,
  codesInWords,
  correctionKind,
  lineOriginLabel,
  originalLineActions,
  soaUploadProblems,
  correctionActions,
  emptyLine,
  hasVariance,
  lineErrors,
  tabParam,
  toDraft,
  toLine,
  totals,
  wrongAccountDrafts,
} from './acsl';
import type { AcslCase, OriginalLine } from './api';

const original: OriginalLine = {
  batchNo: 'PRM-1',
  journalType: 'PREMIUM',
  valueDate: '2026-01-15',
  lineNo: 1,
  accountCode: '1210.01',
  accountName: 'PR basic',
  side: 'DEBIT',
  amount: 1000,
  partyCode: 'CL-1',
};

describe('correction entries', () => {
  it('reverses a line and re-posts it to the right account', () => {
    const drafts = wrongAccountDrafts(original, '1210.06', 'INV-1');
    expect(drafts.map((d) => [d.accountCode, d.side, d.origin])).toEqual([
      ['1210.01', 'CREDIT', 'REVERSAL'],
      ['1210.06', 'DEBIT', 'REPOST'],
    ]);
    const lines = drafts.map(toLine);
    expect(totals(lines)).toEqual({ debit: 1000, credit: 1000, balanced: true });
    expect(lines[0]?.partyCode).toBe('CL-1');
    const repost = lines[1];
    expect(repost ? toDraft(repost).amount : '').toBe('1000');
  });

  it('checks balance and lines', () => {
    expect(totals([{ side: 'DEBIT', amount: 10 }]).balanced).toBe(false);
    expect(
      totals([
        { side: 'DEBIT', amount: 10.1 },
        { side: 'CREDIT', amount: 10.2 },
      ]).balanced,
    ).toBe(false);
    const errors = lineErrors([
      emptyLine(),
      { ...emptyLine(), accountCode: '4101', amount: '-1' },
      { ...emptyLine(), accountCode: '4101', amount: '5', component: 'BASIC' },
      { ...emptyLine(), accountCode: '4101', amount: '5' },
    ]);
    expect(Object.keys(errors)).toEqual(['0', '1', '2']);
    expect(
      toLine({ ...emptyLine('CREDIT'), accountCode: ' 4101 ', amount: '5' }).partyCode,
    ).toBeUndefined();
  });

  it('offers one button per business action', () => {
    expect(correctionActions(['submit', 'return', 'cancel', 'approve'])).toEqual([
      'submit',
      'approve',
    ]);
  });
});

describe('boards', () => {
  it('reads the tab of the URL and flags variances', () => {
    expect(tabParam('INVESTIGATING', CASE_TABS, 'ALL')).toBe('INVESTIGATING');
    expect(tabParam('X', CASE_TABS, 'ALL')).toBe('ALL');
    expect(hasVariance(0)).toBe(false);
    expect(hasVariance(-0.5)).toBe(true);
    expect(hasVariance(undefined)).toBe(false);
  });
});

describe('assignee and kind texts', () => {
  it('offers the assignees by name, sorted, never as a typed user ID', () => {
    const field = assigneeField('Processor', ['acsltl', 'acsl']);
    expect(field.label).toBe('Processor');
    expect(field.required).toBe(true);
    expect(field.options?.map((o) => o.value)).toHaveLength(2);
    expect(field.options?.every((o) => o.label.length > 0)).toBe(true);
    const labels = field.options?.map((o) => o.label) ?? [];
    expect([...labels].sort((a, b) => a.localeCompare(b))).toEqual(labels);
  });

  it('names the kind of a correction in words', () => {
    expect(correctionKind('WRONG_ACCOUNT')).toBe('Posting to a wrong GL account');
    expect(correctionKind('RECLASS')).toBe('Reclassification');
  });
});

describe('posted line row menu', () => {
  it('offers Correct only on a draft the user may edit', () => {
    const correct = vi.fn();
    const actions = originalLineActions(true, correct);
    expect(actions.map((a) => a.label)).toEqual(['Correct']);
    actions[0]?.onSelect('');
    expect(correct).toHaveBeenCalled();
    expect(originalLineActions(false, correct)).toEqual([]);
  });
});

describe('insurer statement upload', () => {
  it('asks to select the insurer by name, never for a code', () => {
    const found = soaUploadProblems('', '2026-09-01', '2026-09-30', undefined);
    expect(found.insurer).toBe('Select the insurer');
    expect(found.file).toBe('Choose the statement file');
    expect(soaUploadProblems('MGIC', '2026-09-30', '2026-09-01', undefined).to).toBe(
      'Enter an end on or after the start',
    );
  });
});

describe('case request facts', () => {
  it('shows the insurer by name', () => {
    const c = {
      subject: 'Premium variance',
      account: { insurerCode: 'INS-MGIC', clientCode: 'CL-2026-000001' },
    } as unknown as AcslCase;
    const facts = caseRequestFacts(c, (code) => (code === 'INS-MGIC' ? 'MAPFRE Insular' : code));
    expect(facts.find(([label]) => label === 'Insurer')?.[1]).toBe('MAPFRE Insular');
  });
});

describe('correction line origin', () => {
  it('names where a correction line comes from in words', () => {
    expect(lineOriginLabel('REVERSAL')).toBe('Reversal');
    expect(lineOriginLabel('REPOST')).toBe('Re-post');
    expect(lineOriginLabel('MANUAL')).toBe('Manual line of the preparer');
  });
});

describe('control account components', () => {
  it('reads the components in words', () => {
    expect(codesInWords('BASIC,PREMIUM_TAX_VAT')).toBe('Basic, Premium Tax VAT');
    expect(codesInWords(undefined)).toBe('—');
  });
});
