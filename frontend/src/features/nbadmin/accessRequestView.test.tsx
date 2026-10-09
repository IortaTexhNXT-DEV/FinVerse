import { render, screen, within } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { lovApi } from '@/api/lov';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest, RoleInfo, UserAccess } from '@/api/nbadmin';
import { ebWrapper } from '@/features/eb/testWrapper';
import AccessRequestPage from './AccessRequestPage';
import { accessRequestStages, accessStatusLabel } from './accessStages';

const role = (code: string, name: string, module?: string, active = true): RoleInfo => ({
  id: code.length,
  code,
  name,
  permissions: [],
  active,
  privilegeLevel: 'STANDARD',
  module,
});

const ROLES = [
  role('MKT_AO', 'Account Officer', 'New Business'),
  role('MIG_DO', 'Migration Data Owner', 'Data Migration'),
  role('SYSADMIN', 'System Administrator'),
  role('OLD', 'Old Profile', 'New Business', false),
];

const REQUEST: AccessRequest = {
  id: 7,
  requestNo: 'AR-2026-000007',
  type: 'MODIFY_USER',
  userType: 'INTERNAL',
  summary: 'Modify user',
  username: 'jdelacruz',
  roleCodes: ['MKT_AO', 'MIG_DO'],
  status: 'PENDING',
  requestedBy: 'requestor',
  requestedAt: '2026-09-25T01:00:00Z',
  permissionsAdded: [],
  permissionsRemoved: [],
  returnedCount: 0,
  details: { unlock: false },
  lifecycle: {
    assignedApprover: 'uamapprover',
    approvers: [{ sequence: 1, approver: 'uamapprover', decision: 'PENDING' }],
    riskFlags: [],
    secondApprovalRequired: false,
  },
};

const USER = {
  username: 'jdelacruz',
  fullName: 'Juan Dela Cruz',
  roleCodes: ['MKT_AO', 'SYSADMIN'],
} as unknown as UserAccess;

describe('access request labels', () => {
  it('names a request waiting for its approver Pending Approval everywhere', () => {
    expect(accessStatusLabel('PENDING')).toBe('Pending Approval');
    expect(accessStatusLabel('RETURNED')).toBe('Returned');
    const stages = accessRequestStages({
      type: 'CREATE_USER',
      status: 'PENDING',
      lifecycle: REQUEST.lifecycle,
    });
    expect(stages.find((s) => s.code === 'PENDING')?.name).toBe('Pending Approval');
  });
});

describe('access request page', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows the subject by name, requested by and on apart, and the profile changes as rows', async () => {
    vi.spyOn(nbadminApi, 'request').mockResolvedValue(REQUEST);
    vi.spyOn(nbadminApi, 'roles').mockResolvedValue(ROLES);
    vi.spyOn(nbadminApi, 'users').mockResolvedValue([USER]);
    vi.spyOn(nbadminApi, 'settings').mockResolvedValue({ anyApprover: false } as never);
    vi.spyOn(lovApi, 'options').mockResolvedValue([]);
    render(
      ebWrapper(
        new Set(),
        'viewer',
        '/requests/7',
      )(
        <Routes>
          <Route path="/requests/:id" element={<AccessRequestPage />} />
        </Routes>,
      ),
    );
    expect(await screen.findByText('Modify user · Juan Dela Cruz (jdelacruz)')).toBeInTheDocument();
    expect(screen.getByText('Requested By')).toBeInTheDocument();
    expect(screen.getByText('Requested On')).toBeInTheDocument();
    expect(screen.getAllByText('Pending Approval').length).toBeGreaterThan(0);
    expect(screen.queryByText('Pending', { exact: true })).not.toBeInTheDocument();
    const changes = await screen.findByRole('table', { name: 'Group profiles added and removed' });
    const rows = within(changes).getAllByRole('row').slice(1);
    expect(rows.map((r) => r.textContent)).toEqual([
      'Migration Data OwnerAdded',
      'System AdministratorRemoved',
    ]);
  });
});
