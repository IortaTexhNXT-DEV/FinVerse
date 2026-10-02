import { MoreHorizontal } from 'lucide-react';
import { useEffect, useId, useRef, useState } from 'react';
import type { CSSProperties, KeyboardEvent, MouseEvent, ReactNode } from 'react';
import type { Confirmation } from './ConfirmButton';
import { ConfirmDialog } from './ConfirmDialog';

/** One action of a table row. Hidden actions are left out; disabled ones show with their reason. */
export interface RowAction {
  label: string;
  /** Runs the action; with `confirm`, runs it from the confirmation with the reason given. */
  onSelect: (reason: string) => unknown;
  /** Ask first (platform standard for cancel, return, release, deactivate): the themed dialog. */
  confirm?: Confirmation;
  /** Shown but not selectable; the reason is the tooltip. */
  disabledReason?: string;
  /** A destructive or reversing action (Cancel, Return, Release): drawn in red, listed last. */
  danger?: boolean;
  hidden?: boolean;
  icon?: ReactNode;
}

interface RowActionsProps {
  /** The record the actions act on, read by screen readers ("Actions for DSQ-2026-000004"). */
  record: string;
  actions: readonly RowAction[];
}

/**
 * The row action menu of a table (screen standard: actions on a record of a list are one menu
 * button at the end of the row, never several buttons or links in the row). The button opens a
 * menu of the actions the user may take now; destructive ones come last, in red. The menu closes
 * on a choice, on Escape and on a click outside, and never selects the row it sits in.
 */
export function RowActions({ record, actions }: Readonly<RowActionsProps>) {
  const shown = [
    ...actions.filter((a) => !a.hidden && !a.danger),
    ...actions.filter((a) => !a.hidden && a.danger),
  ];
  const [open, setOpen] = useState(false);
  const [place, setPlace] = useState<CSSProperties>();
  const [asking, setAsking] = useState<RowAction>();
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const closeDialog = () => {
    setAsking(undefined);
    setError(null);
  };
  const menuId = useId();
  const root = useRef<HTMLDivElement>(null);
  const button = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const close = (e: globalThis.MouseEvent) => {
      if (root.current !== null && !root.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    const shut = () => setOpen(false);
    document.addEventListener('mousedown', close);
    window.addEventListener('resize', shut);
    window.addEventListener('scroll', shut, true);
    return () => {
      document.removeEventListener('mousedown', close);
      window.removeEventListener('resize', shut);
      window.removeEventListener('scroll', shut, true);
    };
  }, [open]);
  // The menu is placed on the page under its button, so a scrolling table never cuts it off.
  const toggle = () => {
    const r = button.current?.getBoundingClientRect();
    if (r !== undefined) {
      setPlace({ position: 'fixed', top: r.bottom + 4, right: window.innerWidth - r.right });
    }
    setOpen((o) => !o);
  };
  if (shown.length === 0) {
    return <span className="muted">—</span>;
  }
  const stop = (e: MouseEvent | KeyboardEvent) => e.stopPropagation();
  const onKey = (e: KeyboardEvent<HTMLDivElement>) => {
    e.stopPropagation();
    if (e.key === 'Escape') {
      setOpen(false);
    }
  };
  return (
    // The wrapper only stops the row's own click and keys from firing; the controls inside are
    // the button and the menu items.
    // eslint-disable-next-line jsx-a11y/no-static-element-interactions
    <div className="row-actions" ref={root} onClick={stop} onKeyDown={onKey}>
      <button
        type="button"
        className="btn btn-ghost btn-sm row-actions-button"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        aria-label={`Actions for ${record}`}
        title="Actions"
        ref={button}
        onClick={toggle}
      >
        <MoreHorizontal size={18} aria-hidden="true" />
      </button>
      {open && (
        <div
          className="row-actions-menu"
          role="menu"
          id={menuId}
          aria-label={`Actions for ${record}`}
          style={place}
        >
          {shown.map((a) => (
            <button
              key={a.label}
              type="button"
              role="menuitem"
              className={a.danger ? 'row-actions-item danger' : 'row-actions-item'}
              disabled={a.disabledReason !== undefined}
              title={a.disabledReason}
              onClick={() => {
                setOpen(false);
                if (a.confirm === undefined) {
                  a.onSelect('');
                } else {
                  setAsking(a);
                }
              }}
            >
              {a.icon}
              {a.label}
            </button>
          ))}
        </div>
      )}
      {asking?.confirm !== undefined && (
        <ConfirmDialog
          title={asking.confirm.title}
          record={asking.confirm.record ?? record}
          effect={asking.confirm.effect}
          confirmLabel={asking.confirm.confirmLabel ?? asking.label}
          reason={asking.confirm.reason}
          destructive={asking.confirm.destructive ?? asking.danger}
          busy={running}
          error={error}
          onClose={closeDialog}
          onConfirm={(reason) => {
            setRunning(true);
            setError(null);
            Promise.resolve(asking.onSelect(reason))
              .then(closeDialog)
              .catch((e: unknown) => setError(e))
              .finally(() => setRunning(false));
          }}
        />
      )}
    </div>
  );
}
