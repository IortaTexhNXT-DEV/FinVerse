import { useLayoutEffect } from 'react';
import type { RefObject } from 'react';

/**
 * Sticky first column of a wide list: the row selection (when there is one) and the first data
 * column, usually the reference, stay in view while the list scrolls sideways.
 */
export function stickyCount(columns: readonly { key: string }[]): number {
  if (columns.length < 2) {
    return 0;
  }
  return columns[0]?.key === 'select' ? 2 : 1;
}

/** The cell class with the sticky class of a leading column. */
export function withSticky(
  base: string | undefined,
  index: number,
  sticky: number,
): string | undefined {
  if (index >= sticky) {
    return base;
  }
  const own = `sticky-col sticky-col-${String(index)}${index === sticky - 1 ? ' sticky-last' : ''}`;
  return base === undefined ? own : `${base} ${own}`;
}

/** Sets the left offset of each sticky column (the widths of the sticky columns before it). */
export function useStickyOffsets(ref: RefObject<HTMLDivElement | null>, sticky: number): void {
  useLayoutEffect(() => {
    const wrap = ref.current;
    const table = wrap?.querySelector('table');
    if (sticky < 2 || table === null || table === undefined) {
      return;
    }
    const heads = table.querySelectorAll<HTMLElement>('thead th.sticky-col');
    let left = 0;
    heads.forEach((th, i) => {
      table.style.setProperty(`--sticky-left-${String(i)}`, `${String(left)}px`);
      left += th.getBoundingClientRect().width;
    });
  });
}
