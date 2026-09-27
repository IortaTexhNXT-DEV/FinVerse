import { useState } from 'react';
import type { ComponentProps, ReactNode } from 'react';
import { Button } from './Button';
import { ConfirmDialog } from './ConfirmDialog';

/** What the confirmation names: the dialog title, the record and the effect of the action. */
export interface Confirmation {
  /** "Authorise Rule SOD-0001" */
  title: string;
  record?: string;
  effect: ReactNode;
  /** Label of the confirming button; the button's own text when omitted. */
  confirmLabel?: string;
  /** Ask for a reason: mandatory for a rejection. */
  reason?: 'required' | 'optional';
  /** Red danger button (reject, cancel, void, deactivate, delete, reverse). */
  destructive?: boolean;
}

type ConfirmButtonProps = Omit<ComponentProps<typeof Button>, 'onClick' | 'children'> & {
  /** Button text, also the confirming label by default. */
  children: string;
  confirm: Confirmation;
  /** Runs the action; the dialog closes when the returned promise resolves. */
  onConfirm: (reason: string) => Promise<unknown>;
};

/**
 * An action button that never acts at once (platform standard): it opens the themed confirmation
 * naming the record and the effect, asks for the reason where the process needs one (every
 * rejection), and runs the action from the dialog, which shows the error if it is refused.
 */
export function ConfirmButton({
  children,
  confirm,
  onConfirm,
  busy,
  ...button
}: Readonly<ConfirmButtonProps>) {
  const [open, setOpen] = useState(false);
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const close = () => {
    setOpen(false);
    setError(null);
  };
  return (
    <>
      <Button {...button} busy={busy} onClick={() => setOpen(true)}>
        {children}
      </Button>
      {open && (
        <ConfirmDialog
          title={confirm.title}
          record={confirm.record}
          effect={confirm.effect}
          confirmLabel={confirm.confirmLabel ?? children}
          reason={confirm.reason}
          destructive={confirm.destructive}
          busy={running}
          error={error}
          onClose={close}
          onConfirm={(reason) => {
            setRunning(true);
            setError(null);
            onConfirm(reason)
              .then(close)
              .catch((e: unknown) => setError(e))
              .finally(() => setRunning(false));
          }}
        />
      )}
    </>
  );
}
