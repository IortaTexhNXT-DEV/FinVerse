import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { dataScopeApi } from '@/api/dataScope';
import type { CompanyUnit, DataScopeValue } from '@/api/dataScope';
import { ebWrapper } from '@/features/eb/testWrapper';
import { DataAccessDialog } from './DataAccessDialog';
import { DataAccessSection } from './DataAccessSection';

const UNITS: CompanyUnit[] = [
  {
    id: 1,
    code: 'HO',
    name: 'Head Company',
    branches: [
      { id: 10, code: 'MKT', name: 'Makati' },
      { id: 11, code: 'CEB', name: 'Cebu' },
    ],
  },
  { id: 2, code: 'SUB', name: 'Subsidiary', branches: [] },
];

function Editable({ initial }: Readonly<{ initial: DataScopeValue }>) {
  const [scope, setScope] = useState(initial);
  return (
    <>
      <DataAccessSection units={UNITS} scope={scope} onChange={setScope} />
      <output data-testid="scope">{JSON.stringify(scope)}</output>
    </>
  );
}

const scopeShown = () => JSON.parse(screen.getByTestId('scope').textContent) as unknown;

describe('Data access section', () => {
  it('shows a read-only scope as a tree of companies and branches without inputs', () => {
    render(
      <DataAccessSection
        units={UNITS}
        scope={{
          allCompanies: false,
          companies: [{ companyId: 1, allBranches: false, branchIds: [11] }],
        }}
      />,
    );
    const grid = screen.getByRole('treegrid', { name: 'Data access' });
    expect(within(grid).queryByRole('checkbox')).not.toBeInTheDocument();
    const rows = within(grid).getAllByRole('row').slice(1);
    expect(rows.map((r) => r.getAttribute('aria-level'))).toEqual(['1', '2', '2', '1']);
    expect(within(rows[0]!).getByText('Yes')).toBeInTheDocument();
    expect(within(rows[0]!).getByText('Selected branches')).toBeInTheDocument();
    expect(within(rows[1]!).getByText('No')).toBeInTheDocument();
    expect(within(rows[2]!).getByText('Yes')).toBeInTheDocument();
    expect(within(rows[3]!).getByText('No')).toBeInTheDocument();
    expect(screen.getByText('Only the companies and branches marked below.')).toBeInTheDocument();
  });

  it('starts from all companies and narrows to a company and a branch', async () => {
    render(<Editable initial={{ allCompanies: true, companies: [] }} />);
    const all = screen.getByRole('checkbox', { name: /All companies/ });
    expect(all).toBeChecked();
    expect(screen.getByRole('checkbox', { name: 'Access to Head Company' })).toBeDisabled();
    await userEvent.click(all);
    expect(scopeShown()).toEqual({ allCompanies: false, companies: [] });
    await userEvent.click(screen.getByRole('checkbox', { name: 'Access to Head Company' }));
    expect(scopeShown()).toEqual({
      allCompanies: false,
      companies: [{ companyId: 1, allBranches: true, branchIds: [] }],
    });
    await userEvent.click(screen.getByRole('checkbox', { name: 'All branches' }));
    await userEvent.click(screen.getAllByRole('button', { name: 'Expand' })[0]!);
    await userEvent.click(screen.getByRole('checkbox', { name: 'Access to Cebu' }));
    expect(scopeShown()).toEqual({
      allCompanies: false,
      companies: [{ companyId: 1, allBranches: false, branchIds: [11] }],
    });
  });
});

describe('Data access dialog', () => {
  afterEach(() => vi.restoreAllMocks());

  it('is read-only without the direct edit and offers a request instead', async () => {
    vi.spyOn(dataScopeApi, 'units').mockResolvedValue(UNITS);
    vi.spyOn(dataScopeApi, 'ofUser').mockResolvedValue({
      allCompanies: true,
      companies: [],
      description: 'All companies',
    });
    const raise = vi.fn();
    render(
      ebWrapper(new Set(['UAM_VIEW']))(
        <DataAccessDialog
          user={{ id: 5, username: 'maker1' }}
          editable={false}
          onRaiseRequest={raise}
          onClose={() => undefined}
        />,
      ),
    );
    expect(await screen.findByText('Head Company')).toBeInTheDocument();
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    expect(screen.getByText('All companies')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Raise Request' }));
    expect(raise).toHaveBeenCalled();
  });

  it('saves a narrowed scope in the emergency direct edit', async () => {
    vi.spyOn(dataScopeApi, 'units').mockResolvedValue(UNITS);
    vi.spyOn(dataScopeApi, 'ofUser').mockResolvedValue({
      allCompanies: true,
      companies: [],
      description: 'All companies',
    });
    const replace = vi.spyOn(dataScopeApi, 'replace').mockResolvedValue({
      allCompanies: false,
      companies: [],
      description: 'SUB (all branches)',
    });
    render(
      ebWrapper(new Set(['USER_MANAGE']))(
        <DataAccessDialog
          user={{ id: 5, username: 'maker1' }}
          editable
          onRaiseRequest={() => undefined}
          onClose={() => undefined}
        />,
      ),
    );
    await screen.findByText('Subsidiary');
    const save = screen.getByRole('button', { name: 'Save' });
    expect(save).toBeDisabled();
    await userEvent.click(screen.getByRole('checkbox', { name: /All companies/ }));
    expect(
      screen.getByText('Select at least one company, or give access to all companies.'),
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('checkbox', { name: 'Access to Subsidiary' }));
    await userEvent.click(save);
    await waitFor(() =>
      expect(replace).toHaveBeenCalledWith(5, {
        allCompanies: false,
        companies: [{ companyId: 2, allBranches: true, branchIds: [] }],
      }),
    );
  });
});
