import type { HoldCoverStatus, HoldCoverView } from '@/api/renewalHoldCover';

const STATUS_LABELS: Record<HoldCoverStatus, string> = {
  REQUESTED: 'Requested',
  CONFIRMED: 'Confirmed',
  DECLINED: 'Declined by the insurer',
  EXPIRED: 'Expired',
  REASSIGNED: 'Closed: insurer re-assigned',
  CANCELLED: 'Cancelled',
};

/** The name of a hold cover status. */
export function holdCoverStatusLabel(status: HoldCoverStatus): string {
  return STATUS_LABELS[status];
}

/** Whether a hold cover is requested or confirmed (a new request is then refused). */
export function holdCoverOpen(current: HoldCoverView | null): boolean {
  return current?.status === 'REQUESTED' || current?.status === 'CONFIRMED';
}

/** The actions on the hold cover of a renewal for the user's permissions. */
export function holdCoverActions(
  current: HoldCoverView | null,
  hasAccount: boolean,
  can: (permission: string) => boolean,
): { request: boolean; confirm: boolean; cancel: boolean } {
  const requester = can('RNW_DISPOSE') || can('RNW_PROCESS') || can('RNW_INSURER');
  const recorder = can('RNW_PROCESS') || can('RNW_INSURER');
  const open = holdCoverOpen(current);
  return {
    request: hasAccount && requester && !open,
    confirm: recorder && current?.status === 'REQUESTED',
    cancel: requester && open,
  };
}
