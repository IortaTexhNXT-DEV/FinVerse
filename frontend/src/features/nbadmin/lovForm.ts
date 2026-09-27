import type { LovValue, LovValueRequest } from '@/api/lov';

export interface LovForm {
  code: string;
  label: string;
  sortOrder: string;
  parentCode: string;
  effectiveFrom: string;
  effectiveTo: string;
}

export type LovFormErrors = Partial<Record<keyof LovForm, string>>;

/** Form of a new value (effective today) or of an existing one. */
export function lovForm(value: LovValue | undefined, today: string, nextOrder: number): LovForm {
  if (value === undefined) {
    return {
      code: '',
      label: '',
      sortOrder: String(nextOrder),
      parentCode: '',
      effectiveFrom: today,
      effectiveTo: '',
    };
  }
  return {
    code: value.code,
    label: value.label,
    sortOrder: String(value.sortOrder),
    parentCode: value.parentCode ?? '',
    effectiveFrom: value.effectiveFrom,
    effectiveTo: value.effectiveTo ?? '',
  };
}

/** Field errors of a list value (BRNB.083). */
export function validateLov(f: LovForm): LovFormErrors {
  const errors: LovFormErrors = {};
  if (!/^[A-Z0-9_]{1,40}$/.test(f.code)) {
    errors.code = 'Use capital letters, digits and _ (up to 40)';
  }
  if (f.label.trim() === '') {
    errors.label = 'Enter the label';
  }
  if (!/^\d{1,6}$/.test(f.sortOrder)) {
    errors.sortOrder = 'Enter a whole number';
  }
  if (f.effectiveFrom === '') {
    errors.effectiveFrom = 'Enter the first valid date';
  }
  if (f.effectiveTo !== '' && f.effectiveTo < f.effectiveFrom) {
    errors.effectiveTo = 'Must not be before the first valid date';
  }
  return errors;
}

/** Request body of the form. */
export function toLovRequest(f: LovForm): LovValueRequest {
  return {
    code: f.code,
    label: f.label.trim(),
    sortOrder: Number(f.sortOrder),
    parentCode: f.parentCode.trim() === '' ? undefined : f.parentCode.trim(),
    effectiveFrom: f.effectiveFrom,
    effectiveTo: f.effectiveTo === '' ? undefined : f.effectiveTo,
  };
}

/** Next sort order after the existing values (steps of 10). */
export function nextSortOrder(values: readonly LovValue[]): number {
  const max = values.reduce((m, v) => Math.max(m, v.sortOrder), 0);
  return (Math.floor(max / 10) + 1) * 10;
}

/** Effectivity state of a value on a date: FUTURE, ACTIVE (in force) or EXPIRED. */
export function effectivity(v: LovValue, today: string): 'FUTURE' | 'ACTIVE' | 'EXPIRED' {
  if (v.effectiveFrom > today) {
    return 'FUTURE';
  }
  return v.effectiveTo !== undefined && v.effectiveTo < today ? 'EXPIRED' : 'ACTIVE';
}
