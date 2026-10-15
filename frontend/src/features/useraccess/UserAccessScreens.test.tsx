import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { adminApi } from '@/api/admin';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessRequest } from '@/api/nbadmin';
import type { UserProfile } from '@/api/types';
import { ebWrapper } from '@/features/eb/testWrapper';
import { PendingTab } from './PendingChangeTab';
import UsersPage from './UsersPage';

const user = (over: Partial<UserProfile>): UserProfile => ({
  id: 1,
  username: 'a100000001',
  fullName: 'Maria Santos',
  enabled: true,
  locked: false,
  roles: ['MKT_AO'],
  permissions: [],
  ...over,
});

const change: AccessRequest = {
  id: 9,
  requestNo: 'AR-2026-000009',
  type: 'MODIFY_ROLE_PERMISSIONS',
  userType: 'INTERNAL',
  summary: 'Change the permissions of group profile Account Officer: add Report view',
  roleCodes: [],
  status: 'PENDING',
  requestedBy: 'requestor',
  requestedAt: '2026-10-09T01:00:00Z',
  roleCode: 'MKT_AO',
  permissionsAdded: ['REPORT_VIEW'],
  permissionsRemoved: [],
  returnedCount: 0,
  justification: 'Reports for the officers',
  details: { unlock: false },
  lifecycle: { approvers: [], riskFlags: [], secondApprovalRequired: false },
};

describe('User Access screens', () => {
  afterEach(() => vi.restoreAllMocks());

  it('lists the users read-only and filters them by status', async () => {
    vi.spyOn(adminApi, 'users').mockResolvedValue([
      user({}),
      user({ id: 2, username: 'a100000002', fullName: 'Jose Reyes', locked: true }),
    ]);
    vi.spyOn(adminApi, 'roles').mockResolvedValue([
      { id: 1, code: 'MKT_AO', name: 'Account Officer', permissions: [] },
    ]);
    render(ebWrapper(new Set(['USER_MANAGE']))(<UsersPage />));
    expect(await screen.findByText('Jose Reyes')).toBeInTheDocument();
    expect(screen.getAllByText('Account Officer').length).toBeGreaterThan(1);
    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Status' }), 'LOCKED');
    const table = screen.getByRole('table', { name: 'Users' });
    expect(within(table).queryByText('Maria Santos')).not.toBeInTheDocument();
    expect(within(table).getByText('Jose Reyes')).toBeInTheDocument();
  });

  it('lets a checker approve a change of another user', async () => {
    const approve = vi
      .spyOn(nbadminApi, 'approve')
      .mockResolvedValue({ request: { ...change, status: 'APPROVED' } });
    render(
      ebWrapper(
        new Set(['ACCESS_APPROVE']),
        'uamapprover',
      )(<PendingTab change={change} onEdit={vi.fn()} />),
    );
    await userEvent.click(screen.getByRole('button', { name: 'Approve' }));
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Approve' }));
    expect(approve).toHaveBeenCalledWith(9, undefined);
  });

  it('never lets the maker decide the change', () => {
    render(
      ebWrapper(
        new Set(['ACCESS_APPROVE', 'UAM_GROUP_REQUEST']),
        'requestor',
      )(<PendingTab change={change} onEdit={vi.fn()} />),
    );
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument();
    expect(
      screen.getByText('Another user with the approval right decides your change.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel Change' })).toBeInTheDocument();
  });
});
