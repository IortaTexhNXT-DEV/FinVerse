import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { renewalChannelsApi } from '@/api/renewalChannels';
import type { ChannelRow } from '@/api/renewalChannels';
import { renewalWrapper } from '../testWrapper';
import ChannelMonitorPage from './ChannelMonitorPage';
import { DeliveriesCard } from './DeliveriesCard';

const FAILED: ChannelRow = {
  messageNo: 'CCM-2026-000004',
  channel: 'CCM',
  direction: 'OUTBOUND',
  docKind: 'RA',
  docRef: 'RA-2026-000004',
  renewalRef: 'RNW-2026-000101',
  fileName: 'MTR_RA_First Notice_RNW-2026-000101_10012026.pdf',
  recipients: 'not-an-address',
  status: 'FAILED',
  externalRef: null,
  attempts: 1,
  lastError: 'One or more recipient email addresses are invalid',
  createdAt: '2026-10-01T02:00:00Z',
  submittedAt: null,
  sentAt: null,
  deliveredAt: null,
  createdBy: 'proc',
};

describe('Channel Monitor', () => {
  it('lists the messages with their delivery status and resends a failed one', async () => {
    vi.spyOn(renewalChannelsApi, 'list').mockResolvedValue([FAILED]);
    vi.spyOn(renewalChannelsApi, 'connection').mockResolvedValue({
      settings: {
        channel: 'CCM',
        mode: 'SIMULATOR',
        endpoint: '',
        keySetting: 'KEY',
        keySet: false,
      },
      check: { channel: 'CCM', live: false, reachable: true, detail: 'CCM simulator' },
    });
    const resend = vi
      .spyOn(renewalChannelsApi, 'resend')
      .mockResolvedValue({ messageNo: 'CCM-2026-000005', status: 'SUBMITTED', error: null });
    const wrap = renewalWrapper(new Set(['RNW_CHANNEL_MONITOR']));
    render(wrap(<ChannelMonitorPage />));
    expect(await screen.findByText(FAILED.fileName ?? '')).toBeInTheDocument();
    expect(screen.getAllByText('Simulator').length).toBeGreaterThan(0);
    fireEvent.click(screen.getByRole('button', { name: 'Actions for CCM-2026-000004' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Resend' }));
    await waitFor(() => expect(resend).toHaveBeenCalledWith(1, 'CCM-2026-000004'));
  });

  it('shows the deliveries of a renewal account with the CCM reference', async () => {
    vi.spyOn(renewalChannelsApi, 'deliveries').mockResolvedValue([
      {
        messageNo: 'CCM-2026-000006',
        channel: 'CCM',
        docKind: 'RA',
        docRef: 'RA-2026-000006',
        fileName: 'MTR_RA_First Notice_RNW-1_10012026.pdf',
        recipients: 'juan@example.ph',
        status: 'DELIVERED',
        statusLabel: 'Delivered',
        externalRef: 'CCMSIM-CCM-2026-000006',
        error: null,
        queuedAt: '2026-10-01T02:00:00Z',
        sentAt: '2026-10-01T02:05:00Z',
        deliveredAt: '2026-10-01T02:10:00Z',
        queuedBy: 'proc',
      },
    ]);
    const wrap = renewalWrapper(new Set(['RNW_VIEW']));
    render(wrap(<DeliveriesCard renewalRef="RNW-1" />));
    expect(await screen.findByText('CCMSIM-CCM-2026-000006')).toBeInTheDocument();
    expect(screen.getByText('Delivered')).toBeInTheDocument();
  });
});
