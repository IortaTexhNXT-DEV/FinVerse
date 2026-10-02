/**
 * Business-user wording (client feedback, 26-Sep-2026): requirement and traceability references
 * such as "(ADJID.021)", "(BRNB.110)", "(CSHID.023 Annex II #1)" or "(OQ42)" and design notes such
 * as "layout to confirm" are removed from texts shown on screens. Mirrors the server's
 * BusinessText, as a safety net for messages and catalogue texts received from the API.
 */
const REFERENCE_PARTS: readonly RegExp[] = [
  /^[A-Z]{2,6}ID\.\d+[a-z]?([-/]\d+[a-z]?)*( addendum)?( Annex II #\d+)?$/,
  /^BR[A-Z]{2,4}\.\d+[a-z]?([-/]\d+[a-z]?)*$/,
  /^[A-Z]{2,4}ADD\d{2}$/,
  /^FRBS \d+\.\d+(\.\d+|\.x)?$/,
  /^Annex II #\d+$/,
  /^OQ\d+(\/OQ\d+)*$/,
  /^[A-Z]{0,3}Q\d{2}$/,
  /^FR-[A-Z]{2}-?\d+$/,
  /^SNSRP-\d+$/,
  /^\d{3}[a-z]?([-/]\d{3}[a-z]?)*$/,
  /^(layout to confirm|draft|to confirm)$/,
];

/** Whether the text of a parenthesis is only references and design notes. */
function onlyReferences(inside: string): boolean {
  const parts = inside
    .split(/[,;]| and /)
    .map((p) => p.trim())
    .filter((p) => p !== '');
  const hasReference = parts.some((p) => REFERENCE_PARTS.slice(0, -2).some((r) => r.test(p)));
  return hasReference && parts.every((p) => REFERENCE_PARTS.some((r) => r.test(p)));
}

/** The text without requirement references and design notes. */
export function businessText(text: string): string {
  return text
    .replace(/ ?\(([^()]*)\)/g, (group: string, inside: string) =>
      onlyReferences(inside) ? '' : group,
    )
    .replace(/ {2,}/g, ' ')
    .replace(/ ([.,;:])/g, '$1')
    .trim();
}
