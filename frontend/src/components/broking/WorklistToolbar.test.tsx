import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { WorklistToolbar } from './WorklistToolbar';

describe('work list toolbar', () => {
  it('searches the trimmed text, toggles the filters and shows the bulk actions', async () => {
    const onSearch = vi.fn();
    const onToggle = vi.fn();
    render(
      <WorklistToolbar onSearch={onSearch} filters={{ open: false, onToggle }}>
        <button type="button">Book Now</button>
      </WorklistToolbar>,
    );
    await userEvent.type(screen.getByLabelText('Search Proposal No.'), '  ARN-1 ');
    await userEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(onSearch).toHaveBeenCalledWith('ARN-1');
    await userEvent.click(screen.getByRole('button', { name: 'Filters' }));
    expect(onToggle).toHaveBeenCalled();
    expect(screen.getByRole('button', { name: 'Book Now' })).toBeInTheDocument();
  });

  it('has no filters toggle when the list has no filters', () => {
    render(<WorklistToolbar placeholder="Search client name" onSearch={vi.fn()} />);
    expect(screen.getByPlaceholderText('Search client name')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Filters' })).toBeNull();
  });
});
