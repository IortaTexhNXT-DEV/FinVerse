import { setUserDirectory } from '@/api/users';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { renewalProposalApi } from '@/api/renewalProposal';
import type { CandidateDetail } from '@/api/renewal';
import { renewalWrapper } from '../testWrapper';
import KycDashboardPage from '../kyc/KycDashboardPage';
import RiskCodesPage from '../riskcodes/RiskCodesPage';
import { ProposalTab } from './ProposalTab';

const DETAIL = {
  row: { renewalRef: 'RNW-2026-000101', disposition: 'FOR_PROPOSAL' },
} as unknown as CandidateDetail;

describe('Proposals, KYC and risk codes', () => {
  it('generates a Full Proposal with the required signatories', async () => {
    vi.spyOn(renewalProposalApi, 'proposals').mockResolvedValue({
      requiredSignatories: 3,
      proposals: [],
    });
    vi.spyOn(renewalProposalApi, 'tsuRequests').mockResolvedValue([]);
    vi.spyOn(renewalProposalApi, 'users').mockResolvedValue([
      { username: 'mkttl', displayName: 'Maria Santos' },
    ]);
    const generate = vi.spyOn(renewalProposalApi, 'generate').mockResolvedValue({
      proposalNo: 'PRP-2026-000001',
      kind: 'FULL',
      fileName: 'Renewal_FullProposal_RNW-2026-000101_10102026.pdf',
      attachmentId: 9,
      signatories: 'mkttl',
      status: 'GENERATED',
      messageNo: null,
      generatedBy: 'ao',
      generatedAt: '2026-10-10T02:00:00Z',
    });
    const wrap = renewalWrapper(new Set(['RNW_DISPOSE', 'RNW_VIEW']));
    render(wrap(<ProposalTab detail={DETAIL} />));
    fireEvent.change(await screen.findByLabelText('Proposal'), { target: { value: 'FULL' } });
    expect(await screen.findByLabelText(/Signatory 3/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Generate Proposal Slip' }));
    await waitFor(() => expect(generate).toHaveBeenCalledWith(1, 'RNW-2026-000101', 'FULL', []));
  });

  it('opens the accounts of a KYC count', async () => {
    vi.spyOn(renewalProposalApi, 'kycDashboard').mockResolvedValue({
      due: 2,
      upcoming: 1,
      completed: 5,
      follow_up: 0,
    });
    const accounts = vi.spyOn(renewalProposalApi, 'kycAccounts').mockResolvedValue([
      {
        renewalRef: 'RNW-2026-000102',
        clientName: 'Juan Dela Cruz',
        kycReviewDue: '2026-10-20',
        status: 'KYC_DUE',
        accountOfficer: 'ao',
        expiryDate: '2026-12-31',
      },
    ]);
    setUserDirectory([{ username: 'ao', displayName: 'Ana Ocampo' }]);
    const wrap = renewalWrapper(new Set(['RNW_VIEW']));
    render(wrap(<KycDashboardPage />));
    expect(await screen.findByText('Juan Dela Cruz')).toBeInTheDocument();
    // the Account Officer by name, not by sign-in
    expect(screen.getByText('Ana Ocampo')).toBeInTheDocument();
    setUserDirectory([]);
    fireEvent.click(screen.getByText('Total Accounts Completed KYC'));
    await waitFor(() => expect(accounts).toHaveBeenCalledWith(1, 'KYC_COMPLETED'));
  });

  it('shows the risk codes read only without the setup function', async () => {
    vi.spyOn(renewalProposalApi, 'riskCodes').mockResolvedValue([
      {
        id: 1,
        riskCode: 'MC01',
        description: 'Motor commercial',
        lineCode: 'MOTOR',
        renewable: false,
        effectiveDate: '2026-11-01',
        remarks: null,
        status: 'ACTIVE',
        createdBy: 'badmin',
        createdAt: '2026-10-01T02:00:00Z',
        updatedBy: null,
        updatedAt: null,
      },
    ]);
    const wrap = renewalWrapper(new Set(['RNW_VIEW']));
    render(wrap(<RiskCodesPage />));
    expect(await screen.findByText('Non-Renewable')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add Risk Code' })).not.toBeInTheDocument();
  });
});
