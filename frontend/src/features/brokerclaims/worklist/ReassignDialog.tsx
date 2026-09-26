import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { claimsHomeApi } from '../home/api';

/** Reassign the selected claims to another handler (NFR p.37, FR-CL-055; WORK_ASSIGN). */
export function ReassignDialog({
  count,
  busy,
  error,
  onClose,
  onSave,
}: Readonly<{
  count: number;
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (handler: string, comment: string) => void;
}>) {
  const assignees = useQuery({
    queryKey: ['broker-claims', 'assignees'],
    queryFn: claimsHomeApi.assignees,
  });
  const [handler, setHandler] = useState('');
  const [comment, setComment] = useState('');
  const [submitted, setSubmitted] = useState(false);
  return (
    <Modal
      open
      title={`Reassign ${count} Claim(s)`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            onClick={() => {
              setSubmitted(true);
              if (handler !== '') {
                onSave(handler, comment.trim());
              }
            }}
          >
            Reassign
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error ?? assignees.error} />
        <Field
          label="New handler"
          required
          error={submitted && handler === '' ? 'Select the new handler' : undefined}
        >
          {(id) => (
            <select
              id={id}
              className="select"
              value={handler}
              onChange={(e) => setHandler(e.target.value)}
            >
              <option value="">Select…</option>
              {(assignees.data ?? []).map((a) => (
                <option key={a.username} value={a.username}>
                  {a.username}
                  {a.unitCode === undefined ? ' (not in the register)' : ` · ${a.unitCode}`}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field label="Comment" hint="Kept in the claim history with the old and new handler.">
          {(id) => (
            <input
              id={id}
              className="input"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
