/** The value of an include / exclude list parameter: "A,B" (only these) or "!A,B" (all except). */
export interface CodeSetValue {
  exclude: boolean;
  codes: string[];
}

/** Reads the stored value of a list parameter. */
export function readCodeSet(value: string): CodeSetValue {
  const exclude = value.startsWith('!');
  const codes = (exclude ? value.slice(1) : value)
    .split(',')
    .map((c) => c.trim())
    .filter(Boolean);
  return { exclude, codes };
}

/** Writes the value of a list parameter; no code means every value. */
export function writeCodeSet({ exclude, codes }: CodeSetValue): string {
  if (codes.length === 0) {
    return '';
  }
  return `${exclude ? '!' : ''}${codes.join(',')}`;
}
