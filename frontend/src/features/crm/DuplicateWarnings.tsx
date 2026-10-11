import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { clientsApi } from '@/api/clients';
import type { DuplicateMatch, DuplicateQuery } from '@/api/clients';
import { DUPLICATE_KEY_LABELS } from './clientForm';
import { Notice } from '@/components/ui/Notice';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';

/** A value that follows its input after a pause in typing. */
function useDebounced(value: string, delayMs: number): string {
  const [settled, setSettled] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setSettled(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);
  return settled;
}

const MATCH_COLUMNS: Column<DuplicateMatch>[] = [
  {
    key: 'code',
    header: 'Client Code',
    kind: 'code',
    render: (m) => (
      <Link to={`/crm/clients/${String(m.clientId)}`} target="_blank">
        {m.code}
      </Link>
    ),
  },
  { key: 'name', header: 'Name', render: (m) => m.displayName },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (m) => <StatusBadge status={m.status} />,
  },
  {
    key: 'same',
    header: 'Same',
    render: (m) => m.keys.map((k) => DUPLICATE_KEY_LABELS[k] ?? k).join(', '),
  },
];

/** The matching clients as a table. */
function MatchTable({
  matches,
  caption,
}: Readonly<{ matches: DuplicateMatch[]; caption: string }>) {
  return (
    <DataTable
      caption={caption}
      columns={MATCH_COLUMNS}
      rows={matches}
      rowKey={(m) => m.clientId}
    />
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
        <>
          <Notice tone="error" title="This client already exists">
            Open the existing record instead of creating a duplicate.
          </Notice>
          <MatchTable matches={hard} caption="Existing clients" />
        </>
      )}
      {soft.length > 0 && (
        <>
          <Notice tone="warning" title="Possible duplicates">
            Check that these are different clients.
          </Notice>
          <MatchTable matches={soft} caption="Possible duplicates" />
        </>
      )}
    </div>
  );
}
