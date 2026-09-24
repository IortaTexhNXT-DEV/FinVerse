import type { PeriodBasis, RatingInput, RatingMethod } from '@/api/catalog';

/** One row of the calculator: numbers are text while typing. */
export interface CalcItem {
  label: string;
  sumInsured: string;
  ratePercent: string;
  biLimit: string;
  pdLimit: string;
}

export interface CalcForm {
  productCode: string;
  insurerCode: string;
  branchCode: string;
  basis: PeriodBasis;
  periodFrom: string;
  periodTo: string;
  multiYear: boolean;
  endorsement: boolean;
  commissionRate: string;
  items: CalcItem[];
}

export const EMPTY_ITEM: CalcItem = {
  label: '',
  sumInsured: '',
  ratePercent: '',
  biLimit: '',
  pdLimit: '',
};

export function newCalcForm(): CalcForm {
  return {
    productCode: '',
    insurerCode: '',
    branchCode: '',
    basis: 'ANNUAL',
    periodFrom: '',
    periodTo: '',
    multiYear: false,
    endorsement: false,
    commissionRate: '',
    items: [{ ...EMPTY_ITEM }],
  };
}

const number = (text: string): number | undefined => {
  const trimmed = text.replace(/,/g, '').trim();
  if (trimmed === '') {
    return undefined;
  }
  const n = Number(trimmed);
  return Number.isNaN(n) ? undefined : n;
};

/** Problems to fix before rating (the server re-validates). */
export function calcProblems(form: CalcForm): string[] {
  const problems: string[] = [];
  if (form.productCode === '') {
    problems.push('Choose a product.');
  }
  if (form.items.every((i) => number(i.sumInsured) === undefined)) {
    problems.push('Enter the sum insured of at least one item.');
  }
  if (form.basis !== 'ANNUAL' && (form.periodFrom === '' || form.periodTo === '')) {
    problems.push(
      'Pro-rata and short-period rating need the period (for endorsements: the remaining term).',
    );
  }
  if (!form.endorsement && form.items.some((i) => (number(i.sumInsured) ?? 0) < 0)) {
    problems.push('Only an endorsement may reduce the sum insured.');
  }
  return problems;
}

/** The rating request of a form. Motor items carry BI / PD limits, others a rate. */
export function toRatingInput(
  form: CalcForm,
  companyId: number,
  method: RatingMethod,
): RatingInput {
  return {
    companyId,
    productCode: form.productCode,
    insurerCode: form.insurerCode || undefined,
    branchCode: form.branchCode || undefined,
    multiYear: form.multiYear,
    basis: form.basis,
    periodFrom: form.periodFrom || undefined,
    periodTo: form.periodTo || undefined,
    commissionRate: number(form.commissionRate),
    endorsement: form.endorsement,
    items: form.items
      .filter((i) => number(i.sumInsured) !== undefined)
      .map((i, index) => ({
        label: i.label.trim() || `Item ${index + 1}`,
        sumInsured: number(i.sumInsured) ?? 0,
        ratePercent: method === 'MOTOR' ? undefined : number(i.ratePercent),
        biLimit: method === 'MOTOR' ? number(i.biLimit) : undefined,
        pdLimit: method === 'MOTOR' ? number(i.pdLimit) : undefined,
      })),
  };
}
