import { humanize } from '@/utils/format';

/** A select option. */
export interface Option {
  value: string;
  label: string;
}

/** Options from code / name pairs. */
export function codeOptions(items: readonly { code: string; name: string }[]): Option[] {
  return items.map((i) => ({ value: i.code, label: `${i.code} – ${i.name}` }));
}

/** Options from enum values, humanized. */
export function enumOptions(values: readonly string[]): Option[] {
  return values.map((v) => ({ value: v, label: humanize(v) }));
}
