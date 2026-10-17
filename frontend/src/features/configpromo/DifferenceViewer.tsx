import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { configPromotionApi } from '@/api/configPromotion';
import type { ChangeType, ImportDataset } from '@/api/configPromotion';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Pager } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { ItemsTable } from './ItemsTable';
import { changeLabel } from './promotion';

type Filter = 'ALL' | ChangeType;

/**
 * The difference viewer of one dataset of an import: the items added, changed or only in this
 * environment, filtered by kind and searched by key, with the value of each field on each side.
 */
export function DifferenceViewer({
  importId,
  dataset,
  onClose,
}: Readonly<{ importId: number; dataset: ImportDataset; onClose: () => void }>) {
  const [filter, setFilter] = useState<Filter>('ALL');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const items = useQuery({
    queryKey: ['config-promotion', 'import', importId, 'items', dataset.code, filter, search, page],
    queryFn: () =>
      configPromotionApi.items(
        importId,
        dataset.code,
        filter === 'ALL' ? undefined : filter,
        search === '' ? undefined : search,
        page,
      ),
  });
  const change = (f: Filter) => {
    setFilter(f);
    setPage(0);
  };
  return (
    <Card
      title={`Differences – ${dataset.name}`}
      actions={
        <Button variant="ghost" size="sm" onClick={onClose}>
          Close
        </Button>
      }
    >
      <div className="stack">
        <div className="row">
          <Tabs<Filter>
            tabs={[
              {
                id: 'ALL',
                label: 'All',
                count: dataset.added + dataset.changed + dataset.onlyInTarget,
              },
              { id: 'ADDED', label: changeLabel('ADDED'), count: dataset.added },
              { id: 'CHANGED', label: changeLabel('CHANGED'), count: dataset.changed },
              { id: 'ONLY_IN_TARGET', label: 'Only Here', count: dataset.onlyInTarget },
            ]}
            active={filter}
            onChange={change}
          />
          <span className="spacer" />
          <label className="visually-hidden" htmlFor="cfp-item-search">
            Search items
          </label>
          <input
            id="cfp-item-search"
            className="input"
            placeholder="Search by code"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
        </div>
        <ErrorAlert error={items.error} />
        <ItemsTable
          items={items.data?.content ?? []}
          loading={items.isLoading}
          label={changeLabel}
          fromHeader="This Environment"
          toHeader="Package"
        />
        <Pager
          page={items.data?.page ?? 0}
          totalPages={items.data?.totalPages ?? 0}
          total={items.data?.totalElements ?? 0}
          size={items.data?.size}
          onPage={setPage}
        />
      </div>
    </Card>
  );
}
