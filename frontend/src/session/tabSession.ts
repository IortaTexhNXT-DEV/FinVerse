import { tokenStore } from '@/api/client';
import { browserChannel, createTabSync } from './tabSync';
import type { SharedSession, TabSync } from './tabSync';

let sync: TabSync | undefined;

/** The session of this tab, as shared with the other tabs. */
export function currentSession(): SharedSession | null {
  const token = tokenStore.get();
  return token === null ? null : { token, expiresAt: tokenStore.expiresAt() ?? undefined };
}

/** The tab sync of the application (one per tab, created on first use). */
export function tabSession(): TabSync {
  sync ??= createTabSync(browserChannel(), currentSession);
  return sync;
}
