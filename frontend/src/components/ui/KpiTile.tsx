import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { rankBreakdown } from './kpiBreakdown';
import type { KpiBreakdownItem } from './kpiBreakdown';

/** A labelled figure of a tile (an amount and what it is: "Premium (PHP)" 25,950.00). */
export interface KpiPair {
  label: string;
  value: ReactNode;
}

interface KpiTileProps {
  label: string;
  value: ReactNode;
  /** The list behind the figure, opened from the figure. */
  to?: string;
  /** One short line under the figure ("2 waiting for the client's answer"). */
  qualifier?: ReactNode;
  /** Draws attention (overdue items): red top bar. */
  alert?: boolean;
  /** Ranked categories with counts, each opening its filtered list. */
  breakdown?: {
    label: string;
    items: readonly KpiBreakdownItem[];
    /** List of every category, for "View all n". */
    allTo?: string;
    limit?: number;
  };
  /** Labelled figures (amounts) under the value. */
  pairs?: readonly KpiPair[];
  /** Stable name for callouts and capture recipes. */
  callout?: string;
}

function Breakdown({ breakdown }: Readonly<{ breakdown: NonNullable<KpiTileProps['breakdown']> }>) {
  const ranked = rankBreakdown(breakdown.items, breakdown.limit);
  return (
    <>
      <ul className="kpi-list" aria-label={breakdown.label}>
        {ranked.shown.map((item) => {
          const content = (
            <>
              <span className="kpi-list-label" title={item.label}>
                {item.label}
              </span>
              <span className="kpi-list-value">{item.count}</span>
            </>
          );
          return (
            <li key={item.key}>
              {item.to === undefined ? (
                <span className="kpi-list-row">{content}</span>
              ) : (
                <Link className="kpi-list-row" to={item.to}>
                  {content}
                </Link>
              )}
            </li>
          );
        })}
      </ul>
      {ranked.hidden > 0 && breakdown.allTo !== undefined && (
        <Link className="kpi-more" to={breakdown.allTo}>
          View all {ranked.total}
        </Link>
      )}
    </>
  );
}

/**
 * Dashboard KPI tile with one anatomy for every tile of a row: the label, the big figure (opening
 * its list), one short qualifier, then an optional compact structured part — a ranked breakdown
 * (top categories with right-aligned counts, each opening its filtered list, and "View all n") or
 * labelled figures. Tiles of a row take one height; the structured part starts at the same line in
 * every tile. Never a dot-separated sentence.
 */
export function KpiTile({
  label,
  value,
  to,
  qualifier,
  alert = false,
  breakdown,
  pairs,
  callout,
}: Readonly<KpiTileProps>) {
  return (
    <section
      className={alert ? 'card kpi-tile kpi-tile-alert' : 'card kpi-tile'}
      aria-label={label}
      data-callout={callout}
    >
      <h3 className="kpi-tile-label">{label}</h3>
      <div className="kpi-tile-value">
        {to === undefined ? value : <Link to={to}>{value}</Link>}
      </div>
      <div className="kpi-tile-qualifier">{qualifier}</div>
      {(breakdown !== undefined || pairs !== undefined) && (
        <div className="kpi-tile-detail">
          {breakdown !== undefined && <Breakdown breakdown={breakdown} />}
          {pairs !== undefined && (
            <dl className="kpi-pairs">
              {pairs.map((p) => (
                <div key={p.label} className="kpi-list-row">
                  <dt className="kpi-list-label">{p.label}</dt>
                  <dd className="kpi-list-value">{p.value}</dd>
                </div>
              ))}
            </dl>
          )}
        </div>
      )}
    </section>
  );
}
