import type { Claim, CostType, EstimateSide, Movement } from '@/api/claims';

/**
 * Client-side claim arithmetic for previews. The server recomputes everything; these helpers only
 * show the user what will be posted before they submit.
 */

const CENTS = 100;

/** Rounds to cents (half away from zero is close enough for a preview). */
export function round2(value: number): number {
  return Math.round(value * CENTS) / CENTS;
}

/** Net settlement = assessed − deductible − excess (never below zero in the preview). */
export function settlementNet(assessed: number, deductible = 0, excess = 0): number {
  return Math.max(0, round2(assessed - deductible - excess));
}

/** Company share of an amount at 100 %. */
export function ourShare(amount100: number, sharePct: number): number {
  return round2((amount100 * sharePct) / CENTS);
}

/**
 * What approving a settlement of `net` pays and books: our share, the amount paid to the payee
 * (100 % when the company leads a coinsurance, else our share) and the coinsurers' share recovered.
 */
export function settlementSplit(
  net: number,
  sharePct: number,
  leader: boolean,
): { ours: number; payable: number; coinsurers: number } {
  const ours = ourShare(net, sharePct);
  if (leader && sharePct < CENTS) {
    return { ours, payable: net, coinsurers: round2(net - ours) };
  }
  return { ours, payable: ours, coinsurers: 0 };
}

/** LPO net = gross − discount. */
export function lpoNet(gross: number, discount = 0): number {
  return round2(gross - discount);
}

/** Current estimate (100 %) of a side and cost type. */
export function currentEstimate(claim: Claim, side: EstimateSide, cost: CostType): number {
  if (side === 'RECOVERY') {
    return claim.totals.estimateRecovery;
  }
  return cost === 'LOSS' ? claim.totals.estimateLoss : claim.totals.estimateExpense;
}

/** Outstanding (100 %) available for a settlement or recovery of a side and cost type. */
export function outstandingOf(claim: Claim, side: EstimateSide, cost: CostType): number {
  if (side === 'RECOVERY') {
    return claim.totals.recoveryOutstanding;
  }
  return cost === 'LOSS' ? claim.totals.outstandingLoss : claim.totals.outstandingExpense;
}

/** Reports Book label of an estimate / paid line type. */
export function estimateTypeLabel(code: number): string {
  const labels: Record<number, string> = {
    1: '1 Payment',
    2: '2 Recovery',
    3: '3 Reversal of Payment',
    4: '4 Reversal of Recovery',
  };
  return labels[code] ?? String(code);
}

/** Running payment outstanding (company share) after each movement, in date order. */
export function runningOutstanding(movements: Movement[]): number[] {
  let outstanding = 0;
  return movements.map((m) => {
    if (m.side === 'PAYMENT') {
      outstanding = round2(outstanding + (m.kind === 'ESTIMATE' ? m.amount : -m.amount));
    }
    return outstanding;
  });
}
