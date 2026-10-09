import type { ParameterSpec } from '@/api/reports';
import { dateRangeError } from '@/utils/dateRange';
import { today } from '@/utils/format';

function isoDay(year: number, month: number, day: number): string {
  // Date.UTC rolls months and days over, so month 0 is December of the year before.
  return new Date(Date.UTC(year, month - 1, day)).toISOString().slice(0, 10);
}

/** The dates a keyword of a default or a variant stands for, from today. */
function keywordDates(now: string): Record<string, string> {
  const year = Number(now.slice(0, 4));
  const month = Number(now.slice(5, 7));
  const day = Number(now.slice(8, 10));
  return {
    TODAY: now,
    MONTH_START: isoDay(year, month, 1),
    MONTH_END: isoDay(year, month + 1, 0),
    YEAR_START: isoDay(year, 1, 1),
    YEAR_END: isoDay(year, 12, 31),
    PREV_YEAR_START: isoDay(year - 1, 1, 1),
    PREV_YEAR_END: isoDay(year - 1, 12, 31),
    TWELVE_MONTHS_AGO: isoDay(year - 1, month, day),
    IN_TWELVE_MONTHS: isoDay(year + 1, month, day),
    PREV_MONTH_START: isoDay(year, month - 1, 1),
    PREV_MONTH_END: isoDay(year, month, 0),
    NEXT_MONTH_START: isoDay(year, month + 1, 1),
    NEXT_MONTH_END: isoDay(year, month + 2, 0),
    IN_THREE_MONTHS: isoDay(year, month + 3, day),
    CURRENT_YEAR: String(year),
    PREV_YEAR: String(year - 1),
  };
}

/**
 * A parameter value with its keyword resolved: dates relative to today ("MONTH_START",
 * "PREV_MONTH_END"...), the year ("CURRENT_YEAR") and the user's branch ("MY_BRANCH").
 */
export function resolveValue(value: string, branchId?: number, now = today()): string {
  if (value === 'MY_BRANCH') {
    return branchId === undefined ? '' : String(branchId);
  }
  return keywordDates(now)[value] ?? value;
}

/** Value a parameter starts with: its declared default, keywords resolved. */
export function initialValue(spec: ParameterSpec): string {
  return resolveValue(spec.defaultValue ?? '');
}

/**
 * Upper bound of a lower-bound parameter, by the naming convention the server uses
 * (ParameterRanges): fromDate / toDate and xxxFrom / xxxTo.
 */
export function rangePartner(name: string): string | undefined {
  if (name === 'fromDate') {
    return 'toDate';
  }
  return name.length > 4 && name.endsWith('From') ? `${name.slice(0, -4)}To` : undefined;
}

function reversedRange(from: ParameterSpec, to: ParameterSpec, low: string, high: string) {
  const labels = { from: from.label, to: to.label };
  if (from.type === 'DATE') {
    return dateRangeError(low, high, labels);
  }
  const reversed = low !== '' && high !== '' && Number(high) < Number(low);
  return reversed ? `${to.label} must not be before ${from.label}` : undefined;
}

/**
 * Field errors of a report's parameter form, by parameter name: required parameters left blank
 * and date or number ranges whose To is before their From. The company comes from the workspace
 * and is not checked here.
 */
export function parameterErrors(
  specs: ParameterSpec[],
  values: Record<string, string>,
): Record<string, string> {
  const errors: Record<string, string> = {};
  const valueOf = (spec: ParameterSpec) => (values[spec.name] ?? initialValue(spec)).trim();
  specs.forEach((spec) => {
    if (
      spec.required &&
      spec.type !== 'COMPANY' &&
      spec.type !== 'BOOLEAN' &&
      valueOf(spec) === ''
    ) {
      errors[spec.name] = `${spec.label} is required`;
    }
  });
  specs.forEach((from) => {
    const to = specs.find((s) => s.name === rangePartner(from.name));
    const ordered = from.type === 'DATE' || from.type === 'NUMBER';
    if (!ordered || to?.type !== from.type) {
      return;
    }
    const problem = reversedRange(from, to, valueOf(from), valueOf(to));
    if (problem !== undefined) {
      errors[to.name] ??= problem;
    }
  });
  return errors;
}

/**
 * The parameter values a report runs with: the entered values or the defaults, the company of the
 * workspace, and the user's branch where a branch parameter is left blank.
 */
export function runParameters(
  specs: readonly ParameterSpec[],
  values: Record<string, string>,
  companyId: number | undefined,
  branchId: number | undefined,
): Record<string, string> {
  const out: Record<string, string> = {};
  specs.forEach((p) => {
    out[p.name] = values[p.name] ?? initialValue(p);
  });
  if (companyId !== undefined) {
    out.companyId = String(companyId);
  }
  if (branchId !== undefined && out.branchId === '') {
    out.branchId = String(branchId);
  }
  return out;
}
