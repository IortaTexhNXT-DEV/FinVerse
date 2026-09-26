import { today } from '@/utils/format';
import type { Bank, Funding, FundingInput, TermsInput } from './api';
import { CREDIT_MODES } from './labels';

/** Form helpers of the voucher terms and the funding requests (DIS 2.7.4, 2.17.0). */

/** Field errors of the terms (DIS 2.7.4: mandatory and wrong fields are flagged). */
export function termsErrors(t: TermsInput, gross: number, type: string): Record<string, string> {
  const errors: Record<string, string> = {};
  if (t.bankAccountId <= 0) {
    errors.bank = 'Select the paying account';
  }
  if (CREDIT_MODES.includes(t.mode) && t.payeeAccountId === undefined) {
    errors.payeeAccount = 'Select the payee account credited';
  }
  if (!Number.isFinite(t.ewt) || t.ewt < 0 || t.ewt >= gross) {
    errors.ewt = 'The withholding tax must be at least zero and below the gross';
  }
  if (t.purpose.trim() === '') {
    errors.purpose = 'Enter the purpose';
  }
  if (t.valueDate === '') {
    errors.valueDate = 'Enter the value date';
  }
  if (type === 'OTHER' && (t.expenseAccount ?? '').trim() === '') {
    errors.expense = 'Enter the expense account of an other disbursement';
  }
  return errors;
}

/** The label of a BDOIR bank account in the funding screens. */
export function bankLabel(banks: readonly Bank[] | undefined, id: number): string {
  const bank = banks?.find((b) => b.id === id);
  return bank === undefined ? `#${id}` : `${bank.code} · ${bank.bankName} ${bank.accountNo}`;
}

export interface FundingForm {
  source: string;
  target: string;
  amount: string;
  currency: string;
  purpose: string;
  valueDate: string;
}

export const EMPTY_FUNDING: FundingForm = {
  source: '',
  target: '',
  amount: '',
  currency: 'PHP',
  purpose: '',
  valueDate: today(),
};

export function fundingFormOf(f: Funding): FundingForm {
  return {
    source: String(f.sourceBankAccountId),
    target: String(f.targetBankAccountId),
    amount: String(f.amount),
    currency: f.currency,
    purpose: f.purpose,
    valueDate: f.valueDate,
  };
}

/** Field errors of a funding request (DIS 2.17.0). */
export function fundingErrors(form: FundingForm): Record<string, string> {
  const errors: Record<string, string> = {};
  if (form.source === '') {
    errors.source = 'Select the source account';
  }
  if (form.target === '' || form.target === form.source) {
    errors.target = 'Select a target account other than the source';
  }
  const amount = Number(form.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    errors.amount = 'Enter an amount above zero';
  }
  if (form.purpose.trim() === '') {
    errors.purpose = 'Enter the purpose';
  }
  if (form.valueDate === '') {
    errors.valueDate = 'Enter the value date';
  }
  return errors;
}

export function fundingInputOf(form: FundingForm, companyId: number): FundingInput {
  return {
    companyId,
    sourceBankAccountId: Number(form.source),
    targetBankAccountId: Number(form.target),
    amount: Number(form.amount),
    currency: form.currency,
    purpose: form.purpose.trim(),
    valueDate: form.valueDate,
  };
}
