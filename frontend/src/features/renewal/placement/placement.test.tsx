import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { renewalPlacementApi, transmissionLabel } from '@/api/renewalPlacement';
import type { PlacementView } from '@/api/renewalPlacement';
import type { CandidateDetail } from '@/api/renewal';
import { renewalWrapper } from '../testWrapper';
import { SendPlacementDialog } from './PlacementDialogs';
import { PlacementTab } from './PlacementTab';

const SENT: PlacementView = {
  id: 7,
  insurerCode: 'INS001',
  share: 70,
  premium: 7000,
  sumInsured: 700000,
  docs: {
    slipFileName: 'INS001_MOTOR_PlacementSlip_RNW-2026-000101_10012026.pdf',
    slipAttachmentId: 11,
    fileName: null,
    fileAttachmentId: null,
  },
  status: 'SENT',
  sending: {
    channel: 'MFT',
    messageNo: 'MFT-2026-000001',
    status: 'PENDING_TRANSMISSION',
    recipients: 'INS001',
    cc: null,
  },
  submitted: '2026-10-01',
  tat: { days: 2, target: 3, status: 'Within SLA', end: '2026-10-05' },
  withIssue: false,
  resolutionDate: null,
  response: { response: null, date: null, reason: null, remarks: null },
  generatedBy: 'Paolo Reyes',
  generatedOn: '2026-10-01',
};

const DETAIL = { row: { renewalRef: 'RNW-2026-000101' } } as unknown as CandidateDetail;

describe('Placement', () => {
  it('shows each insurer slip with its turnaround time and tags it With Issue', async () => {
    vi.spyOn(renewalPlacementApi, 'of').mockResolvedValue([SENT]);
    vi.spyOn(renewalPlacementApi, 'advices').mockResolvedValue([]);
    const issue = vi.spyOn(renewalPlacementApi, 'issue').mockResolvedValue(SENT);
    const wrap = renewalWrapper(new Set(['RNW_PROCESS', 'RNW_VIEW']));
    render(wrap(<PlacementTab detail={DETAIL} />));
    expect(await screen.findByText(SENT.docs.slipFileName)).toBeInTheDocument();
    expect(screen.getByText('2 of 3')).toBeInTheDocument();
    expect(screen.getByText('Pending Upload')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('checkbox', { name: 'With Issue INS001' }));
    await waitFor(() =>
      expect(issue).toHaveBeenCalledWith(1, 'RNW-2026-000101', 7, {
        withIssue: true,
        resolutionDate: null,
      }),
    );
  });

  it('sends the placement to the insurer recipients with copy recipients', async () => {
    vi.spyOn(renewalPlacementApi, 'recipients').mockResolvedValue([
      {
        insurerCode: 'INS002',
        insurerName: 'Second Insurer',
        mft: false,
        recipients: ['uw@insurer.example.ph'],
        accounts: 1,
      },
    ]);
    const confirm = vi.fn();
    const wrap = renewalWrapper(new Set(['RNW_PROCESS']));
    render(
      wrap(
        <SendPlacementDialog
          refs={['RNW-2026-000101']}
          busy={false}
          error={null}
          onClose={() => undefined}
          onConfirm={confirm}
        />,
      ),
    );
    expect(await screen.findByDisplayValue('uw@insurer.example.ph')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('CC recipients'), {
      target: { value: 'copy@bank.example.ph' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));
    expect(confirm).toHaveBeenCalledWith({
      recipients: { INS002: ['uw@insurer.example.ph'] },
      cc: ['copy@bank.example.ph'],
    });
  });

  it('names the transmission statuses of MFT and CCM', () => {
    expect(transmissionLabel('MFT', 'FAILED')).toBe('Upload Failed');
    expect(transmissionLabel('MFT', 'SUBMITTED')).toBe('Uploaded to MFT');
    expect(transmissionLabel('CCM', 'SUBMITTED')).toBe('Submitted to CCM');
  });
});
