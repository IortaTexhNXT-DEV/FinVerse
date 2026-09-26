/**
 * Business-user wording (client feedback, 26-Sep-2026): requirement and traceability references
 * such as "(ADJID.021)", "(BRNB.110)", "(CSHID.023 Annex II #1)" or "(OQ42)" and design notes such
 * as "layout to confirm" are removed from texts shown on screens. Mirrors the server's
 * BusinessText, as a safety net for messages and catalogue texts received from the API.
 */
const REF =
  '(?:[A-Z]{2,6}ID\\.\\d+(?:[-/]\\d+)*(?: addendum)?(?: Annex II #\\d+)?' +
  '|BR[A-Z]{2,4}\\.\\d+(?:[-/]\\d+)*' +
  '|FRBS \\d+\\.\\d+(?:\\.\\d+|\\.x)?' +
  '|Annex II #\\d+' +
  '|OQ\\d+(?:/OQ\\d+)*|[A-Z]{1,3}Q\\d{2}|Q\\d{2}' +
  '|FR-[A-Z]{2}-?\\d+|SNSRP-\\d+' +
  '|layout to confirm|draft|to confirm)';

const REFERENCES = new RegExp(`\\s*\\(${REF}(?:\\s*(?:,|;|/|and)\\s*${REF})*\\)`, 'g');

/** The text without requirement references and design notes. */
export function businessText(text: string): string {
  return text
    .replace(REFERENCES, '')
    .replace(/\s{2,}/g, ' ')
    .replace(/\s+([.,;:])/g, '$1')
    .trim();
}
