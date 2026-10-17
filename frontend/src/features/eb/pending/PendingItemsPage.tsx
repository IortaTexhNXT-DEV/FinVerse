import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { ItemFilters } from '@/api/eb';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { EB_SECTION } from '../EbPlaceholder';
import { EB_LOV } from '../common/ebCodes';
import '../eb.css';
import { ItemDialog } from './ItemDialogs';
import { PendingItemsTable } from './PendingItemsTable';
import { RESPONSIBLE } from './pendingLogic';

const STATUSES = [
  { code: 'PENDING', label: 'Pending' },
  { code: 'RECEIVED', label: 'Received' },
  { code: 'RELEASED', label: 'Released' },
  { code: 'CLOSED', label: 'Closed' },
];

/**
 * Pending Items (BRID-030; FR-EB-057; design 10.1): contracts, HMO cards, card replacements and
 * billings expected per programme or member, filtered by member, type, party, status and overdue,
 * with the follow-ups sent by the daily job; Add Pending Item and the status changes.
 */
export default function PendingItemsPage() {
  const { can } = useAuth();
  const [params] = useSearchParams();
  const [filters, setFilters] = useState<ItemFilters>({
    overdue: params.get('overdue') === 'true',
    status: params.get('overdue') === 'true' ? 'PENDING' : undefined,
  });
  const [showFilters, setShowFilters] = useState(true);
  const [adding, setAdding] = useState(false);
  const set = (next: Partial<ItemFilters>) => setFilters((f) => ({ ...f, ...next }));
  return (
    <div className="stack">
      <PageHeader
        section={EB_SECTION}
        title="Pending Items"
        description="Contracts, cards and billings expected from insurers and clients."
        actions={
          (can('EB_MARKET') || can('EB_PROCESS')) && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setAdding(true)}>
              Add Pending Item
            </Button>
          )
        }
      />
      <Card flush>
        <WorklistToolbar
          placeholder="Search programme, client, item or ARN"
          onSearch={(q) => set({ q })}
          filters={{ open: showFilters, onToggle: () => setShowFilters((f) => !f) }}
        />
        {showFilters && (
          <div className="worklist-filters form-grid">
            <Field label="Member">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  placeholder="Employee no. or name"
                  value={filters.member ?? ''}
                  onChange={(e) => set({ member: e.target.value })}
                />
              )}
            </Field>
            <Field label="Item Type">
              {(id) => (
                <LovSelect
                  id={id}
                  type={EB_LOV.itemType}
                  value={filters.type ?? ''}
                  placeholder="All types"
                  onChange={(v) => set({ type: v })}
                />
              )}
            </Field>
            <Field label="Responsible">
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={filters.responsible ?? ''}
                  onChange={(e) => set({ responsible: e.target.value })}
                >
                  <option value="">All parties</option>
                  {RESPONSIBLE.map((r) => (
                    <option key={r.code} value={r.code}>
                      {r.label}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field label="Status">
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={filters.status ?? ''}
                  onChange={(e) => set({ status: e.target.value })}
                >
                  <option value="">All statuses</option>
                  {STATUSES.map((s) => (
                    <option key={s.code} value={s.code}>
                      {s.label}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={filters.overdue === true}
                onChange={(e) => set({ overdue: e.target.checked })}
              />
              Only Past Due
            </label>
          </div>
        )}
        <PendingItemsTable filters={filters} showProgramme />
      </Card>
      {adding && <ItemDialog onClose={() => setAdding(false)} />}
    </div>
  );
}
