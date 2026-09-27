import type { SodRule, SodRuleInput } from '@/api/nbadmin';

export const EMPTY_SOD_RULE: SodRuleInput = { profileA: '', profileB: '', description: '' };

/** What waits for Information Security on a rule, in words. */
export function pendingText(r: SodRule): string {
  if (r.pendingAction === 'CREATE') {
    return 'New rule to authorise';
  }
  return r.pendingAction === 'DEACTIVATE' ? 'Deactivation to authorise' : '';
}

/** Checks of a new rule before it is sent. */
export function ruleErrors(input: SodRuleInput): Partial<Record<keyof SodRuleInput, string>> {
  const errors: Partial<Record<keyof SodRuleInput, string>> = {};
  if (input.profileA === '') {
    errors.profileA = 'Select the first group profile';
  }
  if (input.profileB === '') {
    errors.profileB = 'Select the second group profile';
  } else if (input.profileB === input.profileA) {
    errors.profileB = 'Choose two different group profiles';
  }
  if (input.description.trim() === '') {
    errors.description = 'Enter why the two profiles are not held together';
  }
  return errors;
}
