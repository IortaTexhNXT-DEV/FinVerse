import { AlarmClock } from 'lucide-react';

/** A tile of a workbench: a count that opens its list. */
export interface WorkTile {
  key: string;
  label: string;
  value: number;
  active?: boolean;
  /** Draws attention to a non-zero count (returns, expiring items). */
  alert?: boolean;
  onClick: () => void;
}

/** Headline counts of a workbench (Placement, Issuance); each tile filters the list below. */
export function WorkTiles({ tiles, label }: Readonly<{ tiles: WorkTile[]; label: string }>) {
  return (
    <nav className="stage-tiles" aria-label={label}>
      {tiles.map((t) => (
        <button
          key={t.key}
          type="button"
          className={t.active === true ? 'stage-tile active' : 'stage-tile'}
          aria-pressed={t.active === true}
          onClick={t.onClick}
        >
          <span className="stage-tile-name">{t.label}</span>
          <span className="stage-tile-count">{t.value}</span>
          {t.alert === true && t.value > 0 && (
            <span className="stage-tile-meta">
              <span className="stage-tile-overdue">
                <AlarmClock size={11} aria-hidden="true" /> Needs attention
              </span>
            </span>
          )}
        </button>
      ))}
    </nav>
  );
}
