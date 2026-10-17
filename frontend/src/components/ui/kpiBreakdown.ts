/** One line of the breakdown of a KPI tile: a category and its count, opening the filtered list. */
export interface KpiBreakdownItem {
  key: string;
  label: string;
  count: number;
  to?: string;
}

/** The lines a KPI tile shows and how many categories are left out. */
export interface RankedBreakdown {
  shown: KpiBreakdownItem[];
  /** Number of categories in all (shown and left out). */
  total: number;
  /** Number of categories left out ("View all n" when above 0). */
  hidden: number;
}

/** Most lines of a tile breakdown (the tile keeps one height across the row). */
export const KPI_BREAKDOWN_LIMIT = 4;

/**
 * The breakdown of a tile as a short ranked list: categories with a count, largest first (ties by
 * label), the first `limit` shown and the rest counted for "View all n".
 */
export function rankBreakdown(
  items: readonly KpiBreakdownItem[],
  limit: number = KPI_BREAKDOWN_LIMIT,
): RankedBreakdown {
  const ranked = items
    .filter((i) => i.count > 0)
    .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label));
  const shown = ranked.length > limit + 1 ? ranked.slice(0, limit) : ranked;
  return { shown, total: ranked.length, hidden: ranked.length - shown.length };
}
