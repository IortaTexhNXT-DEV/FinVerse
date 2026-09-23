import type { Party, PartyInput, PartyType } from '@/api/parties';

export type PartyForm = Partial<Party>;

const INTERMEDIARIES: readonly PartyType[] = ['AGENT', 'BROKER'];
const WITHHOLDING: readonly PartyType[] = ['AGENT', 'BROKER', 'SUPPLIER', 'GARAGE', 'SURVEYOR'];
const CODE_PATTERN = /^[A-Z0-9-]+$/;
const MAX_CREDIT_DAYS = 365;

/** Whether commission and licence fields apply to a party type. */
export function isIntermediary(type: PartyType | undefined): boolean {
  return type !== undefined && INTERMEDIARIES.includes(type);
}

/** Whether a withholding tax rate applies to a party type (payees). */
export function hasWithholding(type: PartyType | undefined): boolean {
  return type !== undefined && WITHHOLDING.includes(type);
}

/** New party defaults. */
export function emptyParty(companyId: number, baseCurrency = 'PHP'): PartyForm {
  return {
    companyId,
    partyType: 'CORPORATE_CLIENT',
    defaultCurrency: baseCurrency,
    creditDays: 30,
  };
}

function percentError(value: number | undefined, label: string): string | undefined {
  if (value === undefined) {
    return undefined;
  }
  return value < 0 || value > 100 ? `${label} must be between 0 and 100` : undefined;
}

function codeError(code: string): string | undefined {
  if (code === '') {
    return 'Code is required';
  }
  return CODE_PATTERN.test(code) ? undefined : 'Use capital letters, digits and dashes only';
}

function creditDaysError(days: number): string | undefined {
  return days < 0 || days > MAX_CREDIT_DAYS ? 'Credit days must be between 0 and 365' : undefined;
}

function isEmail(email: string): boolean {
  const at = email.indexOf('@');
  const dot = email.lastIndexOf('.');
  return at > 0 && dot > at + 1 && dot < email.length - 1 && !/\s/.test(email);
}

function emailError(email: string | undefined): string | undefined {
  return email && !isEmail(email) ? 'Invalid e-mail address' : undefined;
}

/** Client-side validation mirroring the API rules; returns messages by field. */
export function validateParty(form: PartyForm): Record<string, string> {
  const checks: Record<string, string | undefined> = {
    code: codeError((form.code ?? '').trim()),
    name: (form.name ?? '').trim() === '' ? 'Name is required' : undefined,
    defaultCurrency: /^[A-Z]{3}$/.test(form.defaultCurrency ?? '')
      ? undefined
      : 'Three-letter ISO currency code',
    creditDays: creditDaysError(form.creditDays ?? 0),
    email: emailError(form.email),
    commissionRate: percentError(form.commissionRate, 'Commission rate'),
    withholdingTaxRate: percentError(form.withholdingTaxRate, 'Withholding tax rate'),
  };
  const errors: Record<string, string> = {};
  Object.entries(checks).forEach(([field, message]) => {
    if (message !== undefined) {
      errors[field] = message;
    }
  });
  return errors;
}

function blankToUndefined(value: string | undefined): string | undefined {
  return value === undefined || value.trim() === '' ? undefined : value.trim();
}

/** Converts the form to the API request, dropping fields that do not apply to the party type. */
export function toPartyInput(form: PartyForm): PartyInput {
  const type = form.partyType ?? 'CORPORATE_CLIENT';
  return {
    companyId: form.companyId ?? 0,
    code: (form.code ?? '').trim(),
    name: (form.name ?? '').trim(),
    partyType: type,
    taxId: blankToUndefined(form.taxId),
    address: blankToUndefined(form.address),
    email: blankToUndefined(form.email),
    phone: blankToUndefined(form.phone),
    defaultCurrency: form.defaultCurrency ?? 'PHP',
    creditDays: form.creditDays ?? 0,
    commissionRate: isIntermediary(type) ? form.commissionRate : undefined,
    withholdingTaxRate: hasWithholding(type) ? form.withholdingTaxRate : undefined,
    licenceNo: isIntermediary(type) ? blankToUndefined(form.licenceNo) : undefined,
    bankName: blankToUndefined(form.bankName),
    bankAccountNo: blankToUndefined(form.bankAccountNo),
    branchId: form.branchId,
  };
}
