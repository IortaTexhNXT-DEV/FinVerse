/** One row of a tree table with its child rows. */
export interface TreeNode<T> {
  key: string;
  row: T;
  children?: readonly TreeNode<T>[];
}

/** A row as shown: its place in the tree for the treegrid attributes and the keyboard. */
export interface VisibleRow<T> {
  node: TreeNode<T>;
  level: number;
  posInSet: number;
  setSize: number;
  parentKey?: string;
  hasChildren: boolean;
}

/** The rows shown: every root row and the children of the expanded rows, in tree order. */
export function visibleRows<T>(
  nodes: readonly TreeNode<T>[],
  expanded: ReadonlySet<string>,
  level = 1,
  parentKey?: string,
): VisibleRow<T>[] {
  return nodes.flatMap((node, i) => {
    const children = node.children ?? [];
    const here: VisibleRow<T> = {
      node,
      level,
      posInSet: i + 1,
      setSize: nodes.length,
      parentKey,
      hasChildren: children.length > 0,
    };
    return expanded.has(node.key) && children.length > 0
      ? [here, ...visibleRows(children, expanded, level + 1, node.key)]
      : [here];
  });
}

/** Keys of every row that has children (Expand All). */
export function branchKeys<T>(nodes: readonly TreeNode<T>[]): string[] {
  return nodes.flatMap((n) =>
    n.children && n.children.length > 0 ? [n.key, ...branchKeys(n.children)] : [],
  );
}

/** What a key press on a tree row does: focus another row, or toggle the row. */
export type TreeKeyEffect = { focus: string } | { toggle: string } | { activate: true } | null;

/**
 * The treegrid keyboard (WAI-ARIA): Up / Down move between rows, Right expands (or moves to the
 * first child), Left collapses (or moves to the parent), Home / End, Enter activates.
 */
export function treeKey<T>(
  key: string,
  rows: readonly VisibleRow<T>[],
  index: number,
  expanded: ReadonlySet<string>,
): TreeKeyEffect {
  const current = rows[index];
  if (current === undefined) {
    return null;
  }
  const open = expanded.has(current.node.key);
  const focus = (row: VisibleRow<T> | undefined) => (row ? { focus: row.node.key } : null);
  const moves: Record<string, () => TreeKeyEffect> = {
    ArrowDown: () => focus(rows[index + 1]),
    ArrowUp: () => focus(rows[index - 1]),
    Home: () => focus(rows[0]),
    End: () => focus(rows.at(-1)),
    ArrowRight: () => {
      if (!current.hasChildren) {
        return null;
      }
      return open ? focus(rows[index + 1]) : { toggle: current.node.key };
    },
    ArrowLeft: () => {
      if (current.hasChildren && open) {
        return { toggle: current.node.key };
      }
      return current.parentKey === undefined ? null : { focus: current.parentKey };
    },
    Enter: () => ({ activate: true }),
  };
  return moves[key]?.() ?? null;
}
