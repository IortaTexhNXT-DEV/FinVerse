import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { nbadminApi } from '@/api/nbadmin';
import type { SodRule } from '@/api/nbadmin';
import { ebWrapper } from '@/features/eb/testWrapper';
import SodRulesPage from './SodRulesPage';

const PENDING: SodRule = {
  id: 4,
  ruleCode: 'SOD-0004',
  profileA: 'CASHIER',
  profileAName: 'Cashier',
  profileB: 'CASH_APPROVER',
  profileBName: 'Cash Approver',
  description: 'Maker and checker of receipts',
  status: 'PENDING_AUTHORIZATION',
  pendingAction: 'CREATE',
  maker: 'badmin',
  createdAt: '2026-09-27T08:00:00Z',
};

function renderPage() {
  return render(ebWrapper(new Set(['UAM_SOD_AUTHORIZE']), 'infosec')(<SodRulesPage />));
}

describe('Separation of Duties actions', () => {
  beforeEach(() => {
    vi.spyOn(nbadminApi, 'sodRules').mockResolvedValue([PENDING]);
  });
  afterEach(() => vi.restoreAllMocks());

  it('asks for confirmation before authorising a rule', async () => {
    const authorize = vi
      .spyOn(nbadminApi, 'authorizeSodRule')
      .mockResolvedValue({ ...PENDING, status: 'ACTIVE', pendingAction: 'NONE' });
    renderPage();
    // The actions of a rule are in its row action menu.
    await userEvent.click(await screen.findByRole('button', { name: 'Actions for Rule SOD-0004' }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Authorise' }));
    expect(authorize).not.toHaveBeenCalled();
    expect(screen.getByText('Authorise Rule SOD-0004')).toBeInTheDocument();
    expect(screen.getByText('Cashier and Cash Approver')).toBeInTheDocument();
    const dialogButtons = screen.getAllByRole('button', { name: 'Authorise' });
    await userEvent.click(dialogButtons.at(-1)!);
    await waitFor(() => expect(authorize).toHaveBeenCalledWith(4));
  });

  it('needs a reason to reject a rule and sends it', async () => {
    const reject = vi
      .spyOn(nbadminApi, 'rejectSodRule')
      .mockResolvedValue({ ...PENDING, status: 'INACTIVE' });
    renderPage();
    await userEvent.click(await screen.findByRole('button', { name: 'Actions for Rule SOD-0004' }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Reject' }));
    const title = screen.getByText('Reject Rule SOD-0004');
    const dialog = title.closest<HTMLElement>('.modal')!;
    await userEvent.click(within(dialog).getByRole('button', { name: 'Reject' }));
    expect(reject).not.toHaveBeenCalled();
    expect(within(dialog).getByText('Enter the reason.')).toBeInTheDocument();
    await userEvent.type(within(dialog).getByLabelText(/Reason/), 'Both profiles are needed');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Reject' }));
    await waitFor(() => expect(reject).toHaveBeenCalledWith(4, 'Both profiles are needed'));
  });
});
