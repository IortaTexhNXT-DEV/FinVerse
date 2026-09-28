import { statusLabel, statusShortLabel, statusTone } from './statusTones';
import type { Tone } from './statusTones';

interface StatusBadgeProps {
  /** Status code (LOCKED, SUCCEEDED, RETURNED_TO_MARKETING…); picks the tone and the label. */
  status: string;
  /** Label to show instead of the humanized code (e.g. a workflow stage name). */
  label?: string;
  /** Tone to use instead of the status's own tone. */
  tone?: Tone;
  /**
   * Show the full label, never the short form (history tables, where each row is read as a record
   * of what happened); the pill grows to fit and may wrap.
   */
  full?: boolean;
}

/**
 * Colour-coded, outlined status pill (BDO): green approved or done, yellow waiting for review or
 * approval, blue in process, red exception or rejected; unknown statuses render in the neutral
 * Header Blue tone. Every pill has one height, one font and a fixed minimum width; inside a table
 * cell it takes the column's width, so all pills of a column are the same size. Long labels use the
 * agreed short form, with the full label in the tooltip; the pill never wraps. History tables pass
 * `full`: the full label is shown and the pill grows to fit it.
 */
export function StatusBadge({ status, label, tone, full = false }: Readonly<StatusBadgeProps>) {
  const fullLabel = label ?? statusLabel(status);
  const shown = full ? fullLabel : statusShortLabel(status, label);
  const className = `badge ${tone ?? statusTone(status)}${full ? ' badge-full' : ''}`;
  return (
    <span className={className} title={fullLabel}>
      {shown}
    </span>
  );
}
