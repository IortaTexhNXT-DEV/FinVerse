import { render, screen } from '@testing-library/react';
import type { Claim, PolicyCover } from '@/api/claims';
import { ClaimOverview } from './ClaimOverview';
import { PolicyCoverCard } from './PolicyCoverCard';

const claim: Claim = {
  id: 1,
  claimNo: 'CL-HO-2026-000001',
  companyId: 1,
  branchId: 1,
  status: 'REJECTED',
  statusReason: 'Excluded peril',
  closedOn: '2026-05-01',
  policyId: 3,
  policyNo: 'P-FIRE-HO-2026-000001',
  productCode: 'FIRE-COM',
  productName: 'Fire - Commercial',
  businessLine: 'FIRE',
  customerCode: 'C-000201',
  customerName: 'Luzon Steel',
  insuredName: 'Luzon Steel',
  sharePct: 60,
  coinsuranceLeader: true,
  coinsurerCode: 'CO-0001',
  sumInsured: 1_000_000,
  lossDate: '2026-04-02',
  reportedDate: '2026-04-05',
  natureOfLoss: 'Fire',
  causeOfLoss: 'Short circuit',
  lossLocation: 'Calamba',
  description: 'Warehouse fire',
  currency: 'PHP',
  claimantCode: 'C-000201',
  claimantName: 'Luzon Steel',
  totals: {
    estimateLoss: 0,
    estimateExpense: 0,
    estimateRecovery: 0,
    paidLoss: 0,
    paidExpense: 0,
    recovered: 0,
    outstandingLoss: 0,
    outstandingExpense: 0,
    recoveryOutstanding: 0,
    ourEstimate: 1234.5,
    ourPaid: 0,
    ourOutstanding: 0,
    ourRecovered: 0,
  },
  parties: [
    { role: 'SURVEYOR', partyCode: 'SV-0001', partyName: 'Adjusters Inc.', partyType: 'SURVEYOR' },
  ],
  createdBy: 'claims',
  createdAt: '2026-04-05T01:00:00Z',
};

describe('claim overview', () => {
  it('shows figures, coinsurance, decision reason and parties', () => {
    render(<ClaimOverview claim={claim} />);
    expect(screen.getByText('1,234.50')).toBeInTheDocument();
    expect(screen.getByText('60% (leader with CO-0001)')).toBeInTheDocument();
    expect(screen.getByText('Rejected: Excluded peril')).toBeInTheDocument();
    expect(screen.getByText('Adjusters Inc.')).toBeInTheDocument();
    expect(screen.getByText('Whole policy')).toBeInTheDocument();
  });

  it('summarises a looked-up policy and its cover', () => {
    const cover: PolicyCover = {
      policyId: 3,
      policyNo: 'P-1',
      status: 'APPROVED',
      productCode: 'MOTOR-PC',
      productName: 'Motor',
      businessLine: 'MOTOR',
      customerCode: 'C-000101',
      customerName: 'Juan',
      insuredName: 'Juan',
      periodFrom: '2026-01-01',
      periodTo: '2026-12-31',
      currency: 'PHP',
      sharePct: 100,
      coinsuranceLeader: false,
      inForce: false,
      risks: [],
    };
    render(<PolicyCoverCard cover={cover} />);
    expect(screen.getByText('Not in force at loss date')).toBeInTheDocument();
    expect(screen.getByText('100%')).toBeInTheDocument();
  });
});
