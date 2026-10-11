import type { PackageRequest } from '@/api/productmaint';
import type { WorkAction } from '@/api/workflow';

/** The actions run without a dialog of their own (a comment at most). */
export const SIMPLE_ACTIONS = new Set([
  'submit',
  'approve',
  'approve_no_negotiation',
  'release_to_marketing',
  'skip_marketing_review',
  'accept_terms',
  'submit_requirements',
  'signoff',
  'retire',
]);

/** The actions with a dialog of their own. */
export const DIALOG_ACTIONS = new Set([
  'recommend',
  'revise_qs',
  'terms_final',
  'setup',
  'return_incomplete',
]);

/**
 * The actions offered among those of the workflow: the approval that fits the request's negotiation
 * need, set-up or retirement by the request type, and the direct ManCom sign-off only when the
 * ManCom routing is one sign-off by any member (under the routing by selected approvers the
 * request is signed off by their approvals on the Routing & Approvals tab).
 */
export function offeredActions(
  p: PackageRequest,
  actions: WorkAction[],
  manComRouted = false,
): WorkAction[] {
  return actions.filter((a) => {
    if (a.action === 'signoff') {
      return !manComRouted;
    }
    if (a.action === 'approve' && p.status === 'FOR_TSU_APPROVAL') {
      return p.negotiationRequired;
    }
    if (a.action === 'approve_no_negotiation') {
      return !p.negotiationRequired;
    }
    if (a.action === 'setup') {
      return p.requestType !== 'RETIRE';
    }
    if (a.action === 'retire') {
      return p.requestType === 'RETIRE';
    }
    return SIMPLE_ACTIONS.has(a.action) || DIALOG_ACTIONS.has(a.action);
  });
}
