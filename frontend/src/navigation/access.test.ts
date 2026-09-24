import { Circle } from 'lucide-react';
import { lazy } from 'react';
import { landingPath, mayOpen } from './access';
import type { ScreenDef } from './types';

describe('screen access', () => {
  const can = (p: string) => p === 'ACCESS_APPROVE';

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
    screen('/crm/clients/:id', 'CLIENT_VIEW', true),
    screen('/crm/clients', 'CLIENT_VIEW'),
  ];

  it('prefers My Work, else the first permitted menu screen', () => {
    expect(landingPath(screens, (p) => p === 'WORK_VIEW')).toBe('/my-work');
    expect(landingPath(screens, (p) => p === 'CLIENT_VIEW')).toBe('/approvals');
    expect(landingPath(screens.slice(3), (p) => p === 'CLIENT_VIEW')).toBe('/crm/clients');
    expect(landingPath([screen('/', 'X')], () => false)).toBeUndefined();
  });
});
