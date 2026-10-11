/**
 * Session sharing between browser tabs (BRNB.082). The bearer token stays in each tab's
 * sessionStorage (never localStorage): a new tab asks the open tabs for it over a BroadcastChannel
 * handshake, and sign-in, sign-out and user activity are announced to every tab so that signing
 * out in one tab signs out all of them and working in one tab keeps the others from timing out.
 * Pure logic with an injectable channel so it can be unit tested.
 */

/** What a tab shares with a new tab. */
export interface SharedSession {
  token: string;
  expiresAt?: string;
}

export type SessionMessage =
  | { type: 'request'; from: string }
  | { type: 'share'; to: string; session: SharedSession }
  | { type: 'login'; session: SharedSession }
  | { type: 'logout' }
  | { type: 'activity'; extend: boolean };

/** The part of BroadcastChannel the sync uses. */
export interface ChannelLike {
  postMessage: (message: SessionMessage) => void;
  onmessage: ((event: { data: SessionMessage }) => void) | null;
  close: () => void;
}

export interface TabSync {
  /** Asks the other tabs for their session; resolves null when none answers in time. */
  requestSession: (timeoutMs: number) => Promise<SharedSession | null>;
  announceLogin: (session: SharedSession) => void;
  announceLogout: () => void;
  announceActivity: (extend: boolean) => void;
  /** Login, logout and activity messages of the other tabs. */
  subscribe: (listener: (message: SessionMessage) => void) => () => void;
  close: () => void;
}

/** Name of the channel shared by the tabs of the application. */
export const SESSION_CHANNEL = 'brokerverse.session';

/** Random identifier of a tab (getRandomValues also works outside secure contexts). */
function randomTabId(): string {
  const words = new Uint32Array(2);
  globalThis.crypto.getRandomValues(words);
  return Array.from(words, (w) => w.toString(36)).join('-');
}

/**
 * Creates the sync of one tab.
 *
 * @param channel broadcast channel, or null when the browser has none (the tab then works alone)
 * @param currentSession the session of this tab, if signed in
 * @param id identifier of this tab
 */
export function createTabSync(
  channel: ChannelLike | null,
  currentSession: () => SharedSession | null,
  id: string = randomTabId(),
): TabSync {
  const listeners = new Set<(message: SessionMessage) => void>();
  const waiting = new Set<(session: SharedSession | null) => void>();
  const post = (message: SessionMessage) => channel?.postMessage(message);

  const handle = (message: SessionMessage) => {
    if (message.type === 'request') {
      const session = currentSession();
      if (session !== null) {
        post({ type: 'share', to: message.from, session });
      }
      return;
    }
    if (message.type === 'share') {
      if (message.to === id) {
        [...waiting].forEach((resolve) => resolve(message.session));
      }
      return;
    }
    listeners.forEach((listener) => listener(message));
  };
  if (channel !== null) {
    channel.onmessage = (event) => handle(event.data);
  }

  return {
    requestSession: (timeoutMs) => {
      if (channel === null) {
        return Promise.resolve(null);
      }
      return new Promise((resolve) => {
        const done = (session: SharedSession | null) => {
          clearTimeout(timer);
          waiting.delete(done);
          resolve(session);
        };
        const timer = setTimeout(() => done(null), timeoutMs);
        waiting.add(done);
        post({ type: 'request', from: id });
      });
    },
    announceLogin: (session) => post({ type: 'login', session }),
    announceLogout: () => post({ type: 'logout' }),
    announceActivity: (extend) => post({ type: 'activity', extend }),
    subscribe: (listener) => {
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
      };
    },
    close: () => {
      channel?.close();
      listeners.clear();
    },
  };
}

/** The browser's channel for the application, or null when BroadcastChannel is unavailable. */
export function browserChannel(): ChannelLike | null {
  if (typeof BroadcastChannel === 'undefined') {
    return null;
  }
  return new BroadcastChannel(SESSION_CHANNEL) as unknown as ChannelLike;
}
