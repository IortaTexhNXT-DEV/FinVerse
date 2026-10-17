import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { RenewalRow } from '@/api/submitted';
import { InsurerName } from '@/components/broking/LovLabel';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { InsurerSelect } from '../common/InsurerSelect';
import { SBM_LOV } from '../common/submittedCodes';

/** Re-assigns the insurer of a renewal whose hold cover is not accepted (FR-SP-063). */
export function ReassignDialog({
  row,
  onClose,
}: Readonly<{ row: RenewalRow; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [insurer, setInsurer] = useState('');
  const [reason, setReason] = useState('');
  const save = useMutation({
    mutationFn: () => submittedApi.reassign(row.policyId, insurer, reason),
    onSuccess: () => {
      toast.success(`${row.sbmNo} re-assigned`);
      void queryClient.invalidateQueries({ queryKey: ['submitted'] });
      onClose();
    },
  });
  return (
    <Modal
      title="Re-assign Insurer"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            disabled={insurer === '' || reason === '' || save.isPending}
            onClick={() => save.mutate()}
          >
            Re-assign
          </Button>
        </>
      }
    >
      <ErrorAlert error={save.error} />
      <p>
        {row.sbmNo} · {row.assuredName}: the open hold cover request is closed and a new one is sent
        to the insurer chosen.
      </p>
      <Field label="New Insurer" required>
        {(id) => (
          <InsurerSelect
            id={id}
            value={insurer}
            onChange={setInsurer}
            exclude={row.insurerAssigned}
          />
        )}
      </Field>
      <Field label="Reason" required>
        {(id) => <LovSelect id={id} type={SBM_LOV.decline} value={reason} onChange={setReason} />}
      </Field>
      <p className="muted">
        Current insurer: <InsurerName code={row.insurerAssigned} />
      </p>
    </Modal>
  );
}
