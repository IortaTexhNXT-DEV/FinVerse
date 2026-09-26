import type { ScreenDef } from './types';

/** Whether a user may open a screen: it needs no permission, or the user holds one of them. */
export function mayOpen(
  screen: Pick<ScreenDef, 'permission' | 'alsoPermissions'>,
  can: (permission: string) => boolean,
): boolean {
  if (screen.permission === undefined) {
    return true;
  }
  return can(screen.permission) || (screen.alsoPermissions ?? []).some(can);
}

/**
 * Landing page for a user who may not open the finance dashboard: the New Business dashboard of the
 * broking roles (BRNB.012), else My Work, otherwise the first menu screen in sidebar order the user
 * may open.
 */
export function landingPath(
  screens: readonly ScreenDef[],
  can: (permission: string) => boolean,
  preferred: readonly string[] = ['/nb/dashboard', '/my-work'],
): string | undefined {
  const menu = screens.filter(
    (s) => s.hidden !== true && !s.path.includes(':') && s.path !== '/' && mayOpen(s, can),
  );
  const first = preferred.map((p) => menu.find((s) => s.path === p)).find((s) => s !== undefined);
  return (first ?? menu[0])?.path;
}
