import type { ValidationCheck, VersionDetail } from '@/api/productCatalog';

/** The checks of a version validated before they were kept one by one: its confirmed items. */
function legacyChecks(json: string | undefined): ValidationCheck[] {
  if (!json) {
    return [];
  }
  try {
    const parsed: unknown = JSON.parse(json);
    return Array.isArray(parsed)
      ? parsed.map((item, i) => ({
          seq: i + 1,
          code: 'CONFIRMED',
          label: String(item),
          result: 'PASSED',
          detail: 'Confirmed by the validator',
        }))
      : [];
  } catch {
    return [];
  }
}

/** Whether the version has a validation outcome to show: released, or returned and in draft. */
export function hasValidationOutcome(detail: VersionDetail): boolean {
  return (
    detail.summary.validatedBy !== undefined ||
    (detail.validationResult === 'RETURNED' && detail.summary.status === 'DRAFT')
  );
}

/** The checks to list: those kept at the decision, else the confirmed items of an older version. */
export function checksOf(detail: VersionDetail): ValidationCheck[] {
  return detail.validationChecks && detail.validationChecks.length > 0
    ? detail.validationChecks
    : legacyChecks(detail.validationChecklist);
}
