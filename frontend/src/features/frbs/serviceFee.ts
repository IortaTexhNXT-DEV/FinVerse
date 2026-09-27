import type { LineStatus, RuleBody, RunStage, ServiceFeeLine } from './api';

/** Work list tabs of the service-fee runs (FRBS 2.10.0), in workflow order. */
export type RunTab = 'ALL' | RunStage;

export const RUN_TABS: readonly { id: RunTab; label: string }[] = [
  { id: 'ALL', label: 'All' },
  { id: 'COMPUTED', label: 'Computed' },
  { id: 'FOR_APPROVAL', label: 'For Approval' },
  { id: 'APPROVED', label: 'Sent for Payment' },
  { id: 'RELEASED', label: 'Released' },
  { id: 'LIQUIDATED', label: 'Liquidated' },
  { id: 'CANCELLED', label: 'Cancelled' },
];

/** The tab of a URL parameter (unknown values show every run). */
export function tabOf(value: string | null): RunTab {
  return RUN_TABS.some((t) => t.id === value) ? (value as RunTab) : 'ALL';
}

/** Field errors of the period of a new run: both dates, in order, not after today. */
export function periodErrors(
  from: string,
  to: string,
  today: string,
): Partial<Record<'from' | 'to', string>> {
  const errors: Partial<Record<'from' | 'to', string>> = {};
  if (from === '') {
    errors.from = 'Enter the first day of the period';
  }
  if (to === '') {
    errors.to = 'Enter the last day of the period';
  } else if (to > today) {
    errors.to = 'The period cannot end after today';
  } else if (from !== '' && to < from) {
    errors.to = 'The period ends before it starts';
  }
  return errors;
}

/** What can be done with a line: tag it released, tag it liquidated, or send it again. */
export type LineAction = 'release' | 'liquidate' | 'resend';

export function lineActions(line: ServiceFeeLine, runStage: RunStage): LineAction[] {
  const actions: LineAction[] = [];
  if (line.status === 'SENT') {
    actions.push('release');
  }
  if (line.status === 'RELEASED') {
    actions.push('liquidate');
  }
  if (line.status === 'RETURNED' && runStage === 'APPROVED') {
    actions.push('resend');
  }
  return actions;
}

/** Totals of the lines per currency (a run may pay in PHP and USD). */
export function totalsByCurrency(
  lines: readonly ServiceFeeLine[],
): { currency: string; base: number; fee: number; count: number }[] {
  const totals = new Map<string, { currency: string; base: number; fee: number; count: number }>();
  for (const l of lines) {
    const t = totals.get(l.amounts.currency) ?? {
      currency: l.amounts.currency,
      base: 0,
      fee: 0,
      count: 0,
    };
    t.base += l.amounts.base;
    t.fee += l.amounts.fee;
    t.count += l.amounts.invoiceCount;
    totals.set(l.amounts.currency, t);
  }
  return [...totals.values()];
}

/** Progress of the tags: lines released and liquidated out of the lines paid. */
export function tagProgress(lines: readonly ServiceFeeLine[]): {
  paid: number;
  released: number;
  liquidated: number;
} {
  const paid = lines.filter((l) => l.amounts.fee > 0 && l.status !== 'CANCELLED');
  const releasedStatuses: LineStatus[] = ['RELEASED', 'LIQUIDATED'];
  return {
    paid: paid.length,
    released: paid.filter((l) => releasedStatuses.includes(l.status)).length,
    liquidated: paid.filter((l) => l.status === 'LIQUIDATED').length,
  };
}

/** Field errors of a service-fee rule. */
export function ruleErrors(
  r: RuleBody,
): Partial<Record<'segment' | 'marketSegments' | 'rate' | 'effectiveTo', string>> {
  const errors: Partial<Record<'segment' | 'marketSegments' | 'rate' | 'effectiveTo', string>> = {};
  if (r.segment === '') {
    errors.segment = 'Select the service-fee segment';
  }
  if (r.marketSegments.length === 0) {
    errors.marketSegments = 'List the market segments covered';
  }
  if (Number.isNaN(r.rate) || r.rate <= 0 || r.rate > 100) {
    errors.rate = 'Enter a rate above 0 and up to 100';
  }
  if (r.effectiveTo && r.effectiveTo < r.effectiveFrom) {
    errors.effectiveTo = 'Ends before it starts';
  }
  return errors;
}
