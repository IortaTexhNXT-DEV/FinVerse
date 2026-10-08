import { WORKFLOW_NAMES, workflowRecordType } from '@/api/workflow';
import type { QueueCount, QueueFilters, QueueScope } from '@/api/workflow';
import type { ActiveFilter } from '@/components/ui/FilterChips';
import { humanize, titleCase } from '@/utils/format';

/** The filters of My Work kept in the page URL (the page number stays on the page). */
export type QueueView = Omit<QueueFilters, 'companyId' | 'page'>;

const SCOPES: readonly QueueScope[] = ['MINE', 'UNASSIGNED', 'ALL'];

/**
 * The My Work filters of a URL (?scope=MINE&workflow=NB_ACCOUNT&stage=DRAFT&overdue=true&q=…), so
 * a dashboard figure opens the filtered queue and Back returns to it. Unknown values fall back to
 * all the user's queues.
 */
export function queueViewOf(params: URLSearchParams): QueueView {
  const scope = params.get('scope') as QueueScope | null;
  const view: QueueView = { scope: scope !== null && SCOPES.includes(scope) ? scope : 'ALL' };
  const workflow = params.get('workflow');
  const stage = params.get('stage');
  const text = params.get('q');
  if (workflow) {
    view.workflow = workflow;
  }
  if (workflow && stage) {
    view.stage = stage;
  }
  if (params.get('overdue') === 'true') {
    view.overdue = true;
  }
  if (text) {
    view.text = text;
  }
  return view;
}

/** The URL parameters of My Work filters (the inverse of {@link queueViewOf}). */
export function queueParamsOf(view: QueueView): URLSearchParams {
  const params = new URLSearchParams();
  if (view.scope !== undefined && view.scope !== 'ALL') {
    params.set('scope', view.scope);
  }
  if (view.workflow) {
    params.set('workflow', view.workflow);
    if (view.stage) {
      params.set('stage', view.stage);
    }
  }
  if (view.overdue) {
    params.set('overdue', 'true');
  }
  if (view.text) {
    params.set('q', view.text);
  }
  return params;
}

/** The My Work address of a view, for the figures that open a filtered queue. */
export function queuePath(view: QueueView): string {
  const query = queueParamsOf(view).toString();
  return query === '' ? '/my-work' : `/my-work?${query}`;
}

/** The name of a workflow in a filter chip ("Accounts"), never its code. */
function workflowLabel(code: string): string {
  return titleCase(WORKFLOW_NAMES[code] ?? (workflowRecordType(code) || humanize(code)));
}

/**
 * The active workflow and stage filter as a removable chip ("Accounts: Draft"), named from the
 * stage counts of the user's queues.
 */
export function stageChips(
  filters: QueueView,
  counts: readonly QueueCount[],
  onClear: () => void,
): ActiveFilter[] {
  if (filters.workflow === undefined) {
    return [];
  }
  const workflow = workflowLabel(filters.workflow);
  const stage = counts.find(
    (c) => c.workflowCode === filters.workflow && c.stageCode === filters.stage,
  );
  const stageName =
    stage === undefined ? filters.stage && humanize(filters.stage) : titleCase(stage.stageName);
  return [
    {
      key: 'stage',
      label: stageName ? `${workflow}: ${stageName}` : workflow,
      onRemove: onClear,
    },
  ];
}
