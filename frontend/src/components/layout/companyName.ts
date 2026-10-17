/** Legal-form endings left out of the company selector ("…, Inc." and the like). */
const LEGAL_FORMS = new Set([
  'inc',
  'inc.',
  'incorporated',
  'corp',
  'corp.',
  'corporation',
  'co.',
  'company',
  'ltd',
  'ltd.',
  'limited',
]);

/**
 * The company's short name for the header selector: the legal form is left out; the selector
 * truncates what does not fit and shows the full name in its tooltip.
 */
export function shortCompanyName(name: string): string {
  const words = name.trim().split(' ');
  const last = words[words.length - 1] ?? '';
  if (words.length > 1 && LEGAL_FORMS.has(last.toLowerCase())) {
    words.pop();
  }
  const short = words.join(' ');
  return short.endsWith(',') ? short.slice(0, -1) : short;
}
