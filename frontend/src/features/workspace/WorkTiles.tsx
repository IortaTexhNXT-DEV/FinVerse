import { AlarmClock } from 'lucide-react';
import { WORKFLOW_NAMES } from '@/api/workflow';
import type { QueueCount, QueueFilters } from '@/api/workflow';
import { Card } from '@/components/ui/Card';

type Filters = Omit<QueueFilters, 'companyId'>;

interface SummaryProps {
  counts: QueueCount[];
  onFilter: (patch: Partial<Filters>) => void;
}

const ALL_STAGES = { workflow: undefined, stage: undefined };

/** Headline figures of My Work; each figure filters the queue below. */
export function WorkSummary({ counts, onFilter }: Readonly<SummaryProps>) {
  const totals = counts.reduce(
    (acc, c) => ({
      open: acc.open + c.open,
      overdue: acc.overdue + c.overdue,
      mine: acc.mine + c.mine,
    }),
    { open: 0, overdue: 0, mine: 0 },
  );
  return (
    <div className="grid-4">
      <button
        type="button"
        className="kpi kpi-button"
        onClick={() => onFilter({ ...ALL_STAGES, scope: 'MINE', overdue: false })}
      >
        <span className="kpi-label">Assigned to me</span>
        <span className="kpi-value">{totals.mine}</span>
      </button>
      <button
        type="button"
        className="kpi kpi-button"
        onClick={() => onFilter({ ...ALL_STAGES, scope: 'ALL', overdue: false })}
      >
        <span className="kpi-label">Open in my queues</span>
        <span className="kpi-value">{totals.open}</span>
      </button>
      <button
        type="button"
        className="kpi kpi-button kpi-alert"
        onClick={() => onFilter({ ...ALL_STAGES, scope: 'ALL', overdue: true })}
      >
        <span className="kpi-label">Overdue</span>
        <span className="kpi-value">{totals.overdue}</span>
      </button>
      <div className="kpi kpi-button">
        <span className="kpi-label">Queues I work</span>
        <span className="kpi-value">{counts.length}</span>
      </div>
    </div>
  );
}

interface TilesProps {
  counts: QueueCount[];
  filters: Filters;
  onFilter: (patch: Partial<Filters>) => void;
}

/** One tile per stage the user works, grouped by workflow. */
export function StageTiles({ counts, filters, onFilter }: Readonly<TilesProps>) {
  const groups = new Map<string, QueueCount[]>();
  counts.forEach((c) => groups.set(c.workflowCode, [...(groups.get(c.workflowCode) ?? []), c]));
  return (
    <>
      {[...groups.entries()].map(([workflow, stages]) => (
        <Card key={workflow} title={WORKFLOW_NAMES[workflow] ?? workflow}>
          <div className="stage-tiles">
            {stages.map((s) => (
              <StageTile
                key={s.stageCode}
                count={s}
                active={filters.workflow === workflow && filters.stage === s.stageCode}
                onClick={() => onFilter({ workflow, stage: s.stageCode, scope: 'ALL' })}
              />
            ))}
          </div>
        </Card>
      ))}
    </>
  );
}

function StageTile({
  count,
  active,
  onClick,
}: Readonly<{ count: QueueCount; active: boolean; onClick: () => void }>) {
  return (
    <button type="button" className={active ? 'stage-tile active' : 'stage-tile'} onClick={onClick}>
      <span className="stage-tile-name">{count.stageName}</span>
      <span className="stage-tile-count">{count.open}</span>
      <span className="stage-tile-meta">
        {count.overdue > 0 && (
          <span className="stage-tile-overdue">
            <AlarmClock size={11} aria-hidden="true" /> {count.overdue} overdue
          </span>
        )}
        {count.mine > 0 && <span>{count.mine} mine</span>}
      </span>
    </button>
  );
}
