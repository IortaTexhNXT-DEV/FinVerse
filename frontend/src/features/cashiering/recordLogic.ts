import type { ReceiptKind } from './cashieringTypes';
import type {
  AccountRow,
  BankOption,
  EntryType,
  FormSettings,
  ReceiptRecord,
  RecordBody,
  TenderType,
} from './recordsApi';

/** One account of the record with the amount the cashier pays to it. */
export interface AccountLine {
  row: AccountRow;
  amount: string;
}

/** The Create AR / OR form (FRS.CSH.02.01.02, 02.02.02). */
export interface RecordForm {
  receiptKind: ReceiptKind;
  receiptType: string;
  branchId: string;
  entryType: EntryType;
  clientCode: string;
  clientName: string;
  insurerCode: string;
  insurerName: string;
  insurerBank: string;
  payorName: string;
  tenderType: TenderType;
  currency: string;
  bankAccount: string;
  amount: string;
  vat: string;
  wtax: string;
  certificateRef: string;
  checkNo: string;
  checkDate: string;
  checkBank: string;
  receiptDate: string;
  remarks: string;
  otherIncomeRef: string;
  accounts: AccountLine[];
}

/** AR types of non-premium payments (FRS.CSH.02.01.15). */
export const NON_PREMIUM_TYPES = ['REFUND', 'OTHER_EXPENSES', 'AR_INSURANCE'] as const;

/** The empty form of a receipt kind. */
export function emptyForm(
  kind: ReceiptKind,
  settings: FormSettings,
  branchId: number,
  baseCurrency: string,
): RecordForm {
  const branches = kind === 'OR' ? settings.orBranches : settings.arBranches;
  const branch = branches.find((b) => b.id === branchId) ?? branches[0];
  return {
    receiptKind: kind,
    receiptType: kind === 'AR' ? 'PREMIUM' : '',
    branchId: branch ? String(branch.id) : '',
    entryType: kind === 'AR' ? 'CLIENT' : 'INSURER',
    clientCode: '',
    clientName: '',
    insurerCode: '',
    insurerName: '',
    insurerBank: '',
    payorName: '',
    tenderType: 'CASH',
    currency: baseCurrency,
    bankAccount: defaultBankAccount(settings.bankAccounts, baseCurrency),
    amount: '',
    vat: '',
    wtax: '',
    certificateRef: '',
    checkNo: '',
    checkDate: '',
    checkBank: '',
    receiptDate: settings.today,
    remarks: '',
    otherIncomeRef: '',
    accounts: [],
  };
}

/** The bank account of a currency marked as its default (DFLB Savings for PHP, FX for USD). */
export function defaultBankAccount(banks: readonly BankOption[], currency: string): string {
  const own = banks.filter((b) => b.currency === currency);
  return (own.find((b) => b.defaultForCurrency) ?? own[0])?.code ?? '';
}

/** The currencies of the bank accounts of the list, in their order. */
export function currenciesOf(banks: readonly BankOption[]): string[] {
  return [...new Set(banks.map((b) => b.currency))];
}

const num = (value: string | number | undefined): number => {
  const n = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(n) ? n : 0;
};

/**
 * The excess of an account (FRS.CSH.02.01.03): what the paid amount leaves over the outstanding
 * balance less the PR 2307 kept for the BIR 2307 certificate; none for a pre-booked account.
 */
export function excessOf(line: AccountLine): number {
  if (line.row.prebooked || line.row.outstanding === undefined) {
    return 0;
  }
  const expected = num(line.row.outstanding) - num(line.row.pr2307);
  return Math.max(0, Math.round((num(line.amount) - expected) * 100) / 100);
}

/** Totals per receipt record (FRS.CSH.02.01.05): total paid and excess payments. */
export function recordTotals(lines: readonly AccountLine[]): { paid: number; excess: number } {
  const paid = lines.reduce((sum, l) => sum + num(l.amount), 0);
  const excess = lines.reduce((sum, l) => sum + excessOf(l), 0);
  return { paid: Math.round(paid * 100) / 100, excess: Math.round(excess * 100) / 100 };
}

/**
 * The required fields of the record with BDOI's messages (FRS.CSH.02.01.06 to 02.01.08), checked
 * before the record goes to the server, which checks them again with the currency of the accounts
 * and the holding period.
 */
export function recordErrors(form: RecordForm, settings: FormSettings): Record<string, string> {
  const errors: Record<string, string> = {};
  const need = (key: keyof RecordForm, label: string) => {
    const value = form[key];
    if (typeof value === 'string' && value.trim() === '') {
      errors[key] = `${label} is required`;
    }
  };
  need('receiptType', form.receiptKind === 'OR' ? 'OR Type' : 'AR Type');
  need('branchId', 'Receipting Branch');
  need('payorName', 'Payor Name');
  need('currency', 'Currency');
  need('bankAccount', 'Post to Bank Account');
  need('amount', 'Paid Amount');
  partyErrors(form, need);
  if (form.tenderType === 'CHECK') {
    need('checkNo', 'Check Number');
    need('checkDate', 'Check Date');
  }
  if (settings.remarksRequired) {
    need('remarks', 'Remarks');
  }
  return { ...errors, ...amountErrors(form, settings) };
}

function partyErrors(form: RecordForm, need: (key: keyof RecordForm, label: string) => void) {
  if (form.entryType === 'CLIENT' && form.clientName.trim() === '') {
    need('clientCode', 'Client');
  }
  if (form.entryType === 'INSURER' || isNonPremium(form)) {
    need('insurerCode', 'Insurer');
  }
}

function amountErrors(form: RecordForm, settings: FormSettings): Record<string, string> {
  const errors: Record<string, string> = {};
  const amount = num(form.amount);
  if (form.amount !== '' && (amount <= 0 || amount > settings.maxAmount)) {
    errors.amount = 'Paid Amount must be above zero and not more than 1,000,000,000.00';
  }
  if (form.accounts.length > 0 && recordTotals(form.accounts).paid !== amount) {
    errors.accounts = 'The amounts of the accounts must add up to the Paid Amount';
  }
  return errors;
}

/** Whether the form is a non-premium AR (the insurer pays). */
export function isNonPremium(form: RecordForm): boolean {
  return (
    form.receiptKind === 'AR' && (NON_PREMIUM_TYPES as readonly string[]).includes(form.receiptType)
  );
}

const blank = (value: string): string | undefined =>
  value.trim() === '' ? undefined : value.trim();

/** The record as the server takes it. */
export function bodyOf(companyId: number, form: RecordForm): RecordBody {
  const check = form.tenderType === 'CHECK';
  return {
    companyId,
    receiptKind: form.receiptKind,
    receiptType: blank(form.receiptType),
    branchId: form.branchId === '' ? undefined : Number(form.branchId),
    entryType: form.entryType,
    clientCode: blank(form.clientCode),
    clientName: blank(form.clientName),
    insurerCode: blank(form.insurerCode),
    insurerName: blank(form.insurerName),
    insurerBank: blank(form.insurerBank),
    payorName: form.payorName.trim(),
    tenderType: form.tenderType,
    currency: form.currency,
    bankAccount: blank(form.bankAccount),
    amount: form.amount === '' ? undefined : num(form.amount),
    vat: form.vat === '' ? undefined : num(form.vat),
    wtax: form.wtax === '' ? undefined : num(form.wtax),
    certificateRef: blank(form.certificateRef),
    checkNo: check ? blank(form.checkNo) : undefined,
    checkDate: check ? blank(form.checkDate) : undefined,
    checkBank: check ? blank(form.checkBank) : undefined,
    receiptDate: blank(form.receiptDate),
    remarks: form.remarks.trim(),
    otherIncomeRef: blank(form.otherIncomeRef),
    accounts: form.accounts.map((a) => ({ reference: a.row.invoiceNo, amount: num(a.amount) })),
  };
}

const text = (value: string | number | null | undefined): string =>
  value === undefined || value === null ? '' : String(value);

/** The form of a saved record (edit). */
export function formOf(
  record: ReceiptRecord,
  settings: FormSettings,
  baseCurrency: string,
): RecordForm {
  const p = record.party ?? {};
  const t = record.tender ?? {};
  const base = emptyForm(record.receiptKind, settings, record.branchId ?? 0, baseCurrency);
  return {
    ...base,
    receiptType: text(record.receiptType),
    branchId: record.branchId === undefined ? base.branchId : String(record.branchId),
    entryType: p.entryType ?? base.entryType,
    clientCode: text(p.clientCode),
    clientName: text(p.clientName),
    insurerCode: text(p.insurerCode),
    insurerName: text(p.insurerName),
    insurerBank: text(p.insurerBank),
    payorName: text(p.payorName),
    tenderType: t.tenderType ?? base.tenderType,
    currency: t.currency ?? base.currency,
    bankAccount: text(t.bankAccount),
    amount: text(t.amount),
    vat: text(t.vat),
    wtax: text(t.wtax),
    certificateRef: text(t.certificateRef),
    checkNo: text(t.check?.checkNo),
    checkDate: text(t.check?.checkDate),
    checkBank: text(t.check?.checkBank),
    receiptDate: t.receiptDate ?? base.receiptDate,
    remarks: text(t.remarks),
    otherIncomeRef: text(t.otherIncomeRef),
    accounts: record.accounts.map((a) => ({
      row: { invoiceNo: a.reference, arn: a.reference, bir2307: false, prebooked: false },
      amount: String(a.amount),
    })),
  };
}

/** Which actions the current user has on a record (the creator never posts or returns it). */
export function recordActions(
  record: ReceiptRecord,
  username: string | undefined,
  can: (permission: string) => boolean,
): { edit: boolean; submit: boolean; cancel: boolean; post: boolean } {
  const own =
    username !== undefined && record.createdByUser?.toLowerCase() === username.toLowerCase();
  const poster = can('CASH_APPROVE') && !own;
  return {
    edit: record.editable && own,
    submit: record.editable && own,
    cancel: record.stage === 'CREATED' && own,
    post: record.stage === 'FOR_POSTING' && poster,
  };
}
