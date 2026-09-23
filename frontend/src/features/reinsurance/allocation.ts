import type { AllocationPreviewRow, ClaimMovement, RiLayer } from '@/api/reinsurance';

/** Totals of an allocation preview (policy-currency amounts added as shown). */
export function previewTotals(rows: AllocationPreviewRow[]): {
  premium: number;
  treaty: number;
  fac: number;
  retention: number;
} {
  return rows.reduce(
    (acc, r) => ({
      premium: acc.premium + r.ourPremium,
      treaty: acc.treaty + r.quotaShare + r.surplus,
      fac: acc.fac + r.fac,
      retention: acc.retention + r.retention,
    }),
    { premium: 0, treaty: 0, fac: 0, retention: 0 },
  );
}

/** Reinsurers' share of a claim movement per layer. */
export function sharesByLayer(m: Pick<ClaimMovement, 'shares'>): Partial<Record<RiLayer, number>> {
  const out: Partial<Record<RiLayer, number>> = {};
  m.shares.forEach((s) => {
    out[s.layer] = (out[s.layer] ?? 0) + s.baseAmount;
  });
  return out;
}
