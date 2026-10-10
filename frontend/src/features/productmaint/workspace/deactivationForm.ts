import type { Deactivation } from '@/api/pmWorkspace';

/** What a user may do on a deactivation request. */
export type Decision = 'approve' | 'reject' | 'reassign' | 'cancel';

/**
 * Who may do what on a pending request: the selected approver approves or rejects, a team head
 * reassigns, the requestor withdraws.
 *
 * @param r the request
 * @param me the user
 * @param mayReassign whether the user may reassign approvers
 * @returns the choices
 */
export function deactivationChoices(
  r: Deactivation,
  me: string | undefined,
  mayReassign: boolean,
): Decision[] {
  if (r.status !== 'PENDING') {
    return [];
  }
  const mine = (user: string) => me?.toLowerCase() === user.toLowerCase();
  const choices: Decision[] = [];
  if (mine(r.approver)) {
    choices.push('approve', 'reject');
  }
  if (mayReassign) {
    choices.push('reassign');
  }
  if (mine(r.requestedBy)) {
    choices.push('cancel');
  }
  return choices;
}

/** What the requestor enters to deactivate a package (BDOI FRS FRPM.003.04). */
export interface DeactivationForm {
  effectiveDate: string;
  reason: string;
  remarks: string;
  approver: string;
}

/**
 * The problems of the form, by field, before it is submitted (the server checks again).
 *
 * @param form the form
 * @param today today's date (ISO)
 * @returns message by field
 */
export function deactivationProblems(
  form: DeactivationForm,
  today: string,
): Partial<Record<keyof DeactivationForm, string>> {
  const problems: Partial<Record<keyof DeactivationForm, string>> = {};
  if (form.effectiveDate === '') {
    problems.effectiveDate = 'Enter the deactivation effective date';
  } else if (form.effectiveDate < today) {
    problems.effectiveDate = 'The deactivation effective date must be today or later';
  }
  if (form.reason === '') {
    problems.reason = 'Select the reason for deactivation';
  }
  if (form.approver === '') {
    problems.approver = 'Select the approver';
  }
  return problems;
}
