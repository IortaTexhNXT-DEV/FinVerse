import { render, screen } from '@testing-library/react';
import { WidgetCard } from './WidgetCard';
import { Figure } from './WidgetParts';
import { tooltipAmount, tooltipMonth } from './widgetSupport';

function card(state: { loading?: boolean; error?: unknown; empty?: boolean }) {
  return render(
    <WidgetCard
      title="Premium"
      loading={state.loading ?? false}
      error={state.error}
      empty={state.empty ?? false}
      emptyMessage="No premium yet."
    >
      <span>chart</span>
    </WidgetCard>,
  );
}

describe('dashboard widget card', () => {
  it('shows the content when there is data', () => {
    card({});
    expect(screen.getByText('Premium')).toBeInTheDocument();
    expect(screen.getByText('chart')).toBeInTheDocument();
  });

  it('degrades to a message without data, while loading and on error', () => {
    card({ empty: true });
    expect(screen.getByText('No premium yet.')).toBeInTheDocument();
    expect(screen.queryByText('chart')).not.toBeInTheDocument();
  });

  it('shows loading and errors instead of the content', () => {
    const { unmount } = card({ loading: true });
    expect(screen.getByText('Loading…')).toBeInTheDocument();
    unmount();
    card({ error: new Error('Widget failed') });
    expect(screen.getByRole('alert')).toHaveTextContent('Widget failed');
  });

  it('renders figures compactly with the exact amount as title', () => {
    render(<Figure label="Overdue" value={-1500000} hint="2 items" danger />);
    const value = screen.getByTitle('(1,500,000.00)');
    expect(value).toHaveTextContent('-1.5M');
    expect(value).toHaveClass('danger');
    expect(screen.getByText('2 items')).toBeInTheDocument();
  });

  it('formats chart tooltips', () => {
    expect(tooltipAmount(1234.5)).toBe('1,234.50');
    expect(tooltipAmount('10')).toBe('10.00');
    expect(tooltipMonth('2026-04')).toBe('Apr');
    expect(tooltipMonth(4)).toBe('');
  });
});
