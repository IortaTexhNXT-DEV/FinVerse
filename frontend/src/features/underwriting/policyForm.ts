import type {
  IterationInput,
  Policy,
  PolicyInput,
  Quotation,
  QuotationInput,
  RiskInput,
} from '@/api/underwriting';
import { oneYearFrom } from './premiumMath';

/** Draft policy form state and conversions. */

let rowSequence = 0;

/** New unique key for an editable risk row. */
export function nextRowKey(): string {
  rowSequence += 1;
  return `risk-${String(rowSequence)}`;
}

export function emptyRisk(): RiskInput {
  return { key: nextRowKey(), description: '', sumInsured: 0 };
}

export function newPolicy(companyId: number, branchId: number, today: string): PolicyInput {
  return {
    companyId,
    branchId,
    productId: 0,
    customerCode: '',
    insuredName: '',
    sourceType: 'DIRECT',
    issueDate: today,
    periodFrom: today,
    periodTo: oneYearFrom(today),
    currency: 'PHP',
    businessType: 'DIRECT',
    sharePct: 100,
    coinsuranceLeader: false,
    discountRate: 0,
    loadingRate: 0,
    risks: [emptyRisk()],
  };
}

export function fromPolicy(p: Policy): PolicyInput {
  return {
    companyId: p.companyId,
    branchId: p.branchId,
    productId: p.productId,
    customerCode: p.customerCode,
    insuredName: p.insuredName,
    sourceType: p.sourceType,
    intermediaryCode: p.intermediaryCode,
    issueDate: p.issueDate,
    periodFrom: p.periodFrom,
    periodTo: p.periodTo,
    currency: p.currency,
    businessType: p.businessType,
    sharePct: p.sharePct,
    coinsurerCode: p.coinsurerCode,
    coinsuranceLeader: p.coinsuranceLeader,
    discountRate: p.discountRate,
    loadingRate: p.loadingRate,
    commissionRate: p.premium.commissionRate,
    risks: p.risks.map((r) => ({
      key: nextRowKey(),
      description: r.description,
      sumInsured: r.sumInsured,
      rate: r.rate,
      premium: r.premium,
      occupation: r.occupation,
      accumulationZone: r.accumulationZone,
      ...r.marine,
    })),
  };
}

/** Normalizes dependent fields before sending (no intermediary for direct, 100 % when direct). */
export function normalize(form: PolicyInput): PolicyInput {
  const direct = form.businessType === 'DIRECT';
  return {
    ...form,
    intermediaryCode: form.sourceType === 'DIRECT' ? undefined : form.intermediaryCode,
    commissionRate: form.sourceType === 'DIRECT' ? undefined : form.commissionRate,
    sharePct: direct ? 100 : form.sharePct,
    coinsurerCode: direct ? undefined : form.coinsurerCode,
    coinsuranceLeader: direct ? false : form.coinsuranceLeader,
  };
}

type Check = [failed: boolean, message: string];

function partyChecks(form: PolicyInput): Check[] {
  return [
    [form.productId <= 0, 'Select a product'],
    [form.customerCode === '', 'Select the customer'],
    [form.insuredName.trim() === '', 'Enter the insured name'],
    [
      form.sourceType !== 'DIRECT' && (form.intermediaryCode ?? '') === '',
      'Select the agent or broker',
    ],
    [form.businessType !== 'DIRECT' && (form.coinsurerCode ?? '') === '', 'Select the coinsurer'],
  ];
}

function coverChecks(form: PolicyInput): Check[] {
  return [
    [form.periodTo < form.periodFrom, 'Period to must be on or after period from'],
    [
      form.risks.length === 0 || form.risks.some((r) => r.description.trim() === ''),
      'Every risk needs a description',
    ],
  ];
}

/** Client-side checks giving immediate feedback; the server re-validates everything. */
export function validatePolicy(form: PolicyInput): string[] {
  return [...partyChecks(form), ...coverChecks(form)]
    .filter(([failed]) => failed)
    .map(([, message]) => message);
}

/** Figures of a new iteration, prefilled from the quotation's latest one. */
export function iterationDefaults(q: Quotation): IterationInput {
  const last = q.iterations[q.iterations.length - 1];
  if (last === undefined) {
    return { sumInsured: 0, grossPremium: 0 };
  }
  return {
    sumInsured: last.sumInsured,
    grossPremium: last.grossPremium,
    discount: last.discount,
    loading: last.loading,
    charges: last.charges,
  };
}

/** Coinsurance description of a policy. */
export function businessLabel(p: Policy): string {
  if (p.businessType === 'DIRECT') {
    return 'Direct 100%';
  }
  const leader = p.coinsuranceLeader ? ' (leader)' : '';
  return `Coinsurance – our share ${String(p.sharePct)}% with ${p.coinsurerCode ?? ''}${leader}`;
}

/** New quotation with an empty first iteration. */
export function newQuotation(companyId: number, branchId: number, today: string): QuotationInput {
  return {
    companyId,
    branchId,
    productId: 0,
    customerCode: '',
    insuredName: '',
    sourceType: 'DIRECT',
    issueDate: today,
    validityDays: 30,
    periodFrom: today,
    periodTo: oneYearFrom(today),
    currency: 'PHP',
    sharePct: 100,
    iteration: { sumInsured: 0, grossPremium: 0 },
  };
}
