import { humanize } from './format';

/** The business names of the modules that send or own work (never the internal module code). */
const MODULE_NAMES: Record<string, string> = {
  ACCOUNT: 'Accounts',
  ACSL: 'ACSL',
  ADJUSTMENT: 'Adjustment',
  BOOKING: 'Booking',
  CASHIERING: 'Cashiering',
  CLAIMS: 'Claims',
  COLLECTIONS: 'Collections',
  COMMISSION: 'Commission Receivables',
  DISBURSEMENT: 'Disbursement',
  FRBS: 'Service Fees',
  NB: 'New Business',
  OPERATIONS: 'Operations',
  OPSLEDGER: 'Operations',
  PAYREQUEST: 'Payment Requests',
  PLACEMENT: 'Placement',
  PRODRECON: 'Production Reconciliation',
  REMITTANCE: 'Remittance',
  RENEWAL: 'Renewal',
};

/** The name of a module from its code, e.g. PRODRECON → Production Reconciliation. */
export function moduleLabel(code: string | null | undefined): string {
  if (!code) {
    return '';
  }
  return MODULE_NAMES[code.toUpperCase()] ?? humanize(code);
}

/**
 * Document numbers users know: a prefix of letters, optional segments, a year or a run of digits
 * (ENR-2026-000007, BI-HO-2026-000008, RMB-INS-MGIC-2026-000003, SFR-2026-000001, I97000011).
 */
const DOCUMENT_NUMBER = /\b(?:[A-Z][A-Z0-9]*-(?:[A-Z0-9]+-)*\d{4,}(?:-\d+)*|I\d{6,})\b/g;

/**
 * The document numbers of an internal source reference, in words users read: "PAY:PAY-2026-000011"
 * becomes "PAY-2026-000011" and "RA:ADJUSTMENT:ADJ:ENR-2026-000007:BI-HO-2026-000008" becomes
 * "ENR-2026-000007 · BI-HO-2026-000008". Internal prefixes and record ids ("APP:5", "DSP:3") are
 * never shown; a reference without a document number gives the empty text.
 */
export function referenceText(ref: string | null | undefined): string {
  if (!ref) {
    return '';
  }
  const found: string[] = ref.match(DOCUMENT_NUMBER) ?? [];
  return found.filter((n, i) => found.indexOf(n) === i).join(' · ');
}

/** "Operations · RMB-2026-000001", or the module name alone when the reference has no number. */
export function sourceText(module: string | null | undefined, ref: string | null | undefined) {
  const name = moduleLabel(module);
  const number = referenceText(ref);
  if (name === '') {
    return number;
  }
  return number === '' ? name : `${name} · ${number}`;
}

/**
 * A storage folder in words: "REMITTANCE/WITH_INCENTIVES" becomes "Remittance › With Incentives"
 * and "FS04/CLX_APPLICATION_TO_INVOICE" becomes "FS04 › Collections Application to Invoice"; a
 * party code in the path (an insurer) is shown by the name `partyName` gives.
 */
export function folderLabel(
  folder: string | null | undefined,
  partyName: (code: string) => string = (code) => code,
): string {
  if (!folder) {
    return '';
  }
  return folder
    .split('/')
    .filter((part) => part !== '')
    .map((part, i) => {
      if (/\d/.test(part) && /^[A-Z0-9]+$/.test(part)) {
        return part;
      }
      if (/^[A-Z0-9]+(?:-[A-Z0-9]+)+$/.test(part)) {
        return partyName(part);
      }
      if (part.startsWith('CLX_')) {
        return `Collections ${humanize(part.slice(4))}`;
      }
      return i === 0 ? moduleLabel(part) : humanize(part);
    })
    .join(' › ');
}
