import type { ClaimPhase, ClaimProgress } from './api';

/** Longest next action plan summary (BRCLM.020). */
export const ACTION_PLAN_MAX = 2000;

/** Labels of the fixed claim phases (workflow BCL_CLAIM). */
export const PHASE_LABELS: Record<ClaimPhase, string> = {
  NEW: 'Newly filed',
  IN_PROGRESS: 'In progress',
  TEMP_CLOSED: 'Temporarily closed',
  CLOSED: 'Closed',
};

/** Labels of the tracked claim fields of the History tab. */
export const FIELD_LABELS: Record<string, string> = {
  REPORTED_DATE: 'Reported date',
  CLAIMANT: 'Claimant',
  ADJUSTER: 'Adjuster',
  FOLLOW_UP: 'Next follow-up date',
  ACTION_PLAN: 'Next action plan',
  SETTLEMENT: 'Settlement',
  COVER_VERSION: 'Cover version',
  AUTHORIZATION: 'Authorization code',
  HANDLER: 'Claim handler',
};

export type StatusAction =
  'changeStatus' | 'settle' | 'reopen' | 'followUp' | 'adjuster' | 'actionPlan';

/**
 * The status actions offered on a claim: each needs its permission, and a permanently closed claim
 * offers only Reopen (FR-CM-045).
 */
export function statusActions(
  phase: ClaimPhase,
  can: (permission: string) => boolean,
): StatusAction[] {
  if (phase === 'CLOSED') {
    return can('BCL_REOPEN') ? ['reopen'] : [];
  }
  const rights: [StatusAction, string][] = [
    ['changeStatus', 'BCL_STATUS_UPDATE'],
    ['settle', 'BCL_SETTLEMENT_UPDATE'],
    ['followUp', 'BCL_FOLLOW_UP_OVERRIDE'],
    ['adjuster', 'BCL_ADJUSTER_ASSIGN'],
    ['actionPlan', 'BCL_ACTION_PLAN'],
  ];
  return rights.filter(([, permission]) => can(permission)).map(([action]) => action);
}

/** Flags of the claim summary derived from the progress. */
export function progressFlags(progress: ClaimProgress): string[] {
  const flags: string[] = [];
  if (progress.status.awaitingPremiumRemittance) {
    flags.push('Awaiting premium remittance');
  }
  if (progress.followUp.overridden) {
    flags.push('Follow-up overridden');
  }
  if (progress.status.closureKind === 'TEMPORARY') {
    flags.push('Temporary closure');
  }
  return flags;
}

export interface SettlementForm {
  typeCode: string;
  amount: string;
  dateSettled: string;
  remark: string;
}

/** Field errors of Set Settlement (the type's own rules are checked by the server). */
export function validateSettlement(
  form: SettlementForm,
  today: string,
): Partial<Record<keyof SettlementForm, string>> {
  const errors: Partial<Record<keyof SettlementForm, string>> = {};
  if (form.typeCode === '') {
    errors.typeCode = 'Select the type of settlement';
  }
  if (form.amount !== '' && (Number.isNaN(Number(form.amount)) || Number(form.amount) < 0)) {
    errors.amount = 'Enter an amount of zero or more';
  }
  if (form.dateSettled !== '' && form.dateSettled > today) {
    errors.dateSettled = 'The date settled cannot be in the future';
  }
  return errors;
}

/** Field errors of Override Follow-up Date (FR-CM-050). */
export function validateFollowUp(
  date: string,
  reasonCode: string,
  today: string,
): { date?: string; reasonCode?: string } {
  const errors: { date?: string; reasonCode?: string } = {};
  if (date === '') {
    errors.date = 'Enter the next follow-up date';
  } else if (date < today) {
    errors.date = 'The follow-up date cannot be before today';
  }
  if (reasonCode === '') {
    errors.reasonCode = 'Enter the reason for the change';
  }
  return errors;
}
