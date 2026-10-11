import { Children, isValidElement } from 'react';
import type { ReactNode } from 'react';
import { Button } from './Button';

/**
 * The dialog standard (Modal, ConfirmDialog, ActionDialog): sizes, Title Case titles naming the
 * object, a context block of key facts, Cancel to the left of the primary action, Escape / Enter
 * and focus kept inside the dialog.
 */
export type ModalSize = 'sm' | 'md' | 'lg';

const MINOR = new Set([
  'a',
  'an',
  'and',
  'as',
  'at',
  'by',
  'for',
  'from',
  'in',
  'into',
  'of',
  'on',
  'or',
  'per',
  'the',
  'to',
  'vs',
  'with',
]);

/**
 * A dialog title in Title Case: words written in lower case get a capital ("Resolve alert" becomes
 * "Resolve Alert"); references, codes, names with capitals, file names and minor words after the
 * first are kept as written.
 */
export function dialogTitle(title: string): string {
  return title
    .split(' ')
    .map((word, i) => {
      if (!/^[a-z][a-z-]*[a-z]$|^[a-z]$/.test(word)) {
        return word;
      }
      if (i > 0 && MINOR.has(word)) {
        return word;
      }
      return word
        .split('-')
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join('-');
    })
    .join(' ');
}

/** Labels of buttons that dismiss a dialog. */
const DISMISS = /^(cancel|close|done|back|go back|keep|not now|discard changes)$/i;

interface ElementProps {
  variant?: string;
  className?: string;
  children?: ReactNode;
  type?: string;
}

function textOf(node: ReactNode): string {
  if (typeof node === 'string' || typeof node === 'number') {
    return String(node);
  }
  if (Array.isArray(node)) {
    return node.map(textOf).join('');
  }
  if (isValidElement<ElementProps>(node)) {
    return textOf(node.props.children);
  }
  return '';
}

/** The buttons written directly in a footer (through fragments), or null when it holds anything else. */
function footerButtons(footer: ReactNode): ElementProps[] | null {
  const found: ElementProps[] = [];
  const state = { known: true };
  const walk = (node: ReactNode) => {
    Children.forEach(node, (child) => {
      if (!isValidElement<ElementProps>(child)) {
        return;
      }
      if (child.type === Button || child.type === 'button') {
        found.push(child.props);
      } else if (typeof child.type === 'symbol') {
        walk(child.props.children); // a fragment
      } else {
        state.known = false;
      }
    });
  };
  walk(footer);
  return state.known ? found : null;
}

/** Whether a footer button dismisses the dialog (secondary or ghost style, or a Cancel / Close label). */
function dismisses(button: ElementProps): boolean {
  const style = `${button.variant ?? ''} ${button.className ?? ''}`;
  return /secondary|ghost|link-button/.test(style) || DISMISS.test(textOf(button.children).trim());
}

/**
 * Whether the dialog needs a Cancel button of its own: the footer has an action but no way back.
 * A footer with other components than buttons is left as written.
 */
export function needsCancel(footer: ReactNode): boolean {
  const buttons = footerButtons(footer);
  return buttons !== null && buttons.length > 0 && !buttons.some(dismisses);
}

const FOCUSABLE =
  'a[href], button:not(:disabled), input:not(:disabled):not([type="hidden"]), select:not(:disabled), textarea:not(:disabled), [tabindex]:not([tabindex="-1"])';

/** The elements of a dialog that take the keyboard focus, in order. */
export function focusablesOf(root: HTMLElement): HTMLElement[] {
  return Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
    (e) => !e.closest('[hidden]'),
  );
}

/** The element to focus when a dialog opens: the first field, else the dismissing button. */
export function initialFocusOf(root: HTMLElement): HTMLElement {
  const field = root.querySelector<HTMLElement>(
    '[autofocus], .modal-body input:not(:disabled):not([type="hidden"]):not([type="checkbox"]):not([type="radio"]), .modal-body select:not(:disabled), .modal-body textarea:not(:disabled)',
  );
  const dismiss = root.querySelector<HTMLElement>(
    '.modal-footer .btn-secondary:not(:disabled), .modal-footer .btn-ghost:not(:disabled)',
  );
  return field ?? dismiss ?? root;
}

/** The primary action of a dialog: the last enabled footer button that does not dismiss it. */
export function primaryOf(root: HTMLElement): HTMLButtonElement | null {
  const buttons = Array.from(
    root.querySelectorAll<HTMLButtonElement>('.modal-footer button:not(:disabled)'),
  ).filter(
    (b) =>
      !/btn-secondary|btn-ghost|link-button/.test(b.className) &&
      !DISMISS.test(b.textContent.trim()),
  );
  return buttons.at(-1) ?? null;
}

/** Whether Enter in this element confirms the dialog (a one-line field outside a submitting form). */
export function entersConfirm(target: EventTarget | null): boolean {
  if (!(target instanceof HTMLInputElement) && !(target instanceof HTMLSelectElement)) {
    return false;
  }
  if (
    target instanceof HTMLInputElement &&
    ['checkbox', 'radio', 'file', 'button', 'submit'].includes(target.type)
  ) {
    return false;
  }
  if (target.getAttribute('aria-expanded') === 'true') {
    return false; // an open list takes Enter for its choice
  }
  const form = target.closest('form');
  return (form?.querySelector('[type="submit"]') ?? null) === null;
}

/**
 * The label of the button that closes a dialog without acting: Cancel, or Go Back when the action
 * itself is a cancellation ("Cancel Voucher" next to "Cancel" would read twice).
 */
export function cancelLabelFor(confirmLabel: string): string {
  return /^cancel\b/i.test(confirmLabel.trim()) ? 'Go Back' : 'Cancel';
}

/** The consequence sentence of a destructive action, unless its effect already says it. */
export function consequenceOf(effect: string): string {
  return /cannot be undone|irreversible|cannot be reversed|permanently/i.test(effect)
    ? ''
    : 'This cannot be undone.';
}
