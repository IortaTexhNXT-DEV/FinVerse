import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';
import type { PolicyTransactions } from '@/api/policyTransactions';
import { AuthContext } from '@/auth/authContext';
import { PolicyTransactionsTable } from './PolicyTransactions';
import { journalCountLabel } from './policyTransactionRows';

const JOURNAL = {
  id: 41,
  batchNo: 'JV-2026-000041',
  valueDate: '2026-09-26',
  status: 'POSTED',
  narration: 'Cancellation ENR-2026-000005',
  totalDebit: 1120,
  totalCredit: 1120,
  lines: [
    {
      lineNo: 1,
      accountCode: '2210',
      accountName: 'Due to Insurers',
      debit: 1120,
      partyCode: 'INS-MGIC',
    },
    {
      lineNo: 2,
      accountCode: '1210',
      accountName: 'Premium Receivable - Clients',
      credit: 1120,
      partyCode: 'CL-2026-000005',
    },
  ],
};

const HISTORY: PolicyTransactions = {
  rootInvoiceNo: 'BI-HO-2026-000004',
  arn: 'ARN-2026-940004',
  policyNo: 'MGIC-MC-0042',
  currency: 'PHP',
  rows: [
    {
      seq: 1,
      kind: 'BOOKING',
      date: '2026-09-10',
      effectiveDate: '2026-09-10',
      typeLabel: 'Original Booking',
      refs: { invoiceNo: 'BI-HO-2026-000004' },
      change: { premium: 1000, taxes: 120, gross: 1120, commission: 150 },
      after: { premium: 1000, taxes: 120, gross: 1120, commission: 150 },
      status: 'BOOKED',
      statusLabel: 'Booked',
      posted: true,
      journals: [],
    },
    {
      seq: 2,
      kind: 'CANCELLATION',
      date: '2026-09-26',
      typeLabel: 'Financial – Change of Cover',
      detail: 'Flat Cancellation',
      refs: { invoiceNo: 'BI-HO-2026-000009', requestNo: 'ENR-2026-000005', requestId: 5 },
      change: { premium: -1000, taxes: -120, gross: -1120, commission: -150 },
      after: { premium: 0, taxes: 0, gross: 0, commission: 0 },
      status: 'POSTED',
      statusLabel: 'Posted',
      posted: true,
      journals: [JOURNAL],
    },
    {
      seq: 3,
      kind: 'ENDORSEMENT',
      date: '2026-09-27',
      typeLabel: 'Non-financial – Cover Extension',
      refs: { invoiceNo: 'BI-HO-2026-000004', requestNo: 'ENR-2026-000007', requestId: 7 },
      change: { premium: 0, taxes: 0, gross: 0, commission: 0 },
      status: 'FOR_VALIDATION',
      statusLabel: 'For Validation',
      posted: false,
      journals: [],
    },
  ],
};

function wrap(children: ReactNode, journalAccess = true) {
  const auth = {
    user: null,
    loading: false,
    login: () => Promise.resolve({ expiresAt: '' }),
    completeSignIn: () => undefined,
    logout: () => undefined,
    can: (permission: string) => journalAccess || permission !== 'JOURNAL_VIEW',
    passwordChange: null,
    passwordChanged: () => undefined,
  };
  return (
    <MemoryRouter>
      <AuthContext.Provider value={auth}>{children}</AuthContext.Provider>
    </MemoryRouter>
  );
}

describe('policy transactions', () => {
  it('lists the booking first, then each transaction with its change, position and status', () => {
    render(wrap(<PolicyTransactionsTable history={HISTORY} />));
    const rows = screen.getAllByRole('row').slice(1);
    expect(within(rows[0]!).getByText('Original Booking')).toBeInTheDocument();
    expect(within(rows[1]!).getByText('Financial – Change of Cover')).toBeInTheDocument();
    expect(within(rows[1]!).getByText('Flat Cancellation')).toBeInTheDocument();
    expect(within(rows[1]!).getByText('(1,000.00)')).toBeInTheDocument();
    expect(within(rows[1]!).getByText('Posted')).toHaveClass('badge');
    expect(within(rows[2]!).getByText('Not posted')).toBeInTheDocument();
    expect(within(rows[2]!).getByText('For Validation')).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: 'Premium Change (PHP)' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'ENR-2026-000005' })).toHaveAttribute(
      'href',
      '/adjustment/requests/5',
    );
  });

  it('expands a transaction to its journals with the lines and a link to the journal', async () => {
    render(wrap(<PolicyTransactionsTable history={HISTORY} />));
    const toggle = screen.getByRole('button', { name: '1 Journal' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    await userEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByRole('link', { name: 'JV-2026-000041' })).toHaveAttribute(
      'href',
      '/gl/journals/41',
    );
    const lines = screen.getByRole('table', { name: 'Lines of journal JV-2026-000041' });
    expect(within(lines).getByText('Due to Insurers')).toBeInTheDocument();
    expect(within(lines).getAllByText('1,120.00')).toHaveLength(4);
    await userEvent.click(toggle);
    expect(screen.queryByRole('table', { name: 'Lines of journal JV-2026-000041' })).toBeNull();
  });

  it('shows the journal number without a link to users who cannot open journals', async () => {
    render(wrap(<PolicyTransactionsTable history={HISTORY} />, false));
    await userEvent.click(screen.getByRole('button', { name: '1 Journal' }));
    expect(screen.queryByRole('link', { name: 'JV-2026-000041' })).toBeNull();
    expect(screen.getByText('JV-2026-000041')).toBeInTheDocument();
  });

  it('highlights the transaction of the request being viewed', () => {
    render(
      wrap(<PolicyTransactionsTable history={HISTORY} highlightRequestNo="ENR-2026-000005" />),
    );
    const rows = screen.getAllByRole('row').slice(1);
    expect(rows[1]).toHaveAttribute('aria-selected', 'true');
  });

  it('labels the journal count', () => {
    expect(journalCountLabel(1)).toBe('1 Journal');
    expect(journalCountLabel(3)).toBe('3 Journals');
  });
});
