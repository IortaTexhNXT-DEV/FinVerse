import type { ClaimInput, ClaimPartyInput, PolicyCover } from '@/api/claims';

/** First notification of loss (FNOL) form model, validation and request mapping. */

export interface ClaimForm {
  policyNo: string;
  lossDate: string;
  reportedDate: string;
  riskLineNo?: number;
  natureOfLoss: string;
  causeOfLoss: string;
  lossLocation: string;
  description: string;
  claimantCode?: string;
  surveyorCode?: string;
  thirdPartyCode?: string;
  garageCode?: string;
  initialLossReserve?: number;
  initialExpenseReserve?: number;
}

/** Usual natures of loss offered on the form (free text is also accepted by the server). */
export const NATURES_OF_LOSS = [
  'Fire',
  'Collision',
  'Theft',
  'Flood',
  'Typhoon',
  'Earthquake',
  'Cargo damage',
  'Machinery breakdown',
  'Accidental injury',
  'Hospitalisation',
  'Third-party injury',
  'Contract default',
];

/** Empty form: loss and notification today, first risk. */
export function newClaimForm(today: string): ClaimForm {
  return {
    policyNo: '',
    lossDate: today,
    reportedDate: today,
    riskLineNo: 1,
    natureOfLoss: '',
    causeOfLoss: '',
    lossLocation: '',
    description: '',
  };
}

function required(form: ClaimForm): string[] {
  const missing: [string, string][] = [
    [form.policyNo, 'Enter the policy number'],
    [form.natureOfLoss, 'Select the nature of loss'],
    [form.causeOfLoss, 'Enter the cause of loss'],
    [form.lossLocation, 'Enter the place of loss'],
    [form.description, 'Describe the loss'],
  ];
  return missing.filter(([value]) => value.trim() === '').map(([, message]) => message);
}

/**
 * Validates the form against the looked-up policy.
 *
 * @param form form
 * @param cover policy found for the number and loss date (undefined until looked up)
 * @param today today's date (ISO)
 * @returns messages, empty when the form can be submitted
 */
export function validateClaim(form: ClaimForm, cover: PolicyCover | undefined, today: string) {
  const errors = required(form);
  if (form.lossDate === '' || form.reportedDate === '') {
    errors.push('Enter the dates of loss and notification');
  } else if (form.reportedDate < form.lossDate) {
    errors.push('The notification cannot be before the loss');
  } else if (form.reportedDate > today) {
    errors.push('The notification date cannot be in the future');
  }
  if (cover === undefined) {
    errors.push('Look up the policy first');
  } else if (!cover.inForce) {
    errors.push(`Policy ${cover.policyNo} is not in force on the date of loss`);
  }
  if ((form.initialLossReserve ?? 0) < 0 || (form.initialExpenseReserve ?? 0) < 0) {
    errors.push('Reserves cannot be negative');
  }
  return errors;
}

function parties(form: ClaimForm): ClaimPartyInput[] {
  const list: [ClaimPartyInput['role'], string | undefined][] = [
    ['SURVEYOR', form.surveyorCode],
    ['THIRD_PARTY', form.thirdPartyCode],
    ['GARAGE', form.garageCode],
  ];
  return list
    .filter((entry): entry is [ClaimPartyInput['role'], string] => (entry[1] ?? '') !== '')
    .map(([role, partyCode]) => ({ role, partyCode }));
}

function positive(value: number | undefined): number | undefined {
  return value !== undefined && value > 0 ? value : undefined;
}

/** Registration request of a validated form. */
export function toClaimInput(form: ClaimForm, companyId: number, cover: PolicyCover): ClaimInput {
  return {
    companyId,
    policyNo: cover.policyNo,
    riskLineNo: form.riskLineNo,
    lossDate: form.lossDate,
    reportedDate: form.reportedDate,
    natureOfLoss: form.natureOfLoss.trim(),
    causeOfLoss: form.causeOfLoss.trim(),
    lossLocation: form.lossLocation.trim(),
    description: form.description.trim(),
    currency: cover.currency,
    claimantCode: form.claimantCode === '' ? undefined : form.claimantCode,
    parties: parties(form),
    initialLossReserve: positive(form.initialLossReserve),
    initialExpenseReserve: positive(form.initialExpenseReserve),
  };
}
