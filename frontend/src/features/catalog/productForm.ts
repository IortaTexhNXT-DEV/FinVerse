import type { Product, ProductInput } from '@/api/catalog';

/** Editable product: the input with numbers possibly blank while typing. */
export type ProductForm = Omit<
  ProductInput,
  'defaultCommissionRate' | 'minimumPremium' | 'maxTermYears'
> & {
  defaultCommissionRate?: number;
  minimumPremium?: number;
  maxTermYears?: number;
  /** Set when editing an existing product (the code cannot change). */
  existing: boolean;
};

/** A blank product with the usual defaults (paid before placement, one-year term). */
export function newProductForm(lineCode = ''): ProductForm {
  return {
    code: '',
    name: '',
    lineCode,
    packaged: false,
    fleetCapable: false,
    marketSegments: [],
    mortgageApplicable: false,
    directPaymentEligible: false,
    multiYearAllowed: false,
    maxTermYears: 1,
    ffyEligible: false,
    paymentGate: 'PAID',
    defaultCommissionRate: undefined,
    minimumPremium: 0,
    tsuInvolvement: 'BY_RULES',
    existing: false,
  };
}

/** The form of an existing product. */
export function productFormOf(product: Product): ProductForm {
  return {
    code: product.code,
    name: product.name,
    lineCode: product.lineCode,
    coverTypeCode: product.coverTypeCode ?? undefined,
    packaged: product.packaged,
    fleetCapable: product.fleetCapable,
    marketSegments: [...product.marketSegments],
    mortgageApplicable: product.mortgageApplicable,
    directPaymentEligible: product.directPaymentEligible,
    multiYearAllowed: product.multiYearAllowed,
    maxTermYears: product.maxTermYears,
    ffyEligible: product.ffyEligible,
    paymentGate: product.paymentGate,
    defaultRate: product.defaultRate ?? undefined,
    defaultCommissionRate: product.defaultCommissionRate,
    minimumPremium: product.minimumPremium,
    maxSumInsured: product.maxSumInsured ?? undefined,
    tsuInvolvement: product.tsuInvolvement ?? 'BY_RULES',
    existing: true,
  };
}

/** Problems the server would reject, shown before saving (the server re-validates). */
export function productProblems(form: ProductForm): string[] {
  const problems: string[] = [];
  if (!/^[A-Z0-9]{1,20}$/.test(form.code)) {
    problems.push('Code: 1 to 20 capital letters or digits.');
  }
  if (form.name.trim() === '') {
    problems.push('Name is required.');
  }
  if (form.lineCode === '') {
    problems.push('Choose the product line.');
  }
  if (form.defaultCommissionRate === undefined) {
    problems.push('Default commission rate is required.');
  }
  if (form.multiYearAllowed && (form.maxTermYears ?? 1) < 2) {
    problems.push('A multi-year product needs a maximum term of at least 2 years.');
  }
  return problems;
}

/** The request body of a form without problems. */
export function toProductInput(form: ProductForm): ProductInput {
  const rest: Omit<ProductForm, 'existing'> & { existing?: boolean } = { ...form };
  delete rest.existing;
  return {
    ...rest,
    coverTypeCode: rest.coverTypeCode === '' ? undefined : rest.coverTypeCode,
    maxTermYears: rest.multiYearAllowed ? (rest.maxTermYears ?? 1) : 1,
    defaultCommissionRate: rest.defaultCommissionRate ?? 0,
    minimumPremium: rest.minimumPremium ?? 0,
  };
}

/** Adds or removes one market segment. */
export function toggleSegment(segments: readonly string[], code: string): string[] {
  return segments.includes(code) ? segments.filter((s) => s !== code) : [...segments, code];
}
