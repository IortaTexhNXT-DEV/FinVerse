import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';

interface BatchSendDialogProps {
  references: string[];
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSend: (passwordHint?: string) => void;
}

/**
 * Confirms the batch send of approved quotations (BRNB.042): one e-mail per client with the
 * password-protected PDF and Excel files; the password follows in a separate e-mail.
 */
export function BatchSendDialog({
  references,
  busy,
  error,
  onClose,
  onSend,
}: Readonly<BatchSendDialogProps>) {
  const [hint, setHint] = useState('');
  return (
    <Modal
      open
      title="Send via Email"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={busy} onClick={() => onSend(hint.trim() || undefined)}>
            Send {references.length} Quotation(s)
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p>
          Each client receives one e-mail with its quotations attached, password protected. The
          password is sent in a separate e-mail to the client&apos;s address on file.
        </p>
        <ul className="warning-list">
          {references.map((r) => (
            <li key={r}>{r}</li>
          ))}
        </ul>
        <Field
          label="Password hint for the clients"
          hint="Optional, e.g. how the password is built."
        >
          {(id) => (
            <input
              id={id}
              className="input"
              value={hint}
              onChange={(e) => setHint(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
