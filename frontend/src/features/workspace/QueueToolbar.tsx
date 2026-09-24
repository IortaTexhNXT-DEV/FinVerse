import { Search } from 'lucide-react';
import { useState } from 'react';
import type { QueueFilters, QueueScope } from '@/api/workflow';
import { Button } from '@/components/ui/Button';
import { Tabs } from '@/components/ui/Tabs';

type Filters = Omit<QueueFilters, 'companyId'>;

const SCOPES: readonly { id: QueueScope; label: string }[] = [
  { id: 'MINE', label: 'Assigned to me' },
  { id: 'UNASSIGNED', label: 'Team queue' },
  { id: 'ALL', label: 'All my queues' },
];

/** Scope tabs, overdue toggle, stage filter reset and reference / name search. */
export function QueueToolbar({
  filters,
  onFilter,
}: Readonly<{ filters: Filters; onFilter: (patch: Partial<Filters>) => void }>) {
  const [text, setText] = useState(filters.text ?? '');
  const stageFiltered = filters.workflow !== undefined || filters.stage !== undefined;
  return (
    <div className="row">
      <Tabs
        tabs={SCOPES}
        active={filters.scope ?? 'ALL'}
        onChange={(scope) => onFilter({ scope })}
      />
      <span className="spacer" />
      <label className="checkbox">
        <input
          type="checkbox"
          checked={filters.overdue ?? false}
          onChange={(e) => onFilter({ overdue: e.target.checked })}
        />{' '}
        Overdue only
      </label>
      {stageFiltered && (
        <Button
          variant="ghost"
          size="sm"
          onClick={() => onFilter({ workflow: undefined, stage: undefined })}
        >
          Clear stage filter
        </Button>
      )}
      <form
        className="row"
        onSubmit={(e) => {
          e.preventDefault();
          onFilter({ text: text.trim() || undefined });
        }}
      >
        <label className="visually-hidden" htmlFor="work-search">
          Search reference or name
        </label>
        <input
          id="work-search"
          className="input"
          placeholder="Reference or name…"
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <Button type="submit" variant="secondary" size="sm" icon={<Search size={14} />}>
          Search
        </Button>
      </form>
    </div>
  );
}
