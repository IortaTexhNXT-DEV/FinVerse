import personasJson from './personaMenus.json?raw';
import { mayOpen } from './access';
import { MODULES, NAV_GROUPS } from './modules';

interface Persona {
  seedUser: string;
  /** Only the permissions with this prefix are listed (a role of another business area). */
  permissionScope?: string;
  permissions: string[];
  screens: string[];
}

interface Suite {
  id: string;
  sections: string[];
  sharedGroups: string[];
  sharedModules: string[];
  roles: Record<string, Persona>;
}

interface PersonaFile {
  suites: Suite[];
}

const SUITES = (JSON.parse(personasJson) as PersonaFile).suites;

function menuOf(permissions: readonly string[]) {
  const can = (p: string) => permissions.includes(p);
  return NAV_GROUPS.flatMap((group) =>
    group.modules.flatMap((module) =>
      module.screens
        .filter((s) => s.hidden !== true && mayOpen(s, can))
        .map((s) => ({ group: group.id, module: module.id, path: s.path })),
    ),
  );
}

function sectionScreens(suite: Suite) {
  return MODULES.filter((m) => suite.sections.includes(m.id))
    .flatMap((m) => m.screens)
    .filter((s) => s.hidden !== true);
}

/**
 * Client requirement 14: each role of Sanction Screening (BRD-10), User Access (BRD-11) and Claims
 * Handling (BRD-7) sees exactly its intended screens. The grants and the screens are the shared
 * fixture personaMenus.json, which the backend checks against the database.
 */
describe.each(SUITES.map((s) => [s.id, s] as const))('persona menus of %s', (_id, suite) => {
  const roles = Object.entries(suite.roles);

  it('covers every screen of its sections', () => {
    const byPath = (a: string, b: string) => a.localeCompare(b);
    const granted = new Set(roles.flatMap(([, p]) => p.screens));
    const all = sectionScreens(suite).map((s) => s.path);
    expect([...granted].sort(byPath)).toEqual([...all].sort(byPath));
  });

  it.each(roles)('%s sees exactly its screens of the sections', (_code, persona) => {
    const visible = menuOf(persona.permissions)
      .filter((s) => suite.sections.includes(s.module))
      .map((s) => s.path);
    expect(visible).toEqual(persona.screens);
  });

  it.each(roles.filter(([, p]) => p.permissionScope !== undefined))(
    '%s: the sections need only permissions of its scope',
    (_code, persona) => {
      const scope = persona.permissionScope ?? '';
      const needed = sectionScreens(suite).flatMap((s) => [
        ...(s.permission === undefined ? [] : [s.permission]),
        ...(s.alsoPermissions ?? []),
      ]);
      expect(needed.filter((p) => !p.startsWith(scope))).toEqual([]);
    },
  );

  it.each(roles.filter(([, p]) => p.permissionScope === undefined))(
    '%s sees nothing outside its sections and the shared screens',
    (_code, persona) => {
      const elsewhere = menuOf(persona.permissions).filter(
        (s) =>
          !suite.sections.includes(s.module) &&
          !suite.sharedGroups.includes(s.group) &&
          !suite.sharedModules.includes(s.module),
      );
      expect(elsewhere).toEqual([]);
    },
  );
});

describe('persona menus across suites', () => {
  it('lists every role once, 16 roles in all', () => {
    const codes = SUITES.flatMap((s) => Object.keys(s.roles));
    expect(new Set(codes).size).toBe(codes.length);
    expect(codes).toHaveLength(16);
  });

  it('keeps the insurer-side Claims module away from every listed role', () => {
    const insurerClaims = MODULES.find((m) => m.id === 'claims');
    expect(insurerClaims).toBeDefined();
    const paths = new Set((insurerClaims?.screens ?? []).map((s) => s.path));
    for (const suite of SUITES) {
      for (const persona of Object.values(suite.roles)) {
        expect(menuOf(persona.permissions).filter((s) => paths.has(s.path))).toEqual([]);
      }
    }
  });
});
