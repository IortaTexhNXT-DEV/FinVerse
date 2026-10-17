import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { FilterToggle } from './FilterToggle';
import { KpiTile } from './KpiTile';
import { rankBreakdown } from './kpiBreakdown';

const lines = [
  { key: 'a', label: 'Accounts', count: 31, to: '/a' },
  { key: 'c', label: 'Claim', count: 1, to: '/c' },
  { key: 'o', label: 'Client onboarding', count: 5, to: '/o' },
  { key: 'd', label: 'Disbursement vouchers', count: 4, to: '/d' },
  { key: 's', label: 'Screening cases', count: 8, to: '/s' },
  { key: 'z', label: 'Payees', count: 0, to: '/z' },
  { key: 'e', label: 'Collection escalations', count: 2, to: '/e' },
];

describe('KPI tile', () => {
  it('ranks the breakdown largest first and keeps the rest for View all', () => {
    const ranked = rankBreakdown(lines);
    expect(ranked.shown.map((l) => l.label)).toEqual([
      'Accounts',
      'Screening cases',
      'Client onboarding',
      'Disbursement vouchers',
    ]);
    expect(ranked.total).toBe(6);
    expect(ranked.hidden).toBe(2);
    // One category over the limit is shown rather than hidden behind View all.
    expect(rankBreakdown(lines.slice(0, 5)).hidden).toBe(0);
  });

  it('shows the label, the figure opening its list, the qualifier and a ranked list of links', () => {
    render(
      <MemoryRouter>
        <KpiTile
          label="Overdue (SLA Breaches)"
          value={51}
          to="/my-work?overdue=true"
          qualifier="Past their service level"
          alert
          breakdown={{ label: 'Overdue items by type', items: lines, allTo: '/my-work' }}
        />
      </MemoryRouter>,
    );
    const tile = screen.getByRole('region', { name: 'Overdue (SLA Breaches)' });
    expect(tile).toHaveClass('kpi-tile', 'kpi-tile-alert');
    expect(within(tile).getByRole('link', { name: '51' })).toHaveAttribute(
      'href',
      '/my-work?overdue=true',
    );
    expect(within(tile).getByText('Past their service level')).toHaveClass('kpi-tile-qualifier');
    const list = within(tile).getByRole('list', { name: 'Overdue items by type' });
    const rows = within(list).getAllByRole('link');
    expect(rows.map((r) => r.textContent)).toEqual([
      'Accounts31',
      'Screening cases8',
      'Client onboarding5',
      'Disbursement vouchers4',
    ]);
    expect(rows[0]).toHaveAttribute('href', '/a');
    expect(within(tile).getByRole('link', { name: 'View all 6' })).toHaveAttribute(
      'href',
      '/my-work',
    );
    // Never a dot-separated sentence.
    expect(tile.textContent).not.toContain('·');
  });

  it('shows amounts as labelled pairs', () => {
    render(
      <MemoryRouter>
        <KpiTile
          label="Booked This Month"
          value={5}
          pairs={[
            { label: 'Premium (PHP)', value: '25,950.00' },
            { label: 'Commission (PHP)', value: '5,066.25' },
          ]}
        />
      </MemoryRouter>,
    );
    const terms = screen.getAllByRole('term').map((t) => t.textContent);
    const values = screen.getAllByRole('definition').map((d) => d.textContent);
    expect(terms).toEqual(['Premium (PHP)', 'Commission (PHP)']);
    expect(values).toEqual(['25,950.00', '5,066.25']);
    expect(screen.getByText('5').closest('a')).toBeNull();
  });
});

describe('filter toggle', () => {
  it('is a checkbox control that reports its new state', async () => {
    const onChange = vi.fn();
    const { rerender } = render(
      <FilterToggle label="Overdue Only" checked={false} onChange={onChange} />,
    );
    await userEvent.click(screen.getByRole('checkbox', { name: 'Overdue Only' }));
    expect(onChange).toHaveBeenCalledWith(true);
    rerender(<FilterToggle label="Overdue Only" checked onChange={onChange} />);
    expect(screen.getByRole('checkbox', { name: 'Overdue Only' }).closest('label')).toHaveClass(
      'filter-toggle',
      'checked',
    );
  });
});
