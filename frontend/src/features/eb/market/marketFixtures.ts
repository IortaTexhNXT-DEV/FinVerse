import type { ProgrammeView } from '@/api/eb';
import type {
  ComparativeView,
  Confirmation,
  Franchise,
  MemberChange,
  Proposal,
  RequiredDocument,
  RosterVersion,
  Soa,
  ThresholdRule,
  Tor,
} from '@/api/ebMarket';
import { PROGRAMME } from '../programmes/fixtures';

/** Test data of the EB marketing and servicing screens. */
export function programmeAt(stage: string): ProgrammeView {
  return {
    ...PROGRAMME,
    cycles: PROGRAMME.cycles.map((c) => ({ ...c, stage })),
  };
}

export const FRANCHISE: Franchise = {
  id: 31,
  franchiseNo: 'EBF-2026-000031',
  cycleId: 11,
  insurerCode: 'INS-MGIC',
  insurerName: 'Metro General Insurance',
  status: 'SUBMITTED',
  submittedAt: '2026-09-02T02:00:00Z',
  dueDate: '2026-09-09',
};

export const TOR: Tor = {
  id: 41,
  versionNo: 1,
  status: 'DRAFT',
  items: [
    {
      id: 411,
      sortOrder: 1,
      benefitLine: 'HMO',
      planCode: 'P1',
      description: 'Room and board',
      requirement: 'Private room',
    },
  ],
};

export const PROPOSAL: Proposal = {
  id: 51,
  proposalNo: 'EBPR-2026-000051',
  cycleId: 11,
  insurerCode: 'INS-MGIC',
  insurerName: 'Metro General Insurance',
  kind: 'PROPOSAL',
  versionNo: 1,
  status: 'SUBMITTED',
  receivedOn: '2026-09-05',
  currency: 'PHP',
  attachmentId: 901,
  totalPremium: 1200000,
  lines: [{ benefitLine: 'HMO', planCode: 'P1', annualPremium: 1200000, sumInsured: 150000 }],
  items: [],
  factors: [],
};

export const COMPARATIVE: ComparativeView = {
  comparative: {
    id: 61,
    comparativeNo: 'EBCA-2026-000061',
    cycleId: 11,
    versionNo: 1,
    status: 'FOR_APPROVAL',
    dueDate: '2026-09-12',
    submittedBy: 'ebao',
  },
  programmeId: 7,
  programmeNo: 'EBP-2026-000007',
  programmeName: 'Group Health',
  clientName: 'Pacific Harbor Logistics Inc.',
  accountOfficer: 'ebao',
  cycleNo: 'EBC-2026-000011',
  cycleStage: 'FOR_SIGNOFF',
  summary: 'Metro offers the lowest premium.',
  lines: [{ benefitLine: 'HMO', recommendedProposalId: 51, lowestPremium: 1200000 }],
  matrix: {
    proposals: [
      {
        proposalId: 51,
        proposalNo: 'EBPR-2026-000051',
        insurerCode: 'INS-MGIC',
        insurerName: 'Metro General Insurance',
        kind: 'PROPOSAL',
        versionNo: 1,
        currency: 'PHP',
        totalPremium: 1200000,
      },
    ],
    lines: [
      {
        benefitLine: 'HMO',
        label: 'Health Maintenance Organization (HMO)',
        offers: {
          '51': {
            annualPremium: 1200000,
            sumInsured: 150000,
            plans: [{ planCode: 'P1', annualPremium: 1200000 }],
          },
        },
        lowestProposalId: 51,
        lowestPremium: 1200000,
      },
    ],
    items: [
      {
        torItemId: 411,
        benefitLine: 'HMO',
        description: 'Room and board',
        requirement: 'Private room',
        answers: { '51': { offeredValue: 'Semi-private', deviation: true } },
      },
    ],
    factors: [],
  },
  decisions: [],
  comments: [],
};

export const CONFIRMATION: Confirmation = {
  id: 71,
  cycleId: 11,
  comparativeId: 61,
  channel: 'EMAIL',
  confirmedOn: '2026-09-20',
  evidenceAttachmentId: 902,
  status: 'ACTIVE',
  recordedBy: 'ebao',
  recordedAt: '2026-09-20T02:00:00Z',
  lines: [
    {
      lineNo: 1,
      benefitLine: 'HMO',
      proposalId: 51,
      insurerCode: 'INS-MGIC',
      annualPremium: 1200000,
      sumInsured: 150000,
    },
  ],
};

export const ROSTER: RosterVersion = {
  id: 81,
  policyYear: 2027,
  versionNo: 2,
  sourceRef: 'bulk-81',
  status: 'STAGED',
  headcount: 121,
  loadedBy: 'ebao',
  loadedAt: '2026-09-10T02:00:00Z',
};

export const CHANGE: MemberChange = {
  id: 91,
  changeNo: 'EBMC-2026-000091',
  programmeId: 7,
  programmeNo: 'EBP-2026-000007',
  clientName: 'Pacific Harbor Logistics Inc.',
  lineNo: 1,
  benefitLine: 'HMO',
  policyYear: 2027,
  source: 'CLIENT',
  financial: true,
  directBilled: false,
  status: 'CAPTURED',
  createdBy: 'ebao',
  createdAt: '2026-09-11T02:00:00Z',
  lines: [
    {
      sortOrder: 1,
      action: 'ADD',
      employeeNo: 'E-1001',
      lastName: 'Santos',
      firstName: 'Ana',
      planCode: 'P1',
      effectiveDate: '2026-10-01',
    },
  ],
};

export const SOA: Soa = {
  id: 101,
  soaNo: 'EBSOA-2026-000101',
  programmeId: 7,
  programmeNo: 'EBP-2026-000007',
  clientName: 'Pacific Harbor Logistics Inc.',
  insurerCode: 'INS-MGIC',
  insurerName: 'Metro General Insurance',
  insurerSoaNo: 'MG-SOA-7788',
  periodFrom: '2026-08-01',
  periodTo: '2026-08-31',
  amount: 100000,
  currency: 'PHP',
  attachmentId: 903,
  status: 'RECEIVED',
  receivedOn: '2026-09-03',
  invoices: [{ invoiceNo: 'INV-2026-000001', paymentStatus: 'UNPAID' }],
};

export const RULE: ThresholdRule = {
  id: 111,
  measure: 'ANNUAL_PREMIUM',
  amount: 20000000,
  currency: 'PHP',
  approverPermission: 'EB_THRESHOLD_APPROVE',
  approvalLevel: 1,
  effectiveFrom: '2026-01-01',
  recordStatus: 'PENDING_AUTHORIZATION',
  maker: 'badmin2',
};

export const REQUIRED: RequiredDocument = {
  id: 121,
  processType: 'NB_PLACEMENT',
  documentType: 'EB_CLIENT_CONFIRMATION',
  mandatory: true,
  recordStatus: 'ACTIVE',
  maker: 'badmin',
};
