import { api, toQuery } from './client';
import type { PageResponse } from './types';

export type QueueScope = 'MINE' | 'UNASSIGNED' | 'ALL';

export interface WorkItem {
  id: number;
  workflowCode: string;
  stageCode: string;
  stageName: string;
  entityType: string;
  entityId: string;
  reference: string;
  title: string;
  link?: string;
  originatingUnit?: string;
  assignee?: string;
  stageEnteredAt: string;
  dueAt?: string;
  overdue: boolean;
  createdBy: string;
  createdAt: string;
}

export interface WorkAction {
  action: string;
  label: string;
  toStage: string;
  toStageName?: string;
  generic: boolean;
  reasonLov?: string;
}

export interface HistoryEntry {
  fromStage?: string;
  fromStageName?: string;
  toStage: string;
  toStageName?: string;
  action: string;
  reasonCode?: string;
  comment?: string;
  actor: string;
  automatic: boolean;
  occurredAt: string;
}

/** A stage of a workflow definition, in its defined order. */
export interface WorkflowStageInfo {
  code: string;
  name: string;
  initial: boolean;
  terminal: boolean;
}

export interface WorkCaseDetail {
  item: WorkItem;
  stageTerminal: boolean;
  slaHours?: number;
  actions: WorkAction[];
  history: HistoryEntry[];
  /** Every stage of the workflow in its defined order (the stage stepper). */
  stages?: WorkflowStageInfo[];
}

export interface QueueCount {
  workflowCode: string;
  stageCode: string;
  stageName: string;
  open: number;
  overdue: number;
  mine: number;
}

export interface QueueFilters {
  companyId: number;
  workflow?: string;
  stage?: string;
  scope?: QueueScope;
  overdue?: boolean;
  text?: string;
  page?: number;
}

export interface ActionNote {
  reasonCode?: string;
  comment?: string;
}

/** Human names of the broking workflows. */
export const WORKFLOW_NAMES: Record<string, string> = {
  NB_QUOTATION: 'Quotations',
  NB_PROPOSAL: 'Proposal requests',
  NB_ACCOUNT: 'Accounts',
  NB_CLIENT: 'Client onboarding',
  PM_PACKAGE_REQUEST: 'Package requests',
  CLX_ESCALATION: 'Collection escalations',
  DISB_VOUCHER: 'Disbursement vouchers',
  DISB_STATUS_EDIT: 'Instrument status changes',
  DISB_FUNDING: 'Account funding',
  DISB_PAYEE: 'Payees',
  PRQ_REFUND: 'Refund requests',
  PRQ_CASH_ADVANCE: 'Cash advance requests',
  PRQ_CHECK_CANCEL: 'Check cancellation requests',
  ACSL_CASE: 'ACSL cases',
  ACSL_CORRECTION: 'ACSL correction entries',
  REM_DEDUCTION: 'Remittance deductions',
  FRBS_SERVICE_FEE: 'Service fee runs',
  SCR_CASE: 'Screening cases',
  EB_CYCLE: 'EB cycles',
  EB_FRANCHISE: 'EB franchise requests',
  EB_MEMBER_CHANGE: 'EB member changes',
  EB_SOA: 'EB statements of account',
  BCL_CLAIM: 'Claims',
  RNW_CASE: 'Renewals',
  SBM_POLICY: 'Submitted policies',
};

/**
 * The record type of a work item, one per workflow, as the Type column of My Work shows it
 * ("Account", "EB Cycle"); never the workflow code.
 */
export const WORKFLOW_RECORD_TYPES: Record<string, string> = {
  NB_QUOTATION: 'Quotation',
  NB_PROPOSAL: 'Proposal Request',
  NB_ACCOUNT: 'Account',
  NB_CLIENT: 'Client Onboarding',
  PM_PACKAGE_REQUEST: 'Package Request',
  CLX_ESCALATION: 'Collection Escalation',
  DISB_VOUCHER: 'Disbursement Voucher',
  DISB_STATUS_EDIT: 'Instrument Status Change',
  DISB_FUNDING: 'Account Funding',
  DISB_PAYEE: 'Payee',
  PRQ_REFUND: 'Refund Request',
  PRQ_CASH_ADVANCE: 'Cash Advance Request',
  PRQ_CHECK_CANCEL: 'Check Cancellation',
  ACSL_CASE: 'ACSL Case',
  ACSL_CORRECTION: 'ACSL Correction Entry',
  REM_DEDUCTION: 'Remittance Deduction',
  FRBS_SERVICE_FEE: 'Service Fee Run',
  SCR_CASE: 'Screening Case',
  EB_CYCLE: 'EB Cycle',
  EB_FRANCHISE: 'EB Franchise Request',
  EB_MEMBER_CHANGE: 'EB Member Change',
  EB_SOA: 'EB Statement of Account',
  BCL_CLAIM: 'Claim',
  RNW_CASE: 'Renewal',
  SBM_POLICY: 'Submitted Policy',
  SBM_IAAF: 'IAAF Review',
  SBM_TOR: 'Terms of Reference',
  OPS_BIR_CERT: 'BIR Certificate',
  OPS_CWT_2307: 'CWT Certificate (2307)',
  OPS_DISPOSITION: 'Payment Disposition',
  OPS_DP_BILLING: 'Direct Payment Billing',
  OPS_ENDORSEMENT: 'Endorsement Request',
  OPS_HOLD: 'Remittance Hold',
  OPS_RECEIPT_ACTION: 'Receipt Action',
  OPS_RECON: 'Production Reconciliation',
  OPS_REMITTANCE: 'Remittance',
  OPS_SPECIAL_REMIT: 'Special Remittance',
  MIG_OPENING_TRUEUP: 'Opening Balance True-up',
  MIG_OBJECT_DECISION: 'Migration Object Decision',
  MIG_RESUBMISSION: 'Migration Resubmission',
  MIG_BATCH_ROLLBACK: 'Migration Batch Rollback',
  MIG_MAP_VERSION: 'Code Map Version',
};

/** The record type of a workflow by its name; an unknown workflow reads as the muted dash. */
export function workflowRecordType(code: string | null | undefined): string {
  return code ? (WORKFLOW_RECORD_TYPES[code] ?? '') : '';
}

/** Workflow engine: My Work queues, record workflow panel, generic actions, assignment. */
export const workflowApi = {
  queue: (f: QueueFilters) =>
    api.get<PageResponse<WorkItem>>(`/workflow/queue${toQuery({ ...f, size: 50 })}`),
  counts: (companyId: number) => api.get<QueueCount[]>(`/workflow/counts${toQuery({ companyId })}`),
  byRecord: (entityType: string, entityId: string | number) =>
    api.get<WorkCaseDetail>(
      `/workflow/cases/by-record${toQuery({ entityType, entityId: String(entityId) })}`,
    ),
  act: (caseId: number, action: string, note: ActionNote) =>
    api.post<WorkCaseDetail>(`/workflow/cases/${caseId}/actions/${action}`, note),
  claim: (caseId: number) => api.post<WorkItem>(`/workflow/cases/${caseId}/claim`),
  assignees: (caseId: number) => api.get<string[]>(`/workflow/cases/${caseId}/assignees`),
  assign: (caseId: number, assignee: string | null) =>
    api.post<WorkItem>(`/workflow/cases/${caseId}/assign`, { assignee }),
  stages: (workflowCode: string) =>
    api.get<Record<string, string>>(`/workflow/definitions/${workflowCode}/stages`),
};
