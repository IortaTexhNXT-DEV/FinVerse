import { X } from 'lucide-react';

/** One active filter shown as a removable chip. */
export interface ActiveFilter {
  /** Stable key of the filter. */
  key: string;
  /** "Status: Unapplied" */
  label: string;
  onRemove: () => void;
}

/**
 * The active filters of a list as removable chips, with Clear All (BDO list pattern). Renders
 * nothing when no filter is active.
 */
export function FilterChips({
  filters,
  onClearAll,
}: Readonly<{ filters: readonly ActiveFilter[]; onClearAll?: () => void }>) {
  if (filters.length === 0) {
    return null;
  }
  return (
    <div className="filter-chips" aria-label="Active filters">
      {filters.map((f) => (
        <span key={f.key} className="filter-chip">
          {f.label}
          <button type="button" aria-label={`Remove ${f.label}`} onClick={f.onRemove}>
            <X size={14} aria-hidden="true" />
          </button>
        </span>
      ))}
      {onClearAll !== undefined && (
        <button type="button" className="link-button inline" onClick={onClearAll}>
          Clear All
        </button>
      )}
    </div>
  );
}
