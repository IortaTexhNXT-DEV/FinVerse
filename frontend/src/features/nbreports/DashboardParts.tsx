import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { barWidth } from './dashboardData';

interface DrillTileProps {
  label: string;
  value: ReactNode;
  hint: string;
  to: string;
  /** Draws attention (overdue items). */
  alert?: boolean;
}

/** KPI tile of the NB dashboard that opens the filtered list behind the figure (BRNB.012). */
export function DrillTile({ label, value, hint, to, alert = false }: Readonly<DrillTileProps>) {
  return (
    <Link to={to} className={`card kpi kpi-button nb-tile ${alert ? 'kpi-alert' : ''}`}>
      <span className="kpi-label">{label}</span>
      <span className="kpi-value">{value}</span>
      <span className="kpi-hint">{hint}</span>
    </Link>
  );
}

/** One bar of a {@link BarList}. */
export interface BarItem {
  key: string;
  label: string;
  value: number;
  to: string;
  /** Text after the value (e.g. "42 %"). */
  note?: string;
}

/**
 * Horizontal bars with the label on the left and the count on the bar: white on the Header Blue
 * bar, dark next to a short bar (BDO style guide contrast rule). Every bar opens its list.
 */
export function BarList({ items, label }: Readonly<{ items: BarItem[]; label: string }>) {
  const max = Math.max(0, ...items.map((i) => i.value));
  return (
    <ul className="nb-bars" aria-label={label}>
      {items.map((item) => {
        const width = barWidth(item.value, max);
        const inside = width >= 18;
        return (
          <li key={item.key}>
            <Link
              to={item.to}
              className="nb-bar-row"
              aria-label={`${item.label}: ${String(item.value)}`}
            >
              <span className="nb-bar-label">{item.label}</span>
              <span className="nb-bar-track">
                <span className="nb-bar-fill" style={{ width: `${String(width)}%` }}>
                  {inside && <span className="nb-bar-value">{item.value}</span>}
                </span>
                {!inside && <span className="nb-bar-value outside">{item.value}</span>}
              </span>
              <span className="nb-bar-note">{item.note ?? ''}</span>
            </Link>
          </li>
        );
      })}
    </ul>
  );
}
