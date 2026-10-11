import { X } from 'lucide-react';
import { useEffect, useId, useRef } from 'react';
import type { ReactNode, RefObject } from 'react';
import { Button } from './Button';
import { DefinitionGrid } from './DefinitionGrid';
import type { Definition } from './DefinitionGrid';
import {
  dialogTitle,
  entersConfirm,
  focusablesOf,
  initialFocusOf,
  needsCancel,
  primaryOf,
} from './dialogStandard';
import type { ModalSize } from './dialogStandard';

interface ModalProps {
  /** Names the action and the object: "Acknowledge Alert ALR-000012" (shown in Title Case). */
  title: string;
  open: boolean;
  onClose: () => void;
  footer?: ReactNode;
  children: ReactNode;
  /** sm 480 px (confirmations), md 640 px (a few fields), lg 920 px (forms, tables); by content. */
  size?: ModalSize;
  /** The key facts of the record the dialog acts on, shown first. */
  facts?: readonly Definition[];
  /** One sentence of help under the facts. */
  helper?: ReactNode;
}

/** The open dialogs, the last on top: Escape closes the top one only. */
const openDialogs: string[] = [];

/** Escape closes the top dialog, unless a list inside handled it or the action is running. */
function useEscape(
  open: boolean,
  id: string,
  onClose: () => void,
  root: RefObject<HTMLDialogElement | null>,
) {
  useEffect(() => {
    if (!open) {
      return undefined;
    }
    openDialogs.push(id);
    const onKey = (e: KeyboardEvent) => {
      const busy = Boolean(root.current?.querySelector('.modal-footer .spinner'));
      if (e.key === 'Escape' && !e.defaultPrevented && openDialogs.at(-1) === id && !busy) {
        onClose();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      openDialogs.splice(openDialogs.indexOf(id), 1);
    };
  }, [open, id, onClose, root]);
}

/** The dialog's own keys (Tab and Enter), listened to on the dialog element. */
function useDialogKeys(open: boolean, root: RefObject<HTMLDialogElement | null>) {
  useEffect(() => {
    const dialog = root.current;
    if (!open || dialog === null) {
      return undefined;
    }
    const onKey = (e: KeyboardEvent) => onDialogKey(e, dialog);
    dialog.addEventListener('keydown', onKey);
    return () => dialog.removeEventListener('keydown', onKey);
  }, [open, root]);
}

/** Focus moves into the dialog when it opens and back to where it was when it closes. */
function useFocus(open: boolean, root: RefObject<HTMLDialogElement | null>) {
  useEffect(() => {
    if (!open || root.current === null) {
      return undefined;
    }
    const before = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    initialFocusOf(root.current).focus();
    return () => {
      if (before?.isConnected === true) {
        before.focus();
      }
    };
  }, [open, root]);
}

/** Tab stays inside the dialog; Enter in a one-line field runs the primary action. */
function onDialogKey(e: KeyboardEvent, root: HTMLElement) {
  if (e.key === 'Tab') {
    const items = focusablesOf(root);
    const first = items[0];
    const last = items.at(-1);
    if (first === undefined || last === undefined) {
      return;
    }
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault();
      last.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault();
      first.focus();
    }
    return;
  }
  if (e.key === 'Enter' && !e.defaultPrevented && entersConfirm(e.target)) {
    const primary = primaryOf(root);
    if (primary !== null) {
      e.preventDefault();
      primary.click();
    }
  }
}

/**
 * The dialog standard: a Title Case title naming the object, the key facts of the record, the
 * content, then Cancel (secondary) to the left of the primary action. Escape closes it (not while
 * the action runs), Enter in a field confirms, the focus stays inside and returns on close.
 */
export function Modal({
  title,
  open,
  onClose,
  footer,
  children,
  size,
  facts,
  helper,
}: Readonly<ModalProps>) {
  const id = useId();
  const root = useRef<HTMLDialogElement>(null);
  useEscape(open, id, onClose, root);
  useFocus(open, root);
  useDialogKeys(open, root);
  if (!open) {
    return null;
  }
  const shownTitle = dialogTitle(title);
  return (
    <div className="modal-backdrop">
      <dialog
        ref={root}
        className={`modal modal-${size ?? 'auto'}`}
        aria-labelledby={`${id}-title`}
        aria-modal="true"
        tabIndex={-1}
        open
      >
        <header className="card-header">
          <h2 id={`${id}-title`}>{shownTitle}</h2>
          <div className="spacer" />
          <button
            type="button"
            className="btn btn-ghost btn-sm modal-close"
            onClick={onClose}
            aria-label="Close"
          >
            <X size={22} aria-hidden="true" />
          </button>
        </header>
        <div className="card-body modal-body">
          {facts !== undefined && facts.length > 0 && (
            <div className="dialog-context">
              <DefinitionGrid items={facts} label="Key facts" />
            </div>
          )}
          {helper !== undefined && <p className="dialog-helper">{helper}</p>}
          {children}
        </div>
        {footer !== undefined && (
          <footer className="modal-footer">
            {needsCancel(footer) && (
              <Button variant="secondary" onClick={onClose}>
                Cancel
              </Button>
            )}
            {footer}
          </footer>
        )}
      </dialog>
    </div>
  );
}
