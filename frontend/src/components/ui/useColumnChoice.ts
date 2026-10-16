import { useContext, useMemo, useState } from 'react';
import { AuthContext } from '@/auth/authContext';
import {
  choiceKey,
  defaultChoice,
  loadChoice,
  lockedKeys,
  mergeChoice,
  saveChoice,
  visibleColumns,
} from './tableColumns';
import type { ChoosableColumn, ColumnChoice } from './tableColumns';

/** Lists with this many columns or more keep their reference column in view (sticky). */
export const WIDE_FROM = 8;

/**
 * The column choice of a list (DataTable): the columns to show and their order, kept per user in
 * the browser under the list's name. Only a list with optional columns (hidden by default) gets the
 * chooser; every other list shows all its columns and no tools.
 */
export function useColumnChoice<C extends ChoosableColumn & { kind?: string }>(
  columns: readonly C[],
  list: string | undefined,
  enabled: boolean | undefined,
) {
  const user = useContext(AuthContext)?.user?.username ?? 'guest';
  const on = enabled ?? columns.some((c) => c.defaultHidden === true);
  const name = list ?? columns.map((c) => c.key).join('-');
  const key = choiceKey(user, name);
  const fresh = useMemo(() => defaultChoice(columns), [columns]);
  const [state, setState] = useState<{ key: string; choice: ColumnChoice } | null>(null);
  const saved = on ? loadChoice(key) : null;
  const choice = mergeChoice(columns, state?.key === key ? state.choice : saved);
  const locked = lockedKeys(columns);
  const change = (next: ColumnChoice) => {
    setState({ key, choice: next });
    saveChoice(key, next, fresh);
  };
  return {
    on,
    wide: on || columns.length >= WIDE_FROM,
    choice,
    locked,
    visible: on ? visibleColumns(columns, choice, locked) : [...columns],
    change,
    reset: () => change(fresh),
  };
}
