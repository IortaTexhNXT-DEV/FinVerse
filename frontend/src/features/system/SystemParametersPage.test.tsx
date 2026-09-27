import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { systemApi } from '@/api/system';
import type { SystemParameter } from '@/api/system';
import { setUserDirectory } from '@/api/users';
import { ebWrapper } from '@/features/eb/testWrapper';
import SystemParametersPage from './SystemParametersPage';

const PENDING: SystemParameter = {
  key: 'PASSWORD_MIN_LENGTH',
  value: '8',
  valueType: 'INTEGER',
  category: 'SECURITY',
  description: 'Shortest password',
  secondApproval: true,
  pendingValue: '12',
  pendingBy: 'badmin',
  pendingAt: '2026-09-27T01:00:00Z',
};

describe('system parameters waiting for approval', () => {
  afterEach(() => vi.restoreAllMocks());

  it('labels the requester and the date as field labels, never as a message', async () => {
    setUserDirectory([{ username: 'badmin', displayName: 'Bea Business Admin' }]);
    vi.spyOn(systemApi, 'parameters').mockResolvedValue([PENDING]);
    render(ebWrapper(new Set(['SECURITY_PARAMETER_APPROVE']), 'infosec')(<SystemParametersPage />));
    await userEvent.click(await screen.findByText('PASSWORD_MIN_LENGTH'));
    const grid = screen.getByRole('dialog');
    const terms = within(grid)
      .getAllByRole('term')
      .map((t) => t.textContent);
    expect(terms).toEqual(expect.arrayContaining(['Requested by', 'Requested on']));
    expect(within(grid).getByText('Bea Business Admin')).toBeInTheDocument();
    expect(screen.queryByText(/asked/i)).not.toBeInTheDocument();
  });
});
