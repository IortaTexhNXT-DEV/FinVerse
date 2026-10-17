import { useState } from 'react';

/**
 * The dialog open on a renewal list, the renewals it acts on and the way to clear the selection of
 * the list once it is done.
 */
export function useListDialogs<K extends string>() {
  const [open, setOpen] = useState<K | ''>('');
  const [refs, setRefs] = useState<string[]>([]);
  const [clearSelection, setClearSelection] = useState<() => void>(() => () => undefined);
  const close = () => setOpen('');
  return {
    open,
    refs,
    close,
    /** Closes the dialog and clears the selection. */
    done: () => {
      setOpen('');
      clearSelection();
    },
    show: (kind: K, selected: string[] = [], clear: () => void = () => undefined) => {
      setRefs(selected);
      setClearSelection(() => clear);
      setOpen(kind);
    },
  };
}
