import type { CaseAction, CaseQueue, CaseSla } from '@/api/csfCases';
import type { TagTone } from '@/components/ui/Tag';

/** Lists of values of the cases (FRS BRD-9 section 9.2). */
export const CASE_LOV = {
  channel: 'CSF_CASE_CHANNEL',
  type: 'CSF_CASE_TYPE',
  nature: 'CSF_CASE_NATURE',
  product: 'CSF_CASE_PRODUCT',
  segment: 'CSF_CASE_SEGMENT',
  unit: 'CSF_FULFILMENT_UNIT',
  priority: 'CSF_CASE_PRIORITY',
  escalation: 'CSF_ESCALATION_REASON',
  returnReason: 'CSF_RETURN_REASON',
  closure: 'CSF_CLOSURE_REASON',
  documentType: 'CSF_DOCUMENT_TYPE',
} as const;

/** A work queue with its label and the permission that offers it. */
export interface QueueTab {
  id: CaseQueue;
  label: string;
  permission: string;
}

/** The work queues of the Cases screen (FRS section 5.10). */
export const QUEUES: readonly QueueTab[] = [
  { id: 'MINE', label: 'My Cases', permission: 'CSF_CASE_CREATE' },
  { id: 'RETURNED', label: 'Returned to Me', permission: 'CSF_CASE_CREATE' },
  { id: 'CSR_ACTION', label: 'CSR Action Required', permission: 'CSF_CASE_CREATE' },
  { id: 'TEAM', label: 'Team Queue', permission: 'CSF_CASE_SUPERVISE' },
  { id: 'UNIT', label: 'Fulfilment Unit Queue', permission: 'CSF_CASE_FULFIL' },
  { id: 'OPEN', label: 'All Open', permission: 'CSF_VIEW' },
  { id: 'CLOSED', label: 'Closed', permission: 'CSF_VIEW' },
];

/** The queues offered to a user. */
export function queuesFor(can: (permission: string) => boolean): QueueTab[] {
  return QUEUES.filter((q) => can(q.permission));
}

/** Button labels of the case actions. */
export const ACTION_LABELS: Record<CaseAction, string> = {
  RESOLVE: 'Resolve and Close',
  RETURN: 'Return to Agent',
  CSR_ACTION: 'Request CSR Action',
  REENDORSE: 'Re-endorse to Fulfilment Unit',
  CLOSE: 'Close Case',
  REASSIGN: 'Reassign',
  ESCALATE: 'Escalate',
  CHANGE_CATEGORY: 'Change Category',
  NOTE: 'Add Note',
  UPLOAD: 'Upload',
  FOLLOW_UP: 'Create Follow-up Case',
};

/** Actions shown as buttons of the case header (notes and uploads sit on their tabs). */
export const HEADER_ACTIONS: readonly CaseAction[] = [
  'RESOLVE',
  'RETURN',
  'CSR_ACTION',
  'REENDORSE',
  'CLOSE',
  'REASSIGN',
  'ESCALATE',
  'CHANGE_CATEGORY',
  'FOLLOW_UP',
];

/** Tag tone of an SLA status. */
export function slaTone(sla: CaseSla): TagTone {
  if (sla.status === 'BREACHED') {
    return 'danger';
  }
  return sla.status === 'APPROACHING' ? 'flag' : 'neutral';
}
