import { deactivationChoices, deactivationProblems } from './deactivationForm';

const blank = { effectiveDate: '', reason: '', remarks: '', approver: '' };

describe('package deactivation form', () => {
  it('asks for the effective date, the reason and the approver', () => {
    expect(deactivationProblems(blank, '2026-10-09')).toEqual({
      effectiveDate: 'Enter the deactivation effective date',
      reason: 'Select the reason for deactivation',
      approver: 'Select the approver',
    });
  });

  it('allows today and future dates only', () => {
    const form = { ...blank, reason: 'LOW_UPTAKE', approver: 'tsuhead' };
    expect(deactivationProblems({ ...form, effectiveDate: '2026-10-08' }, '2026-10-09')).toEqual({
      effectiveDate: 'The deactivation effective date must be today or later',
    });
    expect(deactivationProblems({ ...form, effectiveDate: '2026-10-09' }, '2026-10-09')).toEqual(
      {},
    );
  });
});

describe('deactivation request choices', () => {
  const request = {
    id: 1,
    requestNo: 'PKD-2026-000001',
    productCode: 'MTR12',
    versionNo: 1,
    packageName: 'Motor package',
    effectiveDate: '2026-10-20',
    reason: 'LOW_UPTAKE',
    remarks: null,
    approver: 'tsuhead',
    status: 'PENDING' as const,
    statusLabel: 'Pending Approval',
    packageExpiryDate: null,
    decisionRemarks: null,
    decidedBy: null,
    decidedAt: null,
    expiryDate: null,
    requestedBy: 'mbs',
    requestedAt: '2026-10-09T01:00:00Z',
  };

  it('lets the approver decide, the requestor withdraw and a head reassign', () => {
    expect(deactivationChoices(request, 'TSUHEAD', false)).toEqual(['approve', 'reject']);
    expect(deactivationChoices(request, 'mbs', false)).toEqual(['cancel']);
    expect(deactivationChoices(request, 'tsulead', true)).toEqual(['reassign']);
  });

  it('offers nothing once decided', () => {
    expect(deactivationChoices({ ...request, status: 'APPROVED' }, 'tsuhead', true)).toEqual([]);
  });
});
