import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { DialogFooter } from '../record/FormParts';
import type { AdviceDraft, AdviceRecipient } from './api';
import { insurerApi } from './api';
import { addresses, adviceErrors } from './insurerLogic';

function DraftRow({
  draft,
  checked,
  to,
  onToggle,
  onTo,
}: Readonly<{
  draft: AdviceDraft;
  checked: boolean;
  to: string;
  onToggle: () => void;
  onTo: (value: string) => void;
}>) {
  return (
    <div className="stack">
      <label className="checkbox" htmlFor={`advice-${draft.insurerCode}`}>
        <input
          id={`advice-${draft.insurerCode}`}
          type="checkbox"
          checked={checked}
          onChange={onToggle}
        />{' '}
        <strong>{draft.insurerName}</strong>&nbsp;({draft.insurerCode})
      </label>
      {checked && (
        <>
          <input
            aria-label={`Recipients of ${draft.insurerCode}`}
            className="input"
            placeholder="Insurer claims e-mail"
            value={to}
            onChange={(e) => onTo(e.target.value)}
          />
          <div className="message-body">
            <strong>{draft.subject}</strong>
            {'\n\n'}
            {draft.body}
          </div>
        </>
      )}
    </div>
  );
}

/**
 * Send Loss Advice (process p.24-25, FR-CL-024): the advice of each insurer composed from template
 * BCL_LOSS_ADVICE, previewed, then e-mailed to the recipients confirmed for each insurer; each sent
 * advice is kept as a claims report on the claim, the account and the client.
 */
export function LossAdviceDialog({
  companyId,
  claimId,
  onClose,
  onSent,
}: Readonly<{ companyId: number; claimId: number; onClose: () => void; onSent: () => void }>) {
  const toast = useToast();
  const drafts = useQuery({
    queryKey: ['broker-claims', 'advice', claimId],
    queryFn: () => insurerApi.adviceDrafts(companyId, claimId),
  });
  const [selected, setSelected] = useState<string[]>([]);
  const [to, setTo] = useState<Record<string, string>>({});
  const [errors, setErrors] = useState<string[]>([]);
  const send = useMutation({
    mutationFn: (recipients: AdviceRecipient[]) =>
      insurerApi.sendAdvice(companyId, claimId, recipients),
    onSuccess: (sent) => {
      toast.success(`Loss advice queued for ${sent.map((s) => s.insurerCode).join(', ')}`);
      onSent();
    },
  });
  const list = drafts.data ?? [];
  const toggle = (d: AdviceDraft) => {
    if (selected.includes(d.insurerCode)) {
      setSelected(selected.filter((c) => c !== d.insurerCode));
      return;
    }
    setSelected([...selected, d.insurerCode]);
    setTo({ ...to, [d.insurerCode]: to[d.insurerCode] ?? d.suggestedTo.join(', ') });
  };
  const confirm = () => {
    const found = adviceErrors(selected, to);
    setErrors(found);
    if (found.length === 0) {
      send.mutate(
        selected.map((code) => ({ insurerCode: code, to: addresses(to[code] ?? ''), cc: [] })),
      );
    }
  };
  return (
    <Modal
      title="Send Loss Advice"
      open
      onClose={onClose}
      footer={
        <DialogFooter
          label="Send Loss Advice"
          busy={send.isPending}
          onClose={onClose}
          onConfirm={confirm}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={drafts.error ?? send.error} />
        {errors.map((e) => (
          <div key={e} className="field-error" role="alert">
            {e}
          </div>
        ))}
        {list.length === 0 && !drafts.isLoading && (
          <p className="muted">Add the insurers of the claim first.</p>
        )}
        {list.map((d) => (
          <DraftRow
            key={d.insurerCode}
            draft={d}
            checked={selected.includes(d.insurerCode)}
            to={to[d.insurerCode] ?? ''}
            onToggle={() => toggle(d)}
            onTo={(value) => setTo({ ...to, [d.insurerCode]: value })}
          />
        ))}
      </div>
    </Modal>
  );
}
