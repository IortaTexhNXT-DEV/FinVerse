import { describe, expect, it } from 'vitest';
import type { FlagChips } from '@/api/renewalTypes';
import type { HoldCoverView } from '@/api/renewalHoldCover';
import { expiryChips } from './common/expiryChips';
import { holdCoverActions, holdCoverOpen, holdCoverStatusLabel } from './common/holdCover';

const flags: FlagChips = {
  urgent: false,
  returned: false,
  transferred: false,
  endorsed: false,
  claims: false,
  outstanding: false,
  kycDue: false,
  kycFlaggedAt: null,
  nrns: false,
  stp: false,
  locked: false,
  nfrSent: false,
};

const cover = (status: HoldCoverView['status']): HoldCoverView => ({
  status,
  insurerCode: 'INS-1',
  startDate: '2027-03-01',
  expiryDate: '2027-04-30',
  durationDays: 60,
  expiringPolicyNo: 'POL-1',
  remarks: null,
  requestedBy: 'ao',
  requestedAt: null,
  insurerRef: null,
  confirmedOn: null,
  conditions: null,
  cancelReason: null,
});

describe('hold cover of a renewal', () => {
  it('shows the confirmed hold cover and the closing letters as chips', () => {
    expect(expiryChips(flags)).toEqual([]);
    expect(expiryChips({ ...flags, holdCoverUntil: '2027-04-30' })[0]?.label).toBe(
      'HC confirmed to 30-Apr-2027',
    );
    expect(expiryChips({ ...flags, closingRoute: 'NAL' })[0]?.label).toBe('NAL due');
    expect(expiryChips({ ...flags, closingRoute: 'NRL' })[0]?.label).toBe('NRL due');
    expect(expiryChips({ ...flags, closingRoute: 'NRL', closingLetter: 'NFR' })[0]?.label).toBe(
      'NRL Sent',
    );
    expect(expiryChips({ ...flags, attention: 'Ageing' })[0]?.label).toBe('Attention: Ageing');
  });

  it('offers a new request only without an open hold cover', () => {
    const all = () => true;
    expect(holdCoverActions(null, true, all)).toEqual({
      request: true,
      confirm: false,
      cancel: false,
    });
    expect(holdCoverActions(cover('REQUESTED'), true, all)).toEqual({
      request: false,
      confirm: true,
      cancel: true,
    });
    expect(holdCoverActions(cover('CONFIRMED'), true, all).request).toBe(false);
    expect(holdCoverActions(cover('CANCELLED'), true, all).request).toBe(true);
    expect(holdCoverActions(null, false, all).request).toBe(false);
    const aoOnly = (p: string) => p === 'RNW_DISPOSE';
    expect(holdCoverActions(cover('REQUESTED'), true, aoOnly).confirm).toBe(false);
    expect(holdCoverOpen(cover('DECLINED'))).toBe(false);
    expect(holdCoverStatusLabel('CANCELLED')).toBe('Cancelled');
  });
});
