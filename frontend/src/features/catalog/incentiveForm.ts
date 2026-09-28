import type {
  IncentiveCriteria,
  IncentiveInput,
  IncentiveRuleParameter,
  IncentiveScope,
  IncentiveValueBasis,
} from '@/api/productCatalog';

/** One rule parameter row: the parameter and its value as typed. */
export interface ParamRow {
  key: string;
  value: string;
}

/** Editable incentive criterion (PMADD07/08); numbers as text while editing. */
export interface IncentiveForm {
  code: string;
  name: string;
  incentiveType: string;
  valueBasis: IncentiveValueBasis;
  value: string;
  params: ParamRow[];
  description: string;
  products: IncentiveScope[];
  effectiveFrom: string;
  effectiveTo: string;
}

/** An empty criterion. */
export function newIncentiveForm(): IncentiveForm {
  return {
    code: '',
    name: '',
    incentiveType: '',
    valueBasis: 'RATE',
    value: '',
    params: [],
    description: '',
    products: [],
    effectiveFrom: '',
    effectiveTo: '',
  };
}

/** The form of an existing row (to change a pending row or amend an active one). */
export function incentiveFormOf(c: IncentiveCriteria): IncentiveForm {
  return {
    code: c.code,
    name: c.name,
    incentiveType: c.incentiveType,
    valueBasis: c.valueBasis,
    value: c.value === undefined ? '' : String(c.value),
    params: paramRowsOf(c.ruleParams),
    description: c.description ?? '',
    products: c.products,
    effectiveFrom: c.effectiveFrom,
    effectiveTo: c.effectiveTo ?? '',
  };
}

/** The rows of stored rule parameters ({"minimumPremium": 5000}); none when unreadable. */
export function paramRowsOf(stored: string | undefined): ParamRow[] {
  if (!stored || stored.trim() === '') {
    return [];
  }
  try {
    const parsed: unknown = JSON.parse(stored);
    if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return [];
    }
    return Object.entries(parsed as Record<string, unknown>).map(([key, value]) => ({
      key,
      value: typeof value === 'string' || typeof value === 'number' ? String(value) : '',
    }));
  } catch {
    return [];
  }
}

const NUMERIC = new Set(['AMOUNT', 'NUMBER', 'PERCENT']);

function paramError(
  row: ParamRow,
  definition: IncentiveRuleParameter | undefined,
  seen: Set<string>,
): string | undefined {
  if (row.key === '') {
    return 'Select the parameter';
  }
  const label = definition?.label ?? row.key;
  if (seen.has(row.key)) {
    return `${label} is listed twice`;
  }
  seen.add(row.key);
  if (row.value.trim() === '') {
    return 'Enter a value for each parameter';
  }
  if (definition === undefined || !NUMERIC.has(definition.valueType)) {
    return undefined;
  }
  const n = Number(row.value.replaceAll(',', ''));
  if (Number.isNaN(n)) {
    return `${label} must be a number`;
  }
  if (n < 0) {
    return `${label} cannot be negative`;
  }
  return definition.valueType === 'PERCENT' && n > 100
    ? `${label} must be between 0 and 100`
    : undefined;
}

/** Errors of the parameter rows, keyed params.<row index>. */
export function paramErrors(
  rows: readonly ParamRow[],
  definitions: readonly IncentiveRuleParameter[],
): Record<string, string> {
  const errors: Record<string, string> = {};
  const seen = new Set<string>();
  rows.forEach((row, i) => {
    const error = paramError(
      row,
      definitions.find((d) => d.key === row.key),
      seen,
    );
    if (error) {
      errors[`params.${String(i)}`] = error;
    }
  });
  return errors;
}

/** The stored form of the rows: one JSON object, numbers for numeric parameters. */
export function storedParams(
  rows: readonly ParamRow[],
  definitions: readonly IncentiveRuleParameter[],
): string | undefined {
  const filled = rows.filter((r) => r.key !== '');
  if (filled.length === 0) {
    return undefined;
  }
  const entries = filled.map((r) => {
    const definition = definitions.find((d) => d.key === r.key);
    const numeric = definition !== undefined && NUMERIC.has(definition.valueType);
    return [r.key, numeric ? Number(r.value.replaceAll(',', '')) : r.value.trim()] as const;
  });
  return JSON.stringify(Object.fromEntries(entries));
}

function valueError(form: IncentiveForm): string | undefined {
  if (form.valueBasis === 'RULE') {
    return undefined;
  }
  const n = Number(form.value);
  if (form.value.trim() === '' || Number.isNaN(n) || n < 0) {
    return 'Enter the value';
  }
  return form.valueBasis === 'RATE' && n > 100 ? 'A rate is between 0 and 100' : undefined;
}

function identityErrors(form: IncentiveForm, errors: Record<string, string>): void {
  if (!/^[A-Z0-9_-]+$/.test(form.code)) {
    errors.code = 'Use A-Z, 0-9, _ and -';
  }
  if (form.name.trim() === '') {
    errors.name = 'Enter the name';
  }
  if (form.incentiveType === '') {
    errors.incentiveType = 'Select the type';
  }
}

/**
 * Field errors of a criterion (PMADD07: incomplete set-ups refused, logical dates).
 *
 * @param form form
 * @param amending whether an active row is amended (the change must start later)
 * @param activeFrom start of the active row being amended
 * @param definitions rule parameters of the incentive type (kinds of value)
 */
export function incentiveErrors(
  form: IncentiveForm,
  amending = false,
  activeFrom = '',
  definitions: readonly IncentiveRuleParameter[] = [],
): Record<string, string> {
  const errors: Record<string, string> = paramErrors(form.params, definitions);
  identityErrors(form, errors);
  const value = valueError(form);
  if (value) {
    errors.value = value;
  }
  if (form.products.length === 0) {
    errors.products = 'Select at least one product of the matrix';
  }
  if (form.effectiveFrom === '') {
    errors.effectiveFrom = 'Enter the start date';
  } else if (amending && form.effectiveFrom <= activeFrom) {
    errors.effectiveFrom = `The change must start after ${activeFrom}`;
  }
  if (form.effectiveTo !== '' && form.effectiveTo < form.effectiveFrom) {
    errors.effectiveTo = 'The end is before the start';
  }
  return errors;
}

/** The API body of a form. */
export function toIncentiveInput(
  form: IncentiveForm,
  companyId: number,
  definitions: readonly IncentiveRuleParameter[] = [],
): IncentiveInput {
  return {
    companyId,
    code: form.code,
    name: form.name.trim(),
    incentiveType: form.incentiveType,
    valueBasis: form.valueBasis,
    value: form.valueBasis === 'RULE' ? undefined : Number(form.value),
    ruleParams: storedParams(form.params, definitions),
    description: form.description.trim() === '' ? undefined : form.description.trim(),
    products: form.products,
    effectiveFrom: form.effectiveFrom,
    effectiveTo: form.effectiveTo === '' ? undefined : form.effectiveTo,
  };
}

/** Whether a row is the current row of its code (not ended, not replaced). */
export function isCurrent(c: IncentiveCriteria, today: string): boolean {
  return c.recordStatus !== 'INACTIVE' && (!c.effectiveTo || c.effectiveTo >= today);
}
