import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { workflowApi } from '@/api/workflow';
import type { WorkItem } from '@/api/workflow';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';

interface AssignDialogProps {
  item: WorkItem;
  onClose: () => void;
  onDone: () => Promise<void>;
}

/** Team leader: assign an item to a user of the stage's team, or back to the team queue. */
export function AssignDialog({ item, onClose, onDone }: Readonly<AssignDialogProps>) {
  const toast = useToast();
  const [assignee, setAssignee] = useState(item.assignee ?? '');
  const users = useQuery({
    queryKey: ['workflow', 'assignees', item.id],
    queryFn: () => workflowApi.assignees(item.id),
  });
  const assign = useMutation({
    mutationFn: () => workflowApi.assign(item.id, assignee || null),
    onSuccess: async (updated) => {
      await onDone();
      toast.success(
        updated.assignee ? `Assigned to ${updated.assignee}` : 'Returned to the team queue',
      );
      onClose();
    },
  });
  return (
    <Modal
      open
      title={`Assign ${item.reference}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={assign.isPending} onClick={() => assign.mutate()}>
            Save
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={users.error ?? assign.error} />
        <p className="muted">
          {item.title} · {item.stageName}
        </p>
        <Field label="Assignee" hint="Users of the team that works this stage.">
          {(id) => (
            <select
              id={id}
              className="select"
              value={assignee}
              onChange={(e) => setAssignee(e.target.value)}
            >
              <option value="">Team queue (unassigned)</option>
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
