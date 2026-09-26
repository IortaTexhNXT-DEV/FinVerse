import type {
  CashAdvanceInput,
  ExpenseInput,
  RefundInput,
  RefundLineInput,
  RequestKind,
  RequestStage,
} from './api';

/** Work list tabs of the requests (MKT 1.3.0, 1.18.0). */
export type StageTab = 'ALL' | RequestStage;

export const STAGE_TABS: readonly { id: StageTab; label: string }[] = [
  { id: 'ALL', label: 'All' },
  { id: 'DRAFT', label: 'Draft' },
  { id: 'PREPARING', label: 'Preparing' },
  { id: 'FOR_VALIDATION', label: 'For Validation' },
  { id: 'FOR_REVIEW', label: 'For Review' },
  { id: 'FOR_APPROVAL', label: 'For Approval' },
  { id: 'HR_APPROVAL', label: 'For HR Approval' },
  { id: 'SENT_TO_DISBURSEMENT', label: 'With Disbursement' },
  { id: 'DISBURSED', label: 'Disbursed' },
  { id: 'CANCELLED', label: 'Cancelled' },
];

const STAGES = new Set<string>(STAGE_TABS.map((t) => t.id));

/** The tab of a URL parameter (unknown values show every request). */
export function tabOf(value: string | null): StageTab {
  return value !== null && STAGES.has(value) ? (value as StageTab) : 'ALL';
}

export const KIND_LABELS: Record<RequestKind, string> = {
  REFUND: 'Refund (RRF)',
  CASH_ADVANCE: 'Cash Advance (RFP)',
  CHECK_CANCELLATION: 'Check Cancellation',
};

/** Business actions of the request workflows and the button each shows. */
export type BusinessAction = 'assign' | 'submit' | 'endorse' | 'approve';

const ACTIONS: Record<string, BusinessAction> = {
  assign: 'assign',
  submit: 'submit',
  submit_for_validation: 'submit',
  endorse: 'endorse',
  approve: 'approve',
};

export const ACTION_LABELS: Record<BusinessAction, string> = {
  assign: 'Assign Preparer',
  submit: 'Submit',
  endorse: 'Endorse for Approval',
  approve: 'Approve',
};

/** The distinct business buttons for the workflow actions the user may take now. */
export function businessActions(actions: readonly string[]): BusinessAction[] {
  return [
    ...new Set(actions.map((a) => ACTIONS[a]).filter((a): a is BusinessAction => a !== undefined)),
  ];
}

/** Label of the submit button: to ACSL and Cashiering first when validation is needed. */
export function submitLabel(kind: RequestKind, validationRequired: boolean): string {
  if (kind === 'REFUND' && validationRequired) {
    return 'Send for Validation';
  }
  return 'Submit for Review';
}

/** Label of the approve button: HR approval of a cash advance, else Marketing. */
export function approveLabel(stage: RequestStage, kind: RequestKind): string {
  if (stage === 'HR_APPROVAL') {
    return 'Approve (HR) and Send to Disbursement';
  }
  return kind === 'CASH_ADVANCE' ? 'Approve and Send to HR' : 'Approve and Send';
}

/** Field errors of a form, by field name. */
export type FieldErrors = Record<string, string>;

const AMOUNT = /^\d{1,15}(\.\d{1,2})?$/;
const ACCOUNT_NO = /^\d{10,16}$/;

/** Whether a text is a positive amount with at most two decimals. */
export function isAmount(text: string): boolean {
  return AMOUNT.test(text.trim()) && Number(text) > 0;
}

/** A refund line being edited (text inputs). */
export interface LineDraft {
  arNo: string;
  clientCode: string;
  assuredName: string;
  invoiceNo: string;
  amount: string;
  reasonCode: string;
  branchUnit: string;
  categoryA: string;
  categoryB: string;
}

export function emptyLine(clientCode = '', assuredName = ''): LineDraft {
  return {
    arNo: '',
    clientCode,
    assuredName,
    invoiceNo: '',
    amount: '',
    reasonCode: '',
    branchUnit: '',
    categoryA: '',
    categoryB: '',
  };
}

/** The payout part of a form. */
export interface PayoutDraft {
  paymentMode: string;
  accountNo: string;
  accountName: string;
}

function payoutErrors(p: PayoutDraft, errors: FieldErrors): void {
  if (p.paymentMode === '') {
    errors.paymentMode = 'Select the mode of payment';
  }
  if (p.paymentMode === 'CTA' && !ACCOUNT_NO.test(p.accountNo.trim())) {
    errors.accountNo = 'Enter the BDO account number (10 to 16 digits)';
  }
  if ((p.paymentMode === 'CTA' || p.paymentMode === 'CHECK') && p.accountName.trim() === '') {
    errors.accountName =
      p.paymentMode === 'CTA' ? 'Enter the account name' : 'Enter the name on the check';
  }
}

/** Checks a refund form (MKT 1.10.0, 2.23.0): one client, unique AR numbers, amounts, reasons. */
export function refundErrors(payout: PayoutDraft, lines: readonly LineDraft[]): FieldErrors {
  const errors: FieldErrors = {};
  payoutErrors(payout, errors);
  if (lines.length === 0) {
    errors.lines = 'Add at least one account';
  }
  const seen = new Set<string>();
  lines.forEach((l, i) => {
    const ar = l.arNo.trim().toUpperCase();
    if (ar === '') {
      errors[`arNo${String(i)}`] = 'Required';
    } else if (seen.has(ar)) {
      errors[`arNo${String(i)}`] = 'Duplicate AR';
    }
    seen.add(ar);
    if (!isAmount(l.amount)) {
      errors[`amount${String(i)}`] = 'Positive amount';
    }
    if (l.reasonCode === '') {
      errors[`reasonCode${String(i)}`] = 'Required';
    }
    if (l.clientCode.trim() === '' || l.assuredName.trim() === '') {
      errors[`clientCode${String(i)}`] = 'Client and assured required';
    }
  });
  const clients = new Set(lines.map((l) => l.clientCode.trim()).filter((c) => c !== ''));
  if (clients.size > 1) {
    errors.lines = 'All accounts of a refund request belong to one client';
  }
  return errors;
}

/** Total of the refund lines. */
export function linesTotal(lines: readonly LineDraft[]): number {
  return lines.reduce((sum, l) => sum + (isAmount(l.amount) ? Number(l.amount) : 0), 0);
}

function optional(text: string): string | undefined {
  const t = text.trim();
  return t === '' ? undefined : t;
}

/** The refund request body of a form. */
export function refundInput(
  header: { segment: string; referenceText: string; requestingUnit: string; purpose: string },
  payout: PayoutDraft,
  lines: readonly LineDraft[],
): RefundInput {
  return {
    segment: optional(header.segment),
    referenceText: optional(header.referenceText),
    requestingUnit: optional(header.requestingUnit),
    purpose: optional(header.purpose),
    currency: 'PHP',
    paymentMode: payout.paymentMode,
    accountNo: optional(payout.accountNo),
    accountName: optional(payout.accountName),
    lines: lines.map((l): RefundLineInput => ({
      arNo: l.arNo.trim(),
      clientCode: l.clientCode.trim(),
      assuredName: l.assuredName.trim(),
      invoiceNo: optional(l.invoiceNo),
      amount: Number(l.amount),
      reasonCode: l.reasonCode,
      branchUnit: optional(l.branchUnit),
      categoryA: optional(l.categoryA),
      categoryB: optional(l.categoryB),
      accountName: optional(payout.accountName),
    })),
  };
}

/** A cash-advance form being edited. */
export interface CashAdvanceDraft extends PayoutDraft {
  employeeNo: string;
  employeeName: string;
  rfpType: string;
  purpose: string;
  amount: string;
  segment: string;
  requestingUnit: string;
}

/** Checks a cash-advance form (Appendix D RFP). */
export function cashAdvanceErrors(d: CashAdvanceDraft): FieldErrors {
  const errors: FieldErrors = {};
  payoutErrors(d, errors);
  if (d.employeeNo.trim() === '') {
    errors.employeeNo = 'Enter the employee number';
  }
  if (d.employeeName.trim() === '') {
    errors.employeeName = 'Enter the employee name';
  }
  if (d.purpose.trim() === '') {
    errors.purpose = 'Enter the purpose';
  }
  if (!isAmount(d.amount)) {
    errors.amount = 'Enter a positive amount with at most two decimals';
  }
  return errors;
}

/** The cash-advance request body of a form. */
export function cashAdvanceInput(d: CashAdvanceDraft): CashAdvanceInput {
  return {
    segment: optional(d.segment),
    requestingUnit: optional(d.requestingUnit),
    rfpType: optional(d.rfpType) ?? 'CASH_ADVANCE',
    purpose: d.purpose.trim(),
    currency: 'PHP',
    employeeNo: d.employeeNo.trim(),
    employeeName: d.employeeName.trim(),
    paymentMode: d.paymentMode,
    accountNo: optional(d.accountNo),
    accountName: optional(d.accountName),
    amount: Number(d.amount),
  };
}

/** A fieldwork day being edited (text inputs). */
export interface DayDraft {
  fieldworkDate: string;
  particulars: string;
  perDiem: string;
  representation: string;
  transport: string;
  lodging: string;
  others: string;
}

const EXPENSES = ['perDiem', 'representation', 'transport', 'lodging', 'others'] as const;

function money(text: string): number {
  const t = text.trim();
  return t === '' ? 0 : Number(t);
}

/** Total of one day. */
export function dayTotal(d: DayDraft): number {
  return EXPENSES.reduce((sum, k) => sum + money(d[k]), 0);
}

/** Checks the fieldwork days of a liquidation. */
export function daysErrors(days: readonly DayDraft[]): FieldErrors {
  const errors: FieldErrors = {};
  days.forEach((d, i) => {
    if (d.fieldworkDate === '' || d.particulars.trim() === '') {
      errors[`day${String(i)}`] = 'Date and particulars are required';
    }
    const bad = EXPENSES.some(
      (k) => d[k].trim() !== '' && !/^\d{1,15}(\.\d{1,2})?$/.test(d[k].trim()),
    );
    if (bad) {
      errors[`day${String(i)}`] = 'Amounts are zero or positive with two decimals';
    }
  });
  return errors;
}

/** The liquidation days of a form. */
export function expenseInputs(days: readonly DayDraft[]): ExpenseInput[] {
  return days.map((d) => ({
    fieldworkDate: d.fieldworkDate,
    particulars: d.particulars.trim(),
    perDiem: money(d.perDiem),
    representation: money(d.representation),
    transport: money(d.transport),
    lodging: money(d.lodging),
    others: money(d.others),
  }));
}
