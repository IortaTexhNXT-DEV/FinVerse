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

export interface WorkCaseDetail {
  item: WorkItem;
  stageTerminal: boolean;
  slaHours?: number;
  actions: WorkAction[];
  history: HistoryEntry[];
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
};

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
