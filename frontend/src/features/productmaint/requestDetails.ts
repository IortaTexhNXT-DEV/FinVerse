import { formatAmount } from '@/utils/format';
import type { RequestDetails } from '@/api/productmaint';
import type { ComboOption } from '@/components/ui/comboOptions';

/** The Source of a package request (BDOI FRS FRPM.011.02). */
export const SOURCES: readonly ComboOption[] = [
  { value: 'MARKETING', label: 'Marketing' },
  { value: 'TSU', label: 'TSU' },
  { value: 'INSURER', label: 'Insurer' },
];

/** The Annex E fields as typed (numbers are text while typing). */
export interface DetailsForm {
  businessOrigin: string;
  accountOfficer: string;
  unitHead: string;
  insuredName: string;
  estimatedPolicies: string;
  estimatedPremium: string;
  typeOfCover: string;
  descriptionOfCover: string;
  cover: string;
  extensions: string;
  warranties: string;
  otherInstructions: string;
  maximumLimits: string;
  premium: string;
  incentiveEligible: boolean;
  incentiveAmount: string;
  incentiveRate: string;
}

const TEXT_KEYS = [
  'businessOrigin',
  'accountOfficer',
  'unitHead',
  'insuredName',
  'typeOfCover',
  'descriptionOfCover',
  'cover',
  'extensions',
  'warranties',
  'otherInstructions',
  'maximumLimits',
] as const;

const NUMBER_KEYS = [
  'estimatedPolicies',
  'estimatedPremium',
  'premium',
  'incentiveAmount',
  'incentiveRate',
] as const;

/** Blank Annex E fields. */
export function emptyDetails(): DetailsForm {
  return {
    businessOrigin: '',
    accountOfficer: '',
    unitHead: '',
    insuredName: '',
    estimatedPolicies: '',
    estimatedPremium: '',
    typeOfCover: '',
    descriptionOfCover: '',
    cover: '',
    extensions: '',
    warranties: '',
    otherInstructions: '',
    maximumLimits: '',
    premium: '',
    incentiveEligible: false,
    incentiveAmount: '',
    incentiveRate: '',
  };
}

/** The form of saved details. */
export function detailsOf(d: RequestDetails | null | undefined): DetailsForm {
  const form = emptyDetails();
  if (d === null || d === undefined) {
    return form;
  }
  TEXT_KEYS.forEach((k) => {
    form[k] = d[k] ?? '';
  });
  NUMBER_KEYS.forEach((k) => {
    const v = d[k];
    form[k] = v === undefined ? '' : String(v);
  });
  form.incentiveEligible = d.incentiveEligible === true;
  return form;
}

/** The details to send; undefined when every field is blank. */
export function toDetails(f: DetailsForm | undefined): RequestDetails | undefined {
  if (f === undefined) {
    return undefined;
  }
  const out: RequestDetails = { incentiveEligible: f.incentiveEligible };
  let any = f.incentiveEligible;
  TEXT_KEYS.forEach((k) => {
    const v = f[k].trim();
    if (v !== '') {
      out[k] = v;
      any = true;
    }
  });
  NUMBER_KEYS.forEach((k) => {
    const v = f[k].trim();
    if (v !== '') {
      out[k] = Number(v);
      any = true;
    }
  });
  return any ? out : undefined;
}

/**
 * The field errors of the Annex E fields (the server checks again with its parameters).
 *
 * @param f the fields
 * @param minPolicies least estimated number of policies
 * @param minPremium least estimated total basic premium
 * @returns message by field
 */
export function detailsErrors(
  f: DetailsForm | undefined,
  minPolicies = 20,
  minPremium = 5_000_000,
): Record<string, string> {
  if (f === undefined) {
    return {};
  }
  return { ...volumeErrors(f, minPolicies, minPremium), ...incentiveErrors(f) };
}

function volumeErrors(f: DetailsForm, minPolicies: number, minPremium: number) {
  const errors: Record<string, string> = {};
  const policies = f.estimatedPolicies.trim();
  if (policies !== '' && (!Number.isInteger(Number(policies)) || Number(policies) < minPolicies)) {
    errors.estimatedPolicies = `Enter at least ${String(minPolicies)} policies`;
  }
  const premium = f.estimatedPremium.trim();
  if (premium !== '' && (Number.isNaN(Number(premium)) || Number(premium) < minPremium)) {
    errors.estimatedPremium = `Enter at least ${formatAmount(minPremium)}`;
  }
  return errors;
}

function incentiveErrors(f: DetailsForm) {
  const errors: Record<string, string> = {};
  const amount = f.incentiveAmount.trim();
  const rate = f.incentiveRate.trim();
  if (amount !== '' && (Number.isNaN(Number(amount)) || Number(amount) <= 0)) {
    errors.incentiveAmount = 'The incentive amount must be greater than zero';
  }
  if (rate !== '' && (Number.isNaN(Number(rate)) || Number(rate) <= 0 || Number(rate) > 100)) {
    errors.incentiveRate = 'The incentive commission rate must be above 0% and at most 100%';
  }
  if (f.incentiveEligible && amount === '' && rate === '') {
    errors.incentiveAmount = 'Enter the incentive amount or the incentive commission rate';
  }
  return errors;
}
