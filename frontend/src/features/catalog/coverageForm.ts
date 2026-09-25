import type { ClauseInput, CoverageInput } from '@/api/productCatalog';

/** Client-side checks of the coverage and clause forms (PMADD01/02). */

const CODE = /^[A-Z0-9_]+$/;

/** Field errors of a coverage (empty when valid). */
export function coverageErrors(input: CoverageInput): Record<string, string> {
  const errors: Record<string, string> = {};
  if (input.lineCode === '') {
    errors.lineCode = 'Select the product line';
  }
  if (!CODE.test(input.code)) {
    errors.code = 'Use A-Z, 0-9 and _';
  }
  if (input.name.trim() === '') {
    errors.name = 'Enter the name';
  }
  if (input.kind === '') {
    errors.kind = 'Select the kind';
  }
  return errors;
}

/** Field errors of a clause (empty when valid). */
export function clauseErrors(input: ClauseInput): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!CODE.test(input.code)) {
    errors.code = 'Use A-Z, 0-9 and _';
  }
  if (input.kind === '') {
    errors.kind = 'Select the kind';
  }
  if (input.title.trim() === '') {
    errors.title = 'Enter the title';
  }
  if (input.wording.trim() === '') {
    errors.wording = 'Enter the wording';
  }
  if (input.effectiveFrom === '') {
    errors.effectiveFrom = 'Enter the effective date';
  } else if (input.effectiveTo && input.effectiveTo < input.effectiveFrom) {
    errors.effectiveTo = 'The end is before the start';
  }
  return errors;
}
