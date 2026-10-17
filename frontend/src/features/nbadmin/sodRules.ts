import type { SodRule, SodRuleInput } from '@/api/nbadmin';

export const EMPTY_SOD_RULE: SodRuleInput = {
  profileA: '',
  profileB: '',
  description: '',
  kind: 'PROFILES',
};

/** The kinds of rule, in words. */
export const RULE_KINDS = [
  { value: 'PROFILES', label: 'Two group profiles one user may not hold' },
  { value: 'PERMISSIONS', label: 'Two permissions not held together' },
] as const;

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
  const what = input.kind === 'PERMISSIONS' ? 'permission' : 'group profile';
  if (input.profileA === '') {
    errors.profileA = `Select the first ${what}`;
  }
  if (input.profileB === '') {
    errors.profileB = `Select the second ${what}`;
  } else if (input.profileB === input.profileA) {
    errors.profileB = `Choose two different ${what}s`;
  }
  if (input.description.trim() === '') {
    errors.description = 'Enter why the two profiles are not held together';
  }
  return errors;
}
