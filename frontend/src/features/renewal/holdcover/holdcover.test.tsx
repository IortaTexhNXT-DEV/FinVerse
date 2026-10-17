import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { renewalHoldCoverRequestsApi } from '@/api/renewalHoldCoverRequests';
import { renewalWrapper } from '../testWrapper';
import { HoldCoverRequestsCard } from './HoldCoverRequestsCard';
import { InsurerAllocationCard } from './InsurerAllocationCard';

describe('Hold cover requests', () => {
  it('requests the default hold cover per insurer and records the approval', async () => {
    vi.spyOn(renewalHoldCoverRequestsApi, 'list').mockResolvedValue({
      defaultDays: 30,
      requests: [
        {
          requestNo: 'HCR-2026-000001',
          kind: 'REQUEST',
          insurerCode: 'INS001',
          share: 60,
          days: 30,
          start: '2026-10-31',
          end: '2026-11-30',
          status: 'REQUESTED',
          channel: 'MFT',
          batchNo: null,
          insurerRef: null,
          remarks: null,
          respondedAt: null,
          requestedBy: 'Paolo Reyes',
          requestedAt: '2026-10-01T02:00:00Z',
        },
      ],
    });
    const request = vi.spyOn(renewalHoldCoverRequestsApi, 'request').mockResolvedValue([]);
    const respond = vi.spyOn(renewalHoldCoverRequestsApi, 'respond').mockResolvedValue({} as never);
    const wrap = renewalWrapper(new Set(['RNW_PROCESS']));
    render(wrap(<HoldCoverRequestsCard renewalRef="RNW-2026-000101" />));
    expect(await screen.findByText('HCR-2026-000001')).toBeInTheDocument();
    expect(screen.getByText('31-Oct-2026')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Request Hold Cover (30 days)' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith(1, 'RNW-2026-000101'));
    fireEvent.click(screen.getByRole('button', { name: 'Actions for HCR-2026-000001' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Record Approval' }));
    await waitFor(() =>
      expect(respond).toHaveBeenCalledWith(1, 'RNW-2026-000101', 'HCR-2026-000001', {
        approved: true,
      }),
    );
  });

  it('shows the insurer shares with their premium', async () => {
    vi.spyOn(renewalHoldCoverRequestsApi, 'insurers').mockResolvedValue([
      { insurerCode: 'INS001', percent: 60, amount: 600000, premium: 6000 },
      { insurerCode: 'INS002', percent: 40, amount: 400000, premium: 4000 },
    ]);
    const wrap = renewalWrapper(new Set(['RNW_PROCESS']));
    render(wrap(<InsurerAllocationCard renewalRef="RNW-2026-000101" />));
    expect(await screen.findByText('600,000.00')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Save Allocation' })).toBeDisabled();
  });
});
