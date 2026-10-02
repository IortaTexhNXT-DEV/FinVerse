import type { Soa, SoaLayout } from '@/api/reinsurance';

/** Statement of account helpers (pure, unit tested). */

export const QUARTERS = [1, 2, 3, 4] as const;

/** Quarter (1-4) of an ISO date. */
export function quarterOf(iso: string): number {
  const month = Number(iso.slice(5, 7));
  return Math.floor((month - 1) / 3) + 1;
}

/** The last completed quarter before an ISO date. */
export function previousQuarter(iso: string): { year: number; quarter: number } {
  const year = Number(iso.slice(0, 4));
  const quarter = quarterOf(iso);
  return quarter === 1 ? { year: year - 1, quarter: 4 } : { year, quarter: quarter - 1 };
}

/** Rows of the printed statement: lines, sub-total, balance on the smaller side and total. */
export interface StatementRow {
  label: string;
  income?: number;
  outgo?: number;
  kind: 'line' | 'subtotal' | 'balance' | 'total';
}

export function statementRows(layout: SoaLayout): StatementRow[] {
  const rows: StatementRow[] = layout.lines.map((l) => ({
    label: l.label,
    income: l.income === 0 ? undefined : l.income,
    outgo: l.outgo === 0 ? undefined : l.outgo,
    kind: 'line',
  }));
  rows.push({
    label: 'Sub-total',
    income: layout.incomeSubtotal,
    outgo: layout.outgoSubtotal,
    kind: 'subtotal',
  });
  rows.push(
    layout.balanceOnIncome
      ? { label: layout.balanceLabel, income: layout.balance, kind: 'balance' }
      : { label: layout.balanceLabel, outgo: layout.balance, kind: 'balance' },
  );
  rows.push({ label: 'Total', income: layout.total, outgo: layout.total, kind: 'total' });
  return rows;
}

/** Parameters of the RI-SOA report (PDF export) for a stored statement. */
export function soaReportParams(soa: Soa, companyId: number): Record<string, string> {
  return {
    companyId: String(companyId),
    treatyYear: String(soa.year),
    treatyCode: soa.treatyCode,
    quarter: String(soa.quarter),
    statementDate: soa.statementDate,
    reinsurerCode: soa.reinsurerCode,
  };
}

/** Actions a user may take on a statement (maker-checker mirrors the server). */
export function soaActions(
  soa: Pick<Soa, 'status' | 'preparedBy'>,
  username: string | undefined,
  can: (permission: string) => boolean,
): { approve: boolean; settle: boolean } {
  const checker = can('REINSURANCE_AUTHORIZE');
  return {
    approve: checker && soa.status === 'PENDING_APPROVAL' && soa.preparedBy !== username,
    settle: checker && soa.status === 'APPROVED',
  };
}
