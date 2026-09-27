import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { issuanceApi } from '@/api/issuance';
import type { Advice, Outcome } from '@/api/issuance';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { addresses } from './issuanceLogic';

/**
 * Sends the selected Insurance Advices (BRNB.060/035): one e-mail per advice to the mortgagee bank
 * unit, the PDF encrypted and the password in a separate e-mail.
 */
export function SendAdviceDialog({
  advices,
  onClose,
  onSent,
}: Readonly<{ advices: Advice[]; onClose: () => void; onSent: (sent: Advice[]) => void }>) {
  const [to, setTo] = useState('');
  const [cc, setCc] = useState('');
  const [hint, setHint] = useState('');
  const send = useMutation({
    mutationFn: () =>
      issuanceApi.sendAdvices(
        advices.map((a) => a.id),
        addresses(to),
        addresses(cc),
        hint.trim() || undefined,
      ),
    onSuccess: onSent,
  });
  return (
    <Modal
      open
      title={`Send ${advices.length} Insurance Advice(s)`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            busy={send.isPending}
            disabled={addresses(to).length === 0}
            onClick={() => send.mutate()}
          >
            Send via Email
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={send.error} />
        <p className="muted">{advices.map((a) => a.iaNo).join(', ')}</p>
        <Field
          label="To"
          required
          hint="The mortgagee bank unit; separate several addresses with commas."
        >
          {(id) => (
            <input id={id} className="input" value={to} onChange={(e) => setTo(e.target.value)} />
          )}
        </Field>
        <Field label="Cc">
          {(id) => (
            <input id={id} className="input" value={cc} onChange={(e) => setCc(e.target.value)} />
          )}
        </Field>
        <Field label="Password hint" hint="Optional, e.g. how the password is built.">
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

/** Generates the Insurance Advice of one or more mortgaged accounts given by ARN (BRNB.070). */
export function GenerateAdviceDialog({
  onClose,
  onDone,
}: Readonly<{ onClose: () => void; onDone: (outcomes: Outcome[]) => void }>) {
  const [arns, setArns] = useState('');
  const generate = useMutation({
    mutationFn: () => issuanceApi.generateAdvices(addresses(arns)),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title="Generate Insurance Advice"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            busy={generate.isPending}
            disabled={addresses(arns).length === 0}
            onClick={() => generate.mutate()}
          >
            Generate
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={generate.error} />
        <Field
          label="Accounts (ARN)"
          required
          hint="Mortgaged accounts placed or issued; separate several ARNs with commas or new lines."
        >
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={4}
              value={arns}
              onChange={(e) => setArns(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
