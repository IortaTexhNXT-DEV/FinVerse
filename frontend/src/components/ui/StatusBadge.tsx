import { humanize } from '@/utils/format';
import { statusShortLabel, statusTone } from './statusTones';
import type { Tone } from './statusTones';

interface StatusBadgeProps {
  /** Status code (LOCKED, SUCCEEDED, RETURNED_TO_MARKETING…); picks the tone and the label. */
  status: string;
  /** Label to show instead of the humanized code (e.g. a workflow stage name). */
  label?: string;
  /** Tone to use instead of the status's own tone. */
  tone?: Tone;
}

/**
 * Colour-coded, outlined status pill (BDO): green approved or done, yellow waiting for review or
 * approval, blue in process, red exception or rejected; unknown statuses render in the neutral
 * Header Blue tone. Every pill has one height, one font and a fixed minimum width; inside a table
 * cell it takes the column's width, so all pills of a column are the same size. Long labels use the
 * agreed short form, with the full label in the tooltip; the pill never wraps.
 */
export function StatusBadge({ status, label, tone }: Readonly<StatusBadgeProps>) {
  const full = label ?? humanize(status);
  const shown = statusShortLabel(status, label);
  return (
    <span className={`badge ${tone ?? statusTone(status)}`} title={full}>
      {shown}
    </span>
  );
}
