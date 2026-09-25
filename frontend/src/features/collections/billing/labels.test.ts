import { describe, expect, it } from 'vitest';
import type { Statement } from './api';
import { cycleText, periodError, statementEmail, statusesOfTab } from './labels';

const SOA: Statement = {
  id: 1,
  soaNo: 'SOA-2026-000001',
  planId: 2,
  cycleSeq: 1,
  arn: 'ARN-2026-000001',
  clientCode: 'CL-1',
  assuredName: 'Juan Dela Cruz',
  currency: 'PHP',
  frequency: 'ANNUAL',
  cycleFrom: '2026-09-01',
  cycleTo: '2027-08-31',
  dueDate: '2026-09-01',
  total: 22268.75,
  paid: 0,
  balance: 22268.75,
  status: 'GENERATED',
  generatedBy: 'mktcoll',
  createdAt: '2026-09-25T00:00:00Z',
  lines: [],
};

describe('billing labels', () => {
  it('maps tabs to statuses', () => {
    expect(statusesOfTab('SENT')).toEqual(['SENT']);
  });

  it('proposes the e-mail of a statement', () => {
    const mail = statementEmail(SOA);
    expect(mail.subject).toBe('Statement of Account SOA-2026-000001 – Juan Dela Cruz');
    expect(mail.body).toContain('ARN-2026-000001');
    expect(mail.body).toContain('PHP 22,268.75');
    expect(cycleText(SOA)).toContain('–');
  });

  it('checks the billing run period', () => {
    expect(periodError('', '2026-09-30')).toBe('Enter both dates');
    expect(periodError('2026-09-30', '2026-09-01')).toBe('The period ends before it starts');
    expect(periodError('2026-09-01', '2026-09-30')).toBeUndefined();
  });
});
