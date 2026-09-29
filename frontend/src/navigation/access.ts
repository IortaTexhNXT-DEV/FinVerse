import { MODULE_OFF } from './productModules';
import type { ScreenDef } from './types';

/**
 * Whether a user may open a screen: its product module is not switched off, it needs no
 * permission or the user holds one of them, and the user holds every permission of `requiresAll`.
 */
export function mayOpen(
  screen: Pick<ScreenDef, 'permission' | 'alsoPermissions' | 'requiresAll' | 'productModule'>,
  can: (permission: string) => boolean,
): boolean {
  if (screen.productModule !== undefined && can(`${MODULE_OFF}${screen.productModule}`)) {
    return false;
  }
  if (!(screen.requiresAll ?? []).every(can)) {
    return false;
  }
  if (screen.permission === undefined) {
    return true;
  }
  return can(screen.permission) || (screen.alsoPermissions ?? []).some(can);
}

/**
 * Landing page for a user who may not open the finance dashboard: the New Business dashboard of the
 * broking roles (BRNB.012), else My Work, else the Customer Search of the contact centre roles
 * (BRD-9), else the home of a module whose work permissions the user holds (`landingFor`, e.g.
 * Migration Home for the migration roles), otherwise the first menu screen in sidebar order the
 * user may open.
 */
export function landingPath(
  screens: readonly ScreenDef[],
  can: (permission: string) => boolean,
  preferred: readonly string[] = ['/nb/dashboard', '/my-work', '/csf'],
): string | undefined {
  const menu = screens.filter(
    (s) => s.hidden !== true && !s.path.includes(':') && s.path !== '/' && mayOpen(s, can),
  );
  const first =
    preferred.map((p) => menu.find((s) => s.path === p)).find((s) => s !== undefined) ??
    menu.find((s) => s.landingFor?.some((p) => can(p)) === true);
  return (first ?? menu[0])?.path;
}
