/** Legal-form endings left out of the company selector ("…, Inc." and the like). */
const LEGAL_FORMS = /,?\s+(inc\.?|incorporated|corp\.?|corporation|co\.|company|ltd\.?|limited)$/i;

/**
 * The company's short name for the header selector: the legal form is left out; the selector
 * truncates what does not fit and shows the full name in its tooltip.
 */
export function shortCompanyName(name: string): string {
  return name.trim().replace(LEGAL_FORMS, '').trim();
}
