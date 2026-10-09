import type { NbDashboard, StatusCount } from '@/api/nbReports';
import { WORKFLOW_NAMES, workflowNoun } from '@/api/workflow';
import type { KpiBreakdownItem } from '@/components/ui/kpiBreakdown';
import { formatDate, humanize } from '@/utils/format';
import { queuePath } from '@/features/workspace/queueParams';
import { nounFor } from '@/utils/wording';

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

/** A workflow or status label for business users: a code such as PM_PACKAGE_REQUEST becomes its name. */
function readableLabel(label: string): string {
  if (!/^[A-Z0-9_]+$/.test(label)) {
    return label;
  }
  return WORKFLOW_NAMES[label] ?? humanize(label);
}

/** My Work filtered on the overdue items of a workflow, or of every workflow (all the user's queues). */
export function overdueQueuePath(workflow?: string): string {
  return queuePath({ scope: 'ALL', overdue: true, workflow });
}

/**
 * The overdue items per workflow as tile breakdown lines: the business name in the right number
 * ("1 Claim", "4 Disbursement vouchers"), never a code, each opening the overdue items of its
 * queue.
 */
export function overdueBreakdown(overdue: readonly StatusCount[]): KpiBreakdownItem[] {
  return overdue.map((o) => ({
    key: o.code,
    label: /^[A-Z0-9_]+$/.test(o.code) ? workflowNoun(o.code, o.count) : readableLabel(o.label),
    count: o.count,
    to: overdueQueuePath(o.code),
  }));
}

/** The qualifier of the overdue tile. */
export function overdueQualifier(overdue: readonly StatusCount[]): string {
  const n = total(overdue);
  return n === 0 ? 'Every item is within its service level' : 'Past their service level';
}

/** Quotations and proposal requests in progress, as breakdown lines of the New Requests tile. */
export function requestsBreakdown(d: NbDashboard): KpiBreakdownItem[] {
  const quotations = total(d.requests.filter((r) => r.group === 'QUOTATION'));
  const proposals = total(d.requests.filter((r) => r.group === 'PROPOSAL'));
  return [
    {
      key: 'quotations',
      label: nounFor(quotations, 'Quotation in progress', 'Quotations in progress'),
      count: quotations,
      to: '/quotations',
    },
    {
      key: 'proposals',
      label: nounFor(proposals, 'Proposal request in progress', 'Proposal requests in progress'),
      count: proposals,
      to: '/proposals',
    },
  ];
}

/** First day of the month of a business date, as dd-MMM-yyyy ("Since 01-Oct-2026"). */
export function monthStartOf(asOf: string): string {
  return formatDate(`${asOf.slice(0, 7)}-01`);
}

/** Heading of a group of request counts (Requests by Status). */
export function groupName(group: string): string {
  const names: Record<string, string> = {
    REQUEST: 'Quotation Requests',
    PROPOSAL: 'Proposal Requests',
  };
  return names[group] ?? 'Quotations';
}
