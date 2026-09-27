import type { BorChecklist } from '@/api/eb';

/** The checklist errors of a BOR validation (FR-EB-031). */
export function checklistErrors(c: BorChecklist): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!c.signedBySignatory || !c.notBlank || !c.clientNameMatches) {
    errors.checklist = 'Complete the BOR checklist before validating';
  }
  if (c.validFrom === '' || c.validTo === '') {
    errors.validity = 'Enter the validity dates of the BOR';
  } else if (c.validTo <= c.validFrom) {
    errors.validity = 'The BOR must be valid to a date after it is valid from';
  }
  return errors;
}
