import { formatAmount } from '@/utils/format';
import type { ClaimExperience } from '../home/api';

/** Link to the claim record of Claims Handling. */
export function claimLink(claimId: number): string {
  return `/claims-handling/${claimId}`;
}

/** One-line summary of a cover's claims: counts and the paid / outstanding amounts. */
export function experienceSummary(e: ClaimExperience): string {
  if (e.claimCount === 0) {
    return 'No claim recorded on this account';
  }
  const claims = e.claimCount === 1 ? '1 claim' : `${e.claimCount} claims`;
  return `${claims}, ${e.openCount} open · paid ${formatAmount(e.paid)} · outstanding ${formatAmount(e.outstanding)}`;
}
