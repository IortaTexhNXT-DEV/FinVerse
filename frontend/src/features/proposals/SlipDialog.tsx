import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { proposalsApi } from '@/api/proposals';
import type { WorkAction } from '@/api/workflow';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { formatAmount } from '@/utils/format';

export interface SlipInput {
  replyBy?: string;
  closePending: boolean;
  insurerCode?: string;
  comment?: string;
}

function InsurerChoice({
  proposalId,
  value,
  onChange,
}: Readonly<{ proposalId: number; value: string; onChange: (code: string) => void }>) {
  const responses = useQuery({
    queryKey: ['proposal', proposalId, 'responses'],
    queryFn: () => proposalsApi.responses(proposalId),
  });
  const received = (responses.data ?? []).filter((r) => r.status === 'RECEIVED');
  return (
    <SelectInput
      label="Insurer of the proposal slip"
      blank="The recommended insurer"
      value={value}
      options={received.map((r) => ({
        value: r.insurerCode,
        label: `${r.insurerName} – PHP ${formatAmount(r.premium)}${r.recommended ? ' (recommended)' : ''}`,
      }))}
      onChange={onChange}
    />
  );
}

/**
 * The parameters of the slip actions: reply date of the quotation slip (BRNB.008), closing the
 * request for terms with responses pending (BRNB.009), and the insurer of the proposal slip
 * (BRNB.017).
 */
export function SlipDialog({
  action,
  proposalId,
  busy,
  error,
  onClose,
  onConfirm,
}: Readonly<{
  action: WorkAction;
  proposalId: number;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onConfirm: (input: SlipInput) => void;
}>) {
  const [replyBy, setReplyBy] = useState('');
  const [closePending, setClosePending] = useState(false);
  const [insurer, setInsurer] = useState('');
  const [comment, setComment] = useState('');
  return (
    <Modal
      open
      title={action.label}
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
              onConfirm({
                replyBy: replyBy || undefined,
                closePending,
                insurerCode: insurer || undefined,
                comment: comment.trim() || undefined,
              })
            }
          >
            {action.label}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        {action.action === 'submit_qs' && (
          <TextInput
            label="Insurers reply by"
            type="date"
            hint="Blank = the configured reply time"
            value={replyBy}
            onChange={setReplyBy}
          />
        )}
        {action.action === 'terms_complete' && (
          <label className="checkbox">
            <input
              type="checkbox"
              checked={closePending}
              onChange={(e) => setClosePending(e.target.checked)}
            />
            Close the request although some insurers have not answered
          </label>
        )}
        {action.action === 'submit_ps' && (
          <InsurerChoice proposalId={proposalId} value={insurer} onChange={setInsurer} />
        )}
        <Field label="Comment">
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={1000}
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
