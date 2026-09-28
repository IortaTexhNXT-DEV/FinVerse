/**
 * Business wording of the Cashiering codes shown on screens (modes of payment, receipt sources,
 * application references): users read the words, never the codes.
 */
import { humanize } from '@/utils/format';

const MODES: Record<string, string> = {
  CASH: 'Cash',
  CHECK: 'Check',
  BILLS_PAYMENT: 'Bills Payment',
  TRADE: 'Trade',
  CLPC: 'CLPC',
  PDC: 'Post-dated Check',
  DIRECT_CREDIT: 'Direct Credit',
  ADA: 'Auto-debit Arrangement (ADA)',
  CREDIT_TO_ACCOUNT: 'Credit to Account',
  NON_CASH: 'Non-cash (settlement)',
};

const SOURCES: Record<string, string> = {
  OTC: 'Over the Counter',
  UPLOAD: 'Payment File',
  PDC: 'Matured Post-dated Check',
  PICKUP: 'Check Pick-up',
  SETTLEMENT: 'Settlement',
  COMMISSION_UPLOAD: 'Commission Payment File',
  CWT: 'BIR 2307',
  REINSTATEMENT: 'Reinstatement',
};

const ACTIONS: Record<string, string> = {
  CANCEL: 'Cancellation',
  REINSTATE_FULL: 'Full Reinstatement',
  REINSTATE_PARTIAL: 'Partial Reinstatement',
};

/** A receipt transaction in words: CANCEL → Cancellation. */
export function receiptActionLabel(action: string | null | undefined): string {
  return action ? (ACTIONS[action] ?? humanize(action)) : '';
}

/** The mode of payment in words: ADA → Auto-debit Arrangement (ADA), NON_CASH → Non-cash (settlement). */
export function modeLabel(mode: string | null | undefined): string {
  return mode ? (MODES[mode] ?? humanize(mode)) : '';
}

/** Where a receipt comes from, in words: OTC → Over the Counter, UPLOAD → Payment File. */
export function receiptSourceLabel(source: string | null | undefined): string {
  return source ? (SOURCES[source] ?? humanize(source)) : '';
}

const DISPOSITIONS: Record<string, string> = {
  APPLY_OTHER_INVOICE: 'Apply to Another Invoice',
  DST_APPLICATION: 'Apply to DST Only',
  REFUND: 'Refund to Payor',
  RECLASS: 'Reclassify to Another Client',
  TRANSFER_UNIT: 'Transfer to Another Unit',
  OTHERS: 'Other (Settled Outside the System)',
  HANDLING_FEE: 'Handling Fee Income',
};

/** A disposition type in words: REFUND → Refund to Payor. */
export function dispositionLabel(type: string | null | undefined): string {
  return type ? (DISPOSITIONS[type] ?? humanize(type)) : '';
}

const ORIGINS: Record<string, string> = {
  NO_MATCH: 'No Matching Account',
  EXCESS: 'Excess Payment',
  CANCELLED_REFERENCE: 'Payment on a Cancelled Booking',
  ADJUSTMENT: 'Excess from an Adjustment',
  CANCELLATION: 'Excess from a Cancellation',
  DP_REINSTATE: 'Direct Payment Reinstatement',
  REMITTANCE_RETURN: 'Returned by Remittance',
  REAPPLY: 'Released by Adjustment for Re-application',
  PREBOOKED: 'Released Pre-booked Payment',
  OTHER: 'Other',
  MIGRATED: 'Carried from Legacy',
};

/** Why a payment is unapplied, in words: REAPPLY → Released by Adjustment for Re-application. */
export function unappliedOriginLabel(origin: string | null | undefined): string {
  return origin ? (ORIGINS[origin] ?? humanize(origin)) : '';
}

const FILES: Record<string, string> = {
  PAY_BILLS: 'Bills Payment',
  PAY_TRADE: 'Trade Payment',
  PAY_CLPC: 'CLPC Payment',
  PAY_DIRECT_CREDIT: 'Direct Credit',
  PAY_PDC: 'Post-dated Checks',
  COMMISSION_PAYMENT: 'Commission Payments',
  CWT_TAGS: 'BIR 2307 Tags',
};

/** The payment file of an upload type in words: PAY_PDC → Post-dated Checks. */
export function fileLabel(handler: string | null | undefined): string {
  return handler ? (FILES[handler] ?? humanize(handler)) : '';
}

const LAYOUT_KINDS: Record<string, string> = {
  AUTO: 'Separator found from the header line',
  DELIMITED: 'Separated fields',
  FIXED_WIDTH: 'Fixed-width fields',
};

/** How the fields of a text file are laid out, in words. */
export function layoutKindLabel(kind: string | null | undefined): string {
  return kind ? (LAYOUT_KINDS[kind] ?? humanize(kind)) : '';
}
