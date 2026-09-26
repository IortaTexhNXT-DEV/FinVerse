import type { PremiumStatus } from '../cover/api';
import type { ClaimFlags, ClaimSource } from './api';

/**
 * Pure rules of the claim screens (FR-CL-011/016/020/021/033): form validation with the FRS
 * messages, amounts, premium labels and the flags of the summary card.
 */

/** The Record Claim form as typed by the user. */
export interface ClaimForm {
  arn: string;
  source: ClaimSource;
  lossDate: string;
  reportedDate: string;
  lossNature: string;
  claimType: string;
  lossDescription: string;
  lossPlace: string;
  catastropheCode: string;
  catastropheEvent: string;
  claimAmount: string;
  deductible: string;
  initialReserve: string;
  /** Insurer claim numbers typed on the insurer lines. */
  insurerClaimNos: string[];
}

export type ClaimFormErrors = Partial<Record<keyof ClaimForm, string>>;

/** A blank Record Claim form reported today. */
export function emptyClaimForm(today: string): ClaimForm {
  return {
    arn: '',
    source: 'BDOI_NOTICE',
    lossDate: '',
    reportedDate: today,
    lossNature: '',
    claimType: '',
    lossDescription: '',
    lossPlace: '',
    catastropheCode: '',
    catastropheEvent: '',
    claimAmount: '',
    deductible: '',
    initialReserve: '',
    insurerClaimNos: [],
  };
}

/** An optional amount: undefined when blank, NaN when not a number. */
export function toAmount(text: string): number | undefined {
  const value = text.trim().replaceAll(',', '');
  return value === '' ? undefined : Number(value);
}

/** Error of an optional amount field. */
export function amountError(text: string): string | undefined {
  const value = toAmount(text);
  if (value === undefined) {
    return undefined;
  }
  return Number.isNaN(value) || value < 0 ? 'Enter an amount of 0 or more' : undefined;
}

/** Errors of the loss dates (FR-CL-011/012). */
export function dateErrors(lossDate: string, reportedDate: string, today: string): ClaimFormErrors {
  const errors: ClaimFormErrors = {};
  if (lossDate === '' || lossDate > today) {
    errors.lossDate = 'Enter a loss date that is not in the future';
  }
  if (reportedDate === '' || reportedDate > today || (lossDate !== '' && reportedDate < lossDate)) {
    errors.reportedDate = 'The reported date must be between the loss date and today';
  }
  return errors;
}

function required(form: ClaimForm): ClaimFormErrors {
  const errors: ClaimFormErrors = {};
  if (form.arn.trim() === '') {
    errors.arn = 'Select the cover of the claim';
  }
  if (form.lossNature === '') {
    errors.lossNature = 'Select the nature of loss';
  }
  if (form.claimType === '') {
    errors.claimType = 'Select the claim type';
  }
  if (form.lossDescription.trim() === '') {
    errors.lossDescription = 'Enter the loss description';
  }
  if (form.catastropheCode === '' && form.catastropheEvent.trim() !== '') {
    errors.catastropheEvent = 'Select the catastrophe code of the event';
  }
  return errors;
}

/** Every error of the Record Claim form; empty when it can be saved. */
export function claimFormErrors(form: ClaimForm, today: string): ClaimFormErrors {
  const errors: ClaimFormErrors = {
    ...required(form),
    ...dateErrors(form.lossDate, form.reportedDate, today),
  };
  (['claimAmount', 'deductible', 'initialReserve'] as const).forEach((key) => {
    const error = amountError(form[key]);
    if (error !== undefined) {
      errors[key] = error;
    }
  });
  const numbered = form.insurerClaimNos.some((n) => n.trim() !== '');
  if (form.source === 'INSURER_REPORTED' && !numbered) {
    errors.insurerClaimNos = "Enter the insurer's claim number of an insurer-reported claim";
  }
  return errors;
}

/** Wording of a premium check result (BRCLM.001). */
export const PREMIUM_LABELS: Record<PremiumStatus, string> = {
  PAID: 'Paid',
  UNPAID: 'Unpaid premium',
  PARTIALLY_PAID: 'Partly paid premium',
  DIRECT_PAYMENT: 'Direct payment to the insurer',
  NO_INVOICE: 'No invoice',
};

/** Why the authorization code cannot be generated, or undefined when it can. */
export function authorizationBlock(
  status: PremiumStatus,
  dpPolicy: string,
  authorized: boolean,
): string | undefined {
  if (authorized) {
    return 'The authorization code was already generated';
  }
  if (status === 'PAID') {
    return undefined;
  }
  if (status === 'DIRECT_PAYMENT' && dpPolicy.toUpperCase() !== 'BLOCK') {
    return undefined;
  }
  return 'The premium is not fully paid: the authorization code cannot be generated';
}

/** The flag chips of the summary card, in display order (design 11). */
export function flagLabels(flags: ClaimFlags): string[] {
  const labels: [boolean, string][] = [
    [flags.unpaidPremium, 'Unpaid premium'],
    [flags.awaitingPremiumRemittance, 'Awaiting premium remittance'],
    [flags.newerCoverVersion, 'Newer cover version'],
    [flags.multiLocation, 'Multi-location'],
    [flags.multiInsurer, 'Multi-insurer'],
    [flags.catastrophe, 'CAT'],
    [flags.claimantOverridden, 'Claimant overridden'],
  ];
  return labels.filter(([on]) => on).map(([, label]) => label);
}

/** Label of a claim source. */
export const SOURCE_LABELS: Record<ClaimSource, string> = {
  BDOI_NOTICE: 'BDOI notice',
  INSURER_REPORTED: 'Insurer-reported',
  MIGRATED: 'Migrated',
};

/** Label of a policy year of a cover's term. */
export function policyYearLabel(year: number, from?: string, to?: string): string {
  return from === undefined || to === undefined
    ? `Year ${year}`
    : `Year ${year} (${from} to ${to})`;
}
