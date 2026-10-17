import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { pmTermsApi } from '@/api/pmTerms';
import type { TermsView } from '@/api/pmTerms';
import { Providers } from '../workspace/testProviders';
import { ClientResponseCard } from './ClientResponseCard';
import { TermsTablePanel } from './TermsTablePanel';

const VIEW: TermsView = {
  table: {
    requestNo: 'PRF-2026-000010',
    fields: ['PREMIUM', 'DEDUCTIBLES'],
    shown: ['PREMIUM', 'DEDUCTIBLES'],
    clientFields: ['PREMIUM'],
    qsValues: { PREMIUM: null, DEDUCTIBLES: '50,000 each loss' },
    columns: [
      {
        insurerCode: 'INS-LAC',
        insurerName: 'Luzon Assurance Co.',
        optionNo: 1,
        answer: 'APPROVED',
        otherAnswer: null,
        values: { PREMIUM: '110,000.00' },
        saved: false,
      },
      {
        insurerCode: 'INS-LAC',
        insurerName: 'Luzon Assurance Co.',
        optionNo: 2,
        answer: 'OTHERS',
        otherAnswer: 'Subject to survey',
        values: { PREMIUM: '105,000.00' },
        saved: true,
      },
      {
        insurerCode: 'INS-VMI',
        insurerName: 'Visayas Mutual Insurance',
        optionNo: 1,
        answer: null,
        otherAnswer: null,
        values: {},
        saved: false,
      },
    ],
    finalTerms: { PREMIUM: '110,000.00' },
    selectedInsurers: [],
  },
  labels: { PREMIUM: 'Premium', DEDUCTIBLES: 'Deductibles' },
  answers: { APPROVED: 'Approved', NOT_COVERED: 'Not Covered', OTHERS: 'Others' },
};

describe('Comparative table with the Final Terms for Proposal', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows the QS value, the options with their responses and saves an edited Final Term', async () => {
    vi.spyOn(pmTermsApi, 'table').mockResolvedValue(VIEW);
    vi.spyOn(pmTermsApi, 'files').mockResolvedValue([]);
    const save = vi.spyOn(pmTermsApi, 'saveFinalTerms').mockResolvedValue(VIEW);
    const proceed = vi.spyOn(pmTermsApi, 'proceed').mockResolvedValue(VIEW);
    render(
      <Providers username="ao" permissions={['PROPOSAL_REQUEST']}>
        <TermsTablePanel
          type="quotation"
          id={10}
          keyInPermission="TSU_PROCESS"
          selectPermissions={['PROPOSAL_REQUEST']}
          selectable
          generatable={false}
        />
      </Providers>,
    );
    expect(await screen.findByText('Luzon Assurance Co. – Option 2')).toBeInTheDocument();
    expect(screen.getByText('Others: Subject to survey')).toBeInTheDocument();
    expect(screen.getByText('Awaiting response')).toBeInTheDocument();
    expect(screen.getByText('50,000 each loss')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Choose Fields' })).not.toBeInTheDocument();
    const premium = screen.getByLabelText('Final Terms for Premium');
    await userEvent.clear(premium);
    await userEvent.type(premium, '108,000.00');
    await userEvent.click(screen.getByRole('button', { name: 'Save Final Terms' }));
    expect(save).toHaveBeenCalledWith('quotation', 10, { PREMIUM: '108,000.00' });
    expect(screen.queryByLabelText('Visayas Mutual Insurance')).not.toBeInTheDocument();
    await userEvent.click(screen.getByLabelText('Luzon Assurance Co.'));
    await userEvent.click(screen.getByRole('button', { name: 'Proceed to Proposal' }));
    expect(proceed).toHaveBeenCalledWith('quotation', 10, ['INS-LAC']);
  });

  it('lets TSU generate the proposal of the selected insurers', async () => {
    vi.spyOn(pmTermsApi, 'table').mockResolvedValue({
      ...VIEW,
      table: { ...VIEW.table, selectedInsurers: ['INS-LAC'] },
    });
    vi.spyOn(pmTermsApi, 'files').mockResolvedValue([]);
    const generate = vi.spyOn(pmTermsApi, 'generate').mockResolvedValue([]);
    render(
      <Providers username="tsu" permissions={['TSU_PROCESS']}>
        <TermsTablePanel
          type="quotation"
          id={10}
          keyInPermission="TSU_PROCESS"
          selectPermissions={['PROPOSAL_REQUEST']}
          selectable={false}
          generatable
        />
      </Providers>,
    );
    expect(await screen.findByRole('button', { name: 'Choose Fields' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Generate Proposal' }));
    expect(generate).toHaveBeenCalledWith('quotation', 10);
  });

  it('records a client return for revision only with remarks', async () => {
    vi.spyOn(pmTermsApi, 'clientResponses').mockResolvedValue([]);
    const record = vi.spyOn(pmTermsApi, 'recordClientResponse').mockResolvedValue({
      id: 1,
      response: 'RETURNED',
      responseName: 'Return for Revision',
      responseDate: '2026-10-09',
      remarks: 'Lower deductible',
      insurers: null,
      recordedBy: 'Ana Officer',
      recordedAt: '2026-10-09T02:00:00Z',
    });
    render(
      <Providers username="ao" permissions={['PROPOSAL_REQUEST']}>
        <ClientResponseCard id={10} canRecord />
      </Providers>,
    );
    await userEvent.click(await screen.findByRole('button', { name: 'Record Client Response' }));
    await userEvent.selectOptions(
      screen.getByRole('combobox', { name: /Client Response/ }),
      'RETURNED',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Record Response' }));
    expect(screen.getByText("Enter the client's remarks")).toBeInTheDocument();
    expect(record).not.toHaveBeenCalled();
    await userEvent.type(
      screen.getByRole('textbox', { name: /Client Remarks/ }),
      'Lower deductible',
    );
    await userEvent.click(screen.getByRole('button', { name: 'Record Response' }));
    expect(record).toHaveBeenCalledWith(
      10,
      expect.objectContaining({ response: 'RETURNED', remarks: 'Lower deductible' }),
    );
  });
});
