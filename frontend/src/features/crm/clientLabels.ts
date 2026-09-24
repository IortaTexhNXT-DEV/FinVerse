import type { ClientDetail } from '@/api/clients';
import type { SearchCriteria } from './ClientSearchPanel';

export type QuickFilter = 'ALL' | 'PROSPECT' | 'CONFIRMED' | 'KYC_DUE';

export const QUICK_FILTERS: { id: QuickFilter; label: string }[] = [
  { id: 'ALL', label: 'All clients' },
  { id: 'PROSPECT', label: 'Prospects' },
  { id: 'CONFIRMED', label: 'Confirmed' },
  { id: 'KYC_DUE', label: 'KYC review due' },
];

/** Search criteria with a quick filter applied on top (the quick filter wins). */
export function applyQuickFilter(criteria: SearchCriteria, quick: QuickFilter): SearchCriteria {
  switch (quick) {
    case 'PROSPECT':
    case 'CONFIRMED':
      return { ...criteria, status: quick };
    case 'KYC_DUE':
      return { ...criteria, kycDue: true };
    default:
      return criteria;
  }
}

/** Actions the client page offers, given the client and the user's permissions. */
export interface ClientActions {
  edit: boolean;
  submitKyc: boolean;
  verifyKyc: boolean;
  confirm: boolean;
  deactivate: boolean;
}

/**
 * Which onboarding actions apply (BRNB.090): submit the KYC as maker, verify it as a checker who
 * is not the maker (also the periodic review of a confirmed client), confirm once verified, and
 * deactivate while active. The server enforces the same rules.
 */
export function clientActions(
  c: ClientDetail,
  can: (permission: string) => boolean,
  username: string | undefined,
): ClientActions {
  const active = c.status !== 'INACTIVE';
  const stage = c.onboardingStage ?? 'PROSPECT';
  const maintain = can('CLIENT_MAINTAIN');
  const eitherRole = maintain || can('CLIENT_APPROVE');
  return {
    edit: active && maintain,
    submitKyc: active && maintain && stage === 'PROSPECT',
    verifyKyc: active && mayVerify(c, can, username),
    confirm: eitherRole && stage === 'KYC_VERIFIED' && c.status === 'PROSPECT',
    deactivate: active && eitherRole,
  };
}

/** A checker who is not the maker verifies a submitted KYC or reviews a confirmed client's KYC. */
function mayVerify(
  c: ClientDetail,
  can: (permission: string) => boolean,
  username: string | undefined,
): boolean {
  const maker = username === c.lifecycle.createdBy || username === c.kyc.submittedBy;
  if (maker || !can('CLIENT_APPROVE')) {
    return false;
  }
  if (c.onboardingStage === 'KYC_REVIEW') {
    return true;
  }
  return c.status === 'CONFIRMED' && c.kyc.status !== 'PENDING';
}

/** Labels of the fields of the client page. */
export function describeBank(c: ClientDetail): string {
  if (!c.bankClient) {
    return 'Non-bank client';
  }
  return c.bankCif === undefined ? 'BDO bank client' : `BDO bank client · CIF ${c.bankCif}`;
}
