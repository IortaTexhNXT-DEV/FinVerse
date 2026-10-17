import { describe, expect, it } from 'vitest';
import { REINSURANCE_VALUES, withoutReinsurance } from '@/features/setup/reinsurance';
import { participationReady } from './coinsuranceForm';

describe('co-insurance participation', () => {
  it('needs an insurer, a share above 0 and up to 100, and a start date', () => {
    expect(participationReady({})).toBe(false);
    expect(
      participationReady({ participantCode: 'INS-A', sharePct: 60, effectiveFrom: '2026-01-01' }),
    ).toBe(true);
    expect(
      participationReady({ participantCode: 'INS-A', sharePct: 0, effectiveFrom: '2026-01-01' }),
    ).toBe(false);
    expect(
      participationReady({ participantCode: 'INS-A', sharePct: 101, effectiveFrom: '2026-01-01' }),
    ).toBe(false);
  });

  it('is not hidden with the deferred reinsurance functions', () => {
    expect(REINSURANCE_VALUES).not.toContain('COINSURER');
    expect(REINSURANCE_VALUES).not.toContain('COINSURANCE');
    expect(withoutReinsurance(['COINSURER', 'REINSURER'], () => false)).toEqual(['COINSURER']);
  });
});
