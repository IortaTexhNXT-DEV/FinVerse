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
    return snapshot.get(login.toLowerCase())?.displayName ?? login;
  };
}

/** Main role name of a login id (for "By" columns), if known. */
export function useRoleName(): (login: string | null | undefined) => string | undefined {
  const snapshot = useSyncExternalStore(userDirectory.subscribe, userDirectory.snapshot);
  return (login) => (login ? snapshot.get(login.toLowerCase())?.roleName : undefined);
}

interface UserNameProps {
  /** Login id as stored on the record. */
  login: string | null | undefined;
  /** Show the user's main role in a muted second line. */
  withRole?: boolean;
  /** Text when there is no user (e.g. an automatic system step). */
  empty?: string;
}

/**
 * A user's display name (never the login id), with the login id in the tooltip; optionally the
 * main role on a muted second line.
 */
export function UserName({ login, withRole = false, empty = '—' }: Readonly<UserNameProps>) {
  const name = useDisplayName();
  const role = useRoleName();
  if (!login) {
    return <span className="muted">{empty}</span>;
  }
  const roleName = withRole ? role(login) : undefined;
  if (roleName === undefined) {
    return <span title={login}>{name(login)}</span>;
  }
  return (
    <span className="cell-stack" title={login}>
      <span>{name(login)}</span>
      <span className="muted">{roleName}</span>
    </span>
  );
}
