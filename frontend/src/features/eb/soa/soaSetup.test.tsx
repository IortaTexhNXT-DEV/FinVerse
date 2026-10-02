import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Insurer } from '@/api/catalog';
import { catalogApi } from '@/api/catalog';
import { ebMarketApi } from '@/api/ebMarket';
import { ebServiceApi } from '@/api/ebService';
import { lovApi } from '@/api/lov';
import { REQUIRED, RULE, SOA, programmeAt } from '../market/marketFixtures';
import { page } from '../programmes/fixtures';
import EbSetupPage from '../setup/EbSetupPage';
import { ruleErrors, ruleInput } from '../setup/setupLogic';
import { SubmissionsTab } from '../submission/SubmissionsTab';
import { ebWrapper } from '../testWrapper';
import { BillingTab } from './BillingTab';
import { EMPTY_SOA, soaErrors } from './soaLogic';
import SoaRegisterPage from './SoaRegisterPage';

const PROCESSING = new Set(['EB_VIEW', 'EB_PROCESS']);
const INSURERS = [{ partyCode: 'INS-MGIC', name: 'Metro General Insurance' }] as Insurer[];

describe('EB SOA, submissions and set-up', () => {
  beforeEach(() => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([
      { code: 'NB_PLACEMENT', label: 'New business placement' },
    ]);
    vi.spyOn(catalogApi, 'insurers').mockResolvedValue(INSURERS);
  });
  afterEach(() => vi.restoreAllMocks());

  it('lists the SOAs to validate and validates one', async () => {
    const user = userEvent.setup();
    const list = vi.spyOn(ebServiceApi, 'soas').mockResolvedValue(page([SOA]));
    vi.spyOn(ebServiceApi, 'soa').mockResolvedValue(SOA);
    const validate = vi
      .spyOn(ebServiceApi, 'validateSoa')
      .mockResolvedValue({ ...SOA, status: 'VALIDATED' });
    render(ebWrapper(PROCESSING, 'ebproc', '/eb/soa?status=RECEIVED')(<SoaRegisterPage />));
    expect(await screen.findByText('EBSOA-2026-000101')).toBeInTheDocument();
    expect(list).toHaveBeenCalledWith(1, { status: 'RECEIVED' }, 0);
    await user.click(screen.getByText('EBSOA-2026-000101'));
    const dialog = await screen.findByRole('dialog');
    expect(await within(dialog).findByText('INV-2026-000001')).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Validate' }));
    await waitFor(() => expect(validate).toHaveBeenCalledWith(1, 101));
  });

  it('gives Collection the register without the Processing actions', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebServiceApi, 'soas').mockResolvedValue(page([SOA]));
    vi.spyOn(ebServiceApi, 'soa').mockResolvedValue(SOA);
    render(ebWrapper(new Set(['EB_VIEW', 'EB_COLLECT']), 'ebcoll')(<SoaRegisterPage />));
    await user.click(await screen.findByText('EBSOA-2026-000101'));
    const dialog = await screen.findByRole('dialog');
    expect(await within(dialog).findByRole('button', { name: 'Download SOA' })).toBeInTheDocument();
    expect(within(dialog).queryByRole('button', { name: 'Validate' })).not.toBeInTheDocument();
  });

  it('checks a received SOA on the programme', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebServiceApi, 'soas').mockResolvedValue(page([]));
    vi.spyOn(ebServiceApi, 'invoices').mockResolvedValue([]);
    const receive = vi.spyOn(ebServiceApi, 'receiveSoa');
    render(ebWrapper(PROCESSING)(<BillingTab programme={programmeAt('PLACED')} />));
    await user.click(await screen.findByRole('button', { name: 'Receive SOA' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Register' }));
    expect(within(dialog).getByText("Enter the insurer's SOA number")).toBeInTheDocument();
    expect(within(dialog).getByText('Attach the SOA')).toBeInTheDocument();
    expect(receive).not.toHaveBeenCalled();
  });

  it('submits the documents on file to the insurer', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebMarketApi, 'submissions').mockResolvedValue([]);
    vi.spyOn(ebMarketApi, 'checklist').mockResolvedValue([
      {
        documentType: 'EB_CLIENT_CONFIRMATION',
        label: 'Client confirmation',
        mandatory: true,
        present: true,
        attachmentIds: [902],
      },
    ]);
    const submit = vi.spyOn(ebMarketApi, 'submit').mockResolvedValue({} as never);
    render(
      ebWrapper(new Set(['EB_VIEW', 'EB_MARKET']))(
        <SubmissionsTab programme={programmeAt('IN_PLACEMENT')} />,
      ),
    );
    expect(await screen.findByText('No submission sent')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Submit to Insurer' }));
    const dialog = screen.getByRole('dialog');
    await user.selectOptions(await within(dialog).findByLabelText(/Process/), 'NB_PLACEMENT');
    await user.selectOptions(await within(dialog).findByLabelText(/Insurer/), 'INS-MGIC');
    expect(
      await within(dialog).findByRole('checkbox', { name: /Client confirmation/ }),
    ).toBeChecked();
    await user.click(within(dialog).getByRole('button', { name: 'Submit' }));
    await waitFor(() =>
      expect(submit).toHaveBeenCalledWith(1, {
        programmeId: 7,
        cycleId: 11,
        processType: 'NB_PLACEMENT',
        insurerCode: 'INS-MGIC',
        attachmentIds: [902],
        remarks: undefined,
      }),
    );
  });

  it('lets another user authorize a pending threshold rule', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebServiceApi, 'thresholdRules').mockResolvedValue([RULE]);
    vi.spyOn(ebServiceApi, 'requiredDocuments').mockResolvedValue([REQUIRED]);
    const act = vi
      .spyOn(ebServiceApi, 'thresholdRuleAction')
      .mockResolvedValue({ ...RULE, recordStatus: 'ACTIVE' });
    render(ebWrapper(new Set(['EB_VIEW', 'EB_SETUP']), 'badmin')(<EbSetupPage />));
    await user.click(await screen.findByRole('button', { name: 'Authorize rule Annual premium' }));
    await waitFor(() => expect(act).toHaveBeenCalledWith(1, 111, 'authorize'));
    expect(screen.getByRole('button', { name: /Deactivate/ })).toBeInTheDocument();
  });

  it('does not offer authorization to the maker', async () => {
    vi.spyOn(ebServiceApi, 'thresholdRules').mockResolvedValue([RULE]);
    vi.spyOn(ebServiceApi, 'requiredDocuments').mockResolvedValue([]);
    render(ebWrapper(new Set(['EB_VIEW', 'EB_SETUP']), 'badmin2')(<EbSetupPage />));
    expect(
      await screen.findByRole('button', { name: 'Edit rule Annual premium' }),
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Authorize/ })).not.toBeInTheDocument();
  });
});

describe('SOA and set-up rules', () => {
  it('checks an SOA intake', () => {
    expect(soaErrors(EMPTY_SOA, undefined)).toEqual({
      insurer: 'Select the insurer',
      soaNo: "Enter the insurer's SOA number",
      period: 'Enter the period covered',
      amount: 'Enter the amount of the SOA',
      file: 'Attach the SOA',
    });
    const ready = {
      ...EMPTY_SOA,
      insurerCode: 'INS-MGIC',
      insurerSoaNo: 'S-1',
      periodFrom: '2026-08-01',
      periodTo: '2026-08-31',
      amount: '1000.50',
    };
    expect(soaErrors(ready, new File(['x'], 'soa.pdf'))).toEqual({});
    expect(soaErrors({ ...ready, periodTo: '2026-07-01' }, new File(['x'], 'soa.pdf')).period).toBe(
      'Enter the period covered',
    );
  });

  it('checks a threshold rule', () => {
    const blank = ruleInput(undefined);
    expect(ruleErrors(blank)).toEqual({
      measure: 'Select what is measured',
      amount: 'Enter an amount greater than zero',
      effectiveFrom: 'Enter the effective date',
    });
    const edited = ruleInput(RULE);
    expect(edited.benefitLine).toBe('');
    expect(ruleErrors({ ...edited, effectiveTo: '2025-01-01' }).effectiveFrom).toBe(
      'The end date cannot be before the start date',
    );
    expect(ruleErrors(edited)).toEqual({});
  });
});
