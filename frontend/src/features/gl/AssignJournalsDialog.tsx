import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { glPlatformApi } from './glPlatformApi';

interface Props {
  open: boolean;
  count: number;
  busy: boolean;
  error: unknown;
  onAssign: (assignee: string) => void;
  onClose: () => void;
}

/**
 * Assigns the selected journals to the user who will post them, or clears the assignment (FRBS
 * 2.5.1). Only users allowed to authorize journals are offered.
 */
export function AssignJournalsDialog({
  open,
  count,
  busy,
  error,
  onAssign,
  onClose,
}: Readonly<Props>) {
  const [assignee, setAssignee] = useState('');
  const users = useQuery({
    queryKey: ['journal-assignees'],
    queryFn: glPlatformApi.assignees,
    enabled: open,
  });
  return (
    <Modal
      title={`Assign ${count} journal(s)`}
      open={open}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={busy} onClick={() => onAssign(assignee)}>
            {assignee === '' ? 'Clear Assignment' : 'Assign'}
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error ?? users.error} />
        <Field label="Assign to" hint="The assignee sees the entries under “Assigned to me”.">
          {(id) => (
            <select
              id={id}
              className="select"
              value={assignee}
              onChange={(e) => setAssignee(e.target.value)}
            >
              <option value="">No one (clear the assignment)</option>
              {(users.data ?? []).map((u) => (
                <option key={u} value={u}>
                  {u}
                </option>
              ))}
            </select>
          )}
        </Field>
      </div>
    </Modal>
  );
}
