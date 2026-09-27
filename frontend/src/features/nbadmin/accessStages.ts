import type { AccessRequest } from '@/api/nbadmin';
import type { StageDef, StageMove } from '@/components/broking/stageSteps';
import { isGroupProfile } from './accessRequest';

/**
 * The stages of an access request for its stepper: draft, approval, second approval when the
 * request needs one, then implementation by the System Administrator (group profiles) or the
 * approval taking effect (users, scheduled when the effective date is later); returned, rejected
 * and cancelled are shown only when the request is in them.
 */
export function accessRequestStages(
  r: Pick<AccessRequest, 'type' | 'status' | 'lifecycle'>,
): StageDef[] {
  const stages: StageDef[] = [
    { code: 'DRAFT', name: 'Draft', terminal: false },
    { code: 'PENDING', name: 'Pending approval', terminal: false },
  ];
  if (r.lifecycle.secondApprovalRequired || r.status === 'PENDING_SECOND') {
    stages.push({ code: 'PENDING_SECOND', name: 'Second approval', terminal: false });
  }
  if (isGroupProfile(r.type) || r.status === 'FOR_IMPLEMENTATION') {
    stages.push(
      { code: 'FOR_IMPLEMENTATION', name: 'For implementation', terminal: false },
      { code: 'IMPLEMENTED', name: 'Implemented', terminal: true },
    );
  } else {
    if (r.status === 'SCHEDULED') {
      stages.push({ code: 'SCHEDULED', name: 'Scheduled', terminal: false });
    }
    stages.push({ code: 'APPROVED', name: 'Approved', terminal: true });
  }
  stages.push(
    { code: 'RETURNED', name: 'Returned', terminal: false },
    { code: 'REJECTED', name: 'Rejected', terminal: true },
    { code: 'CANCELLED', name: 'Cancelled', terminal: true },
  );
  return stages;
}

/** Where a request in a side stage came from (the approval it was returned or rejected at). */
export function accessRequestMoves(r: Pick<AccessRequest, 'status' | 'lifecycle'>): StageMove[] {
  if (r.status === 'RETURNED' || r.status === 'REJECTED') {
    return [{ fromStage: 'PENDING', toStage: r.status }];
  }
  if (r.status === 'CANCELLED') {
    return [{ fromStage: r.lifecycle.submittedAt ? 'PENDING' : 'DRAFT', toStage: 'CANCELLED' }];
  }
  return [];
}
