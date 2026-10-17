import { PARTY_TYPES } from '@/api/parties';
import type { PartyType } from '@/api/parties';

/**
 * Party types offered on the setup screens: the platform types plus the payee types of
 * Disbursement (DIS 2.2.2) - employees, government agencies and other payees.
 */
export const SETUP_PARTY_TYPES = [
  ...PARTY_TYPES,
  'EMPLOYEE',
  'GOVERNMENT',
  'OTHER_PAYEE',
] as unknown as readonly PartyType[];
