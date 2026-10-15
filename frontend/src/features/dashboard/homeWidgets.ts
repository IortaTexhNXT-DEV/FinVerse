/** The delivered widgets of the home page, in order (Role Home Set-up changes them per role). */
export const DELIVERED_WIDGETS = [
  'KPIS',
  'WORKLOAD',
  'COLLECTIONS',
  'PAYABLES',
  'CASH',
  'BUDGET',
  'CHARTS',
] as const;

/** Widgets laid out in the narrow top row; the others go in the two-column grid below. */
const TOP_ROW = new Set<string>(['KPIS', 'WORKLOAD']);

/**
 * The widgets of the user's home page split into the top row and the grid, each in the order of
 * the role set-up. While the set-up is loading or cannot be read, the delivered widgets show.
 */
export function arrangeWidgets(mine: readonly string[] | undefined): {
  top: string[];
  grid: string[];
} {
  const codes = mine === undefined || mine.length === 0 ? [...DELIVERED_WIDGETS] : [...mine];
  return {
    top: codes.filter((c) => TOP_ROW.has(c)),
    grid: codes.filter((c) => !TOP_ROW.has(c)),
  };
}
