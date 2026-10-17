import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import type { InsurerDetail } from '@/api/catalog';
import { insurerAccountsApi } from '@/api/insurerPage';
import { ebWrapper } from '@/features/eb/testWrapper';
import InsurerDetailPage from './InsurerDetailPage';
import { bankAccountInputOf, contactInputOf, remittanceInputOf } from './insurerPageForm';

const DETAIL: InsurerDetail = {
  insurer: {
    id: 7,
    companyId: 1,
    partyCode: 'INS-1',
    name: 'Panel Mutual',
    placementChannel: 'EMAIL',
    placementEmails: [],
    defaultCreditDays: 30,
    recordStatus: 'ACTIVE',
    remittance: { paymentTerms: '30 days from statement', cycle: 'MONTHLY', mode: 'CHECK' },
  },
  branches: [],
  commissions: [],
  contacts: [
    {
      id: 1,
      insurerId: 7,
      role: 'CLAIMS',
      name: 'Claims Desk',
      recordStatus: 'PENDING_AUTHORIZATION',
    },
  ],
  bankAccounts: [
    {
      id: 2,
      insurerId: 7,
      purpose: 'REMITTANCE',
      bankName: 'Test Bank',
      accountNo: '0001-2345',
      accountName: 'Panel Mutual',
      currency: 'PHP',
      primaryAccount: true,
      recordStatus: 'ACTIVE',
    },
  ],
  products: [
    {
      productCode: 'PAR01',
      productName: 'Property',
      role: 'LEAD',
      versionNo: 2,
      versionStatus: 'RELEASED',
      commission: true,
    },
  ],
};

describe('insurer page forms', () => {
  it('clears blank fields', () => {
    expect(remittanceInputOf({ paymentTerms: ' ', email: 'a@b.example ' })).toEqual({
      paymentTerms: null,
      cycle: null,
      mode: null,
      email: 'a@b.example',
      currency: null,
    });
    expect(contactInputOf({ name: ' Desk ', phone: '' })).toEqual({
      role: 'OTHER',
      name: 'Desk',
      position: null,
      email: null,
      phone: null,
    });
    expect(bankAccountInputOf({ accountNo: ' 1 ', bankName: 'B' })).toMatchObject({
      purpose: 'REMITTANCE',
      accountNo: '1',
      primaryAccount: false,
    });
  });
});

describe('InsurerDetailPage', () => {
  it('shows contacts, remittance, bank accounts and linked products', async () => {
    vi.spyOn(catalogApi, 'insurer').mockResolvedValue(DETAIL);
    const wrap = ebWrapper(
      new Set(['MASTER_VIEW', 'MASTER_MAINTAIN']),
      'badmin',
      '/catalog/insurers/7',
    );
    render(
      wrap(
        <Routes>
          <Route path="/catalog/insurers/:id" element={<InsurerDetailPage />} />
        </Routes>,
      ),
    );
    expect(await screen.findByText('Panel Mutual', { selector: 'h1' })).toBeInTheDocument();
    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: 'Contacts' }));
    expect(screen.getByText('Claims Desk')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Payment & Remittance' }));
    expect(screen.getByText('30 days from statement')).toBeInTheDocument();
    expect(screen.getByText('Monthly')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Bank Accounts' }));
    expect(screen.getByText('0001-2345')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Products' }));
    expect(screen.getByText(/PAR01/)).toBeInTheDocument();
    vi.spyOn(insurerAccountsApi, 'accounts').mockResolvedValue({
      glAccounts: [
        {
          code: '2101',
          name: 'Premium payable to insurers',
          accountClass: 'LIABILITY',
          controlAccount: true,
          subLedgerType: 'INSURER',
          entries: 2,
          debit: 100,
          credit: 400,
          balance: -300,
          lastDate: '2026-10-10',
        },
      ],
      subLedger: [
        {
          documentType: 'PREMIUM_PAYABLE',
          direction: 'CREDIT',
          currency: 'PHP',
          status: 'PARTIAL',
          items: 1,
          amount: 1000,
          settled: 250,
          outstanding: 750,
        },
      ],
    });
    await user.click(screen.getByRole('tab', { name: 'Ledger Accounts' }));
    expect(await screen.findByText('Premium payable to insurers')).toBeInTheDocument();
    expect(screen.getByText('750.00')).toBeInTheDocument();
  });
});
