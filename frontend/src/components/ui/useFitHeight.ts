import { useLayoutEffect } from 'react';
import type { RefObject } from 'react';

/** Measures of a list and of the page around it, in pixels. */
export interface FitMeasures {
  /** Current height of the list's scroll box. */
  wrap: number;
  /** Full height of the list (all its rows, with a sideways scroll bar if any). */
  table: number;
  /** How far the page overflows the window (scroll height less visible height; negative = room left). */
  overflow: number;
  /** Least height of a capped list. */
  min: number;
}

/**
 * The height cap of a list that scrolls inside its card (BDO style guide), or null for none: the
 * list is as high as its rows unless the page would then scroll too, in which case it takes just
 * the room left in the window (never less than `min`), so the page keeps one vertical scroll bar.
 */
export function fittedHeight(m: FitMeasures): number | null {
  const target = m.wrap - m.overflow;
  if (target >= m.table) {
    return null;
  }
  return Math.max(m.min, Math.floor(target));
}

const DEFAULT_MIN = 240;

/**
 * Caps the height of the list in `ref` to the room left in the window when it is the only list of
 * the page: its header row stays sticky inside the card and the page shows one vertical scroll bar.
 * A page with several lists scrolls as a whole.
 */
export function useFitHeight(ref: RefObject<HTMLDivElement | null>): void {
  useLayoutEffect(() => {
    const wrap = ref.current;
    const main = wrap?.closest<HTMLElement>('.app-main');
    if (
      wrap === null ||
      main === null ||
      main === undefined ||
      typeof ResizeObserver === 'undefined'
    ) {
      return undefined;
    }
    let frame = 0;
    const apply = (cap: string) => {
      if (wrap.style.maxHeight !== cap) {
        wrap.style.maxHeight = cap;
      }
    };
    const fit = () => {
      frame = 0;
      const table = wrap.firstElementChild;
      if (!(table instanceof HTMLElement) || main.querySelectorAll('[data-fit]').length !== 1) {
        apply('');
        return;
      }
      const min = Number.parseFloat(getComputedStyle(wrap).getPropertyValue('--table-min-height'));
      const cap = fittedHeight({
        wrap: wrap.getBoundingClientRect().height,
        table: table.offsetHeight + (wrap.offsetHeight - wrap.clientHeight),
        overflow: main.scrollHeight - main.clientHeight,
        min: Number.isFinite(min) ? min : DEFAULT_MIN,
      });
      apply(cap === null ? '' : `${String(cap)}px`);
    };
    const schedule = () => {
      if (frame === 0) {
        frame = requestAnimationFrame(fit);
      }
    };
    const observer = new ResizeObserver(schedule);
    observer.observe(wrap);
    if (wrap.firstElementChild !== null) {
      observer.observe(wrap.firstElementChild);
    }
    if (main.firstElementChild !== null) {
      observer.observe(main.firstElementChild);
    }
    observer.observe(main);
    window.addEventListener('resize', schedule);
    schedule();
    return () => {
      observer.disconnect();
      window.removeEventListener('resize', schedule);
      cancelAnimationFrame(frame);
    };
  }, [ref]);
}
