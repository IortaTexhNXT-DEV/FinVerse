import type { CoverItem } from '../cover/api';
import type { LocationPick } from './api';

/**
 * Picker of the insured locations of a claim (BRCLM.037; FR-CL-020): only the locations of the
 * claim's cover, each ticked once, with the damage at that location.
 */
export function LocationPicker({
  locations,
  picks,
  onChange,
  exclude = [],
}: Readonly<{
  locations: CoverItem[];
  picks: LocationPick[];
  onChange: (picks: LocationPick[]) => void;
  /** Item numbers already on the claim. */
  exclude?: number[];
}>) {
  const available = locations.filter((l) => !exclude.includes(l.itemNo));
  const picked = (itemNo: number) => picks.find((p) => p.itemNo === itemNo);
  const toggle = (itemNo: number) =>
    onChange(
      picked(itemNo) === undefined
        ? [...picks, { itemNo, description: '' }]
        : picks.filter((p) => p.itemNo !== itemNo),
    );
  const describe = (itemNo: number, description: string) =>
    onChange(picks.map((p) => (p.itemNo === itemNo ? { ...p, description } : p)));
  if (available.length === 0) {
    return <p className="muted">Every location of the cover is already on the claim.</p>;
  }
  return (
    <div className="stack">
      {available.map((l) => {
        const pick = picked(l.itemNo);
        const inputId = `location-${l.itemNo}`;
        return (
          <div key={l.itemNo} className="row">
            <label className="checkbox" htmlFor={`${inputId}-tick`}>
              <input
                id={`${inputId}-tick`}
                type="checkbox"
                checked={pick !== undefined}
                onChange={() => toggle(l.itemNo)}
              />{' '}
              <strong>Item {l.itemNo}</strong>&nbsp;{l.label}
              {l.city ? `, ${l.city}` : ''}
            </label>
            {pick !== undefined && (
              <input
                id={`${inputId}-damage`}
                aria-label={`Damage at item ${l.itemNo}`}
                className="input spacer"
                maxLength={500}
                placeholder="Damage at this location"
                value={pick.description ?? ''}
                onChange={(e) => describe(l.itemNo, e.target.value)}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}
