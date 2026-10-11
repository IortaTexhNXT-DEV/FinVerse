import type { AccessRequest } from '@/api/nbadmin';
import { isCancellable, isEditable } from './accessRequest';

export type RequestAction =
  'edit' | 'cancel' | 'approve' | 'secondApprove' | 'return' | 'reject' | 'implement';

/** Who looks at a request. */
export interface Viewer {
  username: string;
  can: (permission: string) => boolean;
  /** UAM_ANY_APPROVER: any approver may decide. */
  anyApprover: boolean;
}

const same = (a?: string, b?: string) => a !== undefined && a.toLowerCase() === b?.toLowerCase();

function creatorActions(r: AccessRequest, v: Viewer): RequestAction[] {
  const actions: RequestAction[] = [];
  const request = v.can('ACCESS_REQUEST');
  if (isEditable(r.status) && (r.status === 'DRAFT' || request || v.can('UAM_CORRECT'))) {
    actions.push('edit');
  }
  if (isCancellable(r.status) && (request || v.can('UAM_CANCEL'))) {
    actions.push('cancel');
  }
  return actions;
}

function mayDecidePending(r: AccessRequest, v: Viewer): boolean {
  const assigned = r.lifecycle.assignedApprover;
  return (
    v.can(r.userType === 'EXTERNAL' ? 'PORTAL_USER_APPROVE' : 'ACCESS_APPROVE') &&
    (assigned === undefined || same(assigned, v.username) || v.anyApprover)
  );
}

function approvedBefore(r: AccessRequest, username: string): boolean {
  return (
    same(r.decidedBy, username) ||
    r.lifecycle.approvers.some((a) => a.decision === 'APPROVED' && same(a.approver, username))
  );
}

function deciderActions(r: AccessRequest, v: Viewer): RequestAction[] {
  if (same(r.username, v.username) && r.userType === 'INTERNAL') {
    return [];
  }
  if (r.status === 'PENDING' && mayDecidePending(r, v)) {
    return ['approve', 'return', 'reject'];
  }
  if (
    r.status === 'PENDING_SECOND' &&
    v.can('UAM_SECOND_APPROVE') &&
    !approvedBefore(r, v.username)
  ) {
    return ['secondApprove', 'return', 'reject'];
  }
  if (r.status === 'FOR_IMPLEMENTATION' && v.can('ROLE_MANAGE')) {
    return ['implement'];
  }
  if (r.status === 'SCHEDULED' && same(r.decidedBy, v.username)) {
    return ['cancel'];
  }
  return [];
}

/**
 * The actions a viewer may take on a request (FR-UA-010 to FR-UA-045): the creator edits,
 * corrects and cancels; the chosen approver approves, returns or rejects; a second approver other
 * than the first gives the second approval; the System Administrator implements an approved
 * group-profile request; the requester and the user concerned never decide.
 */
export function requestActions(r: AccessRequest, v: Viewer): RequestAction[] {
  const creator = same(r.requestedBy, v.username);
  return creator ? creatorActions(r, v) : deciderActions(r, v);
}
