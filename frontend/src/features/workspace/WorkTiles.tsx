import { AlarmClock } from 'lucide-react';
import { WORKFLOW_NAMES, workflowRecordType } from '@/api/workflow';
import type { QueueCount } from '@/api/workflow';
import { Card } from '@/components/ui/Card';
import { KpiTile } from '@/components/ui/KpiTile';
import { countOf, humanize, titleCase } from '@/utils/format';
import { nounFor } from '@/utils/wording';
import { queuePath } from './queueParams';
import type { QueueView } from './queueParams';

/** Open, overdue and assigned-to-me items over all the stages of the user's queues. */
function workTotals(counts: readonly QueueCount[]) {
  return counts.reduce(
    (acc, c) => ({
      open: acc.open + c.open,
      overdue: acc.overdue + c.overdue,
      mine: acc.mine + c.mine,
    }),
    { open: 0, overdue: 0, mine: 0 },
  );
}

/** Headline figures of My Work; each figure opens the queue filtered on it. */
export function WorkSummary({ counts }: Readonly<{ counts: QueueCount[] }>) {
  const totals = workTotals(counts);
  const workflows = new Set(counts.map((c) => c.workflowCode)).size;
  return (
    <div className="grid-4 kpi-row">
      <KpiTile
        label="Assigned to Me"
        value={totals.mine}
        to={queuePath({ scope: 'MINE' })}
        qualifier="Items in your name"
      />
      <KpiTile
        label="Open in My Queues"
        value={totals.open}
        to={queuePath({ scope: 'ALL' })}
        qualifier="Waiting in the stages your team works"
      />
      <KpiTile
        label="Overdue"
        value={totals.overdue}
        to={queuePath({ scope: 'ALL', overdue: true })}
        qualifier={
          totals.overdue === 0 ? 'Every item is within its service level' : 'Past their due time'
        }
        alert={totals.overdue > 0}
      />
      <KpiTile
        label="Queues I Work"
        value={counts.length}
        qualifier={`${nounFor(counts.length, 'Stage')} across ${countOf(workflows, 'workflow')}`}
      />
    </div>
  );
}

interface TilesProps {
  counts: QueueCount[];
  filters: QueueView;
  onFilter: (patch: Partial<QueueView>) => void;
}

/** The title of a workflow's tiles in Title Case, never its code ("Proposal Requests"). */
function workflowTitle(workflow: string): string {
  const name = WORKFLOW_NAMES[workflow] ?? workflowRecordType(workflow);
  return name === '' ? humanize(workflow) : titleCase(name);
}

/**
 * The stages the user works, in one card: one line per workflow with its name, then a compact
 * tile per stage (open count, overdue and mine) that filters the queue below.
 */
export function StageTiles({ counts, filters, onFilter }: Readonly<TilesProps>) {
  const groups = new Map<string, QueueCount[]>();
  counts.forEach((c) => groups.set(c.workflowCode, [...(groups.get(c.workflowCode) ?? []), c]));
  if (groups.size === 0) {
    return null;
  }
  return (
    <Card title="Queues by Stage" callout="queues-by-stage">
      <div className="stage-groups">
        {[...groups.entries()].map(([workflow, stages]) => (
          <div key={workflow} className="stage-group">
            <h3 className="stage-group-name">{workflowTitle(workflow)}</h3>
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
          </div>
        ))}
      </div>
    </Card>
  );
}

function StageTile({
  count,
  active,
  onClick,
}: Readonly<{ count: QueueCount; active: boolean; onClick: () => void }>) {
  return (
    <button
      type="button"
      className={active ? 'stage-tile active' : 'stage-tile'}
      aria-pressed={active}
      onClick={onClick}
    >
      <span className="stage-tile-name" title={titleCase(count.stageName)}>
        {titleCase(count.stageName)}
      </span>
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
