import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { renewalPlacementApi } from '@/api/renewalPlacement';
import type { PlacementView } from '@/api/renewalPlacement';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';

/** Records the insurer's placement response (FRRN.030.01): Approved, or Rejected with a reason. */
export function RespondDialog({
  renewalRef,
  placement,
  onClose,
}: Readonly<{ renewalRef: string; placement: PlacementView; onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [approved, setApproved] = useState(true);
  const [reason, setReason] = useState('');
  const [remarks, setRemarks] = useState('');
  const save = useMutation({
    mutationFn: () =>
      renewalPlacementApi.respond(companyId, renewalRef, {
        insurerCode: placement.insurerCode,
        approved,
        reason: approved ? undefined : reason,
        remarks: remarks.trim() === '' ? undefined : remarks,
      }),
    onSuccess: async () => {
      toast.success(approved ? 'Placement approved' : 'Placement rejected');
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  return (
    <Modal
      open
      title="Record Insurer Response"
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={save.isPending}
            disabled={!approved && reason === ''}
            onClick={() => save.mutate()}
          >
            Save
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <Field label="Insurer Response" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={approved ? 'A' : 'R'}
            onChange={(e) => setApproved(e.target.value === 'A')}
          >
            <option value="A">Approved</option>
            <option value="R">Rejected</option>
          </select>
        )}
      </Field>
      {!approved && (
        <Field label="Rejection Reason" required>
          {(id) => (
            <LovSelect
              id={id}
              type="RNW_PLACEMENT_REJECT_REASON"
              value={reason}
              onChange={setReason}
              required
            />
          )}
        </Field>
      )}
      <Field label="Insurer Remarks">
        {(id) => (
          <textarea
            id={id}
            className="input"
            rows={2}
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
          />
        )}
      </Field>
    </Modal>
  );
}
