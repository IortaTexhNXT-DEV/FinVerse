import type { ItemInput } from '@/api/accounts';
import type { PeriodBasis } from '@/api/catalog';
import type { Quotation, QuotationInput } from '@/api/quotations';

/** The quotation being entered in the wizard (text fields stay '' while blank). */
export interface QuotationForm {
  id?: number;
  quotationNo?: string;
  arn?: string;
  clientId?: number;
  clientName: string;
  productCode: string;
  marketSegment: string;
  sourceChannel: string;
  requestId?: number;
  insurerCode: string;
  insurerBranch: string;
  periodFrom: string;
  periodTo: string;
  validUntil: string;
  directPayment: boolean;
  ratingBasis: PeriodBasis;
  remarks: string;
  items: ItemInput[];
  /** Risk group of each item (same index); a group becomes one account. */
  groups: number[];
}

export const QUOTATION_STEPS = [
  { id: 'client', label: 'Client' },
  { id: 'product', label: 'Product & Terms' },
  { id: 'items', label: 'Risk Items' },
  { id: 'premium', label: 'Premium' },
  { id: 'review', label: 'Review' },
] as const;

export type QuotationStep = (typeof QUOTATION_STEPS)[number]['id'];

/** The same day one year later. */
export function yearAfter(iso: string): string {
  return iso === '' ? '' : `${Number(iso.slice(0, 4)) + 1}${iso.slice(4)}`;
}

/** Preselections read from the URL (client page, request inbox). */
export interface Preselection {
  clientId?: number;
  productCode?: string;
  requestId?: number;
  marketSegment?: string;
  sourceChannel?: string;
}

export function newQuotationForm(today: string, pre: Preselection = {}): QuotationForm {
  return {
    clientId: pre.clientId,
    clientName: '',
    productCode: pre.productCode ?? '',
    marketSegment: pre.marketSegment ?? '',
    sourceChannel: pre.sourceChannel ?? 'EMAIL',
    requestId: pre.requestId,
    insurerCode: '',
    insurerBranch: '',
    periodFrom: today,
    periodTo: yearAfter(today),
    validUntil: '',
    directPayment: false,
    ratingBasis: 'ANNUAL',
    remarks: '',
    items: [],
    groups: [],
  };
}

const text = (value: string | null | undefined) => value ?? '';

/** The wizard form of a saved quotation (continue a draft or revise it). */
export function formOfQuotation(q: Quotation): QuotationForm {
  const c = q.content;
  return {
    id: q.id,
    quotationNo: q.quotationNo,
    arn: q.arn,
    clientId: q.clientId,
    clientName: q.clientName,
    productCode: q.productCode,
    marketSegment: text(q.marketSegment),
    sourceChannel: text(q.sourceChannel),
    requestId: q.requestId,
    insurerCode: text(c.insurerCode),
    insurerBranch: text(c.insurerBranch),
    periodFrom: text(c.periodFrom),
    periodTo: text(c.periodTo),
    validUntil: c.validUntil,
    directPayment: c.directPayment,
    ratingBasis: (c.ratingBasis ?? 'ANNUAL') as PeriodBasis,
    remarks: text(c.remarks),
    items: c.items.map((i) => i.data),
    groups: c.items.map((i) => i.riskGroup),
  };
}

const blank = (value: string) => (value.trim() === '' ? undefined : value.trim());

/** The request body of a form. */
export function toQuotationInput(f: QuotationForm, companyId: number): QuotationInput {
  return {
    companyId,
    clientId: f.clientId,
    productCode: f.productCode,
    marketSegment: blank(f.marketSegment),
    sourceChannel: blank(f.sourceChannel),
    requestId: f.requestId,
    insurerCode: blank(f.insurerCode),
    insurerBranch: blank(f.insurerCode) === undefined ? undefined : blank(f.insurerBranch),
    periodFrom: blank(f.periodFrom),
    periodTo: blank(f.periodTo),
    validUntil: blank(f.validUntil),
    directPayment: f.directPayment,
    ratingBasis: f.ratingBasis,
    remarks: blank(f.remarks),
    items: f.items.map((item, i) => ({ riskGroup: f.groups[i] ?? 1, item })),
  };
}

/** Whether the form can be saved as a draft (client and product chosen). */
export function canSaveQuotation(f: QuotationForm): boolean {
  return f.clientId !== undefined && f.productCode !== '';
}

function productErrors(f: QuotationForm): Record<string, string> {
  const errors: Record<string, string> = {};
  if (f.productCode === '') {
    errors.productCode = 'Select the product';
  }
  if (f.periodFrom !== '' && f.periodTo !== '' && f.periodTo <= f.periodFrom) {
    errors.periodTo = 'The period end must be after the period start';
  }
  return errors;
}

function itemErrors(f: QuotationForm): Record<string, string> {
  if (f.items.length === 0) {
    return { items: 'Add at least one risk item' };
  }
  const missing = f.items.some((i) => i.sumInsured === undefined && i.location === undefined);
  return missing ? { items: 'Enter the sum insured of every item' } : {};
}

/** Field errors that block leaving a step forward. */
export function quotationStepErrors(step: QuotationStep, f: QuotationForm): Record<string, string> {
  switch (step) {
    case 'client':
      return f.clientId === undefined ? { clientId: 'Select the client or prospect' } : {};
    case 'product':
      return productErrors(f);
    case 'items':
      return itemErrors(f);
    default:
      return {};
  }
}

/** Sets the risk group of an item, keeping groups numbered from 1. */
export function withGroup(groups: number[], index: number, group: number): number[] {
  const next = [...groups];
  next[index] = Math.max(1, Math.round(group));
  return next;
}

/** Keeps the group list aligned with the items (new items join group 1). */
export function alignGroups(groups: number[], itemCount: number): number[] {
  return Array.from({ length: itemCount }, (_, i) => groups[i] ?? 1);
}
