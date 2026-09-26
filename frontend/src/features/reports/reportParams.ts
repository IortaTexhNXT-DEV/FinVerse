import type { ParameterSpec } from '@/api/reports';
import { dateRangeError } from '@/utils/dateRange';
import { today } from '@/utils/format';

/** Value a parameter starts with: its declared default, date keywords resolved. */
export function initialValue(spec: ParameterSpec): string {
  const now = today();
  switch (spec.defaultValue) {
    case 'TODAY':
      return now;
    case 'MONTH_START':
      return `${now.slice(0, 7)}-01`;
    case 'YEAR_START':
      return `${now.slice(0, 4)}-01-01`;
    default:
      return spec.defaultValue ?? '';
  }
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
