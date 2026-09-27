import type {
  Amounts,
  BatchLine,
  BatchStage,
  DtipRow,
  ExtractionTag,
  HoldStage,
  RemittanceType,
  SpecialStage,
} from './api';

/** Labels, tab definitions and pure helpers of the Remittance screens (RMTID, MKTID). */

export const TYPE_LABELS: Record<RemittanceType, string> = {
  WITH_INCENTIVES: 'With Incentives',
  NORMAL_PHP: 'Normal - Peso',
  NORMAL_USD: 'Normal - Dollar',
  SPECIAL: 'Special',
};

export const TAG_LABELS: Record<ExtractionTag, string> = {
  EXTRACTED: 'Extracted',
  UNEXTRACTED_NOT_DUE: 'Not Yet Due',
  UNEXTRACTED_DUE: 'Due - Not Extracted',
  RETURNED: 'Returned',
};

export type BatchTab = 'REVIEW' | 'APPROVAL' | 'APPROVED' | 'REMITTED' | 'OR' | 'RETURNED';

/** Work list tabs of the batches (RMTID.027) and the stages behind each. */
export const BATCH_TABS: readonly { id: BatchTab; label: string; stages: BatchStage[] }[] = [
  { id: 'REVIEW', label: 'Review in Process', stages: ['REVIEW_IN_PROCESS', 'ON_HOLD'] },
  { id: 'APPROVAL', label: 'For Approval', stages: ['FOR_APPROVAL'] },
  { id: 'APPROVED', label: 'With Disbursement', stages: ['APPROVED'] },
  {
    id: 'REMITTED',
    label: 'Awaiting Insurer OR',
    stages: ['PARTIALLY_REMITTED', 'FULLY_REMITTED'],
  },
  { id: 'OR', label: 'OR Received', stages: ['OR_RECEIVED'] },
  { id: 'RETURNED', label: 'Returned', stages: ['RETURNED'] },
];

export type HoldTab = 'APPROVAL' | 'ACTIVE' | 'DRAFT' | 'CLOSED';

/** Work list tabs of the hold requests (MKTID.002-007). */
export const HOLD_TABS: readonly { id: HoldTab; label: string; stages: HoldStage[] }[] = [
  {
    id: 'APPROVAL',
    label: 'For Approval',
    stages: ['FOR_APPROVAL', 'EXTENSION_FOR_APPROVAL', 'CANCEL_FOR_APPROVAL'],
  },
  { id: 'ACTIVE', label: 'Active Holds', stages: ['ACTIVE'] },
  { id: 'DRAFT', label: 'Drafts', stages: ['DRAFT'] },
  { id: 'CLOSED', label: 'Released and Closed', stages: ['RELEASED', 'REJECTED', 'CANCELLED'] },
];

export type SpecialTab = 'APPROVAL' | 'PROCESS' | 'PUSHED' | 'CLOSED';

/** Work list tabs of the special remittance requests (MKTID.009, RMTID.030). */
export const SPECIAL_TABS: readonly { id: SpecialTab; label: string; stages: SpecialStage[] }[] = [
  { id: 'APPROVAL', label: 'For Approval', stages: ['REQUESTED', 'FOR_APPROVAL'] },
  { id: 'PROCESS', label: 'In Process Remittance', stages: ['IN_PROCESS_REMITTANCE'] },
  { id: 'PUSHED', label: 'Pushed to Disbursement', stages: ['PUSHED_TO_DISBURSEMENT'] },
  { id: 'CLOSED', label: 'Rejected and Returned', stages: ['REJECTED', 'RETURNED'] },
];

/** The tab of a batch work list named in the URL (Operations home tiles), else Review. */
export function batchTabOf(value: string | null): BatchTab {
  const tab = BATCH_TABS.find((t) => t.id === value || t.stages.some((s) => s === value));
  return tab?.id ?? 'REVIEW';
}

/** The stages of a tab. */
export function stagesOf<T extends string, S>(
  tabs: readonly { id: T; stages: S[] }[],
  tab: T,
): S[] {
  return tabs.find((t) => t.id === tab)?.stages ?? [];
}

/** Totals of the lines kept (the preview and the totals strip, RMTID.002). */
export function totalsOf(lines: readonly BatchLine[]): Amounts {
  const zero: Amounts = {
    paidAr: 0,
    commission: 0,
    commissionVat: 0,
    wtax: 0,
    dtip: 0,
    incentive: 0,
    incentiveVat: 0,
    netDue: 0,
    cpc2: 0,
    cpc2Vat: 0,
    payable: 0,
  };
  return lines
    .filter((l) => l.exclusion?.excluded !== true)
    .reduce<Amounts>(
      (sum, l) => ({
        paidAr: round(sum.paidAr + l.amounts.paidAr),
        commission: round(sum.commission + l.amounts.commission),
        commissionVat: round(sum.commissionVat + l.amounts.commissionVat),
        wtax: round(sum.wtax + l.amounts.wtax),
        dtip: round(sum.dtip + l.amounts.dtip),
        incentive: round(sum.incentive + l.amounts.incentive),
        incentiveVat: round(sum.incentiveVat + l.amounts.incentiveVat),
        netDue: round(sum.netDue + l.amounts.netDue),
        cpc2: round(sum.cpc2 + l.amounts.cpc2),
        cpc2Vat: round(sum.cpc2Vat + l.amounts.cpc2Vat),
        payable: round(sum.payable + l.amounts.payable),
      }),
      zero,
    );
}

function round(value: number): number {
  return Math.round(value * 100) / 100;
}

/** Whether a line is excluded now. */
export function isExcluded(line: BatchLine): boolean {
  return line.exclusion?.excluded === true;
}

/** Record flags of an invoice as chips (hold, pending adjustment, write-off, lock). */
export function dtipFlags(row: DtipRow): string[] {
  const flags: string[] = [];
  if (row.hold) {
    flags.push('On Hold');
  }
  if (row.pendingNegativeAdjustment) {
    flags.push('Pending Negative Adjustment');
  }
  if (row.writtenOff) {
    flags.push('Written Off');
  }
  if (row.lockOwner !== undefined) {
    flags.push(`Locked by ${row.lockOwner}`);
  }
  return flags;
}

/** Field errors of a hold request form (MKTID.003). */
export function holdFormErrors(
  form: { invoiceNo: string; reasonCode: string; holdUntil: string },
  today: string,
): Partial<Record<'invoiceNo' | 'reasonCode' | 'holdUntil', string>> {
  const errors: Partial<Record<'invoiceNo' | 'reasonCode' | 'holdUntil', string>> = {};
  if (form.invoiceNo.trim() === '') {
    errors.invoiceNo = 'Invoice No. is required';
  }
  if (form.reasonCode === '') {
    errors.reasonCode = 'Select a reason';
  }
  if (form.holdUntil === '') {
    errors.holdUntil = 'Hold Until is required';
  } else if (form.holdUntil <= today) {
    errors.holdUntil = 'Hold Until must be a future date';
  }
  return errors;
}

/** E-mail addresses of a comma or semicolon separated list, and the invalid ones. */
export function parseEmails(text: string): { valid: string[]; invalid: string[] } {
  const all = text
    .split(/[,;]/)
    .map((e) => e.trim())
    .filter((e) => e !== '');
  return {
    valid: all.filter(isEmail),
    invalid: all.filter((e) => !isEmail(e)),
  };
}

/** A plausible e-mail address: one @, a dot in the domain, no spaces. */
export function isEmail(value: string): boolean {
  const at = value.indexOf('@');
  const dot = value.lastIndexOf('.');
  return (
    !/\s/.test(value) &&
    at > 0 &&
    at === value.lastIndexOf('@') &&
    dot > at + 1 &&
    dot < value.length - 1
  );
}

/** Joins the parts that are present with a separator (descriptions, facts). */
export function joinParts(
  parts: readonly (string | false | null | undefined)[],
  separator = ' · ',
): string {
  return parts.filter((p): p is string => typeof p === 'string' && p !== '').join(separator);
}

/** The insurer OR schedule template (RMTID.012; layout parked, OQ22). */
export const OR_TEMPLATE = 'batchNo,invoiceNo,orNo,orDate,orAmount\n';

/** The Collection hold file template (COLLECTION_HOLD). */
export const HOLD_TEMPLATE = 'invoiceNo,reasonCode,holdUntil,remarks\n';

/** The Collection special remittance file template (COLLECTION_SPECIAL_REMIT). */
export const SPECIAL_TEMPLATE = 'invoiceNo,conditionCode,remarks\n';
