import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ebServiceApi } from '@/api/ebService';
import { lovApi } from '@/api/lov';
import { CHANGE, ROSTER, programmeAt } from '../market/marketFixtures';
import { page } from '../programmes/fixtures';
import { ebWrapper } from '../testWrapper';
import { BLANK_ROW, changeErrors, toChangeInput } from './memberChangeForm';
import type { ChangeForm } from './memberChangeForm';
import MemberChangesPage from './MemberChangesPage';
import { MemberChangesTab } from './MemberChangesTab';
import { MembersTab } from './MembersTab';

const AO = new Set(['EB_VIEW', 'EB_MARKET']);

describe('EB members and member changes', () => {
  beforeEach(() => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([]);
  });
  afterEach(() => vi.restoreAllMocks());

  it('lists the open member changes and relays one to the insurer', async () => {
    const user = userEvent.setup();
    const list = vi.spyOn(ebServiceApi, 'memberChanges').mockResolvedValue(page([CHANGE]));
    vi.spyOn(ebServiceApi, 'memberChange').mockResolvedValue(CHANGE);
    const act = vi
      .spyOn(ebServiceApi, 'changeAction')
      .mockResolvedValue({ ...CHANGE, status: 'RELAYED' });
    render(ebWrapper(AO)(<MemberChangesPage />));
    expect(await screen.findByText('EBMC-2026-000091')).toBeInTheDocument();
    expect(list).toHaveBeenCalledWith(1, { status: 'OPEN' }, 0);
    expect(screen.getByText('Addition 1')).toBeInTheDocument();
    await user.click(screen.getByText('EBMC-2026-000091'));
    const dialog = await screen.findByRole('dialog');
    expect(await within(dialog).findByText('E-1001')).toBeInTheDocument();
    expect(within(dialog).queryByRole('button', { name: 'Validate' })).not.toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Relay to Insurer' }));
    await waitFor(() => expect(act).toHaveBeenCalledWith(1, 91, 'relay'));
  });

  it('lets Processing validate a billed change', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebServiceApi, 'memberChanges').mockResolvedValue(page([CHANGE]));
    vi.spyOn(ebServiceApi, 'memberChange').mockResolvedValue({ ...CHANGE, status: 'BILLED' });
    const act = vi.spyOn(ebServiceApi, 'changeAction').mockResolvedValue(CHANGE);
    render(ebWrapper(new Set(['EB_VIEW', 'EB_PROCESS']), 'ebproc')(<MemberChangesPage />));
    await user.click(await screen.findByText('EBMC-2026-000091'));
    const dialog = await screen.findByRole('dialog');
    await user.click(await within(dialog).findByRole('button', { name: 'Validate' }));
    await waitFor(() => expect(act).toHaveBeenCalledWith(1, 91, 'validate'));
  });

  it('checks a member change before capturing it on the programme', async () => {
    const user = userEvent.setup();
    const list = vi.spyOn(ebServiceApi, 'memberChanges').mockResolvedValue(page([]));
    const capture = vi.spyOn(ebServiceApi, 'captureChange');
    render(ebWrapper(AO)(<MemberChangesTab programme={programmeAt('PLACED')} />));
    expect(await screen.findByText('No member changes to display')).toBeInTheDocument();
    expect(list).toHaveBeenCalledWith(1, { programmeId: 7 }, 0);
    await user.click(screen.getByRole('button', { name: 'Capture Member Change' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Capture' }));
    expect(within(dialog).getByText('Select who asked for the change')).toBeInTheDocument();
    expect(capture).not.toHaveBeenCalled();
  });

  it('shows the staged roster with its differences and accepts it', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebServiceApi, 'roster').mockResolvedValue([ROSTER]);
    vi.spyOn(ebServiceApi, 'members').mockResolvedValue(
      page([
        {
          id: 1,
          employeeNo: 'E-1001',
          lastName: 'Santos',
          firstName: 'Ana',
          birthDate: '1990-01-01',
          planCode: 'P1',
          dependants: 1,
          effectiveFrom: '2027-01-10',
          status: 'ACTIVE',
        },
      ]),
    );
    vi.spyOn(ebServiceApi, 'differences').mockResolvedValue({
      headcount: 121,
      currentHeadcount: 120,
      added: 1,
      removed: 0,
      planChanges: 0,
    });
    const accept = vi
      .spyOn(ebServiceApi, 'acceptRoster')
      .mockResolvedValue({ ...ROSTER, status: 'ACCEPTED' });
    render(ebWrapper(AO)(<MembersTab programme={programmeAt('PLACED')} />));
    expect(await screen.findByText('Santos, Ana')).toBeInTheDocument();
    expect(await screen.findByText('121 (now 120)')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Accept roster 2' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Accept' }));
    await waitFor(() => expect(accept).toHaveBeenCalledWith(1, 81));
  });
});

describe('member change form', () => {
  const row = { ...BLANK_ROW, employeeNo: 'E-1', effectiveDate: '2026-10-01' };
  const form: ChangeForm = {
    lineNo: '1',
    policyYear: '',
    source: 'CLIENT',
    financial: true,
    description: '',
    rows: [{ ...row, action: 'ADD' }],
  };

  it('asks for what each change needs', () => {
    expect(changeErrors(form).rows).toBe('Enter the name, birth date and plan of employee E-1');
    const plan = { ...form, rows: [{ ...row, action: 'CHANGE_PLAN' as const }] };
    expect(changeErrors(plan).rows).toBe('Enter the new plan of employee E-1');
    const twice = {
      ...form,
      rows: [
        { ...row, action: 'DELETE' as const },
        { ...row, action: 'DELETE' as const },
      ],
    };
    expect(changeErrors(twice).rows).toBe('Employee E-1 appears twice in the member change');
  });

  it('sends only the member data of the change', () => {
    const del = { ...form, rows: [{ ...row, action: 'DELETE' as const, lastName: 'X' }] };
    expect(changeErrors(del)).toEqual({});
    const input = toChangeInput(del);
    expect(input).toMatchObject({ lineNo: 1, source: 'CLIENT', financial: true });
    expect(input.lines[0]).toEqual({
      action: 'DELETE',
      employeeNo: 'E-1',
      effectiveDate: '2026-10-01',
      member: undefined,
    });
  });
});
