import type { Measure, PackEntry, Schedule, ScheduleFamily, ScheduleValues } from './api';

/** Titles of the Appendix A families of the schedules (FRBS 3.2.0). */
export const FAMILY_LABELS: Record<ScheduleFamily, string> = {
  GARD: 'II. GARD - Bank Format',
  SUBSIDIARIES: 'III. Subsidiaries Accounting',
  SCHEDULE: 'IV. Schedules',
  AGING: 'IV. Schedules with Ageing',
  OTHER: 'Other Schedules',
};

export const MEASURE_LABELS: Record<Measure, string> = {
  OPENING: 'Opening balance',
  DEBITS: 'Debits',
  CREDITS: 'Credits',
  MOVEMENT: 'Movement of the period',
  CLOSING: 'Balance / year to date',
  COMPARATIVE: 'Comparative',
  VARIANCE: 'Variance',
  VARIANCE_PCT: 'Variance %',
};

const FAMILY_ORDER: ScheduleFamily[] = ['GARD', 'SUBSIDIARIES', 'SCHEDULE', 'AGING', 'OTHER'];

/** Schedules grouped by family in Appendix A order, each group in code order. */
export function groupByFamily(
  schedules: readonly Schedule[],
): { family: ScheduleFamily; label: string; schedules: Schedule[] }[] {
  return FAMILY_ORDER.map((family) => ({
    family,
    label: FAMILY_LABELS[family],
    schedules: schedules
      .filter((s) => s.values.family === family)
      .sort((a, b) => a.code.localeCompare(b.code)),
  })).filter((g) => g.schedules.length > 0);
}

/** Parameters of report GL-SCHEDULE for a schedule. */
export function scheduleParams(
  companyId: number,
  code: string,
  asOf: string,
  from: string,
): Record<string, string> {
  const params: Record<string, string> = { companyId: String(companyId), schedule: code, asOf };
  if (from !== '') {
    params.fromDate = from;
  }
  return params;
}

/** The commentary month of an as-of date ({@code yyyy-MM}). */
export function periodOf(asOf: string): string {
  return asOf.slice(0, 7);
}

/** A new schedule: a balance per account, layout to confirm. */
export function emptySchedule(): ScheduleValues {
  return {
    name: '',
    family: 'SCHEDULE',
    selectorKind: 'ACCOUNT_PREFIX',
    accountSelector: '',
    grouping: 'ACCOUNT',
    side: 'DEBIT',
    basis: 'BALANCE',
    comparative: 'NONE',
    commentary: false,
    boardDocument: false,
    layoutStatus: 'TO_CONFIRM',
    active: true,
    columns: [{ measure: 'CLOSING', label: 'Balance' }],
  };
}

const COMPARED: Measure[] = ['COMPARATIVE', 'VARIANCE', 'VARIANCE_PCT'];

export type ScheduleField =
  'code' | 'name' | 'accountSelector' | 'currency' | 'ageingSlots' | 'columns';

function ageingError(v: ScheduleValues): string | undefined {
  if (!v.ageingSlots) {
    return undefined;
  }
  if (v.basis !== 'BALANCE') {
    return 'Only a balance schedule can be aged';
  }
  return /^\d+(\s*,\s*\d+){0,7}$/.test(v.ageingSlots.trim())
    ? undefined
    : 'Up to 8 day limits separated by commas, e.g. 30,90,180';
}

function columnsError(v: ScheduleValues): string | undefined {
  const measures = v.columns.map((c) => c.measure);
  if (measures.length === 0) {
    return 'Choose at least one figure';
  }
  if (new Set(measures).size !== measures.length) {
    return 'Each figure is chosen once';
  }
  return v.comparative === 'NONE' && measures.some((m) => COMPARED.includes(m))
    ? 'Choose a comparative period for the comparative figures'
    : undefined;
}

/** Field errors of a schedule definition (the server applies the same rules). */
export function scheduleErrors(
  code: string,
  v: ScheduleValues,
): Partial<Record<ScheduleField, string>> {
  const found: Record<ScheduleField, string | undefined> = {
    code: /^[A-Z0-9][A-Z0-9-]{1,39}$/.test(code.trim().toUpperCase())
      ? undefined
      : 'Use 2 to 40 letters, digits or dashes',
    name: v.name.trim() === '' ? 'Enter the title of the schedule' : undefined,
    accountSelector: v.accountSelector.split(',').every((s) => s.trim() === '')
      ? 'List at least one account prefix or report group'
      : undefined,
    currency:
      v.currency && !/^[A-Za-z]{3}$/.test(v.currency.trim())
        ? 'Use a 3-letter currency code'
        : undefined,
    ageingSlots: ageingError(v),
    columns: columnsError(v),
  };
  const errors: Partial<Record<ScheduleField, string>> = {};
  (Object.keys(found) as ScheduleField[]).forEach((k) => {
    const message = found[k];
    if (message !== undefined) {
      errors[k] = message;
    }
  });
  return errors;
}

export type RunnerErrors = Partial<Record<'code' | 'asOf' | 'from', string>>;

/** Field errors of a schedule run: a schedule, an as-of date, a period start not after it. */
export function runnerErrors(code: string, asOf: string, from: string): RunnerErrors {
  const errors: RunnerErrors = {};
  if (code === '') {
    errors.code = 'Select a schedule';
  }
  if (asOf === '') {
    errors.asOf = 'Enter the as-of date';
  } else if (from !== '' && from > asOf) {
    errors.from = 'Starts after the as-of date';
  }
  return errors;
}

/** The report pack grouped in Appendix A order. */
export function groupPack(
  entries: readonly PackEntry[],
): { code: string; name: string; entries: PackEntry[] }[] {
  const groups: { code: string; name: string; entries: PackEntry[] }[] = [];
  for (const e of entries) {
    let group = groups.find((g) => g.code === e.groupCode);
    if (group === undefined) {
      group = { code: e.groupCode, name: e.groupName, entries: [] };
      groups.push(group);
    }
    group.entries.push(e);
  }
  return groups;
}

/** Where a pack entry opens: the schedule runner for a schedule, else the report runner. */
export function entryLink(e: PackEntry): string {
  return e.scheduleCode
    ? `/frbs/schedules?code=${encodeURIComponent(e.scheduleCode)}`
    : `/reports/${encodeURIComponent(e.reportCode)}`;
}

/**
 * Parameters for a quick export of a pack entry with the usual defaults: the company, the month
 * to date (from / to, fromDate / toDate), the as-of date, the year, quarter and month of today and
 * the schedule of a GL-SCHEDULE entry. A report ignores the parameters it does not declare.
 */
export function quickParams(
  e: PackEntry,
  companyId: number,
  today: string,
): Record<string, string> {
  const monthStart = `${today.slice(0, 8)}01`;
  const month = Number(today.slice(5, 7));
  const params: Record<string, string> = {
    companyId: String(companyId),
    from: monthStart,
    to: today,
    fromDate: monthStart,
    toDate: today,
    asOf: today,
    year: today.slice(0, 4),
    month: String(month),
    quarter: String(Math.ceil(month / 3)),
  };
  if (e.scheduleCode) {
    params.schedule = e.scheduleCode;
  }
  return params;
}
