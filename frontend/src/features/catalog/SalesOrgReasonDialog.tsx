import { useMutation, useQueryClient } from '@tanstack/react-query';
import { catalogApi } from '@/api/catalog';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { useDisplayName } from '@/components/ui/useDisplayName';
import { useToast } from '@/components/ui/toastContext';
import { LEVEL_LABEL, blockers } from './salesTree';
import type { ReasonAction } from './salesTree';

const SALES = ['catalog', 'sales'] as const;

/**
 * Confirmation with a mandatory reason of a deactivation, reactivation or removal from a team; a
 * unit that still has active sub-units or officers is refused with a notice instead.
 */
export function ReasonDialog({
  action,
  onClose,
}: Readonly<{ action: ReasonAction; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const nameOf = useDisplayName();
  const run = useMutation({
    mutationFn: async (reason: string): Promise<void> => {
      if (action.kind === 'remove') {
        await catalogApi.removeSalesOfficer(action.officer.id, reason);
      } else if (action.kind === 'deactivate') {
        await catalogApi.deactivateSalesUnit(action.node.unit.id, reason);
      } else {
        await catalogApi.reactivateSalesUnit(action.node.unit.id, reason);
      }
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: SALES });
      if (action.kind === 'remove') {
        toast.success(`${nameOf(action.officer.username)} removed from ${action.officer.teamCode}`);
      } else if (action.kind === 'deactivate') {
        toast.success(`${action.node.unit.code} deactivated`);
      } else {
        toast.success(`${action.node.unit.code} reactivated – pending authorization`);
      }
      onClose();
    },
  });
  if (action.kind === 'remove') {
    const o = action.officer;
    return (
      <ConfirmDialog
        title="Remove from Team"
        record={`${nameOf(o.username)} – team ${o.teamCode}`}
        effect="The officer leaves the team. New accounts of this officer no longer take the team's units and cost center; existing accounts keep them."
        confirmLabel="Remove from Team"
        reason="required"
        destructive
        busy={run.isPending}
        error={run.error}
        onConfirm={(reason) => run.mutate(reason)}
        onClose={onClose}
      />
    );
  }
  const u = action.node.unit;
  const unitName = `${LEVEL_LABEL[u.level]} ${u.code} – ${u.name}`;
  if (action.kind === 'reactivate') {
    return (
      <ConfirmDialog
        title={`Reactivate ${LEVEL_LABEL[u.level]}`}
        record={unitName}
        effect="The unit is reactivated and waits for authorization before new accounts can use it."
        confirmLabel="Reactivate"
        reason="required"
        busy={run.isPending}
        error={run.error}
        onConfirm={(reason) => run.mutate(reason)}
        onClose={onClose}
      />
    );
  }
  const open = blockers(action.node);
  if (open.length > 0) {
    return (
      <Modal
        title={`Deactivate ${LEVEL_LABEL[u.level]}`}
        open
        onClose={onClose}
        footer={
          <Button variant="secondary" onClick={onClose}>
            Go Back
          </Button>
        }
      >
        <Notice
          tone="warning"
          title={`Cannot deactivate ${u.code}`}
          items={open.map((o) => `${o} still in the unit`)}
        >
          Deactivate its sub-units and remove or reassign its account officers first.
        </Notice>
      </Modal>
    );
  }
  return (
    <ConfirmDialog
      title={`Deactivate ${LEVEL_LABEL[u.level]}`}
      record={unitName}
      effect="The unit is no longer offered for new accounts and assignments. Existing accounts and history keep it."
      confirmLabel="Deactivate"
      reason="required"
      destructive
      busy={run.isPending}
      error={run.error}
      onConfirm={(reason) => run.mutate(reason)}
      onClose={onClose}
    />
  );
}
