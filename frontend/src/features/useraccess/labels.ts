import type { MismatchKind } from '@/api/userAccess';

/** The record types of User Access in the audit trail. */
export const UAM_AUDIT_SCOPE = [
  'AppUser',
  'Role',
  'AccessRequest',
  'SodRule',
  'IdentityEvent',
  'IdentityMismatch',
  'CertificationExtract',
  'SystemParameter',
].join(',');

/** Reconciliation mismatches in words. */
export const MISMATCH_LABELS: Record<MismatchKind, string> = {
  NOT_IN_DIRECTORY: 'Not in the directory',
  STATUS: 'Status differs',
  GROUP_PROFILES: 'Group profiles differ',
};
