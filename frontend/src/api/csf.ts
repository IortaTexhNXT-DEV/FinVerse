import { api, toQuery } from './client';
import type { DownloadedFile } from './client';
import type { PageResponse } from './types';

/**
 * Customer Servicing Facility API (BRD-9; docs/architecture/CUSTOMER_SERVICING_DESIGN.md sections
 * 5 and 10): customer search, the tabs of the Servicing View, caller verification, contact changes
 * and referrals, resends, documents and the Contact Changes list.
 */

export type SearchKeyType = 'NAME' | 'CLIENT_ID' | 'ACCOUNT_NO' | 'PN_NO' | 'APPLICATION_NO';

export interface AccountLine {
  id: number;
  arn: string;
  productCode: string;
  productName: string;
  lineCode: string;
  insurerCode: string | null;
  insurerName: string | null;
  stage: string;
  csfStatus: string | null;
  policyNumbers: string[];
  pnNumbers: string[];
  loanApplicationNo: string | null;
  periodFrom: string | null;
  periodTo: string | null;
  currency: string;
  balance: number | null;
  paymentStatus: string | null;
  marketSegment: string | null;
  ffy: boolean;
  directPayment: boolean;
}

export interface ClientHit {
  id: number;
  code: string;
  name: string;
  clientType: string;
  status: string;
  email: string | null;
  mobile: string | null;
  city: string | null;
  accounts: AccountLine[];
}

export interface LegacyAccount {
  source: string;
  reference: string;
  clientName: string;
  description: string;
}

export interface SearchResult {
  keyType: SearchKeyType;
  value: string;
  truncated: boolean;
  max: number;
  clients: ClientHit[];
  legacy: LegacyAccount[];
}

export interface ContactDetails {
  email: string | null;
  mobile: string | null;
  phone: string | null;
  addressLine: string | null;
  city: string | null;
  province: string | null;
  postalCode: string | null;
}

export interface CheckView {
  code: string;
  matched: boolean;
}

export interface Verification {
  id: number;
  channel: string;
  result: 'PASSED' | 'FAILED';
  matches: number;
  required: number;
  checks: CheckView[];
  verifiedAt: string;
  validUntil: string;
  agent: string;
}

export interface ClientBanner {
  clientId: number;
  clientCode: string;
  tags: { code: string; label: string }[];
  instructions: { id: number; type: string; typeLabel: string; text: string }[];
}

export interface ClientSummary {
  id: number;
  code: string;
  prospectCode: string;
  name: string;
  clientType: string;
  status: string;
  kycStatus: string;
  marketSegment: string | null;
  bankClient: boolean;
  contact: ContactDetails;
  banner: ClientBanner;
  accounts: number;
  verification: Verification | null;
}

export interface PaymentApplication {
  invoiceNo: string;
  arn: string;
  amount: number;
  invoiceBalance: number | null;
  invoicePaymentStatus: string | null;
}

export interface Payment {
  receiptNo: string;
  orNo: string | null;
  arNo: string | null;
  valueDate: string;
  mode: string | null;
  kind: 'PAYMENT' | 'REVERSAL' | 'LEGACY';
  amount: number;
  applications: PaymentApplication[];
}

export interface PaymentHistory {
  from: string;
  months: number;
  payments: Payment[];
}

export interface CsfDocument {
  id: number;
  fileName: string;
  documentType: string | null;
  contentType: string;
  sizeBytes: number;
  uploadedBy: string;
  uploadedAt: string;
  recordType: 'Client' | 'Account' | 'Quotation';
  reference: string;
}

export interface EpolicyRow {
  id: number;
  arn: string;
  fileName: string;
  policyNumbers: string[];
  status: string;
  receivedAt: string;
  dispatchCount: number;
  lastSentTo: string | null;
  lastSentAt: string | null;
  resendable: boolean;
}

export interface ChangeField {
  field: string;
  oldValue: string | null;
  newValue: string | null;
}

export interface ContactChange {
  id: number;
  changeNo: string;
  clientId: number;
  clientCode: string;
  clientName: string;
  status: 'APPLIED' | 'REFUSED' | 'REFERRED';
  at: string;
  agent: string;
  channel: string | null;
  reasonCode: string | null;
  remarks: string | null;
  verificationResult: string | null;
  verificationMatches: number | null;
  syncStatus: string;
  handoffStatus: string | null;
  fields: ChangeField[];
}

export interface ResendPreview {
  documentName: string;
  registeredEmail: string | null;
  subject: string;
  body: string;
  otherAllowed: boolean;
}

export interface ResendResult {
  recipient: string;
  messageId: number | null;
  documentName: string;
}

export type ResendKind = 'RA' | 'EPOLICY';

export interface VerifyBody {
  channel: string;
  checks: CheckView[];
  remarks?: string;
}

export interface ChangeBody {
  verificationId: number;
  reasonCode: string;
  remarks?: string;
  /** New value by field; an empty text clears the field, a missing field keeps it. */
  values: Record<string, string>;
}

export interface ReferralBody {
  channel: string;
  fields: Record<string, string>;
  remarks?: string;
}

export interface ChangeFilters {
  status?: string;
  agent?: string;
  from?: string;
  to?: string;
  q?: string;
}

const B = '/csf';
const client = (id: number) => `${B}/clients/${String(id)}`;
const co = (companyId: number) => toQuery({ companyId });

export const csfApi = {
  search: (companyId: number, keyType: SearchKeyType, q: string) =>
    api.get<SearchResult>(`${B}/search${toQuery({ companyId, keyType, q })}`),
  summary: (companyId: number, clientId: number) =>
    api.get<ClientSummary>(`${client(clientId)}${co(companyId)}`),
  accounts: (companyId: number, clientId: number) =>
    api.get<AccountLine[]>(`${client(clientId)}/accounts${co(companyId)}`),
  payments: (companyId: number, clientId: number, months?: number, arn?: string) =>
    api.get<PaymentHistory>(
      `${client(clientId)}/payments${toQuery({ companyId, months, arn: arn === '' ? undefined : arn })}`,
    ),
  renewalAdvices: (companyId: number, clientId: number) =>
    api.get<CsfDocument[]>(`${client(clientId)}/renewal-advices${co(companyId)}`),
  epolicies: (companyId: number, clientId: number) =>
    api.get<EpolicyRow[]>(`${client(clientId)}/epolicies${co(companyId)}`),
  documents: (companyId: number, clientId: number) =>
    api.get<CsfDocument[]>(`${client(clientId)}/documents${co(companyId)}`),
  history: (companyId: number, clientId: number) =>
    api.get<ContactChange[]>(`${client(clientId)}/contact-changes${co(companyId)}`),
  verify: (companyId: number, clientId: number, body: VerifyBody) =>
    api.post<Verification>(`${client(clientId)}/verifications${co(companyId)}`, body),
  change: (companyId: number, clientId: number, body: ChangeBody) =>
    api.post<ContactChange>(`${client(clientId)}/contact-changes${co(companyId)}`, body),
  refer: (companyId: number, clientId: number, body: ReferralBody) =>
    api.post<ContactChange>(`${client(clientId)}/referrals${co(companyId)}`, body),
  preview: (companyId: number, clientId: number, kind: ResendKind, documentId: number) =>
    api.get<ResendPreview>(
      `${client(clientId)}/resend-preview${toQuery({ companyId, kind, documentId })}`,
    ),
  resend: (
    companyId: number,
    clientId: number,
    kind: ResendKind,
    body: { documentId: number; recipient?: string; reason?: string },
  ) =>
    api.post<ResendResult>(
      `${client(clientId)}/${kind === 'RA' ? 'resend-advice' : 'resend-epolicy'}${co(companyId)}`,
      body,
    ),
  upload: (
    companyId: number,
    clientId: number,
    file: File,
    options: { accountId?: number; documentType: string; description?: string },
  ) => {
    const form = new FormData();
    form.append('documentType', options.documentType);
    if (options.accountId !== undefined) {
      form.append('accountId', String(options.accountId));
    }
    if (options.description) {
      form.append('description', options.description);
    }
    form.append('file', file);
    return api.upload<CsfDocument>(`${client(clientId)}/documents${co(companyId)}`, form);
  },
  download: (companyId: number, clientId: number, documentId: number): Promise<DownloadedFile> =>
    api.getFile(`${client(clientId)}/documents/${String(documentId)}/content${co(companyId)}`),
  zip: (companyId: number, clientId: number, ids: number[], name: string) =>
    api.getFile(
      `${client(clientId)}/documents/zip${toQuery({ companyId, ids: ids.join(','), name })}`,
    ),
  changes: (companyId: number, filters: ChangeFilters, page: number, size = 25) =>
    api.get<PageResponse<ContactChange>>(
      `${B}/contact-changes${toQuery({ companyId, ...filters, page, size })}`,
    ),
};
