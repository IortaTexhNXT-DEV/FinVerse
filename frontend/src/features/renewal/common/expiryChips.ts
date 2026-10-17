import type { FlagChips } from '@/api/renewalTypes';
import { formatDate } from '@/utils/format';

/** A computed chip of a renewal. */
export interface ComputedChip {
  key: string;
  label: string;
  tone: 'flag' | 'danger' | 'info' | 'neutral';
}

/**
 * The computed chips of a renewal: the attention flag of the listing (FR-RN-102), the hold cover
 * confirmed with its end (the effective expiry date) and the closing letter due at the effective
 * expiry or sent (NAL Sent or NRL Sent; FR-RN-082, 083, 086).
 */
export function expiryChips(flags: FlagChips): ComputedChip[] {
  const chips: ComputedChip[] = [];
  if (flags.attention) {
    chips.push({ key: 'attention', label: `Attention: ${flags.attention}`, tone: 'danger' });
  }
  if (flags.holdCoverUntil) {
    chips.push({
      key: 'hc',
      label: `HC confirmed to ${formatDate(flags.holdCoverUntil)}`,
      tone: 'info',
    });
  }
  if (flags.closingLetter) {
    chips.push({
      key: 'closing-sent',
      label: flags.closingLetter === 'NAL' ? 'NAL Sent' : 'NRL Sent',
      tone: 'neutral',
    });
  } else if (flags.closingRoute) {
    chips.push({
      key: 'closing-due',
      label: flags.closingRoute === 'NAL' ? 'NAL due' : 'NRL due',
      tone: 'flag',
    });
  }
  return chips;
}
