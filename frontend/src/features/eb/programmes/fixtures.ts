import type { ItemRow, ProgrammeRow, ProgrammeView } from '@/api/eb';
import type { WorkCaseDetail } from '@/api/workflow';

/** Test data of the Employee Benefits screens. */
export const page = <T>(content: T[]) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
});

export const ROW: ProgrammeRow = {
  id: 7,
  programmeNo: 'EBP-2026-000007',
  clientId: 3,
  clientCode: 'CL-2026-000003',
  clientName: 'Pacific Harbor Logistics Inc.',
  name: 'Group Health',
  teamCode: 'BDO',
  funding: 'EMPLOYER',
  accountOfficer: 'ebao',
  status: 'ACTIVE',
  renewalEligible: true,
  lines: 'HMO, GLI',
  nextExpiry: '2027-01-10',
  cycle: {
    id: 11,
    cycleNo: 'EBC-2026-000011',
    businessType: 'RENEWAL',
    stage: 'RA_SENT',
    policyYear: 2027,
    raSentAt: '2026-09-01T02:00:00Z',
  },
};

export const PROGRAMME: ProgrammeView = {
  id: 7,
  programmeNo: 'EBP-2026-000007',
  client: { id: 3, code: 'CL-2026-000003', name: 'Pacific Harbor Logistics Inc.' },
  name: 'Group Health',
  teamCode: 'BDO',
  funding: 'EMPLOYER',
  accountOfficer: 'ebao',
  renewalEligible: true,
  status: 'ACTIVE',
  createdAt: '2026-08-01T02:00:00Z',
  createdBy: 'ebao',
  lines: [
    {
      lineNo: 1,
      benefitLine: 'HMO',
      incumbentInsurer: 'INS-MGIC',
      currentPolicyNo: 'HMO-1',
      currentArn: 'ARN-2025-000001',
      periodFrom: '2026-01-10',
      periodTo: '2099-01-10',
      headcount: 120,
      active: true,
    },
  ],
  contacts: [
    {
      id: 21,
      name: 'Liza Marquez',
      email: 'hr@client.example',
      role: 'HR_HEAD',
      receivesRa: true,
      receivesSoa: false,
      active: true,
    },
  ],
  cycles: [
    {
      id: 11,
      cycleNo: 'EBC-2026-000011',
      businessType: 'RENEWAL',
      policyYear: 2027,
      targetInception: '2027-01-10',
      stage: 'RA_SENT',
      remarketing: false,
      accountArns: [],
      borStatus: 'PENDING',
      renewalAdvice: {
        sentAt: '2026-09-01T02:00:00Z',
        sentBy: 'SYSTEM',
        manual: false,
        expiryDate: '2027-01-10',
        recipients: ['hr@client.example'],
        remindersSent: 1,
      },
    },
  ],
  currentCycleId: 11,
};

export const CASE: WorkCaseDetail = {
  item: {
    id: 90,
    workflowCode: 'EB_CYCLE',
    stageCode: 'RA_SENT',
    stageName: 'Renewal advice sent',
    entityType: 'EbCycle',
    entityId: '11',
    reference: 'EBC-2026-000011',
    title: 'Renewal 2027',
    stageEnteredAt: '2026-09-01T02:00:00Z',
    overdue: false,
    createdBy: 'SYSTEM',
    createdAt: '2026-09-01T02:00:00Z',
  },
  stageTerminal: false,
  actions: [
    {
      action: 'record_feedback',
      label: 'Record Feedback',
      toStage: 'REQUIREMENTS',
      generic: false,
    },
    { action: 'not_renewed', label: 'Not Renewed', toStage: 'NOT_RENEWED', generic: true },
  ],
  history: [],
};

export const ITEM: ItemRow = {
  id: 31,
  programmeId: 7,
  programmeNo: 'EBP-2026-000007',
  clientName: 'Pacific Harbor Logistics Inc.',
  itemType: 'HMO_CARD',
  subject: 'HMO card of Juan Dela Cruz',
  memberRef: 'EMP-00123',
  responsible: 'INSURER',
  partyCode: 'INS-MGIC',
  status: 'PENDING',
  dueDate: '2026-09-10',
  daysPastDue: 12,
  followUpsSent: 1,
};
