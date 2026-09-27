import { render, screen } from '@testing-library/react';
import type { DashboardSummary } from '@/api/dashboard';
import { AuthContext } from '@/auth/authContext';
import { SummaryKpis } from './SummaryWidgets';

const summary = {
  totalIncomeYtd: 1,
  totalExpenseYtd: 1,
  netResultYtd: 0,
  cashPosition: 1,
  receivables: 5000,
  technicalReserves: 9000,
  totalAssets: 1,
  pendingJournals: 0,
  draftJournals: 0,
} as DashboardSummary;

function show(permissions: string[]) {
  const auth = {
    user: null,
    loading: false,
    login: () => Promise.resolve(),
    logout: () => undefined,
    can: (p: string) => permissions.includes(p),
    passwordChange: null,
    passwordChanged: () => undefined,
  };
  render(
    <AuthContext.Provider value={auth}>
      <SummaryKpis summary={summary} />
    </AuthContext.Provider>,
  );
}

describe('general ledger KPI tiles', () => {
  it('hides the insurer technical reserves from BDOI roles and keeps the receivables', () => {
    show(['GL_VIEW']);
    expect(screen.getByText('Insurance receivables')).toBeInTheDocument();
    expect(screen.queryByText('Technical reserves')).not.toBeInTheDocument();
  });

  it('shows the technical reserves to users of the insurer suite', () => {
    show(['RESERVE_VIEW']);
    expect(screen.getByText('Technical reserves')).toBeInTheDocument();
  });
});
