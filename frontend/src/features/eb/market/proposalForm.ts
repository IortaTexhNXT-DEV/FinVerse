import type { ProposalInput, TorItem } from '@/api/ebMarket';

/** A plan of the proposal as typed (numbers kept as text until saved). */
export interface PlanRow {
  benefitLine: string;
  planCode: string;
  planName: string;
  members: string;
  premiumRate: string;
  annualPremium: string;
  sumInsured: string;
}

/** The answer to a TOR item as typed. */
export interface AnswerRow {
  torItemId: number;
  offeredValue: string;
  deviation: boolean;
  remark: string;
}

/** A capability factor rating as typed. */
export interface FactorRow {
  factorCode: string;
  rating: string;
  value: string;
}

export interface ProposalForm {
  insurerCode: string;
  receivedOn: string;
  validUntil: string;
  currency: string;
  terms: string;
  exclusions: string;
  plans: PlanRow[];
  answers: AnswerRow[];
  factors: FactorRow[];
}

export const BLANK_PLAN: PlanRow = {
  benefitLine: '',
  planCode: '',
  planName: '',
  members: '',
  premiumRate: '',
  annualPremium: '',
  sumInsured: '',
};

/** A blank proposal with one answer row per released TOR item. */
export function blankProposal(items: TorItem[], benefitLine = ''): ProposalForm {
  return {
    insurerCode: '',
    receivedOn: '',
    validUntil: '',
    currency: 'PHP',
    terms: '',
    exclusions: '',
    plans: [{ ...BLANK_PLAN, benefitLine }],
    answers: items.map((i) => ({
      torItemId: i.id,
      offeredValue: '',
      deviation: false,
      remark: '',
    })),
    factors: [],
  };
}

const isAmount = (v: string) => v.trim() !== '' && Number.isFinite(Number(v)) && Number(v) >= 0;
const optionalAmount = (v: string) => v.trim() === '' || isAmount(v);

/** The errors of the proposal form, by field; empty when it may be saved. */
export function proposalErrors(form: ProposalForm, file: File | undefined): Record<string, string> {
  const errors: Record<string, string> = {};
  if (form.insurerCode === '') {
    errors.insurer = 'Select the insurer';
  }
  if (!file) {
    errors.file = "Attach the insurer's proposal";
  }
  if (form.plans.length === 0) {
    errors.plans = 'Enter the premium of at least one plan';
  }
  form.plans.forEach((p, i) => {
    const n = String(i + 1);
    if (p.benefitLine === '' || p.planCode.trim() === '') {
      errors.plans = `Plan ${n}: select the benefit line and enter the plan`;
    } else if (!isAmount(p.annualPremium)) {
      errors.plans = `Plan ${n}: enter an annual premium of zero or more`;
    } else if (![p.members, p.premiumRate, p.sumInsured].every(optionalAmount)) {
      errors.plans = `Plan ${n}: amounts and members cannot be negative`;
    }
  });
  const ratings = form.factors.map((f) => f.rating).filter((r) => r !== '');
  if (ratings.some((r) => !['1', '2', '3', '4', '5'].includes(r))) {
    errors.factors = 'Rate the capability factors from 1 to 5';
  }
  return errors;
}

const num = (v: string) => (v.trim() === '' ? null : Number(v));
const text = (v: string) => (v.trim() === '' ? undefined : v.trim());

/** The API input of a valid form: blank answers and factors are left out. */
export function toProposalInput(form: ProposalForm): ProposalInput {
  return {
    insurerCode: form.insurerCode,
    receivedOn: text(form.receivedOn),
    validUntil: text(form.validUntil),
    currency: text(form.currency),
    terms: text(form.terms),
    exclusions: text(form.exclusions),
    lines: form.plans.map((p) => ({
      benefitLine: p.benefitLine,
      planCode: p.planCode.trim(),
      planName: text(p.planName) ?? null,
      members: num(p.members),
      premiumRate: num(p.premiumRate),
      annualPremium: Number(p.annualPremium),
      sumInsured: num(p.sumInsured),
    })),
    items: form.answers
      .filter((a) => a.offeredValue.trim() !== '')
      .map((a) => ({
        torItemId: a.torItemId,
        offeredValue: a.offeredValue.trim(),
        deviation: a.deviation,
        remark: text(a.remark) ?? null,
      })),
    factors: form.factors
      .filter((f) => f.factorCode !== '')
      .map((f) => ({
        factorCode: f.factorCode,
        rating: num(f.rating),
        value: text(f.value) ?? null,
      })),
  };
}

/** The total annual premium of the plans typed so far. */
export function totalPremium(form: ProposalForm): number {
  return form.plans.reduce(
    (sum, p) => sum + (isAmount(p.annualPremium) ? Number(p.annualPremium) : 0),
    0,
  );
}
