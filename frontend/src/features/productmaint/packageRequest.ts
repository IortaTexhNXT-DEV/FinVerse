import type {
  CoverageTerm,
  HomeCounts,
  PackageRequest,
  PackageTerms,
  RequestInput,
  RequestScope,
  RequestSearch,
  RequestStage,
  RequestType,
} from '@/api/productmaint';

/** Status tabs of the Package Requests work list (design section 11). */
export const REQUEST_TABS = [
  { id: 'drafts', label: 'Drafts' },
  { id: 'approval', label: 'For Approval' },
  { id: 'tsu', label: 'TSU Review' },
  { id: 'negotiation', label: 'Negotiation' },
  { id: 'mancom', label: 'ManCom' },
  { id: 'mbs', label: 'With MBS' },
  { id: 'validation', label: 'For Validation' },
  { id: 'released', label: 'Released' },
  { id: 'closed', label: 'Closed' },
] as const;

export type RequestTab = (typeof REQUEST_TABS)[number]['id'];

/** The stages listed under each tab. */
export const TAB_STAGES: Record<RequestTab, RequestStage[]> = {
  drafts: ['DRAFT'],
  approval: ['FOR_MKT_APPROVAL'],
  tsu: ['FOR_TSU_REVIEW', 'FOR_TSU_APPROVAL'],
  negotiation: ['NEGOTIATION', 'TERMS_REVIEW', 'FOR_MKT_REVIEW', 'REQUIREMENTS_PREP'],
  mancom: ['FOR_MANCOM'],
  mbs: ['WITH_MBS'],
  validation: ['FOR_VALIDATION'],
  released: ['RELEASED'],
  closed: ['RETIRED', 'NOT_PROCEEDED', 'VOIDED'],
};

/** Whether a string is a tab id (deep links from the home tiles). */
export function isRequestTab(value: string | null): value is RequestTab {
  return REQUEST_TABS.some((t) => t.id === value);
}

/** The tab a stage is listed under. */
export function tabOfStage(stage: RequestStage): RequestTab {
  const found = (Object.keys(TAB_STAGES) as RequestTab[]).find((t) =>
    TAB_STAGES[t].includes(stage),
  );
  return found ?? 'drafts';
}

/** The list criteria of a tab, the search box and the filters. */
export function requestCriteria(
  tab: RequestTab,
  text: string,
  mine: boolean,
  type: RequestType | '',
): RequestSearch {
  return {
    text: text.trim() === '' ? undefined : text.trim(),
    stage: TAB_STAGES[tab],
    type: type === '' ? undefined : [type],
    mine: mine ? true : undefined,
  };
}

/** Request types, in the order of the form. */
export const REQUEST_TYPES: { value: RequestType; label: string }[] = [
  { value: 'NEW', label: 'New package' },
  { value: 'AMEND', label: 'Amend package terms' },
  { value: 'UPDATE', label: 'Update package details' },
  { value: 'RENEW', label: 'Renew package' },
  { value: 'RETIRE', label: 'Retire package' },
  { value: 'REACTIVATE', label: 'Reactivate expired package' },
];

/** Label of a request type. */
export function typeLabel(type: RequestType): string {
  return REQUEST_TYPES.find((t) => t.value === type)?.label ?? type;
}

/** Whether a request type may skip the insurer negotiation (the flag is shown on the form). */
export function negotiationOptional(type: RequestType): boolean {
  return type === 'RENEW' || type === 'UPDATE' || type === 'REACTIVATE';
}

/** A Package Request Form as entered (text fields stay '' while blank). */
export interface RequestForm {
  id?: number;
  requestNo?: string;
  type: RequestType;
  scope: RequestScope;
  title: string;
  clientId?: number;
  lineCode: string;
  coverTypeCode: string;
  productCode: string;
  marketSegments: string[];
  reason: string;
  reasonNote: string;
  negotiationRequired: boolean;
  terms: PackageTerms;
}

/** Empty terms with the suggested sections. */
export function emptyTerms(): PackageTerms {
  return {
    sections: [
      { heading: 'Target market / client', text: '' },
      { heading: 'Requested cover and features', text: '' },
    ],
    coverages: [],
    scheme: {},
    dates: {},
    insurers: [],
  };
}

/** A blank coverage line. */
export function newCoverage(): CoverageTerm {
  return { coverageCode: '', included: true, optional: false, clauseCodes: [] };
}

export function newRequestForm(): RequestForm {
  return {
    type: 'NEW',
    scope: 'GENERIC',
    title: '',
    lineCode: '',
    coverTypeCode: '',
    productCode: '',
    marketSegments: [],
    reason: '',
    reasonNote: '',
    negotiationRequired: true,
    terms: emptyTerms(),
  };
}

/** The form of a saved request. */
export function formOfRequest(p: PackageRequest): RequestForm {
  return {
    id: p.id,
    requestNo: p.requestNo,
    type: p.requestType,
    scope: p.scope,
    title: p.title,
    clientId: p.clientId,
    lineCode: p.lineCode,
    coverTypeCode: p.coverTypeCode ?? '',
    productCode: p.productCode ?? '',
    marketSegments: [...p.marketSegments],
    reason: p.reason,
    reasonNote: p.reasonNote ?? '',
    negotiationRequired: p.negotiationRequired,
    terms: p.requestedTerms,
  };
}

/** Field errors of the form (mandatory fields and formats, BRPM.008). */
export function requestErrors(f: RequestForm): Record<string, string> {
  const errors: Record<string, string> = {};
  const require = (ok: boolean, key: string, message: string) => {
    if (!ok) {
      errors[key] = message;
    }
  };
  require(f.title.trim() !== '', 'title', 'Enter the package or programme name');
  require(f.lineCode !== '', 'lineCode', 'Select the product line');
  require(f.reason !== '', 'reason', 'Select the reason');
  require(f.type !== 'NEW' || f.coverTypeCode !== '', 'coverTypeCode', 'Select the cover type');
  require(f.type === 'NEW' || f.productCode !== '', 'productCode', 'Select the package product');
  require(f.scope === 'GENERIC' || f.clientId !== undefined, 'clientId', 'Select the client');
  const d = f.terms.dates;
  require(d.packageStartDate === undefined ||
    d.packageEndDate === undefined ||
    d.packageEndDate >
      d.packageStartDate, 'packageEndDate', 'The package end date must be after its start date');
  require(f.terms.coverages.every(
    (c) => c.coverageCode.trim() !== '',
  ), 'coverages', 'Enter a code for every coverage or remove the line');
  return errors;
}

/** What the completeness check before submission will still ask for (shown as a hint). */
export function submissionGaps(f: RequestForm): string[] {
  const gaps: string[] = [];
  const hasTerms =
    f.terms.coverages.length > 0 || f.terms.sections.some((s) => (s.text ?? '').trim() !== '');
  if (f.type !== 'RETIRE' && !hasTerms) {
    gaps.push('the requested terms (coverages or sections)');
  }
  if (f.negotiationRequired && f.type !== 'RETIRE' && f.terms.insurers.length === 0) {
    gaps.push('at least one target insurer');
  }
  if (f.type !== 'RETIRE' && f.terms.dates.packageEndDate === undefined) {
    gaps.push('the package end date');
  }
  return gaps;
}

/** The API body of the form. */
export function toRequestInput(f: RequestForm, companyId: number): RequestInput {
  return {
    companyId,
    type: f.type,
    scope: f.scope,
    title: f.title.trim(),
    clientId: f.scope === 'CLIENT_SPECIFIC' ? f.clientId : undefined,
    lineCode: f.lineCode,
    coverTypeCode: f.coverTypeCode || undefined,
    productCode: f.type === 'NEW' ? undefined : f.productCode || undefined,
    marketSegments: f.marketSegments,
    reason: f.reason,
    reasonNote: f.reasonNote.trim() || undefined,
    negotiationRequired: f.type === 'RETIRE' ? false : f.negotiationRequired,
    terms: {
      ...f.terms,
      sections: f.terms.sections.filter((s) => (s.text ?? '').trim() !== ''),
    },
  };
}

/** Toggles an insurer in the target insurers of the terms. */
export function toggleTargetInsurer(terms: PackageTerms, code: string): PackageTerms {
  const has = terms.insurers.some((i) => i.insurerCode === code);
  return {
    ...terms,
    insurers: has
      ? terms.insurers.filter((i) => i.insurerCode !== code)
      : [...terms.insurers, { insurerCode: code, terms: [] }],
  };
}

/** A home tile: a group of stages with its counts. */
export interface HomeTile {
  tab: RequestTab;
  label: string;
  total: number;
  overdue: number;
  dueSoon: number;
}

/** The home tiles by work list tab (BRPM.019): totals and SLA red / amber counts. */
export function homeTiles(counts: HomeCounts | undefined): HomeTile[] {
  const byStage = new Map((counts?.stages ?? []).map((s) => [s.stage, s]));
  return REQUEST_TABS.filter((t) => t.id !== 'closed').map((t) => {
    const stages = TAB_STAGES[t.id].map((s) => byStage.get(s));
    return {
      tab: t.id,
      label: t.label,
      total: stages.reduce((n, s) => n + (s?.total ?? 0), 0),
      overdue: stages.reduce((n, s) => n + (s?.overdue ?? 0), 0),
      dueSoon: stages.reduce((n, s) => n + (s?.dueSoon ?? 0), 0),
    };
  });
}

/** SLA state of a list row: overdue (red), due within eight hours (amber) or on time. */
export function slaState(dueAt: string | undefined, now: Date): 'overdue' | 'soon' | 'ok' {
  if (dueAt === undefined) {
    return 'ok';
  }
  const due = new Date(dueAt).getTime();
  if (due < now.getTime()) {
    return 'overdue';
  }
  const eightHours = 8 * 3600_000;
  return due - now.getTime() < eightHours ? 'soon' : 'ok';
}

/** Days a request has been in its stage. */
export function daysInStage(since: string | undefined, now: Date): number | undefined {
  if (since === undefined) {
    return undefined;
  }
  return Math.max(0, Math.floor((now.getTime() - new Date(since).getTime()) / 86_400_000));
}

/** Expiry tabs: packages without a renewal, and packages with one in progress. */
export type ExpiryTab = 'expiring' | 'renewal' | 'expired';

/** Rows of an expiry tab. */
export function expiryRows<T extends { renewalRequestId?: number }>(
  rows: T[],
  tab: ExpiryTab,
): T[] {
  if (tab === 'expired') {
    return [];
  }
  return rows.filter((r) => (tab === 'renewal') === (r.renewalRequestId !== undefined));
}

/** Urgency of a package end date: 7 days red, 30 days amber. */
export function expiryTone(daysLeft: number): 'danger' | 'warning' | 'info' {
  if (daysLeft <= 7) {
    return 'danger';
  }
  return daysLeft <= 30 ? 'warning' : 'info';
}

/** Field catalogue of the comparative outputs (PMADD03), as on the server. */
export const COMPARATIVE_FIELDS = [
  { code: 'OUTCOME', label: 'Outcome' },
  { code: 'RATE', label: 'Rate %' },
  { code: 'MINIMUM_PREMIUM', label: 'Minimum Premium' },
  { code: 'COVERAGES', label: 'Coverages' },
  { code: 'DEDUCTIBLES', label: 'Deductibles' },
  { code: 'CONDITIONS', label: 'Conditions / Warranties' },
  { code: 'VALID_UNTIL', label: 'Valid Until' },
  { code: 'REMARKS', label: 'Remarks' },
] as const;

/** Insurer outcomes that are offers (not pending, declined or unanswered). */
export function offered(outcome: string): boolean {
  return !['PENDING', 'DECLINED', 'NO_RESPONSE'].includes(outcome);
}
