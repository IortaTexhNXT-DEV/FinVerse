import { useInsurerName, useLineName, useLovLabel } from './useLabels';

interface LovLabelProps {
  /** List of values the code belongs to (e.g. SOURCE_CHANNEL). */
  type: string;
  code: string | null | undefined;
  /** Text when there is no code. */
  empty?: string;
}

/** The label of a list-of-values code (never the code itself); the humanized code while loading. */
export function LovLabel({ type, code, empty = '—' }: Readonly<LovLabelProps>) {
  const label = useLovLabel(type);
  if (!code) {
    return <span className="muted">{empty}</span>;
  }
  return <>{label(code)}</>;
}

/** Labels of several codes of one list, comma separated; the given text when there is none. */
export function LovLabels({
  type,
  codes,
  empty = '—',
}: Readonly<{ type: string; codes: readonly string[]; empty?: string }>) {
  const label = useLovLabel(type);
  return <>{codes.length > 0 ? codes.map((c) => label(c)).join(', ') : empty}</>;
}

/** The name of a product line (e.g. "Personal Accident"), never its code. */
export function LineLabel({ code }: Readonly<{ code: string | null | undefined }>) {
  const name = useLineName();
  return <>{name(code)}</>;
}

/** "CAR00 (Engineering)": a product code with the name of its line. */
export function ProductLineLabel({
  product,
  line,
}: Readonly<{ product: string; line: string | null | undefined }>) {
  const name = useLineName();
  return <>{line ? `${product} (${name(line)})` : product}</>;
}

/** The name of an insurer, from its party code (the code while the list loads). */
export function InsurerName({
  code,
  empty = '—',
}: Readonly<{ code: string | null | undefined; empty?: string }>) {
  const name = useInsurerName();
  if (!code) {
    return <span className="muted">{empty}</span>;
  }
  return <>{name(code)}</>;
}

/** Names of several insurers, comma separated; the given text when there is none. */
export function InsurerNames({
  codes,
  empty = '—',
}: Readonly<{ codes: readonly string[]; empty?: string }>) {
  const name = useInsurerName();
  return <>{codes.length > 0 ? codes.map((c) => name(c)).join(', ') : empty}</>;
}
