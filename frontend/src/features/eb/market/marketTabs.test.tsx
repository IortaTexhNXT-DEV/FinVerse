import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Insurer } from '@/api/catalog';
import { catalogApi } from '@/api/catalog';
import { ebMarketApi } from '@/api/ebMarket';
import { lovApi } from '@/api/lov';
import { ebWrapper } from '../testWrapper';
import { FranchiseTab } from './FranchiseTab';
import { MarketTab } from './MarketTab';
import { FRANCHISE, PROPOSAL, TOR, programmeAt } from './marketFixtures';
import {
  BLANK_PLAN,
  blankProposal,
  proposalErrors,
  toProposalInput,
  totalPremium,
} from './proposalForm';
import { ProposalsTab } from './ProposalsTab';

const AO = new Set(['EB_VIEW', 'EB_MARKET']);
const INSURERS = [
  { partyCode: 'INS-MGIC', name: 'Metro General Insurance' },
  { partyCode: 'INS-LAC', name: 'Luzon Assurance' },
] as unknown as Insurer[];

describe('EB marketing tabs', () => {
  beforeEach(() => {
    vi.spyOn(catalogApi, 'insurers').mockResolvedValue(INSURERS);
    vi.spyOn(lovApi, 'options').mockResolvedValue([
      { code: 'HMO', label: 'Health Maintenance Organization (HMO)' },
    ]);
  });
  afterEach(() => vi.restoreAllMocks());

  it('requests the franchise from the selected insurers and records a decline with its reason', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebMarketApi, 'franchises').mockResolvedValue([FRANCHISE]);
    const request = vi.spyOn(ebMarketApi, 'requestFranchise').mockResolvedValue([FRANCHISE]);
    const decide = vi.spyOn(ebMarketApi, 'decideFranchise').mockResolvedValue(FRANCHISE);
    render(ebWrapper(AO)(<FranchiseTab programme={programmeAt('FRANCHISE')} />));
    expect(await screen.findByText('EBF-2026-000031')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Request Franchise' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Send' }));
    expect(within(dialog).getByText('Select at least one insurer')).toBeInTheDocument();
    await user.click(await within(dialog).findByRole('checkbox', { name: 'Luzon Assurance' }));
    await user.click(within(dialog).getByRole('button', { name: 'Send' }));
    await waitFor(() => expect(request).toHaveBeenCalledWith(1, 11, ['INS-LAC']));

    await user.click(screen.getByRole('button', { name: 'Record decision EBF-2026-000031' }));
    const decision = screen.getByRole('dialog');
    await user.selectOptions(within(decision).getByLabelText(/Decision/), 'DECLINED');
    await user.click(within(decision).getByRole('button', { name: 'Record' }));
    expect(within(decision).getByText('Select the reason')).toBeInTheDocument();
    expect(within(decision).getByText("Add the insurer's reply")).toBeInTheDocument();
    expect(decide).not.toHaveBeenCalled();
  });

  it('shows the draft TOR, releases it and closes a request as declined', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebMarketApi, 'tors').mockResolvedValue([TOR]);
    vi.spyOn(ebMarketApi, 'revisions').mockResolvedValue([]);
    vi.spyOn(ebMarketApi, 'requests').mockResolvedValue([
      {
        id: 44,
        requestNo: 'EBRQ-2026-000044',
        cycleId: 11,
        insurerCode: 'INS-MGIC',
        insurerName: 'Metro General Insurance',
        torVersion: 1,
        sentAt: '2026-09-03T02:00:00Z',
        sentBy: 'ebao',
        dueDate: '2026-09-17',
        status: 'OPEN',
      },
    ]);
    const release = vi
      .spyOn(ebMarketApi, 'releaseTor')
      .mockResolvedValue({ ...TOR, status: 'RELEASED' });
    const close = vi.spyOn(ebMarketApi, 'closeRequest').mockResolvedValue({} as never);
    render(ebWrapper(AO)(<MarketTab programme={programmeAt('PROPOSALS')} />));
    expect(await screen.findByText('Room and board')).toBeInTheDocument();
    expect(screen.getByText('Terms of Reference – Version 1')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Release TOR' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Release' }));
    await waitFor(() => expect(release).toHaveBeenCalledWith(1, 11));

    await user.click(await screen.findByRole('button', { name: 'Mark declined EBRQ-2026-000044' }));
    const dialog = screen.getByRole('dialog');
    await user.type(within(dialog).getByLabelText('Reason'), 'No appetite');
    await user.click(within(dialog).getByRole('button', { name: 'Mark Declined' }));
    await waitFor(() => expect(close).toHaveBeenCalledWith(1, 44, true, 'No appetite'));
  });

  it('lists the proposals, checks a new one and validates a received one', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebMarketApi, 'proposals').mockResolvedValue([PROPOSAL]);
    vi.spyOn(ebMarketApi, 'tors').mockResolvedValue([{ ...TOR, status: 'RELEASED' }]);
    const record = vi.spyOn(ebMarketApi, 'recordProposal');
    const validate = vi
      .spyOn(ebMarketApi, 'validateProposal')
      .mockResolvedValue({ ...PROPOSAL, status: 'VALIDATED' });
    render(ebWrapper(AO)(<ProposalsTab programme={programmeAt('PROPOSALS')} />));
    expect(await screen.findByText('EBPR-2026-000051')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Record Proposal' }));
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('Private room')).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Record' }));
    expect(within(dialog).getByText('Select the insurer')).toBeInTheDocument();
    expect(within(dialog).getByText("Attach the insurer's proposal")).toBeInTheDocument();
    expect(record).not.toHaveBeenCalled();
    await user.click(within(dialog).getByRole('button', { name: 'Cancel' }));

    await user.click(screen.getByRole('button', { name: 'Validate EBPR-2026-000051' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Validate' }));
    await waitFor(() => expect(validate).toHaveBeenCalledWith(1, 51));
  });

  it('hides the marketing actions without EB_MARKET', async () => {
    vi.spyOn(ebMarketApi, 'proposals').mockResolvedValue([PROPOSAL]);
    vi.spyOn(ebMarketApi, 'tors').mockResolvedValue([]);
    render(ebWrapper(new Set(['EB_VIEW']))(<ProposalsTab programme={programmeAt('PROPOSALS')} />));
    expect(await screen.findByText('EBPR-2026-000051')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Record Proposal' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Validate/ })).not.toBeInTheDocument();
  });
});

describe('proposal form', () => {
  const file = new File(['%PDF'], 'proposal.pdf');

  it('checks the plans, premiums and ratings', () => {
    const form = blankProposal(TOR.items, 'HMO');
    expect(proposalErrors(form, undefined)).toMatchObject({
      insurer: 'Select the insurer',
      file: "Attach the insurer's proposal",
      plans: 'Plan 1: select the benefit line and enter the plan',
    });
    const plans = [{ ...BLANK_PLAN, benefitLine: 'HMO', planCode: 'P1', annualPremium: '-1' }];
    expect(proposalErrors({ ...form, insurerCode: 'X', plans }, file).plans).toBe(
      'Plan 1: enter an annual premium of zero or more',
    );
    const factors = [{ factorCode: 'CLAIMS', rating: '7', value: '' }];
    expect(proposalErrors({ ...form, factors }, file).factors).toBe(
      'Rate the capability factors from 1 to 5',
    );
  });

  it('builds the input without blank answers and factors', () => {
    const form = blankProposal(TOR.items, 'HMO');
    const ready = {
      ...form,
      insurerCode: 'INS-MGIC',
      plans: [
        { ...BLANK_PLAN, benefitLine: 'HMO', planCode: 'P1', annualPremium: '1000', members: '10' },
      ],
      factors: [{ factorCode: '', rating: '', value: '' }],
    };
    expect(proposalErrors(ready, file)).toEqual({});
    expect(totalPremium(ready)).toBe(1000);
    const input = toProposalInput(ready);
    expect(input.lines[0]).toMatchObject({ planCode: 'P1', annualPremium: 1000, members: 10 });
    expect(input.items).toEqual([]);
    expect(input.factors).toEqual([]);
  });
});
