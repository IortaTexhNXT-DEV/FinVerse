import { Circle } from 'lucide-react';
import { lazy } from 'react';
import { landingPath, mayOpen } from './access';
import type { ScreenDef } from './types';

describe('screen access', () => {
  const can = (p: string) => p === 'ACCESS_APPROVE';

  it('needs every permission of requiresAll', () => {
    const screen = { permission: 'TAX_VIEW', requiresAll: ['TAX_MANAGE'] };
    expect(mayOpen(screen, (p) => p === 'TAX_VIEW')).toBe(false);
    expect(mayOpen(screen, (p) => ['TAX_VIEW', 'TAX_MANAGE'].includes(p))).toBe(true);
  });

  it('opens screens without permission, with the permission or with an alternative', () => {
    expect(mayOpen({}, can)).toBe(true);
    expect(mayOpen({ permission: 'ACCESS_APPROVE' }, can)).toBe(true);
    expect(mayOpen({ permission: 'ACCESS_REQUEST' }, can)).toBe(false);
    expect(
      mayOpen({ permission: 'ACCESS_REQUEST', alsoPermissions: ['ACCESS_APPROVE'] }, can),
    ).toBe(true);
  });
});

describe('landing page', () => {
  const component = lazy(() => Promise.resolve({ default: () => null }));
  const screen = (path: string, permission?: string, hidden?: boolean): ScreenDef => ({
    path,
    label: path,
    icon: Circle,
    component,
    permission,
    hidden,
  });
  const screens = [
    screen('/', 'DASHBOARD_VIEW'),
    screen('/approvals'),
    screen('/my-work', 'WORK_VIEW'),
    screen('/nb/dashboard', 'WORK_VIEW'),
    screen('/crm/clients/:id', 'CLIENT_VIEW', true),
    screen('/crm/clients', 'CLIENT_VIEW'),
  ];

  it('prefers the NB dashboard, then My Work, else the first permitted menu screen', () => {
    expect(landingPath(screens, (p) => p === 'WORK_VIEW')).toBe('/nb/dashboard');
    expect(landingPath(screens, (p) => p === 'WORK_VIEW', ['/my-work'])).toBe('/my-work');
    expect(landingPath(screens, (p) => p === 'CLIENT_VIEW')).toBe('/approvals');
    expect(landingPath(screens.slice(3), (p) => p === 'CLIENT_VIEW')).toBe('/crm/clients');
    expect(landingPath([screen('/', 'X')], () => false)).toBeUndefined();
  });

  it('sends the contact centre roles to the Customer Search', () => {
    const withCsf = [...screens, screen('/csf', 'CSF_VIEW'), screen('/csf/changes', 'CSF_VIEW')];
    expect(landingPath(withCsf, (p) => p === 'CSF_VIEW')).toBe('/csf');
    expect(landingPath(withCsf, (p) => p === 'CSF_VIEW' || p === 'WORK_VIEW')).toBe(
      '/nb/dashboard',
    );
  });

  it('lands the users of a module with work permissions on its home', () => {
    const home: ScreenDef = {
      ...screen('/migration', 'MIG_VIEW'),
      landingFor: ['MIG_INTAKE'],
    };
    const withHome = [screen('/approvals'), home];
    expect(landingPath(withHome, (p) => p === 'MIG_VIEW' || p === 'MIG_INTAKE')).toBe('/migration');
    expect(landingPath(withHome, (p) => p === 'MIG_VIEW')).toBe('/approvals');
  });
});
