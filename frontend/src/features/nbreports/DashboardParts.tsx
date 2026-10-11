import { Fragment } from 'react';
import { Link } from 'react-router-dom';
import { barWidth } from './dashboardData';

/** One bar of a {@link BarList}. */
export interface BarItem {
  key: string;
  label: string;
  value: number;
  to: string;
  /** Text after the value (e.g. "42 %"). */
  note?: string;
  /** Heading of the group the bar belongs to (bars of one group follow each other). */
  group?: string;
}

/**
 * Horizontal bars with the label on the left and the count on the bar: white on the Header Blue
 * bar, dark next to a short bar (BDO style guide contrast rule). Every bar opens its list; bars of a group follow their group heading.
 */
export function BarList({ items, label }: Readonly<{ items: BarItem[]; label: string }>) {
  const max = Math.max(0, ...items.map((i) => i.value));
  return (
    <ul className="nb-bars" aria-label={label}>
      {items.map((item, i) => {
        const width = barWidth(item.value, max);
        const inside = width >= 18;
        const heading = item.group !== undefined && item.group !== items[i - 1]?.group;
        return (
          <Fragment key={item.key}>
            {heading && (
              <li className="nb-bar-group" aria-hidden="true">
                {item.group}
              </li>
            )}
            <li>
              <Link
                to={item.to}
                className="nb-bar-row"
                aria-label={`${item.group === undefined ? '' : item.group + ', '}${item.label}: ${String(item.value)}`}
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
          </Fragment>
        );
      })}
    </ul>
  );
}
