import type { AlertSeverity } from '@/api/alerts';
import { humanize } from '@/utils/format';

const TONES: Record<AlertSeverity, string> = {
  CRITICAL: 'danger',
  HIGH: 'danger',
  MEDIUM: 'warning',
  LOW: 'neutral',
};

/** Colour-coded alert severity. */
export function SeverityBadge({ severity }: Readonly<{ severity: AlertSeverity }>) {
  return <span className={`badge ${TONES[severity]}`}>{humanize(severity)}</span>;
}
