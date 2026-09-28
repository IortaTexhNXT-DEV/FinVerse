import { render, screen } from '@testing-library/react';
import { lovApi } from '@/api/lov';
import { ebWrapper } from '@/features/eb/testWrapper';
import { LegacyStatus } from './LegacyStatus';

describe('legacy status of an archive record', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows the label of the list with the stored legacy value in the tooltip', async () => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([{ code: 'PAID', label: 'Fully paid' }]);
    render(ebWrapper(new Set())(<LegacyStatus status="PAID" />));
    const label = await screen.findByText('Fully paid');
    expect(label).toHaveAttribute('title', 'Stored in legacy as PAID');
    expect(screen.queryByText('PAID')).not.toBeInTheDocument();
  });

  it('shows the stored value on the record detail and words an unlisted value', async () => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([]);
    render(ebWrapper(new Set())(<LegacyStatus status="ON_HOLD" withValue />));
    expect(await screen.findByText(/stored in legacy as ON_HOLD/)).toBeInTheDocument();
    expect(screen.getByTitle('Stored in legacy as ON_HOLD')).toHaveTextContent(/^On Hold/);
  });
});
