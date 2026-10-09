import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import type { QueueCount } from '@/api/workflow';
import { StageTiles, WorkSummary } from './WorkTiles';

const count = (workflowCode: string, stageCode: string, open = 1, overdue = 0): QueueCount => ({
  workflowCode,
  stageCode,
  stageName: stageCode === 'WITH_TSU' ? 'With TSU' : 'Draft',
  open,
  overdue,
  mine: 0,
});

describe('My Work tiles', () => {
  it('opens the queue filtered on each headline figure', () => {
    render(
      <MemoryRouter>
        <WorkSummary counts={[count('NB_ACCOUNT', 'DRAFT', 3, 2), count('BCL_CLAIM', 'NEW', 1)]} />
      </MemoryRouter>,
    );
    expect(screen.getByRole('link', { name: '2' })).toHaveAttribute(
      'href',
      '/my-work?overdue=true',
    );
    expect(screen.getByText('Stages across 2 workflows')).toBeTruthy();
  });

  it('folds the stages of many workflows so the queue starts on the first screen', async () => {
    const onFilter = vi.fn();
    const counts = ['NB_ACCOUNT', 'NB_QUOTATION', 'NB_PROPOSAL', 'BCL_CLAIM'].map((w) =>
      count(w, 'DRAFT'),
    );
    render(<StageTiles counts={counts} filters={{ scope: 'ALL' }} onFilter={onFilter} />);
    const toggle = screen.getByRole('button', { name: 'Show 4 Stages' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(screen.queryByText('Accounts')).toBeNull();
    await userEvent.click(toggle);
    expect(screen.getByText('Accounts')).toBeTruthy();
    await userEvent.click(screen.getAllByRole('button', { name: /Draft/ })[0]!);
    expect(onFilter).toHaveBeenCalledWith({ workflow: 'NB_ACCOUNT', stage: 'DRAFT', scope: 'ALL' });
  });

  it('shows the stages of one workflow at once', () => {
    render(
      <StageTiles counts={[count('NB_PROPOSAL', 'WITH_TSU')]} filters={{}} onFilter={vi.fn()} />,
    );
    expect(screen.queryByRole('button', { name: /Show/ })).toBeNull();
    expect(screen.getByRole('button', { name: /With TSU/ })).toBeTruthy();
  });
});
