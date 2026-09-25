import type { Deduction, DeductionInput, DeductionStage } from './deductionsApi';

/** Remittance deduction form, work list tabs and labels (ACSL 2.9.2). */

export type DeductionTab = 'CONFIRMATION' | 'OPEN' | 'DRAFT' | 'CLOSED';

/** Work list tabs of the deductions. */
export const DEDUCTION_TABS: readonly {
  id: DeductionTab;
  label: string;
  stages: DeductionStage[];
}[] = [
  { id: 'CONFIRMATION', label: 'For Confirmation', stages: ['FOR_CONFIRMATION'] },
  { id: 'OPEN', label: 'Confirmed', stages: ['CONFIRMED'] },
  { id: 'DRAFT', label: 'Drafts', stages: ['DRAFT'] },
  { id: 'CLOSED', label: 'Applied and Cancelled', stages: ['APPLIED', 'CANCELLED'] },
];

export interface DeductionForm {
  insurerCode: string;
  currency: string;
  sourceType: string;
  sourceRef: string;
  invoiceNo: string;
  amount: string;
  confirmationRef: string;
  confirmationDate: string;
  remarks: string;
}

export type DeductionErrors = Partial<Record<keyof DeductionForm, string>>;

export const EMPTY_DEDUCTION: DeductionForm = {
  insurerCode: '',
  currency: 'PHP',
  sourceType: '',
  sourceRef: '',
  invoiceNo: '',
  amount: '',
  confirmationRef: '',
  confirmationDate: '',
  remarks: '',
};

const MONEY = /^\d{1,17}(\.\d{1,2})?$/;

/** The form of an existing deduction. */
export function formOf(d: Deduction): DeductionForm {
  return {
    insurerCode: d.insurerCode,
    currency: d.currency,
    sourceType: d.sourceType,
    sourceRef: d.sourceRef,
    invoiceNo: d.invoiceNo ?? '',
    amount: d.amount.toFixed(2),
    confirmationRef: d.confirmationRef ?? '',
    confirmationDate: d.confirmationDate ?? '',
    remarks: d.remarks ?? '',
  };
}

/**
 * Field errors: insurer, currency, source and its reference and a positive amount are required;
 * the insurer's confirmation date cannot be in the future.
 */
export function deductionErrors(f: DeductionForm, today: string): DeductionErrors {
  const errors: DeductionErrors = {};
  if (f.insurerCode.trim() === '') {
    errors.insurerCode = 'Insurer code is required';
  }
  if (!/^[A-Z]{3}$/.test(f.currency)) {
    errors.currency = 'Choose the currency';
  }
  if (f.sourceType === '') {
    errors.sourceType = 'Choose what the deduction settles';
  }
  if (f.sourceRef.trim() === '') {
    errors.sourceRef = 'Source reference is required';
  }
  if (!MONEY.test(f.amount.trim()) || Number(f.amount) <= 0) {
    errors.amount = 'Enter a positive amount with at most 2 decimals';
  }
  if (f.confirmationDate !== '' && f.confirmationDate > today) {
    errors.confirmationDate = 'The confirmation date cannot be in the future';
  }
  return errors;
}

/** Whether a deduction can be submitted: the insurer's confirmation is recorded (ACSL 2.9.2). */
export function isConfirmedByInsurer(d: Deduction): boolean {
  return (d.confirmationRef ?? '') !== '' && (d.confirmationDate ?? '') !== '';
}

/** The request body of a form. */
export function inputOf(companyId: number, f: DeductionForm): DeductionInput {
  const optional = (v: string) => (v.trim() === '' ? undefined : v.trim());
  return {
    companyId,
    insurerCode: f.insurerCode.trim().toUpperCase(),
    currency: f.currency,
    sourceType: f.sourceType,
    sourceRef: f.sourceRef.trim(),
    invoiceNo: optional(f.invoiceNo),
    amount: Number(f.amount),
    confirmationRef: optional(f.confirmationRef),
    confirmationDate: optional(f.confirmationDate),
    remarks: optional(f.remarks),
  };
}

/** Applied part of a deduction in percent, for its progress. */
export function appliedPercent(d: Pick<Deduction, 'amount' | 'appliedAmount'>): number {
  if (d.amount <= 0) {
    return 0;
  }
  return Math.min(100, Math.round((d.appliedAmount / d.amount) * 100));
}
