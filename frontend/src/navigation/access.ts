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
