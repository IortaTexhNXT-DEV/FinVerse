/**
 * The wording of a toast, the same on every screen: it starts with a capital, and a one-sentence
 * toast has no closing full stop ("journal posted." becomes "Journal posted"), as the platform
 * writes them; a toast of several sentences keeps its punctuation.
 */
export function toastText(text: string): string {
  const trimmed = text.trim();
  const oneSentence = !/[.!?] [A-Z]/.test(trimmed);
  const shown = oneSentence ? trimmed.replace(/(?<!\.)\.$/, '') : trimmed;
  return shown.charAt(0).toUpperCase() + shown.slice(1);
}
