/**
 * Column choice of a wide list (DataTable): which columns show, in which order, and the row
 * density, saved per user in the browser. Hidden columns stay in the chooser (and in the Excel
 * export, which the server builds with every column).
 */

export type Density = 'comfortable' | 'compact';

/** The saved choice of one list. */
export interface ColumnChoice {
  order: string[];
  hidden: string[];
  density: Density;
}

/** What the chooser needs of a column. */
export interface ChoosableColumn {
  key: string;
  /** Shown only when the user chooses it (a column that does not fit the default view). */
  defaultHidden?: boolean;
}

/** Columns that always show: the row selection, the first data column (reference) and the actions. */
export function lockedKeys(columns: readonly (ChoosableColumn & { kind?: string })[]): Set<string> {
  const locked = new Set<string>();
  const first = columns.find((c) => c.key !== 'select');
  columns.forEach((c) => {
    if (c.key === 'select' || c.kind === 'actions' || c === first) {
      locked.add(c.key);
    }
  });
  return locked;
}

/** The default choice of a list: column order as defined, the default-hidden columns hidden. */
export function defaultChoice(columns: readonly ChoosableColumn[]): ColumnChoice {
  return {
    order: columns.map((c) => c.key),
    hidden: columns.filter((c) => c.defaultHidden === true).map((c) => c.key),
    density: 'comfortable',
  };
}

/**
 * A saved choice applied to the columns of today: columns no longer defined are dropped, new
 * columns take their defined place (after their defined predecessor) with their default visibility.
 */
export function mergeChoice(
  columns: readonly ChoosableColumn[],
  saved: Partial<ColumnChoice> | null,
): ColumnChoice {
  const fresh = defaultChoice(columns);
  if (saved === null || !Array.isArray(saved.order)) {
    return fresh;
  }
  const known = new Set(fresh.order);
  const order = saved.order.filter((k) => known.has(k));
  fresh.order.forEach((key, i) => {
    if (!order.includes(key)) {
      const before = fresh.order[i - 1];
      const at = before === undefined ? 0 : order.indexOf(before) + 1;
      order.splice(at, 0, key);
    }
  });
  const savedHidden = Array.isArray(saved.hidden) ? saved.hidden : [];
  const savedKeys = new Set(saved.order);
  const hidden = [
    ...savedHidden.filter((k) => known.has(k)),
    // A column added since the choice was saved keeps its default visibility.
    ...fresh.hidden.filter((k) => !savedKeys.has(k)),
  ];
  return {
    order,
    hidden: [...new Set(hidden)],
    density: saved.density === 'compact' ? 'compact' : 'comfortable',
  };
}

/** The columns to show, in the chosen order (locked columns always shown). */
export function visibleColumns<C extends ChoosableColumn>(
  columns: readonly C[],
  choice: ColumnChoice,
  locked: ReadonlySet<string>,
): C[] {
  const byKey = new Map(columns.map((c) => [c.key, c]));
  return choice.order
    .filter((k) => locked.has(k) || !choice.hidden.includes(k))
    .map((k) => byKey.get(k))
    .filter((c): c is C => c !== undefined);
}

/** Shows or hides one column. */
export function toggleColumn(choice: ColumnChoice, key: string): ColumnChoice {
  const hidden = choice.hidden.includes(key)
    ? choice.hidden.filter((k) => k !== key)
    : [...choice.hidden, key];
  return { ...choice, hidden };
}

/** Moves one column a place up (-1) or down (+1) in the order. */
export function moveColumn(choice: ColumnChoice, key: string, step: -1 | 1): ColumnChoice {
  const order = [...choice.order];
  const at = order.indexOf(key);
  const to = at + step;
  if (at < 0 || to < 0 || to >= order.length) {
    return choice;
  }
  order.splice(at, 1);
  order.splice(to, 0, key);
  return { ...choice, order };
}

/** The browser storage key of a list's choice, per user. */
export function choiceKey(user: string, list: string): string {
  return `bibs.columns.${user}.${list}`;
}

/** The saved choice of a list, or null (nothing saved, or the browser storage is not available). */
export function loadChoice(key: string): Partial<ColumnChoice> | null {
  try {
    const text = window.localStorage.getItem(key);
    return text === null ? null : (JSON.parse(text) as Partial<ColumnChoice>);
  } catch {
    return null;
  }
}

/** Saves the choice of a list; a choice equal to the default is removed (reset). */
export function saveChoice(key: string, choice: ColumnChoice, fresh: ColumnChoice): void {
  try {
    if (JSON.stringify(choice) === JSON.stringify(fresh)) {
      window.localStorage.removeItem(key);
    } else {
      window.localStorage.setItem(key, JSON.stringify(choice));
    }
  } catch {
    // The choice stays for this page only when the browser keeps no storage.
  }
}
