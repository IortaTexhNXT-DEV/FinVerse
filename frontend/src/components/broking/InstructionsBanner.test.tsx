import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { clientsApi } from '@/api/clients';
import { InstructionsBanner } from './InstructionsBanner';

function wrapper({ children }: Readonly<{ children: ReactNode }>) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('client instructions banner', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows the active tags and instructions of the client', async () => {
    vi.spyOn(clientsApi, 'banner').mockResolvedValue({
      clientId: 7,
      clientCode: 'CL-2026-000007',
      tags: [{ code: 'VIP', label: 'VIP client' }],
      instructions: [
        {
          id: 1,
          type: 'BILLING',
          typeLabel: 'Billing and payment',
          text: 'Bill quarterly',
          effectiveFrom: '2026-01-01',
        },
      ],
    });
    render(<InstructionsBanner clientId={7} />, { wrapper });
    expect(await screen.findByText('VIP client')).toBeInTheDocument();
    expect(screen.getByText('Bill quarterly')).toBeInTheDocument();
    expect(screen.getByText(/until further notice/)).toBeInTheDocument();
  });

  it('renders nothing when the client has no tags or instructions', async () => {
    const banner = vi
      .spyOn(clientsApi, 'banner')
      .mockResolvedValue({ clientId: 8, clientCode: 'PR-1', tags: [], instructions: [] });
    const { container } = render(<InstructionsBanner clientId={8} />, { wrapper });
    await vi.waitFor(() => expect(banner).toHaveBeenCalled());
    expect(container).toBeEmptyDOMElement();
  });
});
