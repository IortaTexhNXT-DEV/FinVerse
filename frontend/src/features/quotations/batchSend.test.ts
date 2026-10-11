import { describe, expect, it } from 'vitest';
import { batchSendSummary, emailList } from './batchSend';

describe('batch send of quotations', () => {
  it('splits the further recipients', () => {
    expect(emailList('a@b.ph, c@d.ph;e@f.ph')).toEqual(['a@b.ph', 'c@d.ph', 'e@f.ph']);
    expect(emailList('  ')).toEqual([]);
  });

  it('lists the quotations not sent with their reason', () => {
    expect(batchSendSummary({ quotations: 2, clients: 1, references: ['Q1', 'Q2'] })).toBe(
      '2 quotations sent in 1 e-mail',
    );
    expect(
      batchSendSummary({
        quotations: 1,
        clients: 1,
        references: ['Q1'],
        notSent: [{ reference: 'Q2', reason: 'it is Draft' }],
      }),
    ).toBe('1 quotation sent in 1 e-mail; not sent: Q2 (it is Draft)');
  });
});
