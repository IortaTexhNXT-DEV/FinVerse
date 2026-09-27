import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { clientsApi } from '@/api/clients';
import type { DuplicateMatch, DuplicateQuery } from '@/api/clients';
import { DUPLICATE_KEY_LABELS } from './clientForm';

/** A value that follows its input after a pause in typing. */
function useDebounced(value: string, delayMs: number): string {
  const [settled, setSettled] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setSettled(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);
  return settled;
}

function MatchLine({ match }: Readonly<{ match: DuplicateMatch }>) {
  return (
    <li>
      <Link to={`/crm/clients/${String(match.clientId)}`} target="_blank">
        {match.code}
      </Link>{' '}
      {match.displayName} ({match.status.toLowerCase()}) — same{' '}
      {match.keys.map((k) => DUPLICATE_KEY_LABELS[k] ?? k).join(', ')}
    </li>
  );
}

/**
 * Live duplicate warnings while the user types (BRNB.032): hard matches (TIN, ID, name with birth
 * date) block saving, other matches ask the user to check they are not the same client.
 */
export function DuplicateWarnings({
  query,
  onHardMatch,
}: Readonly<{ query: DuplicateQuery | undefined; onHardMatch: (blocked: boolean) => void }>) {
  const settledKey = useDebounced(query === undefined ? '' : JSON.stringify(query), 400);
  const settled = settledKey === '' ? undefined : (JSON.parse(settledKey) as DuplicateQuery);
  const matches = useQuery({
    queryKey: ['crm', 'duplicates', settledKey],
    queryFn: () => (settled === undefined ? Promise.resolve([]) : clientsApi.duplicates(settled)),
    enabled: settled !== undefined,
  });
  const found = settled === undefined ? [] : (matches.data ?? []);
  const hard = found.filter((m) => m.hard);
  const soft = found.filter((m) => !m.hard);
  const blocked = hard.length > 0;
  useEffect(() => onHardMatch(blocked), [blocked, onHardMatch]);

  if (found.length === 0) {
    return null;
  }
  return (
    <div className="stack" aria-live="polite">
      {blocked && (
        <div className="alert danger" role="alert">
          <strong>This client already exists.</strong> Open the existing record instead of creating
          a duplicate:
          <ul className="duplicate-list">
            {hard.map((m) => (
              <MatchLine key={m.clientId} match={m} />
            ))}
          </ul>
        </div>
      )}
      {soft.length > 0 && (
        <div className="alert warning" role="status">
          <strong>Possible duplicates.</strong> Check that these are different clients:
          <ul className="duplicate-list">
            {soft.map((m) => (
              <MatchLine key={m.clientId} match={m} />
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
