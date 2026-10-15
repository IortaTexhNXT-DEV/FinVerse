import type { HomeWidget, RoleHome } from '@/api/roleHomes';
import type { RowAction } from '@/components/ui/RowActionMenu';

/** The action chosen from a row menu. */
export type RoleHomeAction =
  | { kind: 'edit'; home: RoleHome }
  | { kind: 'approve'; home: RoleHome }
  | { kind: 'reject'; home: RoleHome }
  | { kind: 'withdraw'; home: RoleHome };

/** Whether a change of the role's widgets waits for approval. */
export function isPendingHome(h: RoleHome): boolean {
  return h.pendingWidgets !== undefined && h.pendingWidgets !== null;
}

/** Status of a role home page: delivered, set, or the change waiting for approval. */
export function roleHomeStatus(h: RoleHome): { status: string; label: string } {
  if (isPendingHome(h)) {
    return { status: 'PENDING_APPROVAL', label: 'Change Pending' };
  }
  return h.delivered
    ? { status: 'INACTIVE', label: 'As Delivered' }
    : { status: 'ACTIVE', label: 'Set' };
}

/** "Cash position, My workload" from widget codes. */
export function widgetNames(codes: readonly string[], catalogue: readonly HomeWidget[]): string {
  return codes.map((c) => catalogue.find((w) => w.code === c)?.name ?? c).join(', ');
}

/** Moves a widget one place up (-1) or down (+1); out-of-range moves leave the list as it is. */
export function moveWidget(codes: readonly string[], index: number, step: -1 | 1): string[] {
  const target = index + step;
  if (index < 0 || index >= codes.length || target < 0 || target >= codes.length) {
    return [...codes];
  }
  const next = [...codes];
  const [moved] = next.splice(index, 1);
  next.splice(target, 0, ...(moved === undefined ? [] : [moved]));
  return next;
}

/** Switches a widget on (added at the end) or off. */
export function toggleWidget(codes: readonly string[], code: string): string[] {
  return codes.includes(code) ? codes.filter((c) => c !== code) : [...codes, code];
}

/** The row actions of a role for the signed-in user. */
export function roleHomeActions(
  h: RoleHome,
  who: { username: string; mayManage: boolean; mayApprove: boolean },
  choose: (a: RoleHomeAction) => void,
): RowAction[] {
  const mine = (h.pendingBy ?? '').toLowerCase() === who.username.toLowerCase();
  if (isPendingHome(h)) {
    const actions: RowAction[] = [];
    if (who.mayApprove && !mine) {
      actions.push({ label: 'Approve', onSelect: () => choose({ kind: 'approve', home: h }) });
      actions.push({
        label: 'Reject',
        danger: true,
        onSelect: () => choose({ kind: 'reject', home: h }),
      });
    }
    if (who.mayManage && mine) {
      actions.push({
        label: 'Withdraw Request',
        danger: true,
        onSelect: () => choose({ kind: 'withdraw', home: h }),
      });
    }
    return actions;
  }
  return who.mayManage
    ? [{ label: 'Change Widgets', onSelect: () => choose({ kind: 'edit', home: h }) }]
    : [];
}
