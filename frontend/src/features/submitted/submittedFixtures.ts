import type {
  Approval,
  FeeView,
  IaafView,
  LetterView,
  PolicyDetail,
  PolicyRow,
  RenewalRow,
  TorView,
} from '@/api/submitted';

/** A page of rows. */
export const page = <T>(content: T[]) => ({
  content,
  page: 0,
  size: 50,
  totalElements: content.length,
  totalPages: 1,
});

/** A masterlist row. */
export function policyRow(over: Partial<PolicyRow> = {}): PolicyRow {
  return {
    id: 7,
    sbmNo: 'SBM-2026-000007',
    segment: 'CBG_MOTOR',
    businessType: 'NB',
    pnNo: 'PN-MTR-26005',
    policyNo: 'POL-MTR-26005',
    assuredName: 'Emilio Santos',
    insurerCode: 'INS-MGIC',
    inceptionDate: '2025-11-10',
    expiryDate: '2026-11-10',
    sumInsured: 6500000,
    currency: 'PHP',
    classification: 'SUBMITTED',
    bucket: 'FOR_RENEWAL',
    renewalTag: 'RENEWABLE',
    status: 'FOR_RENEWAL',
    handlerUsername: 'sbmhandler',
    conversionStatus: null,
    flags: ['RENEWABLE', 'INSURER_APPROVAL'],
    falloutReason: null,
    ...over,
  };
}

/** A masterlist record. */
export function policyDetail(over: Partial<PolicyRow> = {}): PolicyDetail {
  const row = policyRow(over);
  return {
    row,
    data: {
      segment: row.segment,
      businessType: row.businessType,
      loan: {
        pnNo: row.pnNo,
        loanApplicationNo: null,
        cif: 'CIF-12',
        valueDate: null,
        maturityDate: '2030-01-31',
        referringBranch: null,
        originatingUnit: 'Auto Loans Makati',
        borrowerName: row.assuredName,
      },
      assured: {
        assuredName: row.assuredName,
        mailingAddress: 'Makati City',
        telephone: null,
        mobile: '09171234567',
        email: 'emilio.santos@example.ph',
        bankCounterpartEmail: null,
      },
      terms: {
        insurerCode: row.insurerCode,
        policyNo: row.policyNo,
        inceptionDate: row.inceptionDate,
        expiryDate: row.expiryDate,
        coverageDays: 365,
        sumInsured: row.sumInsured,
        totalPremium: 97500,
        currency: 'PHP',
      },
      risk: {
        unitDescription: '2024 Toyota Land Cruiser Prado',
        serialNo: 'SN1',
        motorNo: 'MN1',
        colour: 'White',
        plateNo: 'NDE 5505',
        vehicleType: 'SUV',
        vehicleYear: 2023,
        propertyLocation: null,
        occupancy: null,
        mortgagee: 'BDO Unibank',
      },
      marks: { ffy: false, employeeAccount: false, noTouch: false },
    },
    outcome: {
      loanStatus: 'ACTIVE',
      amortised: true,
      bucketReason: null,
      raTemplate: 'GENERIC',
      renewalTagSource: 'RULE',
      renewalTagReason: null,
      renewalTaggedBy: null,
      renewalTaggedAt: null,
      lastRunNo: 'SBR-2026-000001',
      adequacyStatus: null,
      statusReason: null,
    },
    tracking: {
      handlerUsername: 'sbmhandler',
      aoUsername: 'ao',
      conversionStatus: null,
      opportunityTag: null,
      remarks: null,
    },
    origin: {
      sourceCode: 'LFS_INSURANCE',
      dateReceived: '2026-09-25',
      migrated: false,
      legacyRef: null,
      hasDocuments: false,
      createdBy: 'SYSTEM',
      createdAt: '2026-09-25T01:00:00Z',
    },
    renewal: { renewalRef: null, arn: null, bookedInvoiceNo: null, bookedOn: null },
  };
}

/** An approval in a status. */
export function approval(status: string, preparedBy = 'polreview'): Approval {
  return {
    status,
    currentLevel: 1,
    totalLevels: 1,
    submittedAt: '2026-09-26T01:00:00Z',
    preparedBy,
    preparedAt: '2026-09-26T00:00:00Z',
    returnReason: null,
    attachmentId: 5,
    levels: [
      {
        level: 1,
        permission: 'IAAF_APPROVE',
        approverUsername: 'sbmchecker',
        signatoryTitle: 'Checker',
      },
    ],
    signatures: [],
  };
}

/** An IAAF. */
export function iaaf(status = 'FOR_APPROVAL'): IaafView {
  return {
    id: 3,
    iaafNo: 'IAAF-2026-000003',
    policyId: 7,
    sbmNo: 'SBM-2026-000007',
    assuredName: 'Pacific Harbor Logistics Inc.',
    segment: 'NONCBG_CORPORATE',
    sumInsured: 45000000,
    sentTo: null,
    sentAt: null,
    approval: approval(status),
    reviews: [],
    links: [],
  };
}

/** A TOR. */
export function tor(status = 'FOR_APPROVAL'): TorView {
  return {
    id: 4,
    torNo: 'TOR-2026-000004',
    policyId: 7,
    arn: null,
    sbmNo: 'SBM-2026-000007',
    assuredName: 'Emilio Santos',
    breaches: 'Sum insured: 6,500,000.00 above the limit 5,000,000.00',
    proposedTerms: '20% deductible',
    aoUsername: 'ao',
    approvedAt: null,
    releasedAt: null,
    approval: approval(status, 'sbmhandler'),
  };
}

/** A hand-off. */
export function renewalRow(): RenewalRow {
  return {
    id: 9,
    policyId: 7,
    sbmNo: 'SBM-2026-000007',
    assuredName: 'Kristine Bautista',
    segment: 'CBG_FIRE',
    expiringInsurer: 'INS-VMI',
    expiryDate: '2026-11-16',
    sumInsured: 6500000,
    policyStatus: 'RENEWAL_IN_PROGRESS',
    handoffStatus: 'HANDED_OFF',
    manual: true,
    insurerAssigned: 'INS-VMI',
    raTemplate: 'GENERIC',
    renewalRef: 'RNW-2026-000031',
    arn: null,
    holdCoverOn: null,
    insurerAcceptedOn: null,
    reassignCount: 0,
    outcome: null,
    declineReason: null,
    handedOffAt: '2026-09-26T02:00:00Z',
    message: 'Renewal RNW-2026-000031 Unassigned Disposition',
    handlerUsername: 'firehandler',
    aoUsername: null,
  };
}

/** A letter. */
export function letter(status = 'FAILED'): LetterView {
  return {
    id: 11,
    letterNo: 'SBL-2026-000011',
    policyId: 7,
    sbmNo: 'SBM-2026-000007',
    letterType: 'REMINDER',
    channel: 'EMAIL',
    status,
    recipient: null,
    templateCode: 'SBM_REMINDER',
    templateVersion: 1,
    storedFileId: 20,
    printBatchId: null,
    error: 'No e-mail address',
    sentAt: null,
  };
}

/** A handling fee. */
export function fee(status = 'BILLED'): FeeView {
  return {
    id: 12,
    feeNo: 'SHF-2026-000012',
    policyId: 7,
    pnNo: 'PN-MTR-26001',
    locationRef: null,
    amount: 1120,
    currency: 'PHP',
    billingDate: '2026-09-22',
    status,
    unappliedRef: null,
    channel: null,
    ticketRef: null,
    ticketMessage: null,
    orNo: null,
    taggedBy: null,
    taggedAt: null,
    appliedAt: null,
    cancelReason: null,
    bulkJobNo: null,
  };
}
