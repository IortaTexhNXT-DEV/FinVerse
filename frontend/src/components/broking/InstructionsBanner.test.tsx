import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import type { ReactNode } from 'react';
import { clientsApi } from '@/api/clients';
import { ClientTagFlags, InstructionsBanner } from './InstructionsBanner';

function wrapper({ children }: Readonly<{ children: ReactNode }>) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{children}</QueryClientProvider>;
}

describe('client instructions banner', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows the instructions as an info notice and the tags as standard header tags', async () => {
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
    render(
      <>
        <span className="tag-list">
          <ClientTagFlags clientId={7} />
        </span>
        <InstructionsBanner clientId={7} />
      </>,
      { wrapper },
    );
    expect(await screen.findByText('VIP client')).toHaveClass('tag', 'info');
    expect(screen.getByText('Bill quarterly')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveClass('notice', 'notice-info');
    expect(screen.getByText('Special instructions')).toHaveClass('notice-title');
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
