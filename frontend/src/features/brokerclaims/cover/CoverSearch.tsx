import { useQuery } from '@tanstack/react-query';
import { Search } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import type { CoverHit, SearchBy } from './api';
import { coverApi } from './api';

const SEARCH_BY: { id: SearchBy; label: string }[] = [
  { id: 'ARN', label: 'ARN' },
  { id: 'POLICY_NO', label: 'Policy No.' },
  { id: 'ASSURED', label: 'Assured' },
];

/** Shortest search text (FR-CL-010). */
export const MIN_SEARCH = 3;

/**
 * Cover search of Cover Lookup and Record Claim (FR-CL-010): by ARN, policy number or assured,
 * every cover of the company with no branch restriction; at least three characters.
 */
export function CoverSearch({
  title = 'Find the Cover',
  onSelect,
}: Readonly<{ title?: string; onSelect: (cover: CoverHit) => void }>) {
  const companyId = useCompanyId();
  const [by, setBy] = useState<SearchBy>('ARN');
  const [text, setText] = useState('');
  const [query, setQuery] = useState<{ by: SearchBy; q: string }>();
  const [tooShort, setTooShort] = useState(false);
  const results = useQuery({
    queryKey: ['broker-claims', 'covers', companyId, query],
    queryFn: () => coverApi.search(companyId, query?.by ?? 'ARN', query?.q ?? ''),
    enabled: query !== undefined,
  });
  const search = () => {
    const q = text.trim();
    setTooShort(q.length < MIN_SEARCH);
    if (q.length >= MIN_SEARCH) {
      setQuery({ by, q });
    }
  };
  const hits = results.data ?? [];
  return (
    <Card title={title}>
      <div className="stack">
        <div className="form-grid">
          <Field label="Search By" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={by}
                onChange={(e) => setBy(e.target.value as SearchBy)}
              >
                {SEARCH_BY.map((o) => (
                  <option key={o.id} value={o.id}>
                    {o.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field
            label="Search Text"
            required
            error={tooShort ? 'Enter at least 3 characters' : undefined}
          >
            {(id) => (
              <input
                id={id}
                className="input"
                value={text}
                placeholder="Search ARN, policy no. or assured"
                onChange={(e) => setText(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    search();
                  }
                }}
              />
            )}
          </Field>
        </div>
        <div className="row">
          <Button icon={<Search size={16} />} onClick={search} busy={results.isFetching}>
            Search
          </Button>
        </div>
        <ErrorAlert error={results.error} />
        {query !== undefined && !results.isFetching && hits.length === 0 && !results.error && (
          <EmptyState message={`No cover found for ${query.q}`} />
        )}
        {hits.length > 0 && (
          <DataTable<CoverHit>
            caption="Covers"
            rows={hits}
            rowKey={(c) => c.arn}
            onRowClick={onSelect}
            columns={[
              { key: 'arn', header: 'ARN', render: (c) => <span className="mono">{c.arn}</span> },
              { key: 'assured', header: 'Assured', render: (c) => c.assuredName },
              { key: 'product', header: 'Product', render: (c) => c.productCode },
              { key: 'insurer', header: 'Insurer', render: (c) => c.insurerCode ?? '' },
              {
                key: 'policy',
                header: 'Policy No.',
                render: (c) => c.policyNumbers.join(', ') || 'Pending',
              },
              {
                key: 'period',
                header: 'Period',
                render: (c) => `${formatDate(c.periodFrom)} – ${formatDate(c.periodTo)}`,
              },
              { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
            ]}
          />
        )}
      </div>
    </Card>
  );
}
