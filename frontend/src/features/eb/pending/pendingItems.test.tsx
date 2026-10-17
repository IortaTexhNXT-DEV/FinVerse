import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { catalogApi } from '@/api/catalog';
import { ebApi } from '@/api/eb';
import { lovApi } from '@/api/lov';
import { ITEM, page } from '../programmes/fixtures';
import { ebWrapper } from '../testWrapper';
import PendingItemsPage from './PendingItemsPage';
import { actionsFor, emptyItem, itemForm, itemInput, validateItem } from './pendingLogic';

describe('Pending Items', () => {
  beforeEach(() => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([{ code: 'HMO_CARD', label: 'HMO card' }]);
    vi.spyOn(catalogApi, 'insurers').mockResolvedValue([]);
  });
  afterEach(() => vi.restoreAllMocks());

  it('lists the items with their follow-ups and receives one', async () => {
    const user = userEvent.setup();
    const items = vi.spyOn(ebApi, 'items').mockResolvedValue(page([ITEM]));
    const change = vi.spyOn(ebApi, 'changeItem').mockResolvedValue({ ...ITEM, status: 'RECEIVED' });
    render(
      ebWrapper(
        new Set(['EB_VIEW', 'EB_PROCESS']),
        'ebproc',
        '/eb/pending-items?overdue=true',
      )(<PendingItemsPage />),
    );
    expect(await screen.findByText('HMO card of Juan Dela Cruz')).toBeInTheDocument();
    expect(items).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ overdue: true, status: 'PENDING' }),
      0,
    );
    expect(screen.getByText('12')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Mark Received' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Mark Received' }));
    await waitFor(() =>
      expect(change).toHaveBeenCalledWith(1, 31, expect.objectContaining({ action: 'RECEIVE' })),
    );
    await user.type(screen.getByLabelText('Member'), 'EMP-00123');
    await waitFor(() =>
      expect(items).toHaveBeenLastCalledWith(
        1,
        expect.objectContaining({ member: 'EMP-00123' }),
        0,
      ),
    );
  });

  it('validates a new item before adding it', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebApi, 'items').mockResolvedValue(page([]));
    vi.spyOn(ebApi, 'programmes').mockResolvedValue(page([]));
    const open = vi.spyOn(ebApi, 'openItem');
    render(ebWrapper(new Set(['EB_VIEW', 'EB_MARKET']))(<PendingItemsPage />));
    await user.click(await screen.findByRole('button', { name: 'Add Pending Item' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Save Item' }));
    expect(await within(dialog).findByText('Select the programme')).toBeInTheDocument();
    expect(within(dialog).getByText('Enter the due date')).toBeInTheDocument();
    expect(open).not.toHaveBeenCalled();
  });

  it('hides the maintenance actions from Collection', async () => {
    vi.spyOn(ebApi, 'items').mockResolvedValue(page([ITEM]));
    render(ebWrapper(new Set(['EB_VIEW', 'EB_COLLECT']))(<PendingItemsPage />));
    expect(await screen.findByText('HMO card of Juan Dela Cruz')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Mark Received' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add Pending Item' })).not.toBeInTheDocument();
  });

  it('checks and converts the item form', () => {
    const form = {
      ...emptyItem(7),
      itemType: 'HMO_CARD',
      subject: ' Card ',
      dueDate: '2026-10-01',
    };
    expect(validateItem(form, true)).toEqual({});
    expect(validateItem({ ...form, recipientEmail: 'a@b.example, nope' }, true)).toHaveProperty(
      'recipientEmail',
    );
    expect(validateItem({ ...form, responsible: '' }, false)).toHaveProperty('responsible');
    expect(itemInput(form)).toMatchObject({
      programmeId: 7,
      subject: 'Card',
      memberRef: undefined,
    });
    expect(itemForm(ITEM).memberRef).toBe('EMP-00123');
    expect(actionsFor('PENDING')).toEqual(['RECEIVE', 'CLOSE']);
    expect(actionsFor('RECEIVED')).toEqual(['RELEASE', 'CLOSE']);
    expect(actionsFor('RELEASED')).toEqual(['CLOSE']);
    expect(actionsFor('CLOSED')).toEqual([]);
  });
});
