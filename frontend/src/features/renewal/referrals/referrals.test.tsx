import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { renewalReferralsApi } from '@/api/renewalReferrals';
import type { ReferralView } from '@/api/renewalReferrals';
import { renewalWrapper } from '../testWrapper';
import ReferralsPage from './ReferralsPage';
import { SubmitPostingDialog } from './SubmitPostingDialog';

const PENDING: ReferralView = {
  id: 7,
  referralNo: 'TRQ-2026-000007',
  renewalRef: 'RNW-2026-000101',
  assuredName: 'Juan Dela Cruz',
  fromUnit: 'T-CBG1',
  toUnit: 'T-CORP1',
  justification: 'Group cover for the company',
  status: 'PENDING_ACCEPTANCE',
  statusLabel: 'Pending Acceptance',
  requestedBy: 'mkttl',
  requestedAt: '2026-10-01T02:00:00Z',
  decidedBy: null,
  decidedAt: null,
  decisionRemarks: null,
  nbArn: null,
  nbCreatedAt: null,
};

describe('Transfer Request Monitoring', () => {
  it('lists the requests with their status and lets the receiving unit reject with remarks', async () => {
    vi.spyOn(renewalReferralsApi, 'list').mockResolvedValue([
      PENDING,
      {
        ...PENDING,
        id: 8,
        referralNo: 'TRQ-2026-000008',
        status: 'ACCEPTED',
        statusLabel: 'Accepted',
        decidedBy: 'rnwtl',
        nbArn: 'ARN-2026-000900',
      },
    ]);
    const decide = vi.spyOn(renewalReferralsApi, 'decide').mockResolvedValue({
      ...PENDING,
      status: 'REJECTED',
      statusLabel: 'Rejected',
    });
    const wrap = renewalWrapper(new Set(['RNW_ASSIGN']), 'rnwtl');
    render(wrap(<ReferralsPage />));
    expect(await screen.findByText('TRQ-2026-000007')).toBeInTheDocument();
    expect(screen.getByText('Pending Acceptance')).toBeInTheDocument();
    expect(screen.getByText('ARN-2026-000900')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Actions for TRQ-2026-000007' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Reject' }));
    const confirm = await screen.findByRole('button', { name: 'Confirm' });
    expect(confirm).toBeDisabled();
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'Not our segment' } });
    fireEvent.click(confirm);
    await waitFor(() => expect(decide).toHaveBeenCalledWith(1, 7, 'REJECTED', 'Not our segment'));
  });
});

describe('Submit for Posting', () => {
  it('requires the approver when the setting asks for one', async () => {
    vi.spyOn(renewalReferralsApi, 'postingApprovers').mockResolvedValue({
      mode: 'REQUIRED',
      approvers: [{ username: 'mkttl', fullName: 'Maria Team Lead' }],
    });
    const onConfirm = vi.fn();
    const wrap = renewalWrapper(new Set(['RNW_DISPOSE']));
    render(
      wrap(
        <SubmitPostingDialog
          refs={['RNW-1']}
          busy={false}
          error={null}
          onClose={vi.fn()}
          onConfirm={onConfirm}
        />,
      ),
    );
    const submit = await screen.findByRole('button', { name: 'Submit for Posting' });
    await screen.findByRole('option', { name: 'Maria Team Lead' });
    expect(submit).toBeDisabled();
    fireEvent.change(screen.getByRole('combobox'), { target: { value: 'mkttl' } });
    fireEvent.click(submit);
    expect(onConfirm).toHaveBeenCalledWith('mkttl');
  });
});
