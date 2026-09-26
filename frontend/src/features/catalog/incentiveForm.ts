import type {
  IncentiveCriteria,
  IncentiveInput,
  IncentiveScope,
  IncentiveValueBasis,
} from '@/api/productCatalog';

/** Editable incentive criterion (PMADD07/08); numbers as text while editing. */
export interface IncentiveForm {
  code: string;
  name: string;
  incentiveType: string;
  valueBasis: IncentiveValueBasis;
  value: string;
  ruleParams: string;
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
    ruleParams: '',
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
    ruleParams: c.ruleParams ?? '',
    description: c.description ?? '',
    products: c.products,
    effectiveFrom: c.effectiveFrom,
    effectiveTo: c.effectiveTo ?? '',
  };
}

function isJson(text: string): boolean {
  try {
    JSON.parse(text);
    return true;
  } catch {
    return false;
  }
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
 */
export function incentiveErrors(
  form: IncentiveForm,
  amending = false,
  activeFrom = '',
): Record<string, string> {
  const errors: Record<string, string> = {};
  identityErrors(form, errors);
  const value = valueError(form);
  if (value) {
    errors.value = value;
  }
  if (form.ruleParams.trim() !== '' && !isJson(form.ruleParams)) {
    errors.ruleParams = 'Enter valid JSON, e.g. {"minimumPremium": 5000}';
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
export function toIncentiveInput(form: IncentiveForm, companyId: number): IncentiveInput {
  return {
    companyId,
    code: form.code,
    name: form.name.trim(),
    incentiveType: form.incentiveType,
    valueBasis: form.valueBasis,
    value: form.valueBasis === 'RULE' ? undefined : Number(form.value),
    ruleParams: form.ruleParams.trim() === '' ? undefined : form.ruleParams.trim(),
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
