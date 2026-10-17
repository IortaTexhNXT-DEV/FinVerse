import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessMatrix } from '@/api/nbadmin';
import { ebWrapper } from '@/features/eb/testWrapper';
import AccessMatrixPage from './AccessMatrixPage';

const MATRIX: AccessMatrix = {
  roles: [{ code: 'ACSL_LEAD', name: 'ACSL Team Leader', enabledUsers: 3 }],
  permissions: [{ permission: 'USER_MANAGE', roles: ['ACSL_LEAD'], area: 'USER', actions: [] }],
};

function renderPage() {
  return render(ebWrapper(new Set(['UAM_MATRIX_VIEW']), 'admin')(<AccessMatrixPage />));
}

describe('User Access Matrix layout', () => {
  beforeEach(() => vi.spyOn(nbadminApi, 'matrix').mockResolvedValue(MATRIX));
  afterEach(() => vi.restoreAllMocks());

  it('has one scroll box with a sticky header and a sticky permission column', async () => {
    const { container } = renderPage();
    const head = await screen.findByRole('columnheader', { name: /ACSL Team Leader/ });
    expect(head.querySelector('.matrix-role-name')).toHaveTextContent('ACSL Team Leader');
    expect(head).toHaveTextContent('3 users');
    expect(screen.getByRole('columnheader', { name: 'Permission' })).toHaveClass(
      'matrix-sticky-col',
    );
    expect(screen.getAllByRole('rowheader')[0]).toHaveClass('matrix-sticky-col');
    expect(container.querySelectorAll('.matrix-wrap')).toHaveLength(1);
    expect(container.querySelector('[data-fit]')).toBeNull();
    fireEvent.mouseOver(screen.getByLabelText('granted'));
    expect(head).toHaveClass('matrix-col-hover');
  });

  it('asks for words in the search and keeps the headers when nothing matches', async () => {
    renderPage();
    const search = await screen.findByPlaceholderText('Search by permission, description or area');
    await screen.findByRole('rowheader');
    await userEvent.type(search, 'nothing like this');
    expect(screen.getByText('No permission matches the search.')).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Permission' })).toBeInTheDocument();
  });
});
