import type { QueueCount, QueueScope } from '@/api/workflow';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { FilterToggle } from '@/components/ui/FilterToggle';
import { Tabs } from '@/components/ui/Tabs';
import { stageChips } from './queueParams';
import type { QueueView } from './queueParams';

const SCOPES: readonly { id: QueueScope; label: string }[] = [
  { id: 'MINE', label: 'Assigned to Me' },
  { id: 'UNASSIGNED', label: 'Team Queue' },
  { id: 'ALL', label: 'All My Queues' },
];

/**
 * The scope tabs of My Work on the card's tab line, then one toolbar row: the reference / name
 * search with its button, the Overdue Only toggle and the active stage filter as a chip. Every
 * control has the toolbar height and the row wraps as whole controls on narrow windows.
 */
export function QueueToolbar({
  filters,
  stageCounts,
  onFilter,
  onSearch,
}: Readonly<{
  filters: QueueView;
  stageCounts: readonly QueueCount[];
  onFilter: (patch: Partial<QueueView>) => void;
  onSearch: (text: string) => void;
}>) {
  return (
    <>
      <Tabs
        tabs={SCOPES}
        active={filters.scope ?? 'ALL'}
        onChange={(scope) => onFilter({ scope })}
      />
      <WorklistToolbar
        placeholder="Search reference or name"
        onSearch={onSearch}
        extra={
          <FilterToggle
            label="Overdue Only"
            checked={filters.overdue ?? false}
            onChange={(overdue) => onFilter({ overdue })}
          />
        }
        chips={stageChips(filters, stageCounts, () =>
          onFilter({ workflow: undefined, stage: undefined }),
        )}
      />
    </>
  );
}
