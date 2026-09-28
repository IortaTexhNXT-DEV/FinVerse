import { useEffect, useSyncExternalStore } from 'react';
import { loadUserDirectory, userDirectory } from '@/api/users';

/** Display-name lookup: returns a function from login id to the name shown on screens. */
export function useDisplayName(): (login: string | null | undefined) => string {
  const snapshot = useSyncExternalStore(userDirectory.subscribe, userDirectory.snapshot);
  useEffect(() => {
    void loadUserDirectory();
  }, []);
  return (login) => {
    if (!login) {
      return '';
    }
    if (login.toUpperCase() === 'SYSTEM') {
      return 'System';
    }
    return snapshot.get(login.toLowerCase())?.displayName ?? login;
  };
}

/** Main role name of a login id (for "By" columns), if known. */
export function useRoleName(): (login: string | null | undefined) => string | undefined {
  const snapshot = useSyncExternalStore(userDirectory.subscribe, userDirectory.snapshot);
  return (login) => (login ? snapshot.get(login.toLowerCase())?.roleName : undefined);
}
