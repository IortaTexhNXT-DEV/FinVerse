import { render, screen } from '@testing-library/react';
import { ChecklistView } from './ChecklistView';

describe('closing checklist', () => {
  it('shows pass and fail badges and the readiness banner', () => {
    render(
      <ChecklistView
        loading={false}
        checklist={{
          ready: false,
          items: [
            {
              code: 'TB',
              label: 'Trial balance in balance',
              passed: true,
              detail: 'ok',
              blocking: true,
            },
            {
              code: 'PJ',
              label: 'No pending journals',
              passed: false,
              detail: '2 journals',
              blocking: true,
            },
          ],
        }}
      />,
    );
    expect(screen.getByText('Pass')).toHaveClass('badge', 'success');
    expect(screen.getByText('Fail')).toHaveClass('badge', 'danger');
    expect(screen.getByRole('status')).toHaveTextContent('blocked');
  });

  it('shows a non-blocking failure as a warning that does not block the close', () => {
    render(
      <ChecklistView
        loading={false}
        checklist={{
          ready: true,
          items: [
            {
              code: 'RECONCILIATIONS',
              label: 'Reconciliations completed',
              passed: false,
              detail: '3 unreconciled item(s)',
              blocking: false,
            },
          ],
        }}
      />,
    );
    expect(screen.getByText('Warning')).toHaveClass('badge', 'warning');
    expect(screen.getByRole('status')).toHaveTextContent('review the warnings');
  });

  it('reports readiness when every control passed', () => {
    render(<ChecklistView loading={false} checklist={{ ready: true, items: [] }} />);
    expect(screen.getByRole('status')).toHaveTextContent('ready to close');
  });
});
