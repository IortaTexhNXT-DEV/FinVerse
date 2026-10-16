import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { siBatchApi, canResend, periodStart } from '@/api/siBatches';
import type { SiBatch } from '@/api/siBatches';
import { ebWrapper } from '@/features/eb/testWrapper';
import SiBatchesPage from './SiBatchesPage';

const batch = (over: Partial<SiBatch>): SiBatch => ({
  id: 1,
  companyId: 1,
  insurerCode: 'INS-1',
  insurerName: 'Panel Mutual',
  batchDate: '2026-10-15',
  itemCount: 3,
  currency: 'PHP',
  commissionTotal: 300,
  netTotal: 306,
  status: 'SENT',
  attempts: 1,
  recipientEmail: 'billing@panel.example',
  lastError: null,
  generatedAt: '2026-10-15T13:30:00Z',
  sentAt: '2026-10-15T13:31:00Z',
  ...over,
});

describe('service invoice batches', () => {
  it('only a failed batch can be sent again', () => {
    expect(canResend({ status: 'FAILED' })).toBe(true);
    expect(canResend({ status: 'SENT' })).toBe(false);
    expect(canResend({ status: 'RETRIED' })).toBe(false);
    expect(periodStart('2026-10-31')).toBe('2026-10-01');
  });

  it('lists the batches and re-sends a failed one', async () => {
    vi.spyOn(siBatchApi, 'list').mockResolvedValue([
      batch({}),
      batch({
        id: 2,
        insurerName: 'Other Mutual',
        status: 'FAILED',
        lastError: 'No billing e-mail',
      }),
    ]);
    const resend = vi.spyOn(siBatchApi, 'resend').mockResolvedValue(batch({ id: 2 }));
    const items = vi.spyOn(siBatchApi, 'items').mockResolvedValue([]);
    render(ebWrapper(new Set(['BOOKING_PROCESS']), 'proc')(<SiBatchesPage />));

    expect(await screen.findByText('No billing e-mail')).toBeInTheDocument();
    const buttons = screen.getAllByRole('button', { name: 'Re-send' });
    expect(buttons).toHaveLength(1);
    const user = userEvent.setup();
    await user.click(buttons[0]!);
    expect(resend).toHaveBeenCalledWith(2);
    await user.click(screen.getByRole('button', { name: 'Panel Mutual' }));
    await vi.waitFor(() => expect(items).toHaveBeenCalledWith(1));
  });
});
