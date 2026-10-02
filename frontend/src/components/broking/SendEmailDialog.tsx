import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';

/** What the user confirmed in the send dialog. */
export interface EmailDraft {
  to: string[];
  cc: string[];
  subject: string;
  body: string;
  protect: boolean;
  passwordHint?: string;
}

interface SendEmailDialogProps {
  title: string;
  initial: Omit<EmailDraft, 'to' | 'cc'> & { to?: string; cc?: string };
  /** Attachments that will be generated and sent (names only, for the user's information). */
  attachmentNames?: string[];
  /** Allow switching protection off (documents to insurers may be sent unprotected). */
  protectionOptional?: boolean;
  busy?: boolean;
  error?: unknown;
  onSend: (draft: EmailDraft) => void;
  onClose: () => void;
}

const splitAddresses = (text: string) =>
  text
    .split(/[,;\s]+/)
    .map((a) => a.trim())
    .filter((a) => a.length > 0);

/**
 * Review and send an e-mail from a record: recipients, copy, subject, body and password
 * protection of the attachments; the password is sent in a separate e-mail (BRNB.013/035).
 */
export function SendEmailDialog({
  title,
  initial,
  attachmentNames = [],
  protectionOptional = false,
  busy = false,
  error,
  onSend,
  onClose,
}: Readonly<SendEmailDialogProps>) {
  const [to, setTo] = useState(initial.to ?? '');
  const [cc, setCc] = useState(initial.cc ?? '');
  const [subject, setSubject] = useState(initial.subject);
  const [body, setBody] = useState(initial.body);
  const [protect, setProtect] = useState(initial.protect);
  const [hint, setHint] = useState(initial.passwordHint ?? '');
  const invalid = splitAddresses(to).length === 0 || subject.trim() === '' || body.trim() === '';
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={busy}
            disabled={invalid}
            onClick={() =>
              onSend({
                to: splitAddresses(to),
                cc: splitAddresses(cc),
                subject: subject.trim(),
                body,
                protect,
                passwordHint: hint.trim() || undefined,
              })
            }
          >
            Send
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="To" required hint="Separate several addresses with commas.">
          {(id) => (
            <input id={id} className="input" value={to} onChange={(e) => setTo(e.target.value)} />
          )}
        </Field>
        <Field label="Cc">
          {(id) => (
            <input id={id} className="input" value={cc} onChange={(e) => setCc(e.target.value)} />
          )}
        </Field>
        <Field label="Subject" required>
          {(id) => (
            <input
              id={id}
              className="input"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
            />
          )}
        </Field>
        <Field label="Message" required>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={7}
              value={body}
              onChange={(e) => setBody(e.target.value)}
            />
          )}
        </Field>
        {attachmentNames.length > 0 && (
          <p className="muted">Attachments: {attachmentNames.join(', ')}</p>
        )}
        <label className="checkbox">
          <input
            type="checkbox"
            checked={protect}
            disabled={!protectionOptional}
            onChange={(e) => setProtect(e.target.checked)}
          />{' '}
          Password-protect the attachments and send the password in a separate e-mail
        </label>
        {protect && (
          <Field
            label="Password hint for the recipient"
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
        )}
      </div>
    </Modal>
  );
}
