import type {
  AllocationInput,
  AllocationMethod,
  PayerType,
  PdcInput,
  ReceiptInput,
  ReceiptMode,
} from '@/api/receivables';
import { today } from '@/utils/format';

/** State of the receipt entry form. */
export interface ReceiptForm {
  receiptDate: string;
  payerType: PayerType;
  partyCode: string;
  payerName: string;
  incomeAccountCode: string;
  mode: ReceiptMode;
  instrumentNo: string;
  instrumentDate: string;
  draweeBank: string;
  bankAccountCode: string;
  amount: number;
  method: AllocationMethod;
  narration: string;
}

/** Context of the entry: company, working branch and the bank account currency. */
export interface EntryContext {
  companyId: number;
  branchId: number;
  currency: string;
}

export function emptyReceiptForm(): ReceiptForm {
  return {
    receiptDate: today(),
    payerType: 'POLICYHOLDER',
    partyCode: '',
    payerName: '',
    incomeAccountCode: '4700',
    mode: 'CHEQUE',
    instrumentNo: '',
    instrumentDate: today(),
    draweeBank: '',
    bankAccountCode: '',
    amount: 0,
    method: 'MANUAL',
    narration: '',
  };
}

export function hasParty(form: ReceiptForm): boolean {
  return form.payerType !== 'OTHER';
}

export function isCheque(form: ReceiptForm): boolean {
  return form.mode === 'CHEQUE' || form.mode === 'PDC';
}

/** Currency of the selected bank account (PHP until one is chosen). */
export function bankCurrency(
  bankAccounts: { code: string; currency: string }[],
  code: string,
): string {
  return bankAccounts.find((b) => b.code === code)?.currency ?? 'PHP';
}

/** Whether the payer's open debit notes are shown for manual allocation (or the PDC's cover). */
export function showsAllocations(form: ReceiptForm): boolean {
  const manual = form.method === 'MANUAL' || form.mode === 'PDC';
  return hasParty(form) && form.partyCode !== '' && manual;
}

/** Allocation amounts keyed by debit item id as an API list. */
export function allocationList(allocations: Record<number, number>): AllocationInput[] {
  return Object.entries(allocations).map(([id, amount]) => ({ debitItemId: Number(id), amount }));
}

function orUndefined(value: string): string | undefined {
  return value === '' ? undefined : value;
}

/** Whether the form can be submitted (the server re-validates everything). */
export function canSave(form: ReceiptForm, allocationErrorCount: number): boolean {
  const payer = hasParty(form) ? form.partyCode : form.payerName;
  const cheque = !isCheque(form) || (form.instrumentNo !== '' && form.draweeBank !== '');
  return (
    form.amount > 0 &&
    form.bankAccountCode !== '' &&
    payer !== '' &&
    cheque &&
    allocationErrorCount === 0
  );
}

/** Request of an official receipt. */
export function toReceiptInput(
  form: ReceiptForm,
  ctx: EntryContext,
  allocations: Record<number, number>,
): ReceiptInput {
  const party = hasParty(form);
  return {
    companyId: ctx.companyId,
    branchId: ctx.branchId,
    receiptDate: form.receiptDate,
    payerType: form.payerType,
    partyCode: party ? form.partyCode : undefined,
    payerName: party ? undefined : form.payerName,
    incomeAccountCode: party ? undefined : form.incomeAccountCode,
    mode: form.mode,
    instrumentNo: orUndefined(form.instrumentNo),
    instrumentDate: isCheque(form) ? form.instrumentDate : undefined,
    draweeBank: orUndefined(form.draweeBank),
    currency: ctx.currency,
    amount: form.amount,
    bankAccountCode: form.bankAccountCode,
    allocationMethod: party ? form.method : 'NONE',
    narration: orUndefined(form.narration),
    allocations: party && form.method === 'MANUAL' ? allocationList(allocations) : [],
  };
}

/** Request registering a post-dated cheque (linked to the first selected debit note). */
export function toPdcInput(
  form: ReceiptForm,
  ctx: EntryContext,
  allocations: Record<number, number>,
): PdcInput {
  return {
    companyId: ctx.companyId,
    branchId: ctx.branchId,
    receivedDate: form.receiptDate,
    partyCode: form.partyCode,
    chequeNo: form.instrumentNo,
    chequeDate: form.instrumentDate,
    draweeBank: form.draweeBank,
    currency: ctx.currency,
    amount: form.amount,
    bankAccountCode: form.bankAccountCode,
    debitItemId: allocationList(allocations)[0]?.debitItemId,
    narration: orUndefined(form.narration),
  };
}
