import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import type { ReactNode } from 'react';
import { bookingApi } from '@/api/booking';
import type { BookedInvoice } from '@/api/booking';
import { lovApi } from '@/api/lov';
import { ToastContext } from '@/components/ui/toastContext';
import { CancellationDialog } from './CancellationDialog';

function wrap(children: ReactNode) {
  const queries = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={queries}>
      <ToastContext.Provider
        value={{ success: vi.fn(), error: vi.fn(), warning: vi.fn(), info: vi.fn() }}
      >
        {children}
      </ToastContext.Provider>
    </QueryClientProvider>
  );
}

const invoice = { arn: 'ARN-2026-940001', inceptionDate: '2026-09-01' } as BookedInvoice;

describe('cancel booking dialog', () => {
  beforeEach(() => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([
      { code: 'CLIENT_REQUEST', label: 'Client request' },
    ]);
  });
  afterEach(() => vi.restoreAllMocks());

  it('shows no error before the user acts, and the field errors on submit', async () => {
    const preview = vi.spyOn(bookingApi, 'previewEndorsement');
    const post = vi.spyOn(bookingApi, 'postEndorsement');
    render(wrap(<CancellationDialog invoice={invoice} onClose={vi.fn()} />));
    await screen.findByText('Client request');
    expect(screen.queryByText('Choose the reason')).not.toBeInTheDocument();
    expect(screen.queryByText('Describe the cancellation')).not.toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    expect(preview).not.toHaveBeenCalled();

    fireEvent.click(screen.getAllByRole('button', { name: 'Cancel Booking' }).at(-1)!);
    expect(await screen.findByText('Choose the reason')).toBeInTheDocument();
    expect(screen.getByText('Describe the cancellation')).toBeInTheDocument();
    await waitFor(() => expect(post).not.toHaveBeenCalled());
  });
});
