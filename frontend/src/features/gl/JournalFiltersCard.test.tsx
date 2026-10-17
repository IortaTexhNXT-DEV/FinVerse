import { render, screen } from '@testing-library/react';
import { JournalFiltersCard } from './JournalFiltersCard';

describe('journal filters', () => {
  it('shows the bulk actions (Assign, Post Selected) next to the filters', () => {
    render(
      <JournalFiltersCard
        filters={{ page: 0 }}
        username="gltl"
        onChange={vi.fn()}
        actions={<button type="button">Assign (1)</button>}
      />,
    );
    expect(screen.getByRole('button', { name: 'Assign (1)' })).toBeInTheDocument();
  });
});
