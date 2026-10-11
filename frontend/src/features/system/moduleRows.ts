import type { ModuleProfile, ModuleSwitch } from '@/api/modules';
import type { RowAction } from '@/components/ui/RowActionMenu';

/** The action chosen from a row menu, confirmed in a dialog. */
export type Pending =
  | { kind: 'switch'; module: ModuleSwitch }
  | { kind: 'approve'; module: ModuleSwitch }
  | { kind: 'reject'; module: ModuleSwitch }
  | { kind: 'withdraw'; module: ModuleSwitch }
  | { kind: 'profile'; profile: ModuleProfile };

/** Whether a change of the module waits for approval. */
export function isPending(m: ModuleSwitch): boolean {
  return m.pendingEnabled !== undefined && m.pendingEnabled !== null;
}

/** Status of a module switch: On, Off, or the change waiting for approval. */
export function moduleStatus(m: ModuleSwitch): { status: string; label: string } {
  if (isPending(m)) {
    return {
      status: 'PENDING_APPROVAL',
      label: m.pendingEnabled === true ? 'Switch On Pending' : 'Switch Off Pending',
    };
  }
  return m.enabled ? { status: 'ACTIVE', label: 'On' } : { status: 'INACTIVE', label: 'Off' };
}

/** The row actions of a module for the signed-in user. */
export function moduleActions(
  m: ModuleSwitch,
  who: { username: string; mayManage: boolean; mayApprove: boolean },
  choose: (p: Pending) => void,
): RowAction[] {
  const mine = (m.pendingBy ?? '').toLowerCase() === who.username.toLowerCase();
  if (isPending(m)) {
    const actions: RowAction[] = [];
    if (who.mayApprove && !mine) {
      actions.push({ label: 'Approve', onSelect: () => choose({ kind: 'approve', module: m }) });
      actions.push({
        label: 'Reject',
        danger: true,
        onSelect: () => choose({ kind: 'reject', module: m }),
      });
    }
    if (who.mayManage && mine) {
      actions.push({
        label: 'Withdraw Request',
        danger: true,
        onSelect: () => choose({ kind: 'withdraw', module: m }),
      });
    }
    return actions;
  }
  if (!who.mayManage) {
    return [];
  }
  return [
    {
      label: m.enabled ? 'Switch Off' : 'Switch On',
      danger: m.enabled,
      onSelect: () => choose({ kind: 'switch', module: m }),
    },
  ];
}
