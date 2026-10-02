import type { ReactNode } from 'react';
import { isEmptyValue } from '@/utils/presentation';

/** A label and its value. */
export type DetailRow = readonly [string, ReactNode];

/** Read-only label / value grid of a record (detail pages); an empty value is the muted dash. */
export function DetailList({ rows }: Readonly<{ rows: readonly DetailRow[] }>) {
  return (
    <dl className="form-grid" style={{ margin: 0 }}>
      {rows.map(([label, value]) => (
        <div key={label}>
          <dt className="muted">{label}</dt>
          <dd
            style={{ margin: 0, fontWeight: 600, overflowWrap: 'anywhere', whiteSpace: 'normal' }}
          >
            {isEmptyValue(value) ? <span className="muted">—</span> : value}
          </dd>
        </div>
      ))}
    </dl>
  );
}
