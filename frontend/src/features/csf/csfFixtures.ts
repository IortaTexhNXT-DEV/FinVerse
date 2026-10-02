import type {
  AccountLine,
  ClientSummary,
  ContactChange,
  CsfDocument,
  EpolicyRow,
  PaymentHistory,
  SearchResult,
  Verification,
} from '@/api/csf';
import type { PageResponse } from '@/api/types';

/** Test data of the Customer Service Facility screens. */
export function account(over: Partial<AccountLine> = {}): AccountLine {
  return {
    id: 11,
    arn: 'ARN-2026-900001',
    productCode: 'MTR10',
    productName: 'Motor Comprehensive',
    lineCode: 'MOTOR',
    insurerCode: 'INS-MGIC',
    insurerName: 'Mabuhay General Insurance Corp.',
    stage: 'BOOKED',
    csfStatus: 'OPEN',
    policyNumbers: ['MGIC-MC-2026-000123'],
    pnNumbers: ['PN-0441101'],
    loanApplicationNo: 'AL-2026-004411',
    periodFrom: '2026-10-01',
    periodTo: '2027-10-01',
    currency: 'PHP',
    balance: 1250.5,
    paymentStatus: 'PARTIALLY_PAID',
    marketSegment: 'CBG',
    ffy: true,
    directPayment: false,
    ...over,
  };
}

export function searchResult(over: Partial<SearchResult> = {}): SearchResult {
  return {
    keyType: 'NAME',
    value: 'Santos',
    truncated: false,
    max: 50,
    clients: [
      {
        id: 1,
        code: 'CL-2026-000001',
        name: 'Santos, Maria Clara Reyes',
        clientType: 'INDIVIDUAL',
        status: 'CONFIRMED',
        email: 'maria.santos@seed-client.ph',
        mobile: '09175550101',
        city: 'Makati',
        accounts: [account()],
      },
      {
        id: 2,
        code: 'CL-2026-900001',
        name: 'Santos, Maria Clara',
        clientType: 'INDIVIDUAL',
        status: 'CONFIRMED',
        email: null,
        mobile: null,
        city: null,
        accounts: [],
      },
    ],
    legacy: [],
    ...over,
  };
}

export function verification(result: 'PASSED' | 'FAILED' = 'PASSED'): Verification {
  return {
    id: 5,
    channel: 'HOTLINE',
    result,
    matches: result === 'PASSED' ? 3 : 1,
    required: 2,
    checks: [
      { code: 'ADDRESS', matched: true },
      { code: 'EMAIL', matched: result === 'PASSED' },
    ],
    verifiedAt: '2026-09-27T01:00:00Z',
    validUntil: '2099-09-27T01:30:00Z',
    agent: 'csfagent',
  };
}

export function summary(over: Partial<ClientSummary> = {}): ClientSummary {
  return {
    id: 1,
    code: 'CL-2026-000001',
    prospectCode: 'PR-2026-000001',
    name: 'Santos, Maria Clara Reyes',
    clientType: 'INDIVIDUAL',
    status: 'CONFIRMED',
    kycStatus: 'VERIFIED',
    marketSegment: 'CBG',
    bankClient: true,
    contact: {
      email: 'maria.santos@seed-client.ph',
      mobile: '09175550101',
      phone: null,
      addressLine: '21 Paseo de Roxas',
      city: 'Makati',
      province: 'Metro Manila',
      postalCode: '1226',
    },
    banner: {
      clientId: 1,
      clientCode: 'CL-2026-000001',
      tags: [{ code: 'BDO_EMPLOYEE', label: 'BDO employee' }],
      instructions: [{ id: 1, type: 'SERVICING', typeLabel: 'Servicing', text: 'Call after 3 pm' }],
    },
    accounts: 1,
    verification: null,
    ...over,
  };
}

export const PAYMENTS: PaymentHistory = {
  from: '2025-09-27',
  months: 12,
  payments: [
    {
      receiptNo: 'OR-2026-000010',
      orNo: 'OR-2026-000010',
      arNo: 'AR-2026-000011',
      valueDate: '2026-09-20',
      mode: 'CASH',
      kind: 'PAYMENT',
      amount: 5000,
      applications: [
        {
          invoiceNo: 'INV-2026-000001',
          arn: 'ARN-2026-900001',
          amount: 5000,
          invoiceBalance: 1250.5,
          invoicePaymentStatus: 'PARTIALLY_PAID',
        },
      ],
    },
  ],
};

export function document(over: Partial<CsfDocument> = {}): CsfDocument {
  return {
    id: 21,
    fileName: 'ARN-2026-900001_RENEWAL_ADVICE.pdf',
    documentType: 'RENEWAL_ADVICE',
    contentType: 'application/pdf',
    sizeBytes: 20480,
    uploadedBy: 'proc',
    uploadedAt: '2026-09-25T02:00:00Z',
    recordType: 'Account',
    reference: 'ARN-2026-900001',
    ...over,
  };
}

export const EPOLICY: EpolicyRow = {
  id: 31,
  arn: 'ARN-2026-900001',
  fileName: 'ARN-2026-900001_EPOLICY_1.pdf',
  policyNumbers: ['MGIC-MC-2026-000123'],
  status: 'CONFIRMED',
  receivedAt: '2026-09-21T02:00:00Z',
  dispatchCount: 1,
  lastSentTo: 'maria.santos@seed-client.ph',
  lastSentAt: '2026-09-22T02:00:00Z',
  resendable: true,
};

export function change(over: Partial<ContactChange> = {}): ContactChange {
  return {
    id: 41,
    changeNo: 'CSF-2026-000001',
    clientId: 1,
    clientCode: 'CL-2026-000001',
    clientName: 'Santos, Maria Clara Reyes',
    status: 'APPLIED',
    at: '2026-09-25T02:04:00Z',
    agent: 'csfagent',
    channel: 'HOTLINE',
    reasonCode: 'CLIENT_REQUEST',
    remarks: null,
    verificationResult: 'PASSED',
    verificationMatches: 3,
    syncStatus: 'NOT_CONFIGURED',
    handoffStatus: null,
    fields: [{ field: 'MOBILE', oldValue: '09175550199', newValue: '09175550101' }],
    ...over,
  };
}

export function page<T>(content: T[]): PageResponse<T> {
  return {
    content,
    page: 0,
    size: 25,
    totalElements: content.length,
    totalPages: 1,
  };
}
