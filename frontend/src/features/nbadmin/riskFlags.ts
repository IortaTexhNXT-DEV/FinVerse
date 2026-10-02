/** Risk flags of a request in words; any flag needs a second approval (UAM-NFR-40). */
export const RISK_FLAG_LABELS: Record<string, string> = {
  PRIVILEGE_INCREASE: 'Privilege increase',
  OUTSIDE_HOURS: 'Outside working hours',
};

export function riskFlagLabel(flag: string): string {
  return RISK_FLAG_LABELS[flag] ?? flag.toLowerCase().replace(/_/g, ' ');
}
