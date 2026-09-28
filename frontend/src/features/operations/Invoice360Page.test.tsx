import { render, screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { opsApi } from '@/api/operations';
import type { Invoice360 } from '@/api/operations';
import { workflowApi } from '@/api/workflow';
import { ebWrapper } from '@/features/eb/testWrapper';
import Invoice360Page from './Invoice360Page';

function view(migrated: boolean): Invoice360 {
  return {
    invoice: {
      origin: migrated ? 'MIGRATED' : 'BIBS',
      keys: { invoiceNo: 'INV-1', arn: 'ARN-1', accountId: 42, kind: 'NEW', policyYear: 2027 },
      parties: { clientCode: 'C1', assuredName: 'Lea Santos', insurerCode: 'INS-MGIC' },
      classification: {
        currency: 'PHP',
        bookingDate: '2026-09-01',
        inceptionDate: '2026-09-01',
        expiryDate: '2027-09-01',
      },
      grossPremium: 1000,
      commission: 100,
      vatOnCommission: 12,
      wtaxRate: 0,
      premiumBalance: 1000,
      paymentStatus: 'UNPAID',
      remittanceStatus: 'UNPROCESSED',
      flags: {},
      feedSource: migrated ? 'MIGRATION' : 'EVENT',
      components: [],
      shares: [],
    },
    booking: { journalBatches: [] },
    movements: [],
    history: [],
    related: {},
    accountWorkflow: !migrated,
  } as unknown as Invoice360;
}

function page() {
  return ebWrapper(
    new Set(),
    'recon',
    '/operations/invoices/INV-1',
  )(
    <Routes>
      <Route path="/operations/invoices/:no" element={<Invoice360Page />} />
    </Routes>,
  );
}

describe('invoice 360', () => {
  afterEach(() => vi.restoreAllMocks());

  it('opens a migrated invoice without asking for the account workflow', async () => {
    vi.spyOn(opsApi, 'invoice').mockResolvedValue(view(true));
    const byRecord = vi.spyOn(workflowApi, 'byRecord');
    render(page());
    expect(await screen.findByText('LEGACY')).toBeInTheDocument();
    expect(byRecord).not.toHaveBeenCalled();
    expect(screen.queryByText(/work item not found/i)).not.toBeInTheDocument();
  });

  it('shows the account workflow of a BIBS invoice', async () => {
    vi.spyOn(opsApi, 'invoice').mockResolvedValue(view(false));
    const byRecord = vi
      .spyOn(workflowApi, 'byRecord')
      .mockRejectedValue(new Error('Not loaded in this test'));
    render(page());
    expect(await screen.findByText('Lea Santos')).toBeInTheDocument();
    expect(byRecord).toHaveBeenCalledWith('Account', 42);
  });
});
