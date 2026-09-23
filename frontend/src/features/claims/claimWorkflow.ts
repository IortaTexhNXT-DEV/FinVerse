import type { Approval, ClaimStatus } from '@/api/claims';

/** Which claim actions a user may take, mirroring the server rules (the server stays the judge). */

const ACTIVE: ClaimStatus[] = ['REGISTERED', 'OPEN', 'PARTIALLY_SETTLED', 'REOPENED'];
const SETTLEABLE: ClaimStatus[] = ['OPEN', 'PARTIALLY_SETTLED', 'REOPENED'];
const DECLINABLE: ClaimStatus[] = ['REGISTERED', 'OPEN', 'REOPENED'];

export interface ClaimFacts {
  status: ClaimStatus;
  createdBy: string;
  /** Company share settled so far (a paid claim cannot be declined). */
  ourPaid: number;
}

export interface ClaimActions {
  reserve: boolean;
  settle: boolean;
  recover: boolean;
  lpo: boolean;
  close: boolean;
  reopen: boolean;
  decline: boolean;
}

type Can = (permission: string) => boolean;

/**
 * Actions offered on a claim: makers (CLAIM_MAINTAIN) enter documents while the claim is handled;
 * checkers (CLAIM_AUTHORIZE, not the registering user) close, reopen and decline.
 */
export function claimActions(
  facts: ClaimFacts,
  motor: boolean,
  user: string | undefined,
  can: Can,
): ClaimActions {
  return { ...makerActions(facts.status, motor, can), ...checkerActions(facts, user, can) };
}

function makerActions(status: ClaimStatus, motor: boolean, can: Can) {
  const maker = can('CLAIM_MAINTAIN');
  const active = ACTIVE.includes(status);
  return {
    reserve: maker && active,
    settle: maker && SETTLEABLE.includes(status),
    recover: maker && (active || status === 'CLOSED'),
    lpo: maker && active && motor,
  };
}

function checkerActions(facts: ClaimFacts, user: string | undefined, can: Can) {
  const checker = can('CLAIM_AUTHORIZE') && user !== facts.createdBy;
  return {
    close: checker && SETTLEABLE.includes(facts.status),
    reopen: checker && facts.status === 'CLOSED',
    decline: checker && DECLINABLE.includes(facts.status) && facts.ourPaid === 0,
  };
}

/** Whether the user may approve or reject a pending claim document (never their own). */
export function canDecide(approval: Approval, user: string | undefined, can: Can): boolean {
  return (
    approval.status === 'PENDING_APPROVAL' &&
    can('CLAIM_AUTHORIZE') &&
    user !== undefined &&
    user !== approval.submittedBy
  );
}
