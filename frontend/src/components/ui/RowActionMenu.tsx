import { MoreHorizontal } from 'lucide-react';
import { useEffect, useId, useRef, useState } from 'react';
import type { KeyboardEvent } from 'react';

/** One action of a row menu. */
export interface RowAction {
  label: string;
  onSelect: () => void;
  /** Destructive action (deactivate, remove): red text, listed last after a separator. */
  danger?: boolean;
  disabled?: boolean;
}

interface RowActionMenuProps {
  /** What the row is, for the button's accessible name ("Actions for T-CBG1"). */
  label: string;
  actions: readonly RowAction[];
}

interface Position {
  top: number;
  right: number;
}

/** Where the menu opens: under the button, right-aligned with it. */
function positionOf(button: HTMLButtonElement | null): Position {
  const rect = button?.getBoundingClientRect();
  return { top: (rect?.bottom ?? 0) + 4, right: window.innerWidth - (rect?.right ?? 0) };
}

/**
 * The actions of a table row behind one "more" button (never inline links): a menu of the row's
 * actions, destructive ones last and in red. The menu opens under the button, closes on Escape, a
 * click outside or a scroll, follows the button when the window is resized, and moves with the arrow keys.
 */
export function RowActionMenu({ label, actions }: Readonly<RowActionMenuProps>) {
  const id = useId();
  const button = useRef<HTMLButtonElement>(null);
  const menu = useRef<HTMLDivElement>(null);
  const [position, setPosition] = useState<Position | null>(null);
  const open = position !== null;
  const ordered = [...actions.filter((a) => !a.danger), ...actions.filter((a) => a.danger)];
  const close = (refocus: boolean) => {
    setPosition(null);
    if (refocus) {
      button.current?.focus();
    }
  };
  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const outside = (e: MouseEvent) => {
      const target = e.target as Node;
      if (!menu.current?.contains(target) && !button.current?.contains(target)) {
        setPosition(null);
      }
    };
    const dismiss = () => setPosition(null);
    // A window resize keeps the menu open under its button.
    const follow = () => setPosition(positionOf(button.current));
    document.addEventListener('mousedown', outside);
    window.addEventListener('scroll', dismiss, true);
    window.addEventListener('resize', follow);
    menu.current?.querySelector<HTMLButtonElement>('[role="menuitem"]:not(:disabled)')?.focus();
    return () => {
      document.removeEventListener('mousedown', outside);
      window.removeEventListener('scroll', dismiss, true);
      window.removeEventListener('resize', follow);
    };
  }, [open]);
  if (actions.length === 0) {
    return null;
  }
  const toggle = () => {
    if (open) {
      close(false);
      return;
    }
    setPosition(positionOf(button.current));
  };
  const menuKeys = (e: KeyboardEvent<HTMLDivElement>) => {
    const items = Array.from(
      menu.current?.querySelectorAll<HTMLButtonElement>('[role="menuitem"]:not(:disabled)') ?? [],
    );
    const at = items.indexOf(document.activeElement as HTMLButtonElement);
    let next: HTMLButtonElement | undefined;
    if (e.key === 'ArrowDown') {
      next = items[(at + 1) % items.length];
    } else if (e.key === 'ArrowUp') {
      next = items[(at - 1 + items.length) % items.length];
    } else if (e.key === 'Home') {
      next = items[0];
    } else if (e.key === 'End') {
      next = items.at(-1);
    } else if (e.key === 'Escape' || e.key === 'Tab') {
      e.preventDefault();
      close(true);
      return;
    } else {
      return;
    }
    e.preventDefault();
    next?.focus();
  };
  const firstDanger = ordered.findIndex((a) => a.danger);
  return (
    <>
      <button
        ref={button}
        type="button"
        className="row-menu-button"
        aria-label={`Actions for ${label}`}
        title="Actions"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? id : undefined}
        onClick={(e) => {
          e.stopPropagation();
          toggle();
        }}
        onKeyDown={(e) => {
          if (e.key === 'ArrowDown' && !open) {
            e.preventDefault();
            toggle();
          }
        }}
      >
        <MoreHorizontal size={18} aria-hidden="true" />
      </button>
      {position !== null && (
        <div
          ref={menu}
          id={id}
          role="menu"
          tabIndex={-1}
          aria-label={`Actions for ${label}`}
          className="row-menu"
          style={{ top: position.top, right: position.right }}
          onKeyDown={menuKeys}
        >
          {ordered.map((a, i) => (
            <button
              key={a.label}
              type="button"
              role="menuitem"
              className={`row-menu-item${a.danger ? ' danger' : ''}${
                i === firstDanger && i > 0 ? ' separated' : ''
              }`}
              disabled={a.disabled}
              tabIndex={-1}
              onClick={(e) => {
                e.stopPropagation();
                close(true);
                a.onSelect();
              }}
            >
              {a.label}
            </button>
          ))}
        </div>
      )}
    </>
  );
}
