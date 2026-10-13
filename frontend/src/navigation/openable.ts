import { useCallback } from 'react';
import { matchPath } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { mayOpen } from './access';
import { MODULES } from './modules';
import type { ScreenDef } from './types';

const SCREENS: readonly ScreenDef[] = MODULES.flatMap((m) => m.screens);

/** The screen a link opens, or undefined when no screen answers it (it would be "Page Not Found"). */
export function screenOf(link: string): ScreenDef | undefined {
  const [path = ''] = link.split(/[?#]/);
  return SCREENS.find((s) => matchPath({ path: s.path, end: true }, path) !== null);
}

/** Whether a link leads to a screen that exists and that the user may open. */
export function canOpenLink(link: string, can: (permission: string) => boolean): boolean {
  const screen = screenOf(link);
  return screen !== undefined && mayOpen(screen, can);
}

/**
 * The link of a notification, work item or approval when the user may follow it, else undefined:
 * the item is still listed, but without a link to a screen the user cannot open or that does not
 * exist, so nobody lands on "Page Not Found" or "No access" from their own lists.
 */
export function useOpenableLink(): (link: string | null | undefined) => string | undefined {
  const { can } = useAuth();
  return useCallback((link) => (link && canOpenLink(link, can) ? link : undefined), [can]);
}
