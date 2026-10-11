import { describe, expect, it } from 'vitest';
import { SETTLEMENT_OR_STATUS_LABELS, batchOf, canIssueAgain } from './settlementOrsApi';

describe('Settlement ORs at Disbursement approval (FRS.CSH.07.01.01)', () => {
  it('shows the remittance batch of the OR and a label for every status', () => {
    expect(batchOf({ sourceRef: 'RMB-INS-MGIC-000012:COMMISSION' })).toBe('RMB-INS-MGIC-000012');
    expect(batchOf({ sourceRef: 'DP-000001' })).toBe('DP-000001');
    expect(SETTLEMENT_OR_STATUS_LABELS.PENDING).toBe('Waiting for Disbursement approval');
    expect(Object.keys(SETTLEMENT_OR_STATUS_LABELS)).toHaveLength(4);
  });

  it('issues again only an OR that could not be issued', () => {
    expect(canIssueAgain({ status: 'FAILED' })).toBe(true);
    expect(canIssueAgain({ status: 'PENDING' })).toBe(false);
    expect(canIssueAgain({ status: 'ISSUED' })).toBe(false);
  });
});
