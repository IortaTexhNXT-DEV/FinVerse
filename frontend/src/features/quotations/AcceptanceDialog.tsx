import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';

export interface AcceptanceInput {
  file?: File;
  groups: number[];
  comment?: string;
}

interface AcceptanceDialogProps {
  reference: string;
  /** Risk groups offered; each becomes one account. */
  groups: number[];
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onAccept: (input: AcceptanceInput) => void;
}

/**
 * Records the client's acceptance (BRNB.045): the acceptance e-mail is uploaded as a Client
 * acceptance e-mail document (it may already be attached), and the accepted risk groups are
 * chosen when the client accepts only part of the offer.
 */
export function AcceptanceDialog({
  reference,
  groups,
  busy,
  error,
  onClose,
  onAccept,
}: Readonly<AcceptanceDialogProps>) {
  const [file, setFile] = useState<File>();
  const [chosen, setChosen] = useState<number[]>(groups);
  const [comment, setComment] = useState('');
  const flip = (g: number) =>
    setChosen((c) => (c.includes(g) ? c.filter((x) => x !== g) : [...c, g].sort((a, b) => a - b)));
  return (
    <Modal
      open
      title={`Record Acceptance of ${reference}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={busy}
            disabled={chosen.length === 0}
            onClick={() => onAccept({ file, groups: chosen, comment: comment.trim() || undefined })}
          >
            Record Acceptance
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field
          label="Client's acceptance e-mail"
          hint="Upload the e-mail (.eml, .msg or PDF); leave empty when it is already attached."
        >
          {(id) => (
            <input
              id={id}
              className="input"
              type="file"
              onChange={(e) => setFile(e.target.files?.[0])}
            />
          )}
        </Field>
        {groups.length > 1 && (
          <fieldset className="stack">
            <legend>Risk groups accepted (one account each)</legend>
            {groups.map((g) => (
              <label key={g} className="checkbox">
                <input type="checkbox" checked={chosen.includes(g)} onChange={() => flip(g)} />
                Risk group {g}
              </label>
            ))}
          </fieldset>
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
