import type { RowAction } from '@/components/ui/RowActionMenu';
import type { Rule } from './api';

/**
 * The row actions of an escalation rule (one menu at the end of the row, never buttons in the row):
 * Preview for the set-up and authorizing users, Change for the set-up users while the rule is not
 * inactive, Authorize for another user than its maker while it waits, Deactivate (last, in red)
 * for an active rule.
 */
export function escalationRuleActions(
  rule: Pick<Rule, 'recordStatus'>,
  may: { preview: boolean; setup: boolean; authorize: boolean },
  on: { preview: () => void; change: () => void; authorize: () => void; deactivate: () => void },
): RowAction[] {
  const actions: RowAction[] = [];
  if (may.preview) {
    actions.push({ label: 'Preview', onSelect: on.preview });
  }
  if (may.setup && rule.recordStatus !== 'INACTIVE') {
    actions.push({ label: 'Change', onSelect: on.change });
  }
  if (may.authorize && rule.recordStatus === 'PENDING_AUTHORIZATION') {
    actions.push({ label: 'Authorize', onSelect: on.authorize });
  }
  if (may.setup && rule.recordStatus === 'ACTIVE') {
    actions.push({ label: 'Deactivate', onSelect: on.deactivate, danger: true });
  }
  return actions;
}
