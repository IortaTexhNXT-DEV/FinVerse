import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { misFieldsApi } from '@/api/misFields';
import type { MisField } from '@/api/misFields';
import { ebWrapper } from '@/features/eb/testWrapper';
import MisFieldsPage from './MisFieldsPage';
import { fieldErrors, inputOf, sourceOptions } from './misFieldForm';

const PENDING: MisField = {
  id: 7,
  code: 'CLAIM_STATUS',
  label: 'Claim Status',
  entity: 'CLAIM',
  source: 'CLAIM_STATUS',
  columnKeys: ['claim_status'],
  lovType: null,
  recordStatus: 'PENDING_AUTHORIZATION',
  maker: 'sysadmin',
  authorizedBy: null,
  authorizedAt: null,
};

function renderPage(perms: string[], user = 'sysadmin2') {
  return render(ebWrapper(new Set(perms), user)(<MisFieldsPage />));
}

describe('MIS Fields', () => {
  beforeEach(() => {
    vi.spyOn(misFieldsApi, 'list').mockResolvedValue([PENDING]);
  });
  afterEach(() => vi.restoreAllMocks());

  it('lets another administrator authorize a pending field', async () => {
    const authorize = vi
      .spyOn(misFieldsApi, 'authorize')
      .mockResolvedValue({ ...PENDING, recordStatus: 'ACTIVE' });
    renderPage(['MIS_FIELD_AUTHORIZE']);
    expect((await screen.findAllByText('Claim Status')).length).toBeGreaterThan(0);
    await userEvent.click(
      screen.getByRole('button', { name: 'Actions for MIS Field CLAIM_STATUS' }),
    );
    await userEvent.click(screen.getByRole('menuitem', { name: 'Authorize' }));
    expect(await screen.findByText('Authorize MIS Field CLAIM_STATUS')).toBeInTheDocument();
    await userEvent.click(screen.getAllByRole('button', { name: 'Authorize' }).at(-1)!);
    await waitFor(() => expect(authorize).toHaveBeenCalledWith(7));
  });

  it('offers a new field to the maintainer and checks the code and label', async () => {
    renderPage(['MIS_FIELD_MAINTAIN'], 'sysadmin');
    await userEvent.click(await screen.findByRole('button', { name: /New MIS Field/ }));
    const dialog = await screen.findByRole('dialog', { name: /New MIS Field/ });
    await userEvent.click(within(dialog).getByRole('button', { name: /Save for Authorization/ }));
    expect(
      await within(dialog).findByText('Enter a code of capital letters, digits and underscores'),
    ).toBeInTheDocument();
  });

  it('keeps the form rules', () => {
    expect(fieldErrors(inputOf(PENDING))).toEqual({});
    expect(fieldErrors({ ...inputOf(), code: 'x' }).code).toBeDefined();
    expect(sourceOptions('USER').map((o) => o.value)).toContain('ACCOUNT_OFFICER');
  });
});
