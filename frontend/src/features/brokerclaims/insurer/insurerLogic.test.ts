import { ApiError } from '@/api/client';
import {
  addresses,
  adviceErrors,
  isEmail,
  isReuse,
  totalReserve,
  updateErrors,
} from './insurerLogic';

describe('insurer rules', () => {
  it('sums the reserve of the lines', () => {
    expect(
      totalReserve([
        { id: 1, insurerCode: 'INS-A', reserveAmount: 90000 },
        { id: 2, insurerCode: 'INS-B', reserveAmount: 60000 },
        { id: 3, insurerCode: 'INS-C' },
      ]),
    ).toBe(150000);
  });

  it('recognises a reused insurer claim number refusal', () => {
    expect(isReuse(new ApiError(422, { code: 'BCL_INSURER_CLAIM_NO_REUSED' }))).toBe(true);
    expect(isReuse(new ApiError(422, { code: 'OTHER' }))).toBe(false);
    expect(isReuse(new Error('x'))).toBe(false);
  });

  it('checks an insurer update', () => {
    expect(updateErrors('', '', ' ', '2026-09-26')).toEqual({
      date: 'Enter an update date that is not in the future',
      source: 'Select the source of the update',
      remarks: 'Enter the remarks',
    });
    expect(updateErrors('2026-09-27', 'EMAIL', 'x', '2026-09-26').date).toBeDefined();
    expect(updateErrors('2026-09-26', 'EMAIL', 'Adjuster appointed', '2026-09-26')).toEqual({
      date: undefined,
      source: undefined,
      remarks: undefined,
    });
  });

  it('checks the loss advice recipients', () => {
    expect(adviceErrors([], {})).toEqual(['Select at least one insurer']);
    expect(adviceErrors(['INS-B'], {})).toEqual(['Enter the recipient address for INS-B']);
    expect(adviceErrors(['INS-B'], { 'INS-B': 'claims@' })).toEqual([
      'Invalid e-mail address: claims@',
    ]);
    expect(adviceErrors(['INS-A'], { 'INS-A': 'a@x.ph; b@y.ph' })).toEqual([]);
    expect(addresses(' a@x.ph ,b@y.ph ')).toEqual(['a@x.ph', 'b@y.ph']);
    expect(isEmail('no-at.ph')).toBe(false);
    expect(isEmail('a@b@c.ph')).toBe(false);
    expect(isEmail('a@host')).toBe(false);
  });
});
