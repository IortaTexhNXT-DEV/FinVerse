import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { ebMarketApi } from '@/api/ebMarket';
import { lovApi } from '@/api/lov';
import ComparativePage from '../comparative/ComparativePage';
import { stepsFor } from '../comparative/comparativeSteps';
import { COMPARATIVE, CONFIRMATION, PROPOSAL, programmeAt } from '../market/marketFixtures';
import { PROGRAMME } from '../programmes/fixtures';
import { ebWrapper } from '../testWrapper';
import { ComparativesTab } from './ComparativesTab';
import { confirmationErrors, premiumOn } from './confirmationLogic';
import { ConfirmationTab } from './ConfirmationTab';

const AO = new Set(['EB_VIEW', 'EB_MARKET']);

describe('EB comparative and confirmation', () => {
  beforeEach(() => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([]);
    vi.spyOn(ebMarketApi, 'comparatives').mockResolvedValue([
      { ...COMPARATIVE.comparative, status: 'PRESENTED' },
    ]);
  });
  afterEach(() => vi.restoreAllMocks());

  it('triggers the placement of a confirmed cycle', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebMarketApi, 'confirmations').mockResolvedValue([CONFIRMATION]);
    vi.spyOn(ebMarketApi, 'proposals').mockResolvedValue([PROPOSAL]);
    vi.spyOn(ebMarketApi, 'comparative').mockResolvedValue(COMPARATIVE);
    const trigger = vi.spyOn(ebMarketApi, 'triggerPlacement').mockResolvedValue([
      {
        id: 1,
        arn: 'ARN-2026-000900',
        cycleId: 11,
        cycleNo: 'EBC-2026-000011',
        businessType: 'RENEWAL',
        status: 'SUBMITTED',
      },
    ]);
    render(ebWrapper(AO)(<ConfirmationTab programme={programmeAt('CONFIRMED')} />));
    expect(await screen.findByText('INS-MGIC')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Record Confirmation' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Trigger Placement' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Trigger Placement' }));
    await waitFor(() => expect(trigger).toHaveBeenCalledWith(1, 11));
  });

  it('records the confirmation with the recommended proposal by default', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebMarketApi, 'confirmations').mockResolvedValue([]);
    vi.spyOn(ebMarketApi, 'proposals').mockResolvedValue([{ ...PROPOSAL, status: 'VALIDATED' }]);
    vi.spyOn(ebMarketApi, 'comparative').mockResolvedValue(COMPARATIVE);
    const confirm = vi.spyOn(ebMarketApi, 'confirm');
    render(ebWrapper(AO)(<ConfirmationTab programme={programmeAt('WITH_CLIENT')} />));
    expect(
      await screen.findByText('No confirmation recorded on the current cycle'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Trigger Placement' })).not.toBeInTheDocument();
    await waitFor(() => expect(ebMarketApi.comparative).toHaveBeenCalled());
    await user.click(screen.getByRole('button', { name: 'Record Confirmation' }));
    const dialog = screen.getByRole('dialog');
    await waitFor(() =>
      expect(within(dialog).getByLabelText(/Line 1 – HMO/)).toHaveValue(String(PROPOSAL.id)),
    );
    await user.click(within(dialog).getByRole('button', { name: 'Record' }));
    expect(within(dialog).getByText('Select how the client confirmed')).toBeInTheDocument();
    expect(within(dialog).getByText("Attach the client's confirmation")).toBeInTheDocument();
    expect(confirm).not.toHaveBeenCalled();
  });

  it('builds a comparative and opens it', async () => {
    const user = userEvent.setup();
    const build = vi.spyOn(ebMarketApi, 'buildComparative').mockResolvedValue(COMPARATIVE);
    render(ebWrapper(AO)(<ComparativesTab programme={programmeAt('PROPOSALS')} />));
    expect(await screen.findByText('EBCA-2026-000061')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Build Comparative' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Build' }));
    await waitFor(() => expect(build).toHaveBeenCalledWith(1, 11));
  });

  it('shows the matrix and lets the team lead sign off', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebMarketApi, 'comparative').mockResolvedValue(COMPARATIVE);
    const act = vi.spyOn(ebMarketApi, 'comparativeAction').mockResolvedValue({
      ...COMPARATIVE,
      comparative: { ...COMPARATIVE.comparative, status: 'APPROVED' },
    });
    render(
      ebWrapper(
        new Set(['EB_VIEW', 'EB_COMPARATIVE_APPROVE']),
        'ebtl',
        '/eb/comparatives/61',
      )(
        <Routes>
          <Route path="/eb/comparatives/:id" element={<ComparativePage />} />
        </Routes>,
      ),
    );
    expect(await screen.findByText('Health Maintenance Organization (HMO)')).toBeInTheDocument();
    expect(screen.getByText('Semi-private')).toBeInTheDocument();
    expect(screen.getByText('Deviation')).toBeInTheDocument();
    expect(screen.getByText('Recommended')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Save Recommendation' })).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Sign Off' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Sign Off' }));
    await waitFor(() => expect(act).toHaveBeenCalledWith(1, 61, 'sign-off', undefined));
  });
});

describe('comparative and confirmation rules', () => {
  const can = (granted: string[]) => (p: string) => granted.includes(p);

  it('offers each step to its role', () => {
    const at = (status: string) => ({
      ...COMPARATIVE,
      comparative: { ...COMPARATIVE.comparative, status },
    });
    expect(stepsFor(at('DRAFT'), can(['EB_MARKET']))).toEqual(['submit']);
    expect(stepsFor(at('FOR_APPROVAL'), can(['EB_MARKET']))).toEqual([]);
    expect(stepsFor(at('FOR_APPROVAL'), can(['EB_COMPARATIVE_APPROVE']))).toEqual([
      'sign-off',
      'return',
    ]);
    expect(stepsFor(at('THRESHOLD_APPROVAL'), can(['EB_THRESHOLD_APPROVE']))).toEqual([
      'threshold-approve',
      'return',
    ]);
    expect(stepsFor(at('APPROVED'), can(['EB_MARKET']))).toEqual(['present']);
  });

  it('needs the channel, the evidence and a choice per line', () => {
    const lines = PROGRAMME.lines;
    const input = { channel: '' as const, choices: [] };
    expect(confirmationErrors(input, lines, undefined)).toEqual({
      channel: 'Select how the client confirmed',
      file: "Attach the client's confirmation",
      choices: 'Select the chosen proposal of line 1 (HMO)',
    });
    const ready = { channel: 'EMAIL' as const, choices: [{ lineNo: 1, proposalId: 51 }] };
    expect(confirmationErrors(ready, lines, new File(['x'], 'ok.pdf'))).toEqual({});
    expect(premiumOn(PROPOSAL, 'HMO')).toBe(1200000);
    expect(premiumOn(PROPOSAL, 'GLI')).toBe(0);
  });
});
