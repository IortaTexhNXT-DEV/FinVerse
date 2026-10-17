import { Fragment } from 'react';
import { UserName } from './UserName';

interface UserNamesProps {
  /** Login ids as stored on the record, in order; empty ones are skipped. */
  logins: readonly (string | null | undefined)[];
  /** Text between two names. */
  separator?: string;
}

/**
 * Several users' display names in order (e.g. prepared / approved / signed), each with its login
 * id in the tooltip; nothing when no user is given.
 */
export function UserNames({ logins, separator = ' / ' }: Readonly<UserNamesProps>) {
  const present = logins.filter((l): l is string => Boolean(l));
  if (present.length === 0) {
    return null;
  }
  return (
    <>
      {present.map((login, n) => (
        <Fragment key={`${login}-${String(n)}`}>
          {n > 0 && separator}
          <UserName login={login} />
        </Fragment>
      ))}
    </>
  );
}
