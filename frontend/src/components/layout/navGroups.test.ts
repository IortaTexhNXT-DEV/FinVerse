import { Circle } from 'lucide-react';
import { lazy } from 'react';
import type { NavGroup, ScreenDef } from '@/navigation/types';
import { visibleGroups } from './navGroups';

const component = lazy(() => Promise.resolve({ default: () => null }));

function screen(path: string, extra: Partial<ScreenDef> = {}): ScreenDef {
  return { path, label: path, icon: Circle, component, ...extra };
}

const GROUPS: NavGroup[] = [
  { id: 'home', modules: [{ id: 'dash', section: 'Overview', screens: [screen('/')] }] },
  {
    id: 'client-policy',
    title: 'Client & Policy',
    modules: [
      {
        id: 'crm',
        section: 'Client Management',
        screens: [
          screen('/crm/clients', { permission: 'CLIENT_VIEW' }),
          screen('/crm/clients/:id', { hidden: true }),
        ],
      },
      { id: 'bulk', section: 'Bulk', screens: [screen('/bulk', { permission: 'BULK_PROCESS' })] },
    ],
  },
  {
    id: 'finance',
    title: 'Finance',
    modules: [{ id: 'gl', section: 'GL', screens: [screen('/gl', { permission: 'GL_VIEW' })] }],
  },
];

describe('visibleGroups', () => {
  it('keeps only permitted, non-hidden screens and drops empty sections and groups', () => {
    const groups = visibleGroups(
      GROUPS,
      (s) => s.permission !== 'GL_VIEW' && s.permission !== 'BULK_PROCESS',
    );
    expect(groups.map((g) => g.id)).toEqual(['home', 'client-policy']);
    expect(groups[1]?.sections.map((s) => s.module.id)).toEqual(['crm']);
    expect(groups[1]?.paths).toEqual(['/crm/clients']);
  });

  it('keeps group titles and every section when all screens are allowed', () => {
    const groups = visibleGroups(GROUPS, () => true);
    expect(groups.map((g) => g.title)).toEqual([undefined, 'Client & Policy', 'Finance']);
    expect(groups[1]?.sections).toHaveLength(2);
  });
});
