import type { ReserveLine, ReserveParameterInput, RunStatus, SummaryRow } from '@/api/reserves';

/** Reserves in display order (UCR is the reinsurance side of DAC). */
export const RESERVE_ORDER = ['UPR', 'DAC', 'UCR', 'OSLR', 'IBNR', 'ULAE', 'MFAD', 'PDR'] as const;

const LABELS: Record<string, string> = {
  UPR: 'Unearned premium (UPR)',
  DAC: 'Deferred acquisition cost (DAC)',
  UCR: 'Unearned RI commission (UCR)',
  OSLR: 'Outstanding losses (OSLR)',
  IBNR: 'IBNR',
  ULAE: 'ULAE provision',
  MFAD: 'Margin for adverse deviation',
  PDR: 'Premium deficiency (LAT)',
};

/** Display label of a reserve code. */
export function reserveLabel(code: string): string {
  return LABELS[code] ?? code;
}

function order(code: string): number {
  const i = (RESERVE_ORDER as readonly string[]).indexOf(code);
  return i < 0 ? RESERVE_ORDER.length : i;
}

/** Gross, reinsurers' share and net of one reserve (and its previous valuation). */
export interface ReserveTotals {
  reserve: string;
  gross: number;
  ri: number;
  net: number;
  previousNet: number;
}

/** Totals per reserve of the summary rows, in display order. */
export function totalsByReserve(rows: readonly SummaryRow[]): ReserveTotals[] {
  const map = new Map<string, ReserveTotals>();
  rows.forEach((r) => {
    const t = map.get(r.reserve) ?? { reserve: r.reserve, gross: 0, ri: 0, net: 0, previousNet: 0 };
    t.gross += r.gross;
    t.ri += r.ri;
    t.net += r.net;
    t.previousNet += r.previousNet;
    map.set(r.reserve, t);
  });
  return [...map.values()].sort((a, b) => order(a.reserve) - order(b.reserve));
}

/** Gross and net of one reserve per line of business (chart data). */
export function reserveByLine(
  rows: readonly SummaryRow[],
  reserve: string,
): { line: string; gross: number; net: number; previousNet: number }[] {
  return rows
    .filter((r) => r.reserve === reserve)
    .map((r) => ({ line: r.businessLine, gross: r.gross, net: r.net, previousNet: r.previousNet }))
    .sort((a, b) => b.gross - a.gross);
}

/** A run line summarised per reserve type and line of business. */
export interface LineSummary {
  key: string;
  type: string;
  businessLine: string;
  gross: number;
  ri: number;
  net: number;
}

/** Sums run lines per reserve type and line of business (branches and products merged). */
export function summarizeLines(lines: readonly ReserveLine[]): LineSummary[] {
  const map = new Map<string, LineSummary>();
  lines.forEach((l) => {
    const key = `${l.type}|${l.businessLine}`;
    const s = map.get(key) ?? {
      key,
      type: l.type,
      businessLine: l.businessLine,
      gross: 0,
      ri: 0,
      net: 0,
    };
    s.gross += l.gross;
    s.ri += l.ri;
    s.net += l.net;
    map.set(key, s);
  });
  return [...map.values()].sort(
    (a, b) => order(a.type) - order(b.type) || a.businessLine.localeCompare(b.businessLine),
  );
}

/** Distinct lines of business of run lines, sorted. */
export function linesOfBusiness(lines: readonly ReserveLine[]): string[] {
  return [...new Set(lines.map((l) => l.businessLine))].sort((a, b) => a.localeCompare(b));
}

/** Actions offered for a run, by status and the user's permissions. */
export interface RunActions {
  recalculate: boolean;
  submit: boolean;
  approve: boolean;
  reject: boolean;
  post: boolean;
  cancel: boolean;
}

/** Which life-cycle actions apply (maker: RESERVE_PREPARE, checker: PERIOD_END_RUN). */
export function runActions(status: RunStatus, can: (permission: string) => boolean): RunActions {
  const maker = can('RESERVE_PREPARE');
  const checker = can('PERIOD_END_RUN');
  return {
    recalculate: maker && status === 'PREVIEW',
    submit: maker && status === 'PREVIEW',
    approve: checker && status === 'PENDING_APPROVAL',
    reject: checker && status === 'PENDING_APPROVAL',
    post: checker && status === 'APPROVED',
    cancel: checker && status !== 'CANCELLED',
  };
}

/** Last day of the month of an ISO date (the valuation date of that month). */
export function monthEnd(iso: string): string {
  const year = Number(iso.slice(0, 4));
  const month = Number(iso.slice(5, 7));
  const last = new Date(Date.UTC(year, month, 0)).getUTCDate();
  return `${year}-${String(month).padStart(2, '0')}-${String(last).padStart(2, '0')}`;
}

/** Parameter form state: new record (no id) or a pending record being edited. */
export type ParameterForm = Partial<ReserveParameterInput> & { id?: number };

/** Defaults of a new parameter set. */
export const NEW_PARAMETERS: ParameterForm = {
  ibnrMethod: 'RATE',
  ibnrRate: 5,
  triangleBasis: 'INCURRED',
  developmentPeriod: 'YEAR',
  accidentPeriods: 5,
  mfadPct: 5,
  ulaePct: 3,
  expectedLossRatio: 60,
  treatyCommissionPct: 25,
  facCommissionPct: 20,
};

/** Takaful products for display: the list, or "every product" when blank. */
export function takafulProducts(codes: string | undefined): string {
  return codes !== undefined && codes.trim() !== '' ? codes : 'every product';
}

/** Development factor with six decimals, blank when absent. */
export function formatFactor(value: number | undefined): string {
  return value === undefined ? '' : value.toFixed(6);
}

/** Percentage change from a previous to a current amount (undefined when there is no base). */
export function changePct(current: number, previous: number): number | undefined {
  if (previous === 0) {
    return undefined;
  }
  return ((current - previous) / Math.abs(previous)) * 100;
}
