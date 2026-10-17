import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { renewalPlacementApi } from '@/api/renewalPlacement';
import { renewalWrapper } from '../testWrapper';
import { EpolicyReceiptsCard } from './EpolicyReceiptsCard';
import { EpolicySendingCard } from './EpolicySendingCard';

describe('E-Policies', () => {
  it('lists the receipts and opens the records of one', async () => {
    vi.spyOn(renewalPlacementApi, 'receipts').mockResolvedValue([
      {
        receiptNo: 'EPR-2026-000001',
        receiptType: 'MFT',
        summaryFile: 'summary.txt',
        zipFile: 'policies.zip',
        status: 'SUCCESSFUL',
        remarks: '1 of 2 records unmatched',
        records: 2,
        matched: 1,
        receivedAt: '2026-10-01T02:00:00Z',
        receivedBy: 'MFT',
      },
    ]);
    const lines = vi.spyOn(renewalPlacementApi, 'receiptLines').mockResolvedValue([
      {
        seq: 2,
        renewalRef: 'RNW-NOT-THERE',
        policyNo: 'MC-PC-0',
        pdfFile: 'other.pdf',
        documentTag: 'Policy',
        matchStatus: 'UNMATCHED',
        remarks: 'Record not found',
        attachmentId: null,
      },
    ]);
    const wrap = renewalWrapper(new Set(['RNW_PROCESS']));
    render(wrap(<EpolicyReceiptsCard />));
    fireEvent.click(await screen.findByText('EPR-2026-000001'));
    expect(await screen.findByText('Record not found')).toBeInTheDocument();
    expect(lines).toHaveBeenCalledWith(1, 'EPR-2026-000001', '', '');
  });

  it('sends the eligible accounts only and shows the summary', async () => {
    vi.spyOn(renewalPlacementApi, 'forSending').mockResolvedValue([
      {
        renewalRef: 'RNW-1',
        clientName: 'Juan Dela Cruz',
        policyNo: 'P-1',
        email: 'juan@client.example.ph',
        fileName: 'P-1.pdf',
        attachmentId: 3,
        problem: null,
      },
      {
        renewalRef: 'RNW-2',
        clientName: 'Maria Santos',
        policyNo: null,
        email: null,
        fileName: null,
        attachmentId: null,
        problem: 'No valid policy number',
      },
    ]);
    const send = vi.spyOn(renewalPlacementApi, 'sendEpolicies').mockResolvedValue({
      selected: 1,
      submitted: 1,
      failed: 0,
      pending: 0,
      messages: {},
    });
    const wrap = renewalWrapper(new Set(['RNW_PROCESS']));
    render(wrap(<EpolicySendingCard />));
    expect(await screen.findByText('No valid policy number')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'Select RNW-2' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: 'Select All' }));
    fireEvent.click(screen.getByRole('button', { name: 'Send E-Policy (1)' }));
    await waitFor(() => expect(send).toHaveBeenCalledWith(1, ['RNW-1'], []));
    expect(await screen.findByText(/Successfully Submitted/)).toBeInTheDocument();
  });
});
