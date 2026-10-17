import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ebApi } from '@/api/eb';
import { mayOpen } from '@/navigation/access';
import { MODULES, NAV_GROUPS } from '@/navigation/modules';
import { EbPlaceholder } from './EbPlaceholder';
import EbHomePage from './home/EbHomePage';
import { EB_HOME_TILES, tileValue } from './home/ebHomeTiles';
import { ebModule } from './module';
import { ebWrapper } from './testWrapper';

/** Permissions of the EB_AO role (V1030). */
const AO = new Set(['EB_VIEW', 'EB_MARKET', 'EB_REPORT_VIEW', 'WORK_VIEW']);

const paths = ebModule.screens.map((s) => s.path);

function openFor(permissions: readonly string[]) {
  const can = (p: string) => permissions.includes(p);
  return ebModule.screens.filter((s) => mayOpen(s, can)).map((s) => s.path);
}

describe('Employee Benefits module', () => {
  afterEach(() => vi.restoreAllMocks());

  it('declares every internal route of the design under /eb and no portal route', () => {
    expect(paths).toEqual([
      '/eb',
      '/eb/programmes',
      '/eb/programmes/new',
      '/eb/programmes/:id',
      '/eb/comparatives/:id',
      '/eb/member-changes',
      '/eb/pending-items',
      '/eb/soa',
      '/eb/setup',
    ]);
    expect(ebModule.screens.filter((s) => s.hidden === true).map((s) => s.path)).toEqual([
      '/eb/programmes/:id',
      '/eb/comparatives/:id',
    ]);
    ebModule.screens.forEach((s) => expect(s.permission).toMatch(/^EB_/));
    expect(MODULES.flatMap((m) => m.screens).some((s) => s.path.includes('portal'))).toBe(false);
  });

  it('comes right after Non-Package Management in Client & Policy', () => {
    const group = NAV_GROUPS.find((g) => g.id === 'client-policy');
    const ids = group?.modules.map((m) => m.id) ?? [];
    expect(ids[ids.indexOf('eb') - 1]).toBe('proposals');
    expect(MODULES.filter((m) => m.id === 'eb')).toHaveLength(1);
  });

  it('opens the AO screens for an EB AO, not the SOA register or the set-up', () => {
    const open = openFor([...AO]);
    expect(open).toContain('/eb/programmes/new');
    expect(open).not.toContain('/eb/soa');
    expect(open).not.toContain('/eb/setup');
  });

  it('opens the SOA register for Collection and Processing', () => {
    expect(openFor(['EB_VIEW', 'EB_COLLECT'])).toContain('/eb/soa');
    expect(openFor(['EB_VIEW', 'EB_PROCESS'])).toContain('/eb/soa');
    expect(openFor(['EB_VIEW', 'EB_COLLECT'])).not.toContain('/eb/programmes/new');
  });

  it('shows a placeholder with the section breadcrumb and the empty state', () => {
    render(
      <MemoryRouter>
        <EbPlaceholder
          title="Comparative"
          description="The comparative."
          emptyMessage="No comparative to display"
        />
      </MemoryRouter>,
    );
    expect(screen.getByRole('heading', { level: 1, name: 'Comparative' })).toBeInTheDocument();
    expect(screen.getByText('Client & Policy · Employee Benefits')).toBeInTheDocument();
    expect(screen.getByText('No comparative to display')).toBeInTheDocument();
  });

  it('shows EB Home with its tiles, counts and New Programme for an AO', async () => {
    vi.spyOn(ebApi, 'home').mockResolvedValue({
      raDue: 4,
      awaitingFeedback: 2,
      pendingItemsOverdue: 0,
    });
    render(ebWrapper(AO)(<EbHomePage />));
    expect(screen.getByRole('heading', { level: 1, name: 'EB Home' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'New Programme' })).toBeInTheDocument();
    EB_HOME_TILES.forEach((t) =>
      expect(screen.getByRole('link', { name: t.label })).toHaveAttribute('href', t.to),
    );
    expect(await screen.findByText('4')).toBeInTheDocument();
    expect(screen.getByText('Needs attention')).toBeInTheDocument();
  });

  it('hides New Programme without EB_MARKET', () => {
    vi.spyOn(ebApi, 'home').mockResolvedValue({});
    render(ebWrapper(new Set(['EB_VIEW']))(<EbHomePage />));
    expect(screen.queryByRole('button', { name: 'New Programme' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Open Programmes' })).toBeInTheDocument();
  });

  it('shows a dash for a tile without a count', () => {
    expect(tileValue({ raDue: 3 }, 'raDue')).toBe('3');
    expect(tileValue({ raDue: 3 }, 'memberChangesOpen')).toBe('–');
    expect(tileValue(undefined, 'raDue')).toBe('–');
  });
});
