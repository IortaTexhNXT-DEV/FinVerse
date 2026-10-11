import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { ProfilePicker } from './ProfilePicker';
import {
  ADMINISTRATION,
  areaOfProfile,
  groupProfiles,
  profileChange,
  profileConflicts,
  profileKind,
  profileSummary,
} from './profileAreas';
import type { ProfileInfo } from './profileAreas';

const profile = (code: string, name: string, permissions: string[], over = {}): ProfileInfo => ({
  code,
  name,
  permissions,
  active: true,
  privilegeLevel: 'STANDARD',
  ...over,
});

const COMMON = ['ATTACHMENT_VIEW', 'DASHBOARD_VIEW', 'REPORT_VIEW', 'CLIENT_VIEW', 'ACCOUNT_VIEW'];

const PROFILES = [
  profile('MKT_AO', 'Marketing Account Officer', [
    ...COMMON,
    'CLIENT_MAINTAIN',
    'QUOTE_MAINTAIN',
    'PROPOSAL_REQUEST',
    'ACCOUNT_MAINTAIN',
    'RNW_ACCEPT',
    'RNW_DISPOSE',
    'RNW_RA_SEND',
    'RNW_RA_GENERATE',
    'RNW_ASSIGN',
    'CLX_WORK',
  ]),
  profile('PROCESSOR', 'Processing', [
    ...COMMON,
    'PLACEMENT_MANAGE',
    'BOOKING_PROCESS',
    'ACCOUNT_PROCESS',
    'EPOLICY_MANAGE',
    'RNW_PROCESS',
    'RNW_UPLOAD',
    'RNW_INSURER',
    'RNW_PACKAGE_REMAP',
  ]),
  profile('DISB_APPROVER', 'Disbursement Approver', [...COMMON, 'DISB_APPROVE', 'DISB_EOD']),
  profile('DISBURSEMENT', 'Disbursement', [...COMMON, 'DISB_PROCESS', 'DISB_VIEW']),
  profile('HR_APPROVER', 'Human Resources Approver', [
    'ATTACHMENT_VIEW',
    'PRQ_HR_APPROVE',
    'PRQ_VIEW',
  ]),
  profile('AUDITOR', 'Auditor', [...COMMON, 'AUDIT_VIEW', 'ACSL_VIEW', 'CLX_VIEW', 'BCL_VIEW']),
  profile('MANCOM', 'Management Committee', [...COMMON, 'PKG_MANCOM_SIGNOFF', 'PRODUCT_VIEW']),
  profile('SYSADMIN', 'System Administrator', ['USER_MANAGE', 'ROLE_MANAGE', 'LOV_MANAGE'], {
    privilegeLevel: 'ADMIN',
  }),
  profile('INFOSEC', 'Information Security Officer', ['AUDIT_VIEW', 'MFA_RESET_APPROVE']),
  profile('OLD', 'Old Profile', ['CLIENT_MAINTAIN'], { active: false }),
];

const RULES = [
  {
    profileA: 'DISBURSEMENT',
    profileB: 'DISB_APPROVER',
    description: 'Maker and approver of the same voucher',
    status: 'ACTIVE',
  },
  { profileA: 'MKT_AO', profileB: 'PROCESSOR', description: 'Pending rule', status: 'PENDING' },
];

describe('profile areas', () => {
  it('puts each profile in the business area of its own work, cross-cutting ones under Administration & Control', () => {
    const area = (code: string) => areaOfProfile(PROFILES.find((p) => p.code === code)!);
    expect(area('MKT_AO')).toBe('Client & Sales');
    expect(area('PROCESSOR')).toBe('Placement, Issuance & Booking');
    expect(area('DISB_APPROVER')).toBe('Disbursement & Payment Requests');
    expect(area('HR_APPROVER')).toBe('Disbursement & Payment Requests');
    expect(area('AUDITOR')).toBe(ADMINISTRATION);
    expect(area('MANCOM')).toBe(ADMINISTRATION);
    expect(area('SYSADMIN')).toBe(ADMINISTRATION);
    expect(area('INFOSEC')).toBe(ADMINISTRATION);
  });

  it('describes each profile in one line: kind, permissions and areas', () => {
    expect(profileKind(PROFILES[2]!)).toBe('Approver');
    expect(profileKind(PROFILES[3]!)).toBe('Maker');
    expect(profileKind(PROFILES[5]!)).toBe('View only');
    expect(profileSummary(PROFILES[2]!)).toBe(
      'Approver · 7 permissions · Disbursement & Payment Requests',
    );
    expect(profileSummary({ ...PROFILES[2]!, description: 'Approves vouchers' })).toBe(
      'Approves vouchers',
    );
  });

  it('groups active profiles by area in business order, searchable by what they do', () => {
    const groups = groupProfiles(PROFILES, '', []);
    expect(groups.map((g) => g.area)).toEqual([
      'Client & Sales',
      'Placement, Issuance & Booking',
      'Disbursement & Payment Requests',
      ADMINISTRATION,
    ]);
    expect(groups.flatMap((g) => g.profiles.map((p) => p.code))).not.toContain('OLD');
    const found = groupProfiles(PROFILES, 'approver', ['MKT_AO']);
    expect(found.flatMap((g) => g.profiles.map((p) => p.code))).toEqual([
      'MKT_AO',
      'DISB_APPROVER',
      'HR_APPROVER',
      'INFOSEC',
      'MANCOM',
    ]);
  });

  it('finds the active separation-of-duties rules broken by the selection', () => {
    expect(profileConflicts(['DISBURSEMENT', 'DISB_APPROVER', 'MKT_AO'], RULES)).toEqual([
      { a: 'DISBURSEMENT', b: 'DISB_APPROVER', reason: 'Maker and approver of the same voucher' },
    ]);
    expect(profileConflicts(['MKT_AO', 'PROCESSOR'], RULES)).toEqual([]);
    expect(profileChange('A', ['A'], ['A'])).toBe('current');
    expect(profileChange('B', ['B'], ['A'])).toBe('added');
    expect(profileChange('A', ['B'], ['A'])).toBe('removed');
  });
});

function Harness({ initial, current }: Readonly<{ initial: string[]; current?: string[] }>) {
  const [selected, setSelected] = useState(initial);
  return (
    <ProfilePicker
      legend="Group Profiles"
      profiles={PROFILES}
      selected={selected}
      current={current}
      rules={RULES}
      onChange={setSelected}
    />
  );
}

describe('profile picker', () => {
  it('lists collapsible business areas with counts and one-line descriptions', async () => {
    render(<Harness initial={['DISB_APPROVER']} />);
    const list = screen.getByRole('region', { name: 'Group profiles by business area' });
    const sales = within(list).getByRole('button', { name: /Client & Sales/ });
    expect(sales).toHaveAttribute('aria-expanded', 'false');
    // The group holding a selected profile starts open, with "n of m".
    const disb = within(list).getByRole('button', { name: /Disbursement & Payment Requests/ });
    expect(disb).toHaveAttribute('aria-expanded', 'true');
    expect(disb.textContent).toContain('1 of 3');
    await userEvent.click(sales);
    expect(sales).toHaveAttribute('aria-expanded', 'true');
    const box = screen.getByRole('checkbox', { name: 'Marketing Account Officer' });
    expect(box).toHaveAccessibleDescription(/Approver|Maker/);
    await userEvent.click(screen.getByRole('button', { name: 'Collapse All' }));
    expect(screen.queryByRole('checkbox')).toBeNull();
    await userEvent.click(screen.getByRole('button', { name: 'Expand All' }));
    expect(screen.getAllByRole('checkbox')).toHaveLength(9);
  });

  it('shows the selection on the right, removable, with a separation-of-duties warning', async () => {
    render(<Harness initial={['DISB_APPROVER']} />);
    const side = screen.getByRole('complementary', { name: 'Selected group profiles' });
    expect(within(side).getByText('Disbursement Approver')).toBeTruthy();
    await userEvent.click(screen.getByRole('checkbox', { name: 'Disbursement' }));
    expect(within(side).getByText('Separation of duties')).toBeTruthy();
    expect(side.textContent).toContain('Maker and approver of the same voucher');
    await userEvent.click(within(side).getByRole('button', { name: 'Remove Disbursement' }));
    expect(within(side).queryByText('Separation of duties')).toBeNull();
    expect(screen.getByRole('checkbox', { name: 'Disbursement' })).not.toBeChecked();
  });

  it('marks current, added and removed profiles of a change request and keeps a removed one on demand', async () => {
    render(<Harness initial={['MKT_AO']} current={['MKT_AO', 'AUDITOR']} />);
    const side = screen.getByRole('complementary', { name: 'Selected group profiles' });
    const items = within(side).getAllByRole('listitem');
    expect(items.map((i) => i.textContent)).toEqual([
      expect.stringContaining('Current'),
      expect.stringContaining('Removed'),
    ]);
    await userEvent.click(within(side).getByRole('button', { name: 'Keep' }));
    expect(within(side).queryByText('Removed')).toBeNull();
  });

  it('opens the details of a profile with its key permissions', async () => {
    render(<Harness initial={['DISB_APPROVER']} />);
    await userEvent.click(screen.getByRole('button', { name: 'Details of Disbursement Approver' }));
    const details = screen.getByRole('region', { name: 'Details of Disbursement Approver' });
    expect(within(details).getByText('Approves')).toBeTruthy();
    expect(within(details).getByText("Up to the user's Authorisation Limit")).toBeTruthy();
    await userEvent.click(
      within(details).getByRole('button', { name: 'Close the profile details' }),
    );
    expect(screen.queryByRole('region', { name: /Details of/ })).toBeNull();
  });
});
