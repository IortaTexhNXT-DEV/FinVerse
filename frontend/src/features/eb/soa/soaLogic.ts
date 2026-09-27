import type { SoaInput } from '@/api/ebMarket';

export const EMPTY_SOA: SoaInput = {
  insurerCode: '',
  insurerSoaNo: '',
  periodFrom: '',
  periodTo: '',
  amount: '',
  currency: 'PHP',
  receivedOn: '',
  remarks: '',
};

/** The errors of an SOA intake; empty when it may be registered. */
export function soaErrors(input: SoaInput, file: File | undefined): Record<string, string> {
  const errors: Record<string, string> = {};
  if (input.insurerCode === '') {
    errors.insurer = 'Select the insurer';
  }
  if (input.insurerSoaNo.trim() === '') {
    errors.soaNo = "Enter the insurer's SOA number";
  }
  if (input.periodFrom === '' || input.periodTo === '' || input.periodTo < input.periodFrom) {
    errors.period = 'Enter the period covered';
  }
  if (!/^\d+(\.\d{1,2})?$/.test(input.amount.trim())) {
    errors.amount = 'Enter the amount of the SOA';
  }
  if (!file) {
    errors.file = 'Attach the SOA';
  }
  return errors;
}
