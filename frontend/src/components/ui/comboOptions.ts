/** One value of a list: the stored code and the name users read. */
export interface ComboOption {
  value: string;
  label: string;
  /** A second line (code, unit, role) that is searched too. */
  hint?: string;
}

/** From this many values a choice is searchable (a Combobox) instead of a plain drop-down. */
export const SEARCH_FROM = 12;

function fold(text: string): string {
  return text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}

/** The options whose name, code or hint contain every word typed (case and accents ignored). */
export function filterOptions(options: readonly ComboOption[], text: string): ComboOption[] {
  const words = fold(text).split(/\s+/).filter(Boolean);
  if (words.length === 0) {
    return [...options];
  }
  return options.filter((o) => {
    const haystack = fold(`${o.label} ${o.value} ${o.hint ?? ''}`);
    return words.every((w) => haystack.includes(w));
  });
}
