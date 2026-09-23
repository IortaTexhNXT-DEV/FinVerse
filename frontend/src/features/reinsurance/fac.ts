import type { FacParticipantLine, FacPlacement } from '@/api/reinsurance';

/** Facultative slip rules shared by the screens (maker-checker mirrors the server). */

export interface FacActions {
  edit: boolean;
  approve: boolean;
  close: boolean;
}

export function facActions(
  p: Pick<FacPlacement, 'status' | 'submittedBy'>,
  username: string | undefined,
  can: (permission: string) => boolean,
): FacActions {
  return {
    edit: p.status === 'PROVISIONAL' && can('REINSURANCE_MAINTAIN'),
    approve:
      p.status === 'PENDING_APPROVAL' && can('REINSURANCE_AUTHORIZE') && p.submittedBy !== username,
    close: p.status === 'PLACED' && can('REINSURANCE_MAINTAIN'),
  };
}

/** Share of the facultative requirement placed with the listed reinsurers, %. */
export function placedShare(lines: Pick<FacParticipantLine, 'sharePct'>[]): number {
  return Math.round(lines.reduce((sum, l) => sum + (l.sharePct || 0), 0) * 100) / 100;
}

/** Days a slip has been waiting since the policy was ceded. */
export function ageInDays(riDate: string, asOf: string): number {
  const ms = Date.parse(asOf) - Date.parse(riDate);
  return Math.max(0, Math.floor(ms / 86_400_000));
}
