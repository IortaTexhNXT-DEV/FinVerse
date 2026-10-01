import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { dataScopeApi } from '@/api/dataScope';
import type { CompanyUnit } from '@/api/dataScope';
import type { AccessRequest } from '@/api/nbadmin';
import { ebWrapper } from '@/features/eb/testWrapper';
import {
  EMPTY_ACCESS_REQUEST,
  fromAccessRequest,
  toAccessRequest,
  validateAccessRequest,
} from './accessRequest';
import { DataAccessFields, RequestedDataAccess } from './DataAccessFields';

const UNITS: CompanyUnit[] = [
  { id: 1, code: 'HO', name: 'Head Company', branches: [{ id: 10, code: 'MKT', name: 'Makati' }] },
];

const NARROW = {
  allCompanies: false,
  companies: [{ companyId: 1, allBranches: true, branchIds: [] }],
};

describe('Data access of a user request', () => {
  beforeEach(() => {
    vi.spyOn(dataScopeApi, 'units').mockResolvedValue(UNITS);
  });
  afterEach(() => vi.restoreAllMocks());

  it('gives a new user all companies until the requester selects some', async () => {
    const set = vi.fn();
    render(
      ebWrapper(new Set(['UAM_ENROLL']))(
        <DataAccessFields form={EMPTY_ACCESS_REQUEST} set={set} errors={{}} users={[]} />,
      ),
    );
    await screen.findByText('Head Company');
    expect(screen.getByRole('checkbox', { name: /All companies/ })).toBeChecked();
    await userEvent.click(screen.getByRole('checkbox', { name: /All companies/ }));
    expect(set).toHaveBeenCalledWith({ dataScope: { allCompanies: false, companies: [] } });
  });

  it('shows the current data access of the user of a modification', async () => {
    vi.spyOn(dataScopeApi, 'ofUser').mockResolvedValue({
      allCompanies: false,
      companies: [
        { companyId: 1, code: 'HO', name: 'Head Company', allBranches: true, branches: [] },
      ],
      description: 'HO (all branches)',
    });
    const set = vi.fn();
    render(
      ebWrapper(new Set(['UAM_MODIFY']))(
        <DataAccessFields
          form={{
            ...EMPTY_ACCESS_REQUEST,
            type: 'MODIFY_USER',
            username: 'maker1',
            dataScope: NARROW,
          }}
          set={set}
          errors={{ dataScope: 'Select at least one company, or give access to all companies.' }}
          users={[
            {
              id: 5,
              username: 'maker1',
              fullName: 'Maker',
              enabled: true,
              locked: false,
              roleCodes: [],
            },
          ]}
        />,
      ),
    );
    expect(await screen.findByText('Current: HO (all branches)')).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent('Select at least one company');
    await userEvent.click(screen.getByRole('button', { name: 'Keep the current data access' }));
    expect(set).toHaveBeenCalledWith({ dataScope: undefined });
  });

  it('sends the requested data access and reads it back from a saved request', () => {
    const form = { ...EMPTY_ACCESS_REQUEST, username: 'u000000001', dataScope: NARROW };
    expect(toAccessRequest(form).dataScope).toEqual(NARROW);
    expect(
      validateAccessRequest(
        { ...form, dataScope: { allCompanies: false, companies: [] } },
        { users: [], submit: true },
      ).dataScope,
    ).toBe('Select at least one company, or give access to all companies.');
    const saved = {
      id: 1,
      type: 'MODIFY_USER',
      userType: 'INTERNAL',
      username: 'maker1',
      roleCodes: [],
      permissionsAdded: [],
      permissionsRemoved: [],
      details: { unlock: false, dataScope: '1:*' },
      lifecycle: { approvers: [] },
    } as unknown as AccessRequest;
    expect(fromAccessRequest(saved, []).dataScope).toEqual(NARROW);
  });

  it('shows the requested data access of a request read-only', async () => {
    render(ebWrapper(new Set(['UAM_VIEW']))(<RequestedDataAccess text="1:10" />));
    expect(await screen.findByText('HO: MKT')).toBeInTheDocument();
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    const { container } = render(
      ebWrapper(new Set(['UAM_VIEW']))(<RequestedDataAccess text={undefined} />),
    );
    expect(container).toBeEmptyDOMElement();
  });
});
