import type { RowAction } from '@/components/ui/RowActions';
import { displayNameOf } from '@/api/users';
import type { DialogField } from './FormDialog';
import { humanize } from '@/utils/format';
import type {
  AcslCase,
  CaseStage,
  CaseType,
  CorrectionLine,
  CorrectionStage,
  LineOrigin,
  OriginalLine,
  ReconBucket,
  Side,
} from './api';

/** Tabs of the cases board (ACSL 2.5.x). */
export type CaseTab = 'ALL' | CaseStage;

export const CASE_TABS: readonly { id: CaseTab; label: string }[] = [
  { id: 'ALL', label: 'All' },
  { id: 'RECEIVED', label: 'Received' },
  { id: 'ASSIGNED', label: 'Assigned' },
  { id: 'INVESTIGATING', label: 'Investigating' },
  { id: 'RESULT_PROVIDED', label: 'Result Provided' },
  { id: 'CORRECTION', label: 'Sent for Correction' },
];

/** Tabs of the corrections list (ACSL 2.7-2.15). */
export type CorrectionTab = 'ALL' | CorrectionStage;

export const CORRECTION_TABS: readonly { id: CorrectionTab; label: string }[] = [
  { id: 'ALL', label: 'All' },
  { id: 'ASSIGNED', label: 'To Assign' },
  { id: 'DRAFT', label: 'Draft' },
  { id: 'FOR_REVIEW', label: 'For Review' },
  { id: 'FOR_APPROVAL', label: 'For Approval' },
  { id: 'POSTED', label: 'Posted' },
  { id: 'CANCELLED', label: 'Cancelled' },
];

/** Tabs of an SOA reconciliation (ACSL 2.14.1): buckets and the upload log. */
export type SoaTab = 'ALL' | ReconBucket | 'LOG';

export const SOA_TABS: readonly { id: SoaTab; label: string }[] = [
  { id: 'ALL', label: 'All Lines' },
  { id: 'OUTSTANDING', label: 'Outstanding' },
  { id: 'FOR_REMITTANCE', label: 'For Remittance' },
  { id: 'REMITTED', label: 'Remitted' },
  { id: 'CANCELLED', label: 'Cancelled' },
  { id: 'DIRECT_BILLED', label: 'Direct Billed' },
  { id: 'NOT_FOUND', label: 'Not Found' },
  { id: 'LOG', label: 'Upload Log' },
];

export const CASE_TYPE_LABELS: Record<CaseType, string> = {
  INVESTIGATION: 'Investigation',
  ANALYSIS_REQUEST: 'Analysis Request',
  REFUND_APPLICATION: 'AR Refund Application',
  PAYMENT_REVERSAL: 'Payment Reversal',
};

/** The tab of a URL parameter (unknown values show everything). */
export function tabParam<T extends string>(
  value: string | null,
  tabs: readonly { id: T }[],
  fallback: T,
): T {
  const found = tabs.find((t) => t.id === value);
  return found ? found.id : fallback;
}

/** Totals of correction lines and whether they balance (ACSL 2.9.0). */
export function totals(lines: readonly Pick<CorrectionLine, 'side' | 'amount'>[]): {
  debit: number;
  credit: number;
  balanced: boolean;
} {
  const sum = (side: Side) =>
    lines
      .filter((l) => l.side === side)
      .reduce((s, l) => s + (Number.isFinite(l.amount) ? l.amount : 0), 0);
  const debit = Math.round(sum('DEBIT') * 100) / 100;
  const credit = Math.round(sum('CREDIT') * 100) / 100;
  return { debit, credit, balanced: lines.length >= 2 && debit === credit && debit > 0 };
}

/** A correction line being edited (text inputs). */
export interface LineDraft {
  accountCode: string;
  side: Side;
  amount: string;
  partyCode: string;
  invoiceNo: string;
  component: string;
  costCenter: string;
  narration: string;
  origin?: LineOrigin;
  originalBatchNo?: string;
  originalLineNo?: number;
}

export const emptyLine = (side: Side = 'DEBIT'): LineDraft => ({
  accountCode: '',
  side,
  amount: '',
  partyCode: '',
  invoiceNo: '',
  component: '',
  costCenter: '',
  narration: '',
});

export function toDraft(l: CorrectionLine): LineDraft {
  return {
    accountCode: l.accountCode,
    side: l.side,
    amount: String(l.amount),
    partyCode: l.partyCode ?? '',
    invoiceNo: l.invoiceNo ?? '',
    component: l.component ?? '',
    costCenter: l.costCenter ?? '',
    narration: l.narration ?? '',
    origin: l.origin,
    originalBatchNo: l.originalBatchNo,
    originalLineNo: l.originalLineNo,
  };
}

/** The trimmed text, or undefined when blank. */
export const optionalText = (t: string | undefined): string | undefined =>
  t === undefined || t.trim() === '' ? undefined : t.trim();

const blank = optionalText;

export function toLine(d: LineDraft): CorrectionLine {
  return {
    accountCode: d.accountCode.trim(),
    side: d.side,
    amount: Number(d.amount),
    partyCode: blank(d.partyCode),
    invoiceNo: blank(d.invoiceNo),
    component: blank(d.component),
    costCenter: blank(d.costCenter),
    narration: blank(d.narration),
    origin: d.origin,
    originalBatchNo: d.originalBatchNo,
    originalLineNo: d.originalLineNo,
  };
}

/** Errors of the line drafts, by line index (account and amount). */
export function lineErrors(lines: readonly LineDraft[]): Record<number, string> {
  const errors: Record<number, string> = {};
  lines.forEach((l, i) => {
    if (l.accountCode.trim() === '') {
      errors[i] = 'Enter the GL account';
    } else if (!/^\d{1,15}(\.\d{1,2})?$/.test(l.amount.trim()) || Number(l.amount) <= 0) {
      errors[i] = 'Enter a positive amount with at most two decimals';
    } else if (l.component.trim() !== '' && l.invoiceNo.trim() === '') {
      errors[i] = 'A ledger component needs the invoice';
    }
  });
  return errors;
}

/** Reverses an original line and re-posts it to another account (ACSL 2.9.1), as drafts. */
export function wrongAccountDrafts(
  o: OriginalLine,
  targetAccount: string,
  invoiceNo: string,
): LineDraft[] {
  const opposite: Side = o.side === 'DEBIT' ? 'CREDIT' : 'DEBIT';
  const base = {
    amount: String(o.amount),
    partyCode: o.partyCode ?? '',
    invoiceNo,
    component: '',
    costCenter: o.costCenter ?? '',
    originalBatchNo: o.batchNo,
    originalLineNo: o.lineNo,
  };
  return [
    {
      ...base,
      accountCode: o.accountCode,
      side: opposite,
      narration: `Reversal of ${o.batchNo} line ${String(o.lineNo)}`,
      origin: 'REVERSAL',
    },
    {
      ...base,
      accountCode: targetAccount,
      side: o.side,
      narration: `Re-post of ${o.batchNo} line ${String(o.lineNo)}`,
      origin: 'REPOST',
    },
  ];
}

/** Business buttons of the correction workflow. */
export type CorrectionAction = 'assign' | 'submit' | 'endorse' | 'approve';

const CORRECTION_ACTIONS: Record<string, CorrectionAction> = {
  assign: 'assign',
  submit: 'submit',
  endorse: 'endorse',
  approve: 'approve',
};

export const CORRECTION_LABELS: Record<CorrectionAction, string> = {
  assign: 'Assign Preparer',
  submit: 'Submit for Review',
  endorse: 'Endorse for Approval',
  approve: 'Approve and Post',
};

export function correctionActions(actions: readonly string[]): CorrectionAction[] {
  return [
    ...new Set(
      actions
        .map((a) => CORRECTION_ACTIONS[a])
        .filter((a): a is CorrectionAction => a !== undefined),
    ),
  ];
}

/** Whether a variance is worth highlighting (not zero). */
export function hasVariance(value: number | undefined): boolean {
  return value !== undefined && Math.abs(value) >= 0.005;
}

/** Labels of the kinds of correction, as the dialogs offer them. */
export const CORRECTION_KINDS: readonly { value: string; label: string }[] = [
  { value: 'WRONG_ACCOUNT', label: 'Posting to a wrong GL account' },
  { value: 'AMOUNT', label: 'Wrong amount' },
  { value: 'RECLASS', label: 'Reclassification' },
  { value: 'OTHER', label: 'Other correction' },
];

/** The kind of a correction in words. */
export function correctionKind(kind: string): string {
  return CORRECTION_KINDS.find((k) => k.value === kind)?.label ?? kind;
}

/**
 * The user a case or a correction is assigned to, chosen by name among the ACSL users who may
 * process the work (never typed as a user ID).
 */
export function assigneeField(label: string, users: readonly string[]): DialogField {
  const options = users
    .map((u) => ({ value: u, label: displayNameOf(u) || u }))
    .sort((a, b) => a.label.localeCompare(b.label));
  return { key: 'username', label, required: true, options };
}

/** The row menu of a posted line of the invoice family: the preparer corrects it in the draft. */
export function originalLineActions(editable: boolean, correct: () => void): RowAction[] {
  return editable ? [{ label: 'Correct', onSelect: correct }] : [];
}

type SoaKey = 'insurer' | 'from' | 'to' | 'file';

/** What is missing on an insurer statement upload; the insurer is chosen by name. */
export function soaUploadProblems(
  insurer: string,
  from: string,
  to: string,
  file: File | undefined,
) {
  const found: Partial<Record<SoaKey, string>> = {};
  if (insurer.trim() === '') {
    found.insurer = 'Select the insurer';
  }
  if (from === '') {
    found.from = 'Enter the start of the period';
  }
  if (to === '' || (from !== '' && to < from)) {
    found.to = 'Enter an end on or after the start';
  }
  if (!file) {
    found.file = 'Choose the statement file';
  }
  return found;
}

/**
 * The request facts of a case; the insurer is shown by name (the given lookup), never by its code.
 */
export function caseRequestFacts(
  c: AcslCase,
  insurerName: (code: string) => string,
): [string, string | undefined][] {
  return [
    ['Subject', c.subject],
    ['Details', c.details],
    ['Requesting Module', c.requesterModule ? humanize(c.requesterModule) : undefined],
    ['Requester Reference', c.requesterRef],
    ['Client', c.account.clientCode],
    ['Insurer', c.account.insurerCode ? insurerName(c.account.insurerCode) : undefined],
    ['Root Invoice', c.account.rootInvoiceNo],
  ];
}

const LINE_ORIGINS: Record<LineOrigin, string> = {
  REVERSAL: 'Reversal',
  REPOST: 'Re-post',
  MANUAL: 'Added by the preparer',
};

/** Where a correction line comes from, in words. */
export function lineOriginLabel(origin: LineOrigin): string {
  return LINE_ORIGINS[origin];
}

/** A comma-separated list of codes (BASIC,PREMIUM_TAX_VAT) in words; a dash when there is none. */
export function codesInWords(codes: string | null | undefined): string {
  if (!codes) {
    return '—';
  }
  return codes
    .split(',')
    .map((c) => humanize(c.trim()))
    .join(', ');
}
