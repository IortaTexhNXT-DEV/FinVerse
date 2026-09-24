import type {
  Account,
  AccountCriteria,
  AccountStatus,
  AccountInput,
  ItemInput,
  PaymentArrangement,
} from '@/api/accounts';
import type { PeriodBasis, RiskItemKind } from '@/api/catalog';
import { ApiError } from '@/api/client';
import type { ClientSummary } from '@/api/crm';

/** The account being entered in the wizard (text fields stay '' while blank). */
export interface AccountDraft {
  id?: number;
  arn?: string;
  clientId?: number;
  clientName: string;
  productCode: string;
  marketSegment: string;
  sourceChannel: string;
  insurerCode: string;
  insurerBranch: string;
  periodFrom: string;
  periodTo: string;
  multiYear: boolean;
  termYears: number;
  currency: string;
  paymentArrangement: PaymentArrangement;
  mortgageeBank: string;
  loanApplicationNo: string;
  /** Promissory note numbers, separated by commas. */
  pnNumbers: string;
  contactName: string;
  contactEmail: string;
  contactMobile: string;
  contactAddress: string;
  items: ItemInput[];
  ratingBasis: PeriodBasis;
  commissionRate?: number;
  ffyStart: string;
}

export const WIZARD_STEPS = [
  { id: 'client', label: 'Client' },
  { id: 'product', label: 'Product' },
  { id: 'period', label: 'Period & payment' },
  { id: 'items', label: 'Risk items' },
  { id: 'contact', label: 'Contact & premium' },
  { id: 'review', label: 'Review & submit' },
] as const;

export type WizardStep = (typeof WIZARD_STEPS)[number]['id'];

/** Autosave interval of a draft (BRNB.012). */
export const AUTOSAVE_MS = 30_000;

/** The same day one year later (the usual annual period end). */
export function oneYearAfter(isoDate: string): string {
  if (isoDate === '') {
    return '';
  }
  const [year, rest] = [isoDate.slice(0, 4), isoDate.slice(4)];
  return `${Number(year) + 1}${rest}`;
}

export function newDraft(today: string): AccountDraft {
  return {
    clientName: '',
    productCode: '',
    marketSegment: '',
    sourceChannel: '',
    insurerCode: '',
    insurerBranch: '',
    periodFrom: today,
    periodTo: oneYearAfter(today),
    multiYear: false,
    termYears: 1,
    currency: 'PHP',
    paymentArrangement: 'VIA_BDOI',
    mortgageeBank: '',
    loanApplicationNo: '',
    pnNumbers: '',
    contactName: '',
    contactEmail: '',
    contactMobile: '',
    contactAddress: '',
    items: [],
    ratingBasis: 'ANNUAL',
    ffyStart: '',
  };
}

/** A blank risk item of a product line's kind. */
export function emptyItem(kind: RiskItemKind): ItemInput {
  switch (kind) {
    case 'VEHICLE':
      return { vehicle: {} };
    case 'PROPERTY_LOCATION':
      return { location: { insuredItems: [] } };
    case 'PERSON':
      return { person: {} };
    default:
      return { description: '' };
  }
}

const text = (value: string | null | undefined) => value ?? '';

/** The wizard draft of a saved account (to continue a draft or a returned account). */
export function draftOfAccount(a: Account): AccountDraft {
  return {
    id: a.id,
    arn: a.arn,
    clientId: a.clientId,
    clientName: a.clientName,
    productCode: a.productCode,
    marketSegment: text(a.marketSegment),
    sourceChannel: text(a.sourceChannel),
    insurerCode: text(a.insurerCode),
    insurerBranch: text(a.insurerBranch),
    periodFrom: text(a.periodFrom),
    periodTo: text(a.periodTo),
    multiYear: a.multiYear,
    termYears: a.termYears,
    currency: a.currency,
    paymentArrangement: a.paymentArrangement ?? 'VIA_BDOI',
    mortgageeBank: text(a.mortgageeBank),
    loanApplicationNo: text(a.loanApplicationNo),
    pnNumbers: a.pnNumbers.join(', '),
    contactName: text(a.contact.name),
    contactEmail: text(a.contact.email),
    contactMobile: text(a.contact.mobile),
    contactAddress: text(a.contact.address),
    items: a.items.map(
      ({ description, sumInsured, rate, biLimit, pdLimit, vehicle, location, person }) => ({
        description,
        sumInsured,
        rate,
        biLimit,
        pdLimit,
        vehicle,
        location,
        person,
      }),
    ),
    ratingBasis: a.premium.ratingBasis ?? 'ANNUAL',
    commissionRate: a.premium.commissionRate ?? undefined,
    ffyStart: a.freeFirstYear.active ? text(a.freeFirstYear.start) : '',
  };
}

const orUndefined = (value: string) => (value.trim() === '' ? undefined : value.trim());

/** Splits typed reference numbers (commas, semicolons or spaces). */
export function splitRefs(value: string): string[] {
  return value
    .split(/[\s,;]+/)
    .map((v) => v.trim())
    .filter((v) => v !== '');
}

/** The request body of a draft; the client and product must be chosen. */
export function toAccountInput(d: AccountDraft, companyId: number): AccountInput {
  return {
    companyId,
    clientId: d.clientId ?? 0,
    productCode: d.productCode,
    marketSegment: orUndefined(d.marketSegment),
    sourceChannel: orUndefined(d.sourceChannel),
    insurerCode: orUndefined(d.insurerCode),
    insurerBranch: orUndefined(d.insurerBranch),
    periodFrom: orUndefined(d.periodFrom),
    periodTo: orUndefined(d.periodTo),
    multiYear: d.multiYear,
    termYears: d.multiYear ? d.termYears : 1,
    currency: orUndefined(d.currency),
    paymentArrangement: d.paymentArrangement,
    mortgageeBank: orUndefined(d.mortgageeBank),
    loanApplicationNo: orUndefined(d.loanApplicationNo),
    pnNumbers: splitRefs(d.pnNumbers),
    contactName: orUndefined(d.contactName),
    contactEmail: orUndefined(d.contactEmail),
    contactMobile: orUndefined(d.contactMobile),
    contactAddress: orUndefined(d.contactAddress),
    items: d.items,
    ratingBasis: d.ratingBasis,
    commissionRate: d.commissionRate,
    ffyStart: orUndefined(d.ffyStart),
  };
}

/** A draft can be saved (and autosaved) once its client and product are chosen. */
export function canSave(d: AccountDraft): boolean {
  return d.clientId !== undefined && d.productCode !== '';
}

function itemProblem(item: ItemInput, index: number): string | undefined {
  const n = index + 1;
  if (item.vehicle && !item.vehicle.plateNo && !item.vehicle.conductionSticker) {
    return `Vehicle ${n}: enter the plate number or conduction sticker.`;
  }
  if (item.location && !item.location.address) {
    return `Location ${n}: enter the address.`;
  }
  if (item.person && !item.person.name) {
    return `Person ${n}: enter the name.`;
  }
  return undefined;
}

function itemsProblems(items: readonly ItemInput[]): string[] {
  if (items.length === 0) {
    return ['Add at least one risk item.'];
  }
  return items.flatMap((item, i) => itemProblem(item, i) ?? []);
}

/** What blocks leaving a wizard step (the server checks completeness on submission). */
export function stepProblems(step: WizardStep, d: AccountDraft): string[] {
  switch (step) {
    case 'client':
      return d.clientId === undefined ? ['Choose the client.'] : [];
    case 'product':
      return d.productCode === '' ? ['Choose the product.'] : [];
    case 'period':
      return d.periodFrom !== '' && d.periodTo !== '' && d.periodTo <= d.periodFrom
        ? ['The period must end after it starts.']
        : [];
    case 'items':
      return itemsProblems(d.items);
    default:
      return [];
  }
}

const ARN = /ARN-\d{4}-\d+/g;

/**
 * The existing accounts named by a duplicate rejection (DUPLICATE_ACCOUNT), so the user can open
 * them; empty for any other error.
 */
export function duplicateArns(error: unknown): string[] {
  if (!(error instanceof ApiError) || error.code !== 'DUPLICATE_ACCOUNT') {
    return [];
  }
  return [...new Set(error.message.match(ARN) ?? [])];
}

export type QuickFilter = 'all' | 'drafts' | 'returned' | 'payment' | 'ffy' | 'direct';

/** Quick filters of the account list (BRNB.050). */
export const QUICK_FILTERS: Record<QuickFilter, { label: string; criteria: AccountCriteria }> = {
  all: { label: 'All', criteria: {} },
  drafts: { label: 'My drafts', criteria: { mine: true, status: ['DRAFT'] } },
  returned: {
    label: 'Returned to me',
    criteria: { mine: true, status: ['RETURNED_TO_MARKETING'] },
  },
  payment: { label: 'Awaiting payment', criteria: { status: ['AWAITING_PAYMENT'] } },
  ffy: { label: 'FFY', criteria: { ffy: true } },
  direct: { label: 'Direct payment', criteria: { directPayment: true } },
};

/** Draft changes when a client is chosen: segment and contact default from the client. */
export function withClient(d: AccountDraft, c: ClientSummary | undefined): Partial<AccountDraft> {
  if (c === undefined) {
    return { clientId: undefined, clientName: '' };
  }
  return {
    clientId: c.id,
    clientName: c.displayName,
    marketSegment: d.marketSegment || (c.marketSegment ?? ''),
    contactEmail: d.contactEmail || (c.email ?? ''),
    contactMobile: d.contactMobile || (c.mobile ?? ''),
  };
}

/** Values of the account search panel. */
export interface SearchPanelValues {
  text: string;
  pn: string;
  vehicle: string;
  location: string;
  product: string;
  insurer: string;
  status: string;
  periodFrom: string;
  periodTo: string;
  includeVoided: boolean;
}

export const EMPTY_PANEL: SearchPanelValues = {
  text: '',
  pn: '',
  vehicle: '',
  location: '',
  product: '',
  insurer: '',
  status: '',
  periodFrom: '',
  periodTo: '',
  includeVoided: false,
};

/** Criteria of the search panel combined with a quick filter (the quick filter's status wins). */
export function criteriaOf(panel: SearchPanelValues, quick: QuickFilter): AccountCriteria {
  const typed = Object.fromEntries(
    Object.entries(panel).filter(([key, v]) => key !== 'status' && v !== '' && v !== false),
  ) as AccountCriteria;
  const status = panel.status === '' ? undefined : [panel.status as AccountStatus];
  return { ...typed, status, ...QUICK_FILTERS[quick].criteria };
}
