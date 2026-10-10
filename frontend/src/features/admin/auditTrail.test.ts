import { describe, expect, it } from 'vitest';
import { AUDIT_ACTIONS, dateRangeError, exportParams } from './auditTrail';

describe('audit trail filters', () => {
  it('refuses an end date before the start date with BDOI message', () => {
    expect(dateRangeError('2026-10-09', '2026-10-01')).toBe(
      'The end date must be on or after the start date',
    );
    expect(dateRangeError('2026-10-01', '2026-10-01')).toBeNull();
  });

  it('exports only the filters that are set, with the reference and the action', () => {
    expect(
      exportParams({
        from: '2026-10-01',
        to: '2026-10-09',
        username: '',
        entityType: '',
        entityId: ' AR-2026 ',
        action: 'TIMEOUT',
      }),
    ).toEqual({
      fromDate: '2026-10-01',
      toDate: '2026-10-09',
      reference: 'AR-2026',
      action: 'TIMEOUT',
    });
  });

  it("offers BDOI's action types including Inactivity and Timeout", () => {
    const labels = AUDIT_ACTIONS.map((a) => a.label);
    expect(labels).toEqual(expect.arrayContaining(['Login', 'Logout', 'Inactivity', 'Timeout']));
  });
});
