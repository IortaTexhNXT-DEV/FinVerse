import personasJson from './personaMenus.json?raw';
import { mayOpen } from './access';
import { MODULES, NAV_GROUPS } from './modules';

interface Persona {
  demoUser: string;
  permissions: string[];
  screens: string[];
}

interface PersonaFile {
  sections: string[];
  roles: Record<string, Persona>;
}

const PERSONAS = JSON.parse(personasJson) as PersonaFile;

/** Groups a BRD-10 / BRD-11 role may reach outside its own sections (shared, read-only screens). */
const SHARED_GROUPS = new Set(['home', 'client-policy', 'reports']);
const SHARED_MODULES = new Set(['help']);

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

/**
 * Client requirement 14: each Sanction Screening (BRD-10) and User Access (BRD-11) role sees
 * exactly its intended screens. The grants and the screens are the shared fixture
 * personaMenus.json, which the backend checks against the database.
 */
describe('persona menus of BRD-10 and BRD-11', () => {
  const roles = Object.entries(PERSONAS.roles);

  it('covers every role and every screen of the three sections', () => {
    expect(roles.map(([code]) => code)).toHaveLength(9);
    const sectionScreens = MODULES.filter((m) => PERSONAS.sections.includes(m.id))
      .flatMap((m) => m.screens)
      .filter((s) => s.hidden !== true)
      .map((s) => s.path);
    const granted = new Set(roles.flatMap(([, p]) => p.screens));
    const byPath = (a: string, b: string) => a.localeCompare(b);
    expect([...granted].sort(byPath)).toEqual([...sectionScreens].sort(byPath));
  });

  it.each(roles)('%s sees exactly its screens of the three sections', (_code, persona) => {
    const visible = menuOf(persona.permissions)
      .filter((s) => PERSONAS.sections.includes(s.module))
      .map((s) => s.path);
    expect(visible).toEqual(persona.screens);
  });

  it.each(roles)('%s sees nothing of Finance, Claims or Administration', (_code, persona) => {
    const elsewhere = menuOf(persona.permissions).filter(
      (s) =>
        !PERSONAS.sections.includes(s.module) &&
        !SHARED_GROUPS.has(s.group) &&
        !SHARED_MODULES.has(s.module),
    );
    expect(elsewhere).toEqual([]);
  });
});
