import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { permissionLabel } from '@/utils/permissionLabel';
import { PermissionChanges } from './PermissionChanges';
import { PermissionPicker } from './PermissionPicker';
import {
  GENERAL,
  groupPermissions,
  permissionConflicts,
  permissionDiff,
  privilegeOf,
} from './permissionAreas';
import type { ProfileInfo } from './profileAreas';

const CATALOG = [
  'DISB_PROCESS',
  'DISB_APPROVE',
  'DISB_VIEW',
  'CLIENT_MAINTAIN',
  'CLIENT_VIEW',
  'USER_MANAGE',
  'DASHBOARD_VIEW',
];

const PROFILES: ProfileInfo[] = [
  { code: 'DISBURSEMENT', name: 'Disbursement', permissions: ['DISB_PROCESS', 'DISB_VIEW'] },
  {
    code: 'DISB_APPROVER',
    name: 'Disbursement Approver',
    permissions: ['DISB_APPROVE', 'DISB_VIEW'],
  },
  { code: 'MKT', name: 'Marketing', permissions: ['CLIENT_MAINTAIN', 'CLIENT_VIEW'] },
  { code: 'OLD', name: 'Old Profile', permissions: ['USER_MANAGE'], active: false },
];

const RULE = {
  profileA: 'DISBURSEMENT',
  profileB: 'DISB_APPROVER',
  description: 'Maker and approver of the same voucher',
  status: 'ACTIVE',
};
const RULES = [RULE];

function Harness({ initial = [], current }: Readonly<{ initial?: string[]; current?: string[] }>) {
  const [selected, setSelected] = useState(initial);
  return (
    <>
      <PermissionPicker
        legend="Permissions of the new profile"
        permissions={CATALOG}
        selected={selected}
        onChange={setSelected}
        current={current}
        profiles={PROFILES}
        rules={RULES}
        offerCopy
      />
      <output data-testid="submitted">
        {[...selected].sort((a, b) => a.localeCompare(b)).join(',')}
      </output>
    </>
  );
}

const areaHead = (area: string) => screen.getByRole('button', { name: new RegExp(`^${area}`) });

describe('permission areas', () => {
  it('groups permissions by the business areas of the profile picker, General last', () => {
    const groups = groupPermissions(CATALOG, '');
    expect(groups.map((g) => g.area)).toEqual([
      'Client & Sales',
      'Disbursement & Payment Requests',
      'Administration & Control',
      GENERAL,
    ]);
    expect(groups[1]?.all).toHaveLength(3);
  });

  it('searches names and descriptions across areas', () => {
    const name = permissionLabel('DISB_APPROVE');
    const groups = groupPermissions(CATALOG, name.toLowerCase());
    expect(groups.flatMap((g) => g.shown)).toContain('DISB_APPROVE');
    expect(groupPermissions(CATALOG, 'zzz-nothing')).toEqual([]);
  });

  it('flags a permission of each side of an active separation-of-duties rule', () => {
    expect(permissionConflicts(['DISB_PROCESS', 'DISB_VIEW'], RULES, PROFILES)).toEqual([]);
    const [c] = permissionConflicts(['DISB_PROCESS', 'DISB_APPROVE'], RULES, PROFILES);
    expect(c).toMatchObject({ first: 'DISB_PROCESS', second: 'DISB_APPROVE' });
    expect(
      permissionConflicts(
        ['DISB_PROCESS', 'DISB_APPROVE'],
        [{ ...RULE, status: 'INACTIVE' }],
        PROFILES,
      ),
    ).toEqual([]);
  });

  it('marks administration and approval permissions as privileged', () => {
    expect(privilegeOf('USER_MANAGE')).toBe('Administration');
    expect(privilegeOf('DISB_APPROVE')).toBe('Approval');
    expect(privilegeOf('CLIENT_MAINTAIN')).toBeNull();
  });

  it('lists the permissions added and removed', () => {
    expect(permissionDiff(['A_X', 'B_X'], ['B_X', 'C_X'])).toEqual({
      added: ['C_X'],
      removed: ['A_X'],
    });
  });
});

describe('PermissionPicker', () => {
  it('shows the areas collapsed with their counts and a compact list when opened', async () => {
    render(<Harness initial={['DISB_VIEW']} />);
    const head = areaHead('Disbursement & Payment Requests');
    expect(head).toHaveAttribute('aria-expanded', 'false');
    expect(head).toHaveTextContent('1 of 3 selected');
    expect(screen.queryByLabelText(permissionLabel('DISB_PROCESS'))).toBeNull();
    await userEvent.click(head);
    expect(screen.getByLabelText(permissionLabel('DISB_PROCESS'))).not.toBeChecked();
    expect(screen.getByLabelText(permissionLabel('DISB_VIEW'))).toBeChecked();
  });

  it('opens the areas matching the search', async () => {
    render(<Harness />);
    await userEvent.type(
      screen.getByLabelText('Search permissions'),
      permissionLabel('USER_MANAGE'),
    );
    expect(areaHead('Administration & Control')).toHaveAttribute('aria-expanded', 'true');
    expect(screen.queryByRole('button', { name: /^Client & Sales/ })).toBeNull();
  });

  it('selects all and clears one area and submits the same codes', async () => {
    render(<Harness initial={['CLIENT_VIEW']} />);
    await userEvent.click(
      screen.getByRole('button', { name: 'Select all in Disbursement & Payment Requests' }),
    );
    expect(screen.getByTestId('submitted')).toHaveTextContent(
      'CLIENT_VIEW,DISB_APPROVE,DISB_PROCESS,DISB_VIEW',
    );
    expect(areaHead('Disbursement & Payment Requests')).toHaveTextContent('3 of 3 selected');
    await userEvent.click(
      screen.getByRole('button', { name: 'Clear Disbursement & Payment Requests' }),
    );
    expect(screen.getByTestId('submitted')).toHaveTextContent(/^CLIENT_VIEW$/);
  });

  it('warns on separation of duties and on high privilege, and removes a chip', async () => {
    render(<Harness initial={['DISB_PROCESS', 'DISB_APPROVE', 'USER_MANAGE']} />);
    expect(screen.getByText('Separation of duties')).toBeInTheDocument();
    expect(screen.getByText(/Maker and approver of the same voucher/)).toBeInTheDocument();
    expect(screen.getByText('High privilege')).toBeInTheDocument();
    const side = screen.getByRole('complementary', { name: 'Selected permissions' });
    await userEvent.click(
      within(side).getByRole('button', { name: `Remove ${permissionLabel('DISB_APPROVE')}` }),
    );
    expect(screen.queryByText('Separation of duties')).toBeNull();
  });

  it('copies the permissions of an active profile', async () => {
    render(<Harness />);
    const copy = screen.getByLabelText('Start from an existing profile');
    expect(within(copy).queryByRole('option', { name: 'Old Profile' })).toBeNull();
    await userEvent.selectOptions(copy, 'MKT');
    expect(screen.getByTestId('submitted')).toHaveTextContent('CLIENT_MAINTAIN,CLIENT_VIEW');
  });

  it('shows the change against the current profile in green and red', () => {
    render(
      <Harness initial={['DISB_VIEW', 'DISB_APPROVE']} current={['DISB_VIEW', 'DISB_PROCESS']} />,
    );
    expect(screen.getByText('1 permission added')).toBeInTheDocument();
    expect(screen.getByText('1 permission removed')).toBeInTheDocument();
    const added = document.querySelector('.permission-changes .permission-chip.added');
    const removed = document.querySelector('.permission-changes .permission-chip.removed');
    expect(added).toHaveTextContent(permissionLabel('DISB_APPROVE'));
    expect(removed).toHaveTextContent(permissionLabel('DISB_PROCESS'));
  });
});

describe('PermissionChanges', () => {
  it('shows the request change read only', () => {
    render(<PermissionChanges current={['DISB_PROCESS']} selected={['DISB_APPROVE']} />);
    expect(screen.getByText('Disbursement & Payment Requests')).toBeInTheDocument();
    expect(screen.queryByRole('button')).toBeNull();
    expect(screen.getByText('Added:', { exact: false })).toBeInTheDocument();
  });
});
