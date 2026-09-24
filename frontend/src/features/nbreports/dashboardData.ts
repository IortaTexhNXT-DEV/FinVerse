import type { NbDashboard, StatusCount } from '@/api/nbReports';

/** Drill-down route of an account stage: the Accounts list filtered on the status. */
export function accountsPath(status: string): string {
  return `/accounts?status=${encodeURIComponent(status)}`;
}

/** Quotation list tab of a quotation status (see features/quotations/quotationList.ts). */
const QUOTATION_TAB: Record<string, string> = {
  DRAFT: 'drafts',
  FOR_REVIEW: 'review',
  APPROVED: 'review',
  SENT_TO_CLIENT: 'sent',
  ACCEPTED: 'accepted',
};

/** Drill-down route of a request / quotation / proposal status count. */
export function requestPath(c: StatusCount): string {
  if (c.group === 'REQUEST') {
    return '/quotations/requests';
  }
  if (c.group === 'PROPOSAL') {
    return '/proposals';
  }
  return `/quotations?tab=${QUOTATION_TAB[c.code] ?? 'drafts'}`;
}

/** Drill-down route of a funnel step. */
export function funnelPath(step: string): string {
  const paths: Record<string, string> = {
    quoted: '/quotations',
    sent: '/quotations?tab=sent',
    accepted: '/quotations?tab=accepted',
    accounts: '/accounts',
    placed: accountsPath('PLACED'),
    issued: accountsPath('POLICY_ISSUED'),
    booked: '/booking?tab=BOOKED',
  };
  return paths[step] ?? '/accounts';
}

/** Sum of the counts. */
export function total(counts: readonly StatusCount[]): number {
  return counts.reduce((sum, c) => sum + c.count, 0);
}

/** Funnel steps that count accounts; the others count quotations and PRFs. */
const ACCOUNT_STEPS = new Set(['accounts', 'placed', 'issued', 'booked']);

/**
 * Share of a funnel step in whole percent: quotation steps against the quotations and PRFs
 * raised, account steps against the accounts created (0 when the base is empty).
 */
export function funnelShare(funnel: readonly StatusCount[], step: string): number {
  const base = funnel.find((f) => f.code === (ACCOUNT_STEPS.has(step) ? 'accounts' : 'quoted'));
  const count = funnel.find((f) => f.code === step)?.count ?? 0;
  const total = base?.count ?? 0;
  return total === 0 ? 0 : Math.round((count * 100) / total);
}

/** Width of a bar in percent of the largest value (at least 2 % so a non-zero bar shows). */
export function barWidth(value: number, max: number): number {
  if (max <= 0 || value <= 0) {
    return 0;
  }
  return Math.max(2, Math.round((value * 100) / max));
}

/** Short summary of the overdue items per workflow ("3 Accounts · 1 Quotations"). */
export function overdueHint(overdue: readonly StatusCount[]): string {
  if (overdue.length === 0) {
    return 'Every item is within its service level';
  }
  return overdue.map((o) => `${String(o.count)} ${o.label}`).join(' · ');
}

/** Requests hint: quotations and PRFs in progress next to the new requests. */
export function requestsHint(d: NbDashboard): string {
  const quotations = total(d.requests.filter((r) => r.group === 'QUOTATION'));
  const proposals = total(d.requests.filter((r) => r.group === 'PROPOSAL'));
  return `${String(quotations)} quotations · ${String(proposals)} PRFs in progress`;
}

/** Name of what a request count counts. */
export function groupName(group: string): string {
  const names: Record<string, string> = { REQUEST: 'Request', PROPOSAL: 'PRF' };
  return names[group] ?? 'Quotation';
}
