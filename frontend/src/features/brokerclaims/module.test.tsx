import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext } from '@/auth/authContext';
import type { AuthState } from '@/auth/authContext';
import { mayOpen } from '@/navigation/access';
import { MODULES, NAV_GROUPS } from '@/navigation/modules';
import { ClaimsPlaceholder } from './ClaimsPlaceholder';
import ClaimsHomePage from './home/ClaimsHomePage';
import { brokerClaimsModule } from './module';

/** Permissions of the demo Claims Officer clmofficer (V1020 / V1920). */
const OFFICER = new Set([
  'BCL_VIEW',
  'BCL_COVER_VIEW',
  'BCL_RECORD',
  'BCL_AUTHORIZE',
  'BCL_STATUS_UPDATE',
  'BCL_ACTION_PLAN',
  'BCL_LOCATION_REF_MAINTAIN',
  'BCL_REPORT_VIEW',
  'BCL_REPORT_EXPORT',
]);

const paths = brokerClaimsModule.screens.map((s) => s.path);

function renderHome(permissions: ReadonlySet<string>) {
  const auth: AuthState = {
    user: null,
    loading: false,
    login: () => Promise.resolve(),
    logout: () => undefined,
    can: (p) => permissions.has(p),
    passwordChange: null,
    passwordChanged: () => undefined,
  };
  render(
    <MemoryRouter>
      <AuthContext.Provider value={auth}>
        <ClaimsHomePage />
      </AuthContext.Provider>
    </MemoryRouter>,
  );
}

describe('Claims Handling module', () => {
  it('declares every route of the design under /claims-handling', () => {
    expect(paths).toEqual([
      '/claims-handling',
      '/claims-handling/worklist',
      '/claims-handling/new',
      '/claims-handling/:id',
      '/claims-handling/covers',
      '/claims-handling/diary',
      '/claims-handling/location-refs',
      '/claims-handling/reports',
      '/claims-handling/setup',
    ]);
    expect(brokerClaimsModule.screens.filter((s) => s.hidden === true).map((s) => s.path)).toEqual(
      ['/claims-handling/:id'],
    );
    brokerClaimsModule.screens.forEach((s) => expect(s.permission).toMatch(/^BCL_/));
  });

  it('comes first in the Claims & Insurance group', () => {
    const group = NAV_GROUPS.find((g) => g.id === 'insurance');
    expect(group?.modules[0]?.id).toBe('brokerclaims');
    expect(MODULES.filter((m) => m.id === 'brokerclaims')).toHaveLength(1);
  });

  it('opens the landing and the officer screens for a Claims Officer, not the set-up', () => {
    const can = (p: string) => OFFICER.has(p);
    const open = brokerClaimsModule.screens.filter((s) => mayOpen(s, can)).map((s) => s.path);
    expect(open).toContain('/claims-handling');
    expect(open).toContain('/claims-handling/new');
    expect(open).not.toContain('/claims-handling/setup');
  });

  it('gives Marketing the reports only', () => {
    const can = (p: string) => p === 'BCL_REPORT_VIEW';
    const open = brokerClaimsModule.screens.filter((s) => mayOpen(s, can)).map((s) => s.path);
    expect(open).toEqual(['/claims-handling/reports']);
  });

  it('shows a placeholder with the section breadcrumb and the empty state', () => {
    render(
      <MemoryRouter>
        <ClaimsPlaceholder
          title="Claims Worklist"
          description="Every claim you may see."
          emptyMessage="No claims to display"
          backTo="/claims-handling"
        />
      </MemoryRouter>,
    );
    expect(screen.getByRole('heading', { level: 1, name: 'Claims Worklist' })).toBeInTheDocument();
    expect(screen.getByText('Claims & Insurance · Claims Handling')).toBeInTheDocument();
    expect(screen.getByText('No claims to display')).toBeInTheDocument();
  });

  it('shows the Claims Home landing with Record Claim for a Claims Officer', () => {
    renderHome(OFFICER);
    expect(screen.getByRole('heading', { level: 1, name: 'Claims Home' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Record Claim' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Open Worklist' })).toBeInTheDocument();
    expect(screen.getByText('No claims to show yet')).toBeInTheDocument();
  });

  it('hides Record Claim without BCL_RECORD', () => {
    renderHome(new Set(['BCL_VIEW']));
    expect(screen.queryByRole('button', { name: 'Record Claim' })).not.toBeInTheDocument();
  });
});
