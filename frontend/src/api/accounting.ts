import { api, toQuery } from './client';
import type { JournalLineInput, Side } from './gl';
import type { PageResponse, RecordStatus } from './types';

/** Accounting event type (catalogue entry published by operational modules). */
export interface EventType {
  code: string;
  name: string;
  category: string;
  journalType: string;
  description?: string;
  /** Comma separated amount components, e.g. "GROSS_PREMIUM,DST,TOTAL_DUE". */
  amountComponents: string;
  active: boolean;
}

/** One Dr/Cr line of a rule: a GL account code or an "@ROLE" supplied by the event. */
export interface RuleLine {
  side: Side;
  accountCode: string;
  amountComponent: string;
  partyLine: boolean;
  narration?: string;
}

export interface Rule {
  id: number;
  companyId: number;
  eventType: string;
  name: string;
  businessLine?: string;
  currency?: string;
  priority: number;
  effectiveFrom: string;
  effectiveTo?: string;
  lines: RuleLine[];
  recordStatus: RecordStatus;
  createdBy: string;
  authorizedBy?: string;
}

export type RuleInput = Omit<Rule, 'id' | 'recordStatus' | 'createdBy' | 'authorizedBy'>;

export interface SimulationInput {
  companyId: number;
  branchId: number;
  eventType: string;
  valueDate: string;
  currency: string;
  businessLine?: string;
  partyCode?: string;
  amounts: Record<string, number>;
  accounts: Record<string, string>;
}

export interface Simulation {
  ruleId: number;
  ruleName: string;
  lines: JournalLineInput[];
  /** Balance computed by the server (event currency). */
  totalDebit: number;
  totalCredit: number;
  difference: number;
  balanced: boolean;
}

export type EventStatus = 'POSTED' | 'FAILED';

/** Event register entry: one business event and the journal it produced (or the error). */
export interface EventLog {
  id: number;
  eventType: string;
  sourceModule: string;
  sourceReference: string;
  reference?: string;
  valueDate: string;
  status: EventStatus;
  ruleId?: number;
  batchNo?: string;
  errorMessage?: string;
  amounts: string;
  processedAt: string;
  processedBy: string;
}

export interface EventFilters {
  companyId: number;
  status?: string;
  eventType?: string;
  from: string;
  to: string;
  page?: number;
  size?: number;
}

export const accountingApi = {
  eventTypes: () => api.get<EventType[]>('/accounting/event-types'),
  rules: (companyId: number) => api.get<Rule[]>(`/accounting/rules${toQuery({ companyId })}`),
  createRule: (body: RuleInput) => api.post<Rule>('/accounting/rules', body),
  updateRule: (id: number, body: RuleInput) => api.put<Rule>(`/accounting/rules/${id}`, body),
  authorizeRule: (id: number) => api.post<Rule>(`/accounting/rules/${id}/authorize`),
  simulate: (body: SimulationInput) => api.post<Simulation>('/accounting/simulate', body),
  events: (f: EventFilters) =>
    api.get<PageResponse<EventLog>>(`/accounting/events${toQuery({ ...f })}`),
};
