import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { WorkTiles } from '@/components/broking/WorkTiles';
import { Amount } from './Amount';
import { Card } from './Card';
import { CellStack } from './CellStack';
import { Kpi } from './Kpi';
import { PageHeader } from './PageHeader';

describe('UI sweep presentation rules', () => {
  it('shows a missing amount as the muted dash, never a blank cell', () => {
    const { container } = render(
      <>
        <Amount value={null} />
        <Amount value={2500} />
      </>,
    );
    const [empty, amount] = Array.from(container.querySelectorAll('.num'));
    expect(empty?.textContent).toBe('—');
    expect(empty).toHaveClass('muted');
    expect(amount?.textContent).toBe('2,500.00');
  });

  it('writes card titles and tile labels in Title Case with acronyms kept', () => {
    render(
      <MemoryRouter>
        <Card title="Recent extractions">
          <span />
        </Card>
        <Kpi label="Due in 30 days" value={0} />
        <WorkTiles
          label="Renewals"
          tiles={[{ key: 'r', label: 'Insurer reply overdue', value: 2, onClick: vi.fn() }]}
        />
      </MemoryRouter>,
    );
    expect(screen.getByRole('heading', { name: 'Recent Extractions' })).toBeTruthy();
    expect(screen.getByText('Due in 30 Days')).toBeTruthy();
    expect(screen.getByText('Insurer Reply Overdue')).toBeTruthy();
  });

  it('keeps codes in a two-line cell on one line', () => {
    render(<CellStack main="Pacific Harbor Logistics Inc." sub="CL-2026-000003" />);
    expect(screen.getByText('CL-2026-000003')).toHaveClass('muted', 'nowrap');
    expect(screen.getByText('Pacific Harbor Logistics Inc.')).not.toHaveClass('nowrap');
  });

  it('keeps the page actions on the right of a title block that takes the room left', () => {
    const { container } = render(
      <MemoryRouter>
        <PageHeader
          title="Upload & Intake"
          description="A long line"
          actions={<button type="button">Upload</button>}
        />
      </MemoryRouter>,
    );
    expect(container.querySelector('.page-title h1')?.textContent).toBe('Upload & Intake');
    expect(container.querySelector('.page-actions')?.textContent).toBe('Upload');
  });
});
