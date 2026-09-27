import { createTabSync } from './tabSync';
import type { ChannelLike, SessionMessage, SharedSession } from './tabSync';

/** In-memory broadcast bus: a message reaches every other channel, never the sender. */
function bus() {
  const channels: ChannelLike[] = [];
  const open = (): ChannelLike => {
    const channel: ChannelLike = {
      onmessage: null,
      postMessage: (message: SessionMessage) => {
        channels
          .filter((c) => c !== channel)
          .forEach((c) => {
            c.onmessage?.({ data: { ...message } });
          });
      },
      close: () => {
        channels.splice(channels.indexOf(channel), 1);
      },
    };
    channels.push(channel);
    return channel;
  };
  return { open, size: () => channels.length };
}

const SESSION: SharedSession = { token: 'jwt-1', expiresAt: '2026-09-24T18:00:00Z' };

describe('tab session handshake', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });
  afterEach(() => {
    vi.useRealTimers();
  });

  it('gives a new tab the session of a signed-in tab', async () => {
    const b = bus();
    createTabSync(b.open(), () => null, 'empty');
    createTabSync(b.open(), () => SESSION, 'signed-in');
    const newTab = createTabSync(b.open(), () => null, 'new');
    await expect(newTab.requestSession(300)).resolves.toEqual(SESSION);
  });

  it('answers only the tab that asked', async () => {
    const b = bus();
    createTabSync(b.open(), () => SESSION, 'a');
    const other = createTabSync(b.open(), () => null, 'b');
    const otherPending = other.requestSession(300);
    const asker = createTabSync(b.open(), () => null, 'c');
    await expect(asker.requestSession(300)).resolves.toEqual(SESSION);
    await expect(otherPending).resolves.toEqual(SESSION);
  });

  it('resolves null when no tab is signed in or the browser has no channel', async () => {
    const b = bus();
    createTabSync(b.open(), () => null, 'a');
    const pending = createTabSync(b.open(), () => null, 'b').requestSession(300);
    vi.advanceTimersByTime(300);
    await expect(pending).resolves.toBeNull();
    await expect(createTabSync(null, () => SESSION).requestSession(300)).resolves.toBeNull();
  });

  it('signs out, signs in and shares activity with every other tab', () => {
    const b = bus();
    const first = createTabSync(b.open(), () => SESSION, 'a');
    const second = createTabSync(b.open(), () => SESSION, 'b');
    const third = createTabSync(b.open(), () => SESSION, 'c');
    const seen: SessionMessage[] = [];
    const seenByThird: SessionMessage[] = [];
    const unsubscribe = second.subscribe((m) => seen.push(m));
    third.subscribe((m) => seenByThird.push(m));
    const firstSeen: SessionMessage[] = [];
    first.subscribe((m) => firstSeen.push(m));

    first.announceLogout();
    first.announceLogin({ token: 'jwt-2' });
    first.announceActivity(true);
    expect(seen.map((m) => m.type)).toEqual(['logout', 'login', 'activity']);
    expect(seenByThird).toHaveLength(3);
    expect(firstSeen).toEqual([]);

    unsubscribe();
    first.announceActivity(false);
    expect(seen).toHaveLength(3);
    expect(seenByThird.at(-1)).toEqual({ type: 'activity', extend: false });

    third.close();
    first.announceLogout();
    expect(seenByThird).toHaveLength(4);
    expect(b.size()).toBe(2);
  });

  it('works alone without a channel', () => {
    const alone = createTabSync(null, () => null);
    alone.announceLogout();
    alone.announceLogin(SESSION);
    alone.announceActivity(false);
    alone.close();
  });
});
