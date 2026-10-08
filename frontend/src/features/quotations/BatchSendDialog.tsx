import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import type { BatchRecipients } from '@/api/quotations';
import { emailList } from './batchSend';

interface BatchSendDialogProps {
  references: string[];
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSend: (recipients: BatchRecipients) => void;
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
  const [to, setTo] = useState('');
  const [cc, setCc] = useState('');
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
          <Button
            variant="accent"
            busy={busy}
            onClick={() =>
              onSend({
                passwordHint: hint.trim() || undefined,
                to: emailList(to),
                cc: emailList(cc),
              })
            }
          >
            Send {references.length} Quotation(s)
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p>
          Each client receives one e-mail with its quotations attached, password protected. The
          password is sent in a separate e-mail to the client&apos;s address on file. A quotation
          that is not approved, has expired or has no client e-mail is not sent and is listed with
          its reason; the others are sent.
        </p>
        <ul className="warning-list">
          {references.map((r) => (
            <li key={r}>{r}</li>
          ))}
        </ul>
        <Field
          label="Further recipients"
          hint="Besides each client, e-mail addresses separated by commas."
        >
          {(id) => (
            <input id={id} className="input" value={to} onChange={(e) => setTo(e.target.value)} />
          )}
        </Field>
        <Field label="Copy to">
          {(id) => (
            <input id={id} className="input" value={cc} onChange={(e) => setCc(e.target.value)} />
          )}
        </Field>
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
