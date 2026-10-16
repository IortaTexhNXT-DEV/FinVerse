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
