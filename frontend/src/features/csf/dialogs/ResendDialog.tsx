import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { csfApi } from '@/api/csf';
import type { ResendKind } from '@/api/csf';
import { Button } from '@/components/ui/Button';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { useToast } from '@/components/ui/toastContext';
import { isEmail } from '../csfCodes';
import '../csf.css';

function addressError(other: boolean, recipient: string): string | undefined {
  const text = recipient.trim();
  return other && text !== '' && !isEmail(text) ? 'Enter a valid e-mail address' : undefined;
}

function canSend(
  other: boolean,
  recipient: string,
  reason: string,
  registered: string | null | undefined,
): boolean {
  if (!other) {
    return Boolean(registered);
  }
  return (
    recipient.trim() !== '' && addressError(other, recipient) === undefined && reason.trim() !== ''
  );
}

/** Another address and the reason for it (supervisors). */
function OtherAddress({
  recipient,
  reason,
  error,
  onRecipient,
  onReason,
}: Readonly<{
  recipient: string;
  reason: string;
  error: string | undefined;
  onRecipient: (v: string) => void;
  onReason: (v: string) => void;
}>) {
  return (
    <div className="form-grid">
      <Field label="Recipient" required error={error}>
        {(id) => (
          <input
            id={id}
            className="input"
            type="email"
            value={recipient}
            onChange={(e) => onRecipient(e.target.value)}
          />
        )}
      </Field>
      <Field label="Reason" required>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={500}
            value={reason}
            onChange={(e) => onReason(e.target.value)}
          />
        )}
      </Field>
    </div>
  );
}

/**
 * Resend (FR-CSF-030, 031; BRCSF-006 / 6.001, CSF-EM09): the document, the recipient - the
 * registered e-mail, or another address with a reason for a supervisor - and the e-mail text; the
 * file goes password protected and the password follows in a separate e-mail.
 */
export function ResendDialog({
  companyId,
  clientId,
  kind,
  documentId,
  onClose,
}: Readonly<{
  companyId: number;
  clientId: number;
  kind: ResendKind;
  documentId: number;
  onClose: () => void;
}>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [other, setOther] = useState(false);
  const [recipient, setRecipient] = useState('');
  const [reason, setReason] = useState('');
  const preview = useQuery({
    queryKey: ['csf', 'resend-preview', companyId, clientId, kind, documentId],
    queryFn: () => csfApi.preview(companyId, clientId, kind, documentId),
  });
  const p = preview.data;
  const send = useMutation({
    mutationFn: () =>
      csfApi.resend(companyId, clientId, kind, {
        documentId,
        recipient: other ? recipient.trim() : undefined,
        reason: other ? reason.trim() : undefined,
      }),
    onSuccess: (r) => {
      toast.success(`${r.documentName} sent to ${r.recipient}`);
      void queryClient.invalidateQueries({ queryKey: ['csf'] });
      onClose();
    },
  });
  const recipientError = addressError(other, recipient);
  const ready = canSend(other, recipient, reason, p?.registeredEmail);
  const title = kind === 'RA' ? 'Resend Renewal Advice' : 'Resend E-policy';
  return (
    <Modal
      title={title}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            disabled={!ready}
            busy={send.isPending}
            onClick={() => send.mutate()}
          >
            Resend
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={preview.error ?? send.error} title={`Cannot ${title.toLowerCase()}`} />
        {p && !p.registeredEmail && !other && (
          <Notice tone="warning" title="No registered e-mail">
            The client has no registered e-mail. Update the contact details first.
          </Notice>
        )}
        {p && (
          <DefinitionGrid
            label="Resend details"
            items={[
              { label: 'Document', value: p.documentName },
              { label: 'Registered E-mail', value: p.registeredEmail },
              { label: 'Subject', value: p.subject },
              {
                label: 'Message',
                value: <span className="csf-message">{p.body}</span>,
                wide: true,
              },
            ]}
          />
        )}
        {p?.otherAllowed && (
          <label className="checkbox">
            <input type="checkbox" checked={other} onChange={(e) => setOther(e.target.checked)} />
            Send to another address
          </label>
        )}
        {other && (
          <OtherAddress
            recipient={recipient}
            reason={reason}
            error={recipientError}
            onRecipient={setRecipient}
            onReason={setReason}
          />
        )}
        <p className="muted">
          The file is sent password protected; the password follows in a separate e-mail.
        </p>
      </div>
    </Modal>
  );
}
