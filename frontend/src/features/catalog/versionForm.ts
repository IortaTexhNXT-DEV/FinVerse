import type { InsurerRole, VersionContentInput, VersionDetail } from '@/api/productCatalog';

/**
 * Editable state of a DRAFT package version (BRPM.007, PMADD01/02) and its validation. Numbers are
 * kept as text while editing; `toInput` converts them for the API.
 */

export interface CoverageRow {
  coverageCode: string;
  included: boolean;
  optional: boolean;
  limitAmount: string;
  deductibleAmount: string;
  deductibleText: string;
}

export interface InsurerRow {
  insurerCode: string;
  role: InsurerRole;
  sharePercent: string;
  rate: string;
  minimumPremium: string;
  defaultBranchCode: string;
}

export interface TermRow {
  insurerCode: string;
  coverageCode: string;
  included: boolean;
  limitAmount: string;
  deductibleAmount: string;
  deductibleText: string;
  clauseCodes: string[];
  remarks: string;
}

export interface VersionForm {
  defaultRate: string;
  minimumPremium: string;
  defaultCommissionRate: string;
  maxSumInsured: string;
  ratingBasisNote: string;
  effectiveFrom: string;
  packageStartDate: string;
  packageEndDate: string;
  anniversaryDate: string;
  changeSummary: string;
  mancomSignoffRef: string;
  coverages: CoverageRow[];
  insurers: InsurerRow[];
  terms: TermRow[];
}

/** Checklist the validator confirms (PMADD06; final list PQ09). */
export const VALIDATION_CHECKLIST = [
  'Hierarchy complete: cover type and basic coverage',
  'Every panel insurer has terms for each included coverage',
  'Rates and minimum premiums match the signed-off terms',
  'Dates and package term are correct',
  'Test premium reviewed',
] as const;

const text = (value: number | string | undefined | null): string =>
  value === undefined || value === null ? '' : String(value);

const numberOf = (value: string): number | undefined => {
  const trimmed = value.trim();
  return trimmed === '' ? undefined : Number(trimmed);
};

const blankToUndefined = (value: string): string | undefined =>
  value.trim() === '' ? undefined : value.trim();

/** The form of a loaded version. */
export function formOf(detail: VersionDetail): VersionForm {
  const s = detail.scheme;
  return {
    defaultRate: text(s.defaultRate),
    minimumPremium: text(s.minimumPremium),
    defaultCommissionRate: text(s.defaultCommissionRate),
    maxSumInsured: text(s.maxSumInsured),
    ratingBasisNote: text(s.ratingBasisNote),
    effectiveFrom: detail.summary.effectiveFrom,
    packageStartDate: text(detail.packageStartDate),
    packageEndDate: text(detail.summary.packageEndDate),
    anniversaryDate: text(detail.anniversaryDate),
    changeSummary: text(detail.summary.changeSummary),
    mancomSignoffRef: text(detail.mancomSignoffRef),
    coverages: detail.coverages.map((c) => ({
      coverageCode: c.coverageCode,
      included: c.included,
      optional: c.optional,
      limitAmount: text(c.limitAmount),
      deductibleAmount: text(c.deductibleAmount),
      deductibleText: text(c.deductibleText),
    })),
    insurers: detail.insurers.map((i) => ({
      insurerCode: i.insurerCode,
      role: i.role,
      sharePercent: text(i.sharePercent),
      rate: text(i.rate),
      minimumPremium: text(i.minimumPremium),
      defaultBranchCode: text(i.defaultBranchCode),
    })),
    terms: detail.insurerTerms.map((t) => ({
      insurerCode: t.insurerCode,
      coverageCode: t.coverageCode,
      included: t.included,
      limitAmount: text(t.limitAmount),
      deductibleAmount: text(t.deductibleAmount),
      deductibleText: text(t.deductibleText),
      clauseCodes: t.clauseCodes ? t.clauseCodes.split(',') : [],
      remarks: text(t.remarks),
    })),
  };
}

/**
 * Keeps one term per insurer and included coverage: missing terms are added with the coverage's
 * values, terms of removed insurers or coverages are dropped.
 */
export function syncTerms(form: VersionForm): TermRow[] {
  const included = form.coverages.filter((c) => c.included);
  return form.insurers.flatMap((insurer) =>
    included.map(
      (coverage) =>
        form.terms.find(
          (t) => t.insurerCode === insurer.insurerCode && t.coverageCode === coverage.coverageCode,
        ) ?? {
          insurerCode: insurer.insurerCode,
          coverageCode: coverage.coverageCode,
          included: true,
          limitAmount: coverage.limitAmount,
          deductibleAmount: coverage.deductibleAmount,
          deductibleText: coverage.deductibleText,
          clauseCodes: [],
          remarks: '',
        },
    ),
  );
}

function isPercent(value: string): boolean {
  const n = numberOf(value);
  return n === undefined || (n >= 0 && n <= 100);
}

function isAmount(value: string): boolean {
  const n = numberOf(value);
  return n === undefined || n >= 0;
}

function schemeErrors(form: VersionForm, errors: Record<string, string>): void {
  if (!isPercent(form.defaultRate)) {
    errors.defaultRate = 'Enter a rate between 0 and 100';
  }
  if (form.minimumPremium.trim() === '' || !isAmount(form.minimumPremium)) {
    errors.minimumPremium = 'Enter the minimum premium';
  }
  if (form.defaultCommissionRate.trim() === '' || !isPercent(form.defaultCommissionRate)) {
    errors.defaultCommissionRate = 'Enter a commission between 0 and 100';
  }
  if (!isAmount(form.maxSumInsured)) {
    errors.maxSumInsured = 'Enter a positive amount';
  }
}

function dateErrors(form: VersionForm, today: string, errors: Record<string, string>): void {
  if (form.effectiveFrom === '') {
    errors.effectiveFrom = 'Enter the effective date';
  } else if (form.effectiveFrom < today) {
    errors.effectiveFrom = 'The effective date cannot be before today';
  }
  if (form.packageEndDate !== '' && form.packageEndDate <= form.effectiveFrom) {
    errors.packageEndDate = 'The package end date must be after the effective date';
  }
}

/** Field errors of the form (keys are the form fields; empty when valid). */
export function validateVersionForm(form: VersionForm, today: string): Record<string, string> {
  const errors: Record<string, string> = {};
  schemeErrors(form, errors);
  dateErrors(form, today, errors);
  if (!form.coverages.some((c) => c.included)) {
    errors.coverages = 'Include at least one coverage';
  }
  form.insurers.forEach((i, index) => {
    if (!isPercent(i.rate)) {
      errors[`insurers.${index}.rate`] = 'Enter a rate between 0 and 100';
    }
    if (!isPercent(i.sharePercent)) {
      errors[`insurers.${index}.sharePercent`] = 'Enter a share between 0 and 100';
    }
  });
  if (form.defaultRate.trim() === '' && form.insurers.some((i) => i.rate.trim() === '')) {
    errors.defaultRate = 'Enter the package rate or a rate for every insurer';
  }
  return errors;
}

function deductible(amount: string, deductibleText: string) {
  const value = numberOf(amount);
  const wording = blankToUndefined(deductibleText);
  return value === undefined && wording === undefined
    ? undefined
    : { amount: value, text: wording };
}

/** The API body of a form. */
export function toInput(form: VersionForm, companyId: number): VersionContentInput {
  return {
    companyId,
    rateScheme: {
      defaultRate: numberOf(form.defaultRate),
      minimumPremium: numberOf(form.minimumPremium) ?? 0,
      defaultCommissionRate: numberOf(form.defaultCommissionRate) ?? 0,
      maxSumInsured: numberOf(form.maxSumInsured),
      ratingBasisNote: blankToUndefined(form.ratingBasisNote),
    },
    dates: {
      effectiveFrom: form.effectiveFrom,
      packageStartDate: blankToUndefined(form.packageStartDate),
      packageEndDate: blankToUndefined(form.packageEndDate),
      anniversaryDate: blankToUndefined(form.anniversaryDate),
    },
    coverages: form.coverages.map((c, index) => ({
      coverageCode: c.coverageCode,
      included: c.included,
      optional: c.optional,
      limitAmount: numberOf(c.limitAmount),
      deductible: deductible(c.deductibleAmount, c.deductibleText),
      sortOrder: (index + 1) * 10,
    })),
    insurers: form.insurers.map((i) => ({
      insurerCode: i.insurerCode,
      role: i.role,
      sharePercent: numberOf(i.sharePercent),
      rate: numberOf(i.rate),
      minimumPremium: numberOf(i.minimumPremium),
      defaultBranchCode: blankToUndefined(i.defaultBranchCode),
    })),
    insurerTerms: syncTerms(form).map((t) => ({
      insurerCode: t.insurerCode,
      coverageCode: t.coverageCode,
      included: t.included,
      limitAmount: numberOf(t.limitAmount),
      deductible: deductible(t.deductibleAmount, t.deductibleText),
      clauseCodes: t.clauseCodes,
      remarks: blankToUndefined(t.remarks),
    })),
    changeSummary: blankToUndefined(form.changeSummary),
    mancomSignoffRef: blankToUndefined(form.mancomSignoffRef),
  };
}

const DAY_MS = 86_400_000;

/** Whole days since a submission (Validation Queue age). */
export function ageInDays(submittedAt: string | undefined, now: number = Date.now()): number {
  if (!submittedAt) {
    return 0;
  }
  return Math.max(0, Math.floor((now - Date.parse(submittedAt)) / DAY_MS));
}
