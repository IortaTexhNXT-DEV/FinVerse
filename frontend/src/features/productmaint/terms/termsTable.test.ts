import { describe, expect, it } from 'vitest';
import type { OptionColumn, TermsTable } from '@/api/pmTerms';
import {
  answerName,
  answerTone,
  changedTerms,
  clientResponseProblems,
  optionHeader,
  optionProblems,
  selectableInsurers,
} from './termsTable';

const column = (code: string, optionNo: number, answer: OptionColumn['answer']): OptionColumn => ({
  insurerCode: code,
  insurerName: `${code} Insurance`,
  optionNo,
  answer,
  otherAnswer: answer === 'OTHERS' ? 'Subject to survey' : null,
  values: {},
  saved: optionNo > 1,
});

const lac2 = column('LAC', 2, 'OTHERS');
const vmi = column('VMI', 1, null);

const table: TermsTable = {
  requestNo: 'PRF-2026-000001',
  fields: ['PREMIUM'],
  shown: ['PREMIUM'],
  clientFields: ['PREMIUM'],
  qsValues: {},
  columns: [column('LAC', 1, 'APPROVED'), lac2, column('MGIC', 1, 'NOT_COVERED'), vmi],
  finalTerms: { PREMIUM: '110,000.00' },
  selectedInsurers: [],
};

describe('comparative table', () => {
  it('names the options and the responses', () => {
    expect(optionHeader(lac2)).toBe('LAC Insurance – Option 2');
    const names = { APPROVED: 'Approved', OTHERS: 'Others', NOT_COVERED: 'Not Covered' };
    expect(answerName(lac2, names)).toBe('Others: Subject to survey');
    expect(answerName(vmi, names)).toBe('Awaiting response');
    expect(answerTone('NOT_COVERED')).toBe('danger');
    expect(answerTone(null)).toBe('neutral');
  });

  it('lets only insurers that gave terms be selected, once each', () => {
    expect(selectableInsurers(table)).toEqual([{ code: 'LAC', name: 'LAC Insurance' }]);
  });

  it('sends only the changed Final Terms', () => {
    expect(changedTerms(table.finalTerms, { PREMIUM: '110,000.00', RATE: '0.35' })).toEqual({
      RATE: '0.35',
    });
  });

  it('asks for the response and the wording of Others', () => {
    expect(optionProblems({ answer: '', otherAnswer: '', values: {} })).toHaveProperty('answer');
    expect(optionProblems({ answer: 'OTHERS', otherAnswer: ' ', values: {} })).toHaveProperty(
      'otherAnswer',
    );
    expect(optionProblems({ answer: 'APPROVED', otherAnswer: '', values: {} })).toEqual({});
  });

  it('asks for remarks unless the client accepted, and no future date', () => {
    expect(clientResponseProblems({ response: 'RETURNED' }, '2026-10-09')).toHaveProperty(
      'remarks',
    );
    expect(clientResponseProblems({ response: 'ACCEPTED' }, '2026-10-09')).toEqual({});
    expect(
      clientResponseProblems(
        { response: 'REJECTED', remarks: 'Too dear', responseDate: '2026-10-10' },
        '2026-10-09',
      ),
    ).toEqual({ responseDate: 'The response date cannot be in the future' });
  });
});
