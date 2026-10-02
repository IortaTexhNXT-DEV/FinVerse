import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { MemberChangeFilters } from '@/api/ebMarket';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { EB_SECTION } from '../EbPlaceholder';
import '../eb.css';
import { CHANGE_STATUSES } from '../common/ebStatuses';
import { MemberChangesTable } from './MemberChangesTable';

/**
 * Member Changes (design 10.1): the work list of additions, deletions and plan or data changes of
 * members across the programmes, filtered by status and searchable by change, programme, client
 * or employee. A change is captured on its programme's Member Changes tab; here it is relayed,
 * billed, validated by Processing and closed.
 */
export default function MemberChangesPage() {
  const [params] = useSearchParams();
  const change = Number(params.get('change') ?? '');
  const [filters, setFilters] = useState<MemberChangeFilters>({
    status: change > 0 ? undefined : 'OPEN',
  });
  const [showFilters, setShowFilters] = useState(true);
  const set = (next: Partial<MemberChangeFilters>) => setFilters((f) => ({ ...f, ...next }));
  return (
    <div className="stack">
      <PageHeader
        section={EB_SECTION}
        title="Member Changes"
        description="Additions, deletions and plan or data changes of members, relayed to the insurer, billed, validated by Processing and closed."
      />
      <Card flush>
        <WorklistToolbar
          placeholder="Search change, programme, client or employee"
          onSearch={(q) => set({ q })}
          filters={{ open: showFilters, onToggle: () => setShowFilters((f) => !f) }}
        />
        {showFilters && (
          <div className="worklist-filters form-grid">
            <Field label="Status">
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={filters.status ?? ''}
                  onChange={(e) => set({ status: e.target.value || undefined })}
                >
                  <option value="OPEN">Open</option>
                  <option value="">All statuses</option>
                  {CHANGE_STATUSES.map((s) => (
                    <option key={s.code} value={s.code}>
                      {s.label}
                    </option>
                  ))}
                </select>
              )}
            </Field>
          </div>
        )}
        <MemberChangesTable
          filters={filters}
          showProgramme
          openId={change > 0 ? change : undefined}
        />
      </Card>
    </div>
  );
}
