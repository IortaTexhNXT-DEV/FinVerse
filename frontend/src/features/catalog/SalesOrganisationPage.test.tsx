import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { catalogApi } from '@/api/catalog';
import type { SalesOrganisation, SalesUnit } from '@/api/catalog';
import { resetUserDirectory, setUserDirectory } from '@/api/users';
import { ebWrapper } from '@/features/eb/testWrapper';
import SalesOrganisationPage from './SalesOrganisationPage';
import { blockers, orgView, salesTree } from './salesTree';

const unit = (
  code: string,
  level: SalesUnit['level'],
  parentCode?: string,
  costCenter?: string,
  recordStatus: SalesUnit['recordStatus'] = 'ACTIVE',
): SalesUnit => ({
  id: code.length * 10 + code.charCodeAt(0),
  code,
  name: `${code} name`,
  level,
  parentCode,
  costCenter,
  recordStatus,
  maker: 'badmin',
  authorizedBy: 'SYSTEM',
});

const ORG: SalesOrganisation = {
  units: [
    unit('NCR', 'REGION'),
    unit('VIS', 'REGION'),
    unit('CBG-NCR', 'DEPARTMENT', 'NCR'),
    unit('OLD-NCR', 'DEPARTMENT', 'NCR', undefined, 'INACTIVE'),
    unit('CBG-VIS', 'DEPARTMENT', 'VIS', 'NB-CBG-V'),
    unit('T-CBG1', 'TEAM', 'CBG-NCR', 'NB-CBG-M'),
    unit('T-VIS1', 'TEAM', 'CBG-VIS'),
  ],
  officers: [
    {
      id: 1,
      teamCode: 'T-CBG1',
      username: 'ao',
      recordStatus: 'ACTIVE',
      assignedSince: '2026-09-01',
    },
    {
      id: 2,
      teamCode: 'T-CBG1',
      username: 'mkttl',
      recordStatus: 'INACTIVE',
      statusReason: 'Moved',
    },
  ],
};

function renderPage(permissions: string[]) {
  return render(ebWrapper(new Set(permissions), 'badmin')(<SalesOrganisationPage />));
}

describe('Sales organisation', () => {
  beforeEach(() => {
    setUserDirectory([
      { username: 'ao', displayName: 'Aileen Account Officer', roleName: 'Account Officer' },
      { username: 'mkttl', displayName: 'Mark Team Lead', roleName: 'Marketing Team Leader' },
    ]);
  });
  afterEach(() => {
    vi.restoreAllMocks();
    resetUserDirectory();
  });

  it('builds the rows with inherited cost centers, search matches and counts', () => {
    const tree = salesTree(ORG);
    const nameOf = (login: string) => (login === 'ao' ? 'Aileen Account Officer' : login);
    const all = orgView(tree, { showInactive: true, search: '', nameOf });
    expect(all.counts).toEqual({ regions: 2, departments: 3, teams: 2, officers: 2 });
    const active = orgView(tree, { showInactive: false, search: '', nameOf });
    expect(active.counts).toEqual({ regions: 2, departments: 2, teams: 2, officers: 1 });
    const found = orgView(tree, { showInactive: false, search: 'aileen', nameOf });
    expect(found.nodes.map((n) => n.key)).toEqual(['unit:NCR']);
    expect(found.matchPath).toEqual(['unit:T-CBG1', 'unit:CBG-NCR', 'unit:NCR']);
    const byUnit = orgView(tree, { showInactive: false, search: 'cbg-vis', nameOf });
    expect(byUnit.counts).toEqual({ regions: 1, departments: 1, teams: 1, officers: 0 });
    const visTeam = tree[1]?.children[0]?.children[0];
    expect(visTeam?.costCenter).toBe('NB-CBG-V');
    expect(visTeam?.costCenterFrom).toBe('CBG-VIS');
    expect(blockers(tree[0]!)).toEqual(['1 active sub-unit']);
    expect(blockers(tree[0]!.children[0]!.children[0]!)).toEqual(['1 active account officer']);
  });

  it('shows the organisation as a tree table with names, inherited cost centers and a summary', async () => {
    const user = userEvent.setup();
    vi.spyOn(catalogApi, 'salesOrganisation').mockResolvedValue(ORG);
    renderPage(['MASTER_MAINTAIN']);
    expect(await screen.findByText('NCR')).toBeInTheDocument();
    const grid = screen.getByRole('treegrid', { name: 'Sales organisation' });
    expect(screen.getByText('2 regions, 2 departments, 2 teams and 1 officer')).toBeInTheDocument();
    expect(within(grid).getByText('Inherited from CBG-VIS')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Expand All' }));
    expect(within(grid).getByText('Aileen Account Officer')).toBeInTheDocument();
    expect(within(grid).getByText('Account Officer', { selector: '.muted' })).toBeInTheDocument();
    expect(within(grid).getByText('01-Sep-2026')).toBeInTheDocument();
    expect(within(grid).queryByText('ao')).not.toBeInTheDocument();
    expect(within(grid).queryByText('Mark Team Lead')).not.toBeInTheDocument();
    await user.click(screen.getByLabelText('Show Inactive'));
    expect(within(grid).getByText('Mark Team Lead')).toBeInTheDocument();
    expect(within(grid).getByText('OLD-NCR')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Collapse All' }));
    expect(within(grid).queryByText('CBG-NCR')).not.toBeInTheDocument();
  });

  it('searches officers by name and expands their branch', async () => {
    const user = userEvent.setup();
    vi.spyOn(catalogApi, 'salesOrganisation').mockResolvedValue(ORG);
    renderPage(['MASTER_MAINTAIN']);
    await screen.findByText('NCR');
    await user.type(screen.getByPlaceholderText('Search unit code, name or officer'), 'aileen');
    await user.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByText('Aileen Account Officer')).toBeInTheDocument();
    expect(screen.queryByText('VIS')).not.toBeInTheDocument();
    expect(screen.getByText('1 region, 1 department, 1 team and 1 officer')).toBeInTheDocument();
  });

  it('refuses to deactivate a unit with active members and asks a reason otherwise', async () => {
    const user = userEvent.setup();
    vi.spyOn(catalogApi, 'salesOrganisation').mockResolvedValue(ORG);
    const deactivate = vi
      .spyOn(catalogApi, 'deactivateSalesUnit')
      .mockResolvedValue({ ...unit('T-VIS1', 'TEAM'), recordStatus: 'INACTIVE' });
    renderPage(['MASTER_MAINTAIN']);
    await screen.findByText('NCR');
    await user.click(screen.getByRole('button', { name: 'Actions for NCR' }));
    const menu = screen.getByRole('menu');
    expect(
      within(menu)
        .getAllByRole('menuitem')
        .map((i) => i.textContent),
    ).toEqual(['View / Edit', 'Add Department', 'Deactivate']);
    await user.click(within(menu).getByRole('menuitem', { name: 'Deactivate' }));
    const refused = screen.getByRole('dialog');
    expect(within(refused).getByText('Cannot deactivate NCR')).toBeInTheDocument();
    expect(within(refused).getByText('1 active sub-unit still in the unit')).toBeInTheDocument();
    await user.click(within(refused).getByRole('button', { name: 'Go Back' }));

    await user.click(screen.getByRole('button', { name: 'Expand All' }));
    await user.click(screen.getByRole('button', { name: 'Actions for T-VIS1' }));
    expect(screen.getByRole('menuitem', { name: 'Assign Officer' })).toBeInTheDocument();
    await user.click(screen.getByRole('menuitem', { name: 'Deactivate' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Deactivate' }));
    expect(within(dialog).getByText('Enter the reason.')).toBeInTheDocument();
    await user.type(within(dialog).getByLabelText('Reason'), 'Team merged');
    await user.click(within(dialog).getByRole('button', { name: 'Deactivate' }));
    await waitFor(() =>
      expect(deactivate).toHaveBeenCalledWith(unit('T-VIS1', 'TEAM').id, 'Team merged'),
    );
  });

  it('removes an officer with a reason and shows the unit details with its audit', async () => {
    const user = userEvent.setup();
    vi.spyOn(catalogApi, 'salesOrganisation').mockResolvedValue(ORG);
    const remove = vi
      .spyOn(catalogApi, 'removeSalesOfficer')
      .mockResolvedValue({ ...ORG.officers[0]!, recordStatus: 'INACTIVE' });
    renderPage(['MASTER_MAINTAIN']);
    await screen.findByText('NCR');
    await user.click(screen.getByRole('button', { name: 'Expand All' }));
    await user.click(screen.getByRole('button', { name: 'Actions for ao' }));
    expect(screen.getByRole('menuitem', { name: 'Reassign' })).toBeInTheDocument();
    await user.click(screen.getByRole('menuitem', { name: 'Remove from Team' }));
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText('Aileen Account Officer – team T-CBG1')).toBeInTheDocument();
    await user.type(within(dialog).getByLabelText('Reason'), 'Resigned');
    await user.click(within(dialog).getByRole('button', { name: 'Remove from Team' }));
    await waitFor(() => expect(remove).toHaveBeenCalledWith(1, 'Resigned'));

    await user.click(screen.getByRole('button', { name: 'Actions for T-VIS1' }));
    await user.click(screen.getByRole('menuitem', { name: 'View / Edit' }));
    const detail = screen.getByRole('dialog', { name: 'Team T-VIS1' });
    expect(within(detail).getByText('NB-CBG-V (inherited from CBG-VIS)')).toBeInTheDocument();
    expect(within(detail).getByText('CBG-VIS – CBG-VIS name')).toBeInTheDocument();
    expect(within(detail).getByRole('button', { name: 'Edit' })).toBeInTheDocument();
  });

  it('shows the empty state and hides the maintenance actions from readers', async () => {
    vi.spyOn(catalogApi, 'salesOrganisation').mockResolvedValue({ units: [], officers: [] });
    renderPage([]);
    expect(await screen.findByText('No sales unit defined yet')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'New Unit' })).not.toBeInTheDocument();
  });
});
