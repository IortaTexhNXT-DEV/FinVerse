import type {
  AmountsInput,
  EndorsementRequest,
  RefundBasis,
  RequestClass,
  RequestInput,
  RequestStage,
} from './api';

/** Pure logic of the adjustment screens: stage labels and tones, the request form and its checks. */

export type Tone = 'success' | 'warning' | 'info' | 'neutral' | 'danger';

const STAGES: Record<RequestStage, { label: string; tone: Tone }> = {
  DRAFT: { label: 'Draft', tone: 'neutral' },
  FOR_VALIDATION: { label: 'For Validation', tone: 'warning' },
  FOR_APPROVAL: { label: 'For Approval', tone: 'warning' },
  FOR_POSTING: { label: 'For Posting', tone: 'info' },
  AWAITING_REAPPLICATION: { label: 'Payments to Re-apply', tone: 'danger' },
  POSTED: { label: 'Posted', tone: 'success' },
  RETURNED: { label: 'Returned', tone: 'danger' },
  CANCELLED: { label: 'Cancelled', tone: 'neutral' },
};

export function stageLabel(stage: RequestStage): string {
  return STAGES[stage].label;
}

export function stageTone(stage: RequestStage): Tone {
  return STAGES[stage].tone;
}

export type StageTab = 'ALL' | RequestStage;

export const STAGE_TABS: readonly { id: StageTab; label: string }[] = [
  { id: 'ALL', label: 'All' },
  { id: 'DRAFT', label: 'Draft' },
  { id: 'FOR_VALIDATION', label: 'For Validation' },
  { id: 'FOR_APPROVAL', label: 'For Approval' },
  { id: 'FOR_POSTING', label: 'For Posting' },
  { id: 'AWAITING_REAPPLICATION', label: 'Payments to Re-apply' },
  { id: 'RETURNED', label: 'Returned' },
  { id: 'POSTED', label: 'Posted' },
];

/** The stage tab of a URL parameter (tiles of the Operations home link to `?stage=`). */
export function tabOf(param: string | null): StageTab {
  return STAGE_TABS.find((t) => t.id === param)?.id ?? 'ALL';
}

const COMPONENTS: Record<string, string> = {
  BASIC: 'Basic Premium',
  DST: 'Documentary Stamp Tax',
  PREMIUM_TAX_VAT: 'Premium Tax / VAT',
  LGT: 'Local Government Tax',
  FST: 'Fire Service Tax',
  OTHER: 'Other Charges',
  DTIP: 'Due to Insurer (Gross)',
  COMMISSION: 'Commission',
  COMMISSION_VAT: 'VAT on Commission',
};

export function componentLabel(code: string): string {
  return COMPONENTS[code] ?? code;
}

/** Class of an endorsement type code (FIN_ / NF_ / INT_, ADJID.002/004). */
export function classOfType(code: string): RequestClass | undefined {
  if (code.startsWith('FIN_')) {
    return 'FINANCIAL';
  }
  if (code.startsWith('NF_')) {
    return 'NON_FINANCIAL';
  }
  return code.startsWith('INT_') ? 'INTERNAL' : undefined;
}

export type Mode = 'NONE' | 'CANCELLATION' | 'TSI' | 'AMOUNTS' | 'WRITE_OFF';

/** What the form asks for, from the request type (mirrors the backend computation). */
export function modeOf(requestClass: RequestClass | undefined, requestType: string): Mode {
  if (requestClass === 'NON_FINANCIAL' || requestType === '') {
    return 'NONE';
  }
  if (requestType.endsWith('CANCELLATION') || requestType === 'FLAT_CANCELLATION_RETAIN_DST') {
    return 'CANCELLATION';
  }
  if (requestType === 'TSI_CHANGE') {
    return 'TSI';
  }
  return requestType === 'WRITE_OFF' ? 'WRITE_OFF' : 'AMOUNTS';
}

export const AMOUNT_FIELDS = [
  { key: 'basic', label: 'Basic Premium' },
  { key: 'dst', label: 'DST' },
  { key: 'premiumTaxVat', label: 'Premium Tax / VAT' },
  { key: 'lgt', label: 'LGT' },
  { key: 'fst', label: 'FST' },
  { key: 'other', label: 'Other Charges' },
  { key: 'commission', label: 'Commission' },
  { key: 'vatOnCommission', label: 'VAT on Commission' },
] as const;

export type AmountKey = (typeof AMOUNT_FIELDS)[number]['key'];

export interface RequestForm {
  endorsementType: string;
  requestType: string;
  reasonCode: string;
  endorsementRef: string;
  effectiveDate: string;
  refundBasis: RefundBasis;
  sumInsuredChange: string;
  ratePercent: string;
  newPeriodFrom: string;
  newPeriodTo: string;
  description: string;
  instructions: string;
  amounts: Record<AmountKey, string>;
  duplicateOverride: string;
  baselineOverride: string;
}

const NO_AMOUNTS: Record<AmountKey, string> = {
  basic: '',
  dst: '',
  premiumTaxVat: '',
  lgt: '',
  fst: '',
  other: '',
  commission: '',
  vatOnCommission: '',
};

export const EMPTY_FORM: RequestForm = {
  endorsementType: '',
  requestType: '',
  reasonCode: '',
  endorsementRef: '',
  effectiveDate: '',
  refundBasis: 'PRO_RATA',
  sumInsuredChange: '',
  ratePercent: '',
  newPeriodFrom: '',
  newPeriodTo: '',
  description: '',
  instructions: '',
  amounts: NO_AMOUNTS,
  duplicateOverride: '',
  baselineOverride: '',
};

const text = (value: string | number | undefined): string =>
  value === undefined ? '' : String(value);

/** The form of an existing request (change of a draft or returned request). */
export function formOf(r: EndorsementRequest): RequestForm {
  const t = r.terms;
  const amounts = { ...NO_AMOUNTS };
  AMOUNT_FIELDS.forEach(({ key }) => {
    amounts[key] = text(r.amounts[key]);
  });
  return {
    endorsementType: t.endorsementType,
    requestType: t.requestType ?? '',
    reasonCode: t.reasonCode ?? '',
    endorsementRef: t.endorsementRef ?? '',
    effectiveDate: t.effectiveDate,
    refundBasis: t.refundBasis,
    sumInsuredChange: text(t.sumInsuredChange),
    ratePercent: text(t.ratePercent),
    newPeriodFrom: t.newPeriodFrom ?? '',
    newPeriodTo: t.newPeriodTo ?? '',
    description: t.description,
    instructions: t.instructions ?? '',
    amounts,
    duplicateOverride: r.control.duplicateOverride ?? '',
    baselineOverride: r.control.baselineOverride ?? '',
  };
}

export type FieldErrors = Partial<Record<keyof RequestForm, string>>;

const isNumber = (value: string) => value.trim() !== '' && Number.isFinite(Number(value));

function modeErrors(form: RequestForm, mode: Mode): FieldErrors {
  const errors: FieldErrors = {};
  if (mode === 'CANCELLATION' && form.reasonCode === '') {
    errors.reasonCode = 'Select the reason for cancellation';
  }
  if (mode === 'TSI' && (!isNumber(form.sumInsuredChange) || Number(form.sumInsuredChange) === 0)) {
    errors.sumInsuredChange = 'Enter the increase (positive) or decrease (negative)';
  }
  if (mode === 'AMOUNTS' && !AMOUNT_FIELDS.some(({ key }) => isNumber(form.amounts[key]))) {
    errors.amounts = 'Enter at least one premium, charge or commission change';
  }
  return errors;
}

/** Field errors of the request form (shown under each field). */
export function formErrors(form: RequestForm): FieldErrors {
  const requestClass = classOfType(form.endorsementType);
  const errors: FieldErrors = {};
  if (form.endorsementType === '') {
    errors.endorsementType = 'Select the endorsement type';
  }
  if (requestClass === 'FINANCIAL' && form.requestType === '') {
    errors.requestType = 'A financial endorsement needs its request type';
  }
  if (form.effectiveDate === '') {
    errors.effectiveDate = 'Enter the effective date';
  }
  if (form.description.trim() === '') {
    errors.description = 'Describe the change';
  }
  if (
    form.newPeriodFrom !== '' &&
    form.newPeriodTo !== '' &&
    form.newPeriodTo <= form.newPeriodFrom
  ) {
    errors.newPeriodTo = 'The new expiry must be after the new inception';
  }
  return { ...errors, ...modeErrors(form, modeOf(requestClass, form.requestType)) };
}

export function isValid(errors: FieldErrors): boolean {
  return Object.keys(errors).length === 0;
}

const optional = (value: string): string | undefined =>
  value.trim() === '' ? undefined : value.trim();

const optionalNumber = (value: string): number | undefined =>
  isNumber(value) ? Number(value) : undefined;

function amountsOf(form: RequestForm): AmountsInput | undefined {
  const amounts: AmountsInput = {};
  AMOUNT_FIELDS.forEach(({ key }) => {
    const value = optionalNumber(form.amounts[key]);
    if (value !== undefined) {
      amounts[key] = value;
    }
  });
  return Object.keys(amounts).length === 0 ? undefined : amounts;
}

/** The API body of the form for the invoices chosen. */
export function toInput(form: RequestForm, invoiceNos: string[]): RequestInput {
  const mode = modeOf(classOfType(form.endorsementType), form.requestType);
  return {
    invoiceNos,
    endorsementType: form.endorsementType,
    requestType: optional(form.requestType),
    reasonCode: optional(form.reasonCode),
    endorsementRef: optional(form.endorsementRef),
    effectiveDate: form.effectiveDate,
    refundBasis: form.refundBasis,
    sumInsuredChange: mode === 'TSI' ? optionalNumber(form.sumInsuredChange) : undefined,
    ratePercent: mode === 'TSI' ? optionalNumber(form.ratePercent) : undefined,
    newPeriodFrom: optional(form.newPeriodFrom),
    newPeriodTo: optional(form.newPeriodTo),
    description: form.description.trim(),
    instructions: optional(form.instructions),
    amounts: mode === 'AMOUNTS' ? amountsOf(form) : undefined,
    duplicateOverride: optional(form.duplicateOverride),
    baselineOverride: optional(form.baselineOverride),
  };
}
