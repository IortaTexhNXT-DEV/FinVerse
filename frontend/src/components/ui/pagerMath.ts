/** "Showing 1 to 20 of 57 results" for a zero-based page. */
export function showingText(page: number, size: number, total: number): string {
  const from = Math.min(total, page * size + 1);
  const to = Math.min(total, (page + 1) * size);
  return `Showing ${String(from)} to ${String(to)} of ${String(total)} results`;
}

/**
 * Page numbers to show (zero-based): the first, the last and the current page with its
 * neighbours; `null` marks a gap. Seven pages or fewer are all shown.
 */
export function pageWindow(page: number, totalPages: number): (number | null)[] {
  const all = Array.from({ length: totalPages }, (_, i) => i);
  if (totalPages <= 7) {
    return all;
  }
  const keep = new Set([0, totalPages - 1, page - 1, page, page + 1]);
  const out: (number | null)[] = [];
  all
    .filter((p) => keep.has(p))
    .forEach((p, i, kept) => {
      const previous = kept[i - 1];
      if (previous !== undefined && p - previous > 1) {
        out.push(null);
      }
      out.push(p);
    });
  return out;
}
