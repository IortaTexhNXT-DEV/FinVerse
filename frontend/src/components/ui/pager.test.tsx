import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Pager } from './Pager';
import { pageWindow, showingText } from './pagerMath';

describe('BDO pager', () => {
  it('describes the rows shown', () => {
    expect(showingText(0, 20, 57)).toBe('Showing 1 to 20 of 57 results');
    expect(showingText(2, 20, 57)).toBe('Showing 41 to 57 of 57 results');
  });

  it('numbers every page up to seven and elides the others', () => {
    expect(pageWindow(0, 3)).toEqual([0, 1, 2]);
    expect(pageWindow(0, 10)).toEqual([0, 1, null, 9]);
    expect(pageWindow(5, 10)).toEqual([0, null, 4, 5, 6, null, 9]);
    expect(pageWindow(9, 10)).toEqual([0, null, 8, 9]);
  });

  it('pages with the numbers and the arrows', async () => {
    const onPage = vi.fn();
    render(<Pager page={1} totalPages={3} total={45} size={20} onPage={onPage} />);
    expect(screen.getByText('Showing 21 to 40 of 45 results')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Page 2' })).toHaveAttribute('aria-current', 'page');
    await userEvent.click(screen.getByRole('button', { name: 'Page 3' }));
    await userEvent.click(screen.getByRole('button', { name: 'Previous page' }));
    expect(onPage.mock.calls).toEqual([[2], [0]]);
  });

  it('shows nothing without rows', () => {
    const { container } = render(<Pager page={0} totalPages={0} total={0} onPage={vi.fn()} />);
    expect(container).toBeEmptyDOMElement();
  });
});
