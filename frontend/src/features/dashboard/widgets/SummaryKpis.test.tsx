import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { DashboardSummary } from '@/api/dashboard';
import { AuthContext } from '@/auth/authContext';
import { SummaryKpis } from './SummaryWidgets';
import { approvalLines } from './widgetSupport';

const summary = {
  totalIncomeYtd: 1,
  totalExpenseYtd: 1,
  netResultYtd: 0,
  cashPosition: 1,
  receivables: 5000,
  totalAssets: 1,
  pendingJournals: 0,
  draftJournals: 0,
} as DashboardSummary;

function show(permissions: string[]) {
  const auth = {
    user: null,
    loading: false,
    login: () => Promise.resolve({ expiresAt: '' }),
    completeSignIn: () => undefined,
    logout: () => undefined,
    can: (p: string) => permissions.includes(p),
    passwordChange: null,
    passwordChanged: () => undefined,
  };
  render(
    <MemoryRouter>
      <AuthContext.Provider value={auth}>
        <SummaryKpis summary={summary} />
      </AuthContext.Provider>
    </MemoryRouter>,
  );
}

describe('general ledger KPI tiles', () => {
  it('shows the ledger tiles without an insurer technical reserves tile', () => {
    show(['GL_VIEW']);
    expect(screen.getByText('Insurance Receivables')).toBeInTheDocument();
    expect(screen.queryByText(/Technical reserves/i)).not.toBeInTheDocument();
    // Journals in progress as labelled lines, never "0 pending · 0 draft".
    expect(screen.getByRole('list', { name: 'Journals in progress by status' })).toBeTruthy();
    expect(document.body.textContent).not.toContain('·');
  });
});

describe('pending approvals tile', () => {
  it('lists the approvals by area as breakdown lines', () => {
    expect(approvalLines({ RECEIVABLES: 2, MASTER_DATA: 1 })).toEqual([
      { key: 'RECEIVABLES', label: 'Receivables', count: 2, to: '/approvals' },
      { key: 'MASTER_DATA', label: 'Master Data', count: 1, to: '/approvals' },
    ]);
  });
});
