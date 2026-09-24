/** Splits a list of e-mail addresses typed with commas, semicolons or spaces. */
export function splitEmails(text: string): string[] {
  return text
    .split(/[\s,;]+/)
    .map((e) => e.trim())
    .filter((e) => e !== '');
}
