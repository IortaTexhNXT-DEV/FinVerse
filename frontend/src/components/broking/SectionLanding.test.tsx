import { render, screen } from '@testing-library/react';
import { SectionLanding } from './SectionLanding';

describe('section landing', () => {
  it('shows the header and the empty state until the module adds its queues', () => {
    render(
      <SectionLanding
        section="Finance"
        title="Collections Home"
        description="Outstanding premium receivables to follow up."
        emptyMessage="No collection work yet"
      />,
    );
    expect(screen.getByRole('heading', { level: 1, name: 'Collections Home' })).toBeInTheDocument();
    expect(screen.getByText('Finance')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Work Queues' })).toBeInTheDocument();
    expect(screen.getByText('No collection work yet')).toBeInTheDocument();
  });

  it('shows the module content in place of the empty state', () => {
    render(
      <SectionLanding
        section="Finance"
        title="ACSL Cases"
        description="Cases"
        cardTitle="Cases by Stage"
        emptyMessage="No cases"
      >
        <p>Three cases</p>
      </SectionLanding>,
    );
    expect(screen.getByText('Three cases')).toBeInTheDocument();
    expect(screen.queryByText('No cases')).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Cases by Stage' })).toBeInTheDocument();
  });
});
