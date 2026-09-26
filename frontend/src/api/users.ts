import { api, tokenStore } from './client';

/** One user of the directory: login id, display name and main role name. */
export interface DirectoryEntry {
  username: string;
  displayName: string;
  roleName?: string;
}

type Directory = ReadonlyMap<string, DirectoryEntry>;

const EMPTY: Directory = new Map();
let directory: Directory = EMPTY;
let loading: Promise<void> | undefined;
const listeners = new Set<() => void>();

/**
 * Loads the user directory once per session (signed in only). Screens read it through
 * {@link userDirectory} to show display names instead of login ids.
 */
export function loadUserDirectory(): Promise<void> {
  if (loading !== undefined || tokenStore.get() === null) {
    return loading ?? Promise.resolve();
  }
  loading = api
    .get<DirectoryEntry[]>('/users/directory')
    .then((entries) => {
      directory = new Map(entries.map((e) => [e.username.toLowerCase(), e]));
      listeners.forEach((l) => l());
    })
    .catch(() => {
      loading = undefined;
    });
  return loading;
}

/** Forgets the directory (sign-out) so the next user loads a fresh copy. */
export function resetUserDirectory(): void {
  directory = EMPTY;
  loading = undefined;
  listeners.forEach((l) => l());
}

/** Seeds the directory directly (tests and screens that already hold the names). */
export function setUserDirectory(entries: DirectoryEntry[]): void {
  directory = new Map(entries.map((e) => [e.username.toLowerCase(), e]));
  loading = Promise.resolve();
  listeners.forEach((l) => l());
}

export const userDirectory = {
  subscribe: (listener: () => void): (() => void) => {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
  snapshot: (): Directory => directory,
  /** The entry of a login id, if known. */
  find: (login: string | null | undefined): DirectoryEntry | undefined =>
    login ? directory.get(login.toLowerCase()) : undefined,
};
