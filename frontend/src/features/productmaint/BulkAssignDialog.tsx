import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { PACKAGE_REQUEST_ENTITY } from '@/api/productmaint';
import type { RequestListItem } from '@/api/productmaint';
import { workflowApi } from '@/api/workflow';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';

interface BulkAssignDialogProps {
  /** Selected requests, all in the same stage. */
  requests: RequestListItem[];
  onClose: () => void;
  onDone: () => Promise<void>;
}

/**
 * Team leader: assigns the selected package requests (one stage) to a user of the stage's team,
 * or returns them to the team queue (BRPM.011: the right assignee).
 */
export function BulkAssignDialog({ requests, onClose, onDone }: Readonly<BulkAssignDialogProps>) {
  const toast = useToast();
  const [assignee, setAssignee] = useState('');
  const first = requests[0];
  const firstCase = useQuery({
    queryKey: ['workflow', 'case', PACKAGE_REQUEST_ENTITY, first?.id],
    queryFn: () => workflowApi.byRecord(PACKAGE_REQUEST_ENTITY, first?.id ?? 0),
    enabled: first !== undefined,
  });
  const caseId = firstCase.data?.item.id;
  const users = useQuery({
    queryKey: ['workflow', 'assignees', caseId],
    queryFn: () => workflowApi.assignees(caseId ?? 0),
    enabled: caseId !== undefined,
  });
  const assign = useMutation({
    mutationFn: async () => {
      for (const r of requests) {
        const detail = await workflowApi.byRecord(PACKAGE_REQUEST_ENTITY, r.id);
        await workflowApi.assign(detail.item.id, assignee || null);
      }
    },
    onSuccess: async () => {
      await onDone();
      toast.success(
        assignee
          ? `${requests.length} request(s) assigned to ${assignee}`
          : `${requests.length} request(s) returned to the team queue`,
      );
      onClose();
    },
  });
  return (
    <Modal
      open
      title="Assign Package Requests"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={assign.isPending} onClick={() => assign.mutate()}>
            Assign
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={firstCase.error ?? users.error ?? assign.error} />
        <p className="muted">
          {requests.map((r) => r.requestNo).join(', ')} ·{' '}
          {first === undefined ? '' : humanize(first.status)}
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
