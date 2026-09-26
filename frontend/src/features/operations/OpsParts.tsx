import { AlarmClock } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { InvoiceFlags, WorkCount } from '@/api/operations';
import { flagChips, tileTone } from './opsLabels';
import './operations.css';

/** Record flags as gold chips, beside the status pill (BDO UX: flags never inside the pill). */
export function FlagChips({ flags }: Readonly<{ flags: InvoiceFlags }>) {
  const chips = flagChips(flags);
  if (chips.length === 0) {
    return null;
  }
  return (
    <span className="flag-chips">
      {chips.map((chip) => (
        <span key={chip} className="flag-chip">
          {chip}
        </span>
      ))}
    </span>
  );
}

/** Work count tiles of an Operations section; each tile opens the list behind it. */
export function WorkCountTiles({
  counts,
  label,
}: Readonly<{ counts: WorkCount[]; label: string }>) {
  return (
    <nav className="ops-tiles" aria-label={label}>
      {counts.map((c) => (
        <Link
          key={c.key}
          to={c.link ?? '/operations'}
          className={`ops-tile ${tileTone(c.count, c.severity)}`}
        >
          <span className="ops-tile-label">{c.label}</span>
          <span className="ops-tile-count">{c.count}</span>
          {c.severity !== 'INFO' && c.count > 0 && (
            <span className="ops-muted">
              <AlarmClock size={11} aria-hidden="true" /> Needs attention
            </span>
          )}
        </Link>
      ))}
    </nav>
  );
}
