import { api } from './client';
import type { Authorizable, Commission, Insurer, InsurerBranch } from './catalog';

const insurers = '/catalog/insurers';

/** VAT registration of an insurer: VAT on premium or premium tax. */
export type InsurerTaxStatus = 'VAT_REGISTERED' | 'NON_VAT';

export type RemittanceCycle = 'DAILY' | 'WEEKLY' | 'SEMI_MONTHLY' | 'MONTHLY';
export type RemittanceMode = 'CHECK' | 'FUND_TRANSFER' | 'ONLINE_BANKING';

/** Payment terms and remittance details of an insurer. */
export interface RemittanceTerms {
  paymentTerms?: string | null;
  cycle?: RemittanceCycle | null;
  mode?: RemittanceMode | null;
  email?: string | null;
  currency?: string | null;
}

export type ContactRole =
  'PLACEMENT' | 'CLAIMS' | 'ACCOUNTING' | 'REMITTANCE' | 'RELATIONSHIP' | 'OTHER';

export interface ContactInput {
  role: ContactRole;
  name: string;
  position?: string | null;
  email?: string | null;
  phone?: string | null;
}

/** Contact person of an insurer (maker-checker). */
export interface InsurerContact extends ContactInput, Authorizable {
  insurerId: number;
}

export type AccountPurpose = 'REMITTANCE' | 'REFUND' | 'COMMISSION';

export interface BankAccountInput {
  purpose: AccountPurpose;
  bankName: string;
  bankBranch?: string | null;
  accountNo: string;
  accountName: string;
  currency: string;
  primaryAccount: boolean;
}

/** Bank account of an insurer (maker-checker). */
export interface InsurerBankAccount extends BankAccountInput, Authorizable {
  insurerId: number;
}

/** A product the insurer is linked to: on a package version or by a commission rate. */
export interface LinkedProduct {
  productCode: string;
  productName: string;
  role: string | null;
  versionNo: number | null;
  versionStatus: string | null;
  commission: boolean;
}

/** An insurer with its branches, commissions, contacts, bank accounts and linked products. */
export interface InsurerDetail {
  insurer: Insurer;
  branches: InsurerBranch[];
  commissions: Commission[];
  contacts: InsurerContact[];
  bankAccounts: InsurerBankAccount[];
  products: LinkedProduct[];
}

/** The full insurer page (G08): remittance details, contacts and bank accounts, maker-checker. */
export const insurerPageApi = {
  updateRemittance: (insurerId: number, input: RemittanceTerms) =>
    api.put<Insurer>(`${insurers}/${insurerId}/remittance`, input),
  createContact: (insurerId: number, input: ContactInput) =>
    api.post<InsurerContact>(`${insurers}/${insurerId}/contacts`, input),
  updateContact: (contactId: number, input: ContactInput) =>
    api.put<InsurerContact>(`${insurers}/contacts/${contactId}`, input),
  createBankAccount: (insurerId: number, input: BankAccountInput) =>
    api.post<InsurerBankAccount>(`${insurers}/${insurerId}/bank-accounts`, input),
  updateBankAccount: (accountId: number, input: BankAccountInput) =>
    api.put<InsurerBankAccount>(`${insurers}/bank-accounts/${accountId}`, input),
};

/** A general ledger account posted for the insurer (read-only Accounts tab). */
export interface InsurerGlAccount {
  code: string;
  name: string;
  accountClass: string;
  controlAccount: boolean;
  subLedgerType: string;
  entries: number;
  debit: number;
  credit: number;
  balance: number;
  lastDate: string | null;
}

/** Sub-ledger open items of the insurer of one kind. */
export interface InsurerSubLedgerLine {
  documentType: string;
  direction: 'DEBIT' | 'CREDIT';
  currency: string;
  status: string;
  items: number;
  amount: number;
  settled: number;
  outstanding: number;
}

export interface InsurerAccounts {
  glAccounts: InsurerGlAccount[];
  subLedger: InsurerSubLedgerLine[];
}

/** A participating insurer of the insurer's default co-insurance arrangement (CRQ08). */
export interface ParticipationInput {
  participantCode: string;
  sharePct: number;
  lead: boolean;
  effectiveFrom: string;
  effectiveTo?: string | null;
}

/** A participation with its maker-checker status. */
export interface InsurerParticipation extends ParticipationInput, Authorizable {
  insurerId: number;
}

/** The arrangement: participations and the total of the authorized shares in force today. */
export interface CoinsuranceArrangement {
  participations: InsurerParticipation[];
  totalToday: number;
  complete: boolean;
}

/** Co-insurance / Risk Participation of an insurer (maker-checker). */
export const coinsuranceApi = {
  arrangement: (insurerId: number) =>
    api.get<CoinsuranceArrangement>(`${insurers}/${String(insurerId)}/coinsurance`),
  create: (insurerId: number, input: ParticipationInput) =>
    api.post<InsurerParticipation>(`${insurers}/${String(insurerId)}/coinsurance`, input),
  update: (id: number, input: ParticipationInput) =>
    api.put<InsurerParticipation>(`${insurers}/coinsurance/${String(id)}`, input),
};

/** The general ledger and sub-ledger accounts of an insurer (read-only). */
export const insurerAccountsApi = {
  accounts: (insurerId: number) =>
    api.get<InsurerAccounts>(`${insurers}/${String(insurerId)}/accounts`),
};
