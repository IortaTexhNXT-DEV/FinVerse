import type { FieldChange } from '@/api/changeHistory';

/** Label of a changed field: "defaultCreditDays" reads "Default credit days". */
export function fieldLabel(change: Pick<FieldChange, 'changeKind' | 'field'>): string {
  if (change.changeKind === 'CREATE') {
    return 'Record created';
  }
  const words = change.field.replace(/([a-z0-9])([A-Z])/g, '$1 $2').toLowerCase();
  return words.charAt(0).toUpperCase() + words.slice(1);
}

/** Authorization state of a change: who authorized it, or that it is pending. */
export function checkerState(change: Pick<FieldChange, 'checker'>, makerChecker: boolean): string {
  if (change.checker !== null) {
    return change.checker;
  }
  return makerChecker ? 'Pending authorization' : 'Not subject to authorization';
}

/** Names of the record types whose class name does not read well on its own. */
const RECORD_TYPES: Record<string, string> = {
  InsurerProfile: 'Insurer',
  RiskProduct: 'Product',
  LovValue: 'List of values value',
  GlAccount: 'GL account',
};

/** The record type of the change log in words: "InsurerProfile" reads "Insurer". */
export function recordTypeLabel(entityType: string): string {
  const named = RECORD_TYPES[entityType];
  if (named !== undefined) {
    return named;
  }
  const words = entityType.replace(/([a-z])([A-Z])/g, '$1 $2').toLowerCase();
  return words.charAt(0).toUpperCase() + words.slice(1);
}
