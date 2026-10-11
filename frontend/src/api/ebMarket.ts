import { api, toQuery } from './client';
import type { CycleAccount } from './ebTypes';
import type {
  ChecklistItem,
  ComparativeSummary,
  ComparativeView,
  Confirmation,
  ConfirmationInput,
  Franchise,
  FranchiseDecisionInput,
  InsurerRequest,
  Proposal,
  ProposalInput,
  Revision,
  RevisionChange,
  Submission,
  SubmissionInput,
  Tor,
  TorItemInput,
} from './ebMarketTypes';

/**
 * Employee Benefits marketing API (BRD-8; docs/architecture/EMPLOYEE_BENEFITS_DESIGN.md section
 * 16.8): franchise requests, TOR, insurer requests, proposals, revisions, comparative with its
 * sign-off and threshold approval, client confirmation, Trigger Placement and submissions.
 */

export type * from './ebMarketTypes';
export type * from './ebServiceTypes';

const BASE = '/eb';

function jsonForm(field: string, value: unknown, file?: File) {
  const form = new FormData();
  form.append(field, JSON.stringify(value));
  if (file) {
    form.append('file', file);
  }
  return form;
}

const q = (companyId: number) => toQuery({ companyId });
const cycle = (id: number) => `${BASE}/cycles/${String(id)}`;
const programme = (id: number) => `${BASE}/programmes/${String(id)}`;
const comparative = (id: number) => `${BASE}/comparatives/${String(id)}`;

export const ebMarketApi = {
  franchises: (companyId: number, programmeId: number) =>
    api.get<Franchise[]>(`${programme(programmeId)}/franchise${q(companyId)}`),
  requestFranchise: (companyId: number, cycleId: number, insurerCodes: string[]) =>
    api.post<Franchise[]>(`${cycle(cycleId)}/franchise${q(companyId)}`, { insurerCodes }),
  decideFranchise: (companyId: number, id: number, input: FranchiseDecisionInput, file?: File) => {
    const form = new FormData();
    form.append('approve', String(input.approve));
    if (input.decidedOn) {
      form.append('decidedOn', input.decidedOn);
    }
    if (input.reasonCode) {
      form.append('reasonCode', input.reasonCode);
    }
    if (input.remarks) {
      form.append('remarks', input.remarks);
    }
    if (file) {
      form.append('file', file);
    }
    return api.upload<Franchise>(`${BASE}/franchise/${String(id)}/decision${q(companyId)}`, form);
  },
  adviseFranchise: (companyId: number, id: number) =>
    api.post<Franchise>(`${BASE}/franchise/${String(id)}/advise${q(companyId)}`),
  tors: (companyId: number, cycleId: number) =>
    api.get<Tor[]>(`${cycle(cycleId)}/tor${q(companyId)}`),
  saveTor: (companyId: number, cycleId: number, items: TorItemInput[]) =>
    api.put<Tor>(`${cycle(cycleId)}/tor${q(companyId)}`, { items }),
  releaseTor: (companyId: number, cycleId: number) =>
    api.post<Tor>(`${cycle(cycleId)}/tor/release${q(companyId)}`),
  requests: (companyId: number, cycleId: number) =>
    api.get<InsurerRequest[]>(`${cycle(cycleId)}/requests${q(companyId)}`),
  sendRequests: (companyId: number, cycleId: number, insurerCodes: string[]) =>
    api.post<InsurerRequest[]>(`${cycle(cycleId)}/requests${q(companyId)}`, { insurerCodes }),
  closeRequest: (companyId: number, id: number, declined: boolean, reason: string) =>
    api.post<InsurerRequest>(`${BASE}/requests/${String(id)}/close${q(companyId)}`, {
      declined,
      reason,
    }),
  proposals: (companyId: number, cycleId: number) =>
    api.get<Proposal[]>(`${cycle(cycleId)}/proposals${q(companyId)}`),
  recordProposal: (companyId: number, cycleId: number, input: ProposalInput, file?: File) =>
    api.upload<Proposal>(
      `${cycle(cycleId)}/proposals${q(companyId)}`,
      jsonForm('proposal', input, file),
    ),
  validateProposal: (companyId: number, id: number) =>
    api.post<Proposal>(`${BASE}/proposals/${String(id)}/validate${q(companyId)}`),
  rejectProposal: (companyId: number, id: number, reason: string) =>
    api.post<Proposal>(`${BASE}/proposals/${String(id)}/reject${q(companyId)}`, { reason }),
  revisions: (companyId: number, cycleId: number) =>
    api.get<Revision[]>(`${cycle(cycleId)}/revisions${q(companyId)}`),
  requestRevision: (
    companyId: number,
    cycleId: number,
    input: { description?: string; changes: RevisionChange[]; insurerCodes: string[] },
  ) => api.post<Revision>(`${cycle(cycleId)}/revisions${q(companyId)}`, input),
  comparatives: (companyId: number, programmeId: number) =>
    api.get<ComparativeSummary[]>(`${programme(programmeId)}/comparatives${q(companyId)}`),
  buildComparative: (companyId: number, cycleId: number) =>
    api.post<ComparativeView>(`${cycle(cycleId)}/comparatives${q(companyId)}`),
  comparative: (companyId: number, id: number) =>
    api.get<ComparativeView>(`${comparative(id)}${q(companyId)}`),
  recommend: (
    companyId: number,
    id: number,
    input: { recommendation: Record<string, number>; summary?: string },
  ) => api.put<ComparativeView>(`${comparative(id)}/recommendation${q(companyId)}`, input),
  comparativeAction: (
    companyId: number,
    id: number,
    action: 'submit' | 'sign-off' | 'threshold-approve' | 'return' | 'present',
    remarks?: string,
  ) => api.post<ComparativeView>(`${comparative(id)}/${action}${q(companyId)}`, { remarks }),
  comment: (companyId: number, id: number, input: { text: string; client: boolean }) =>
    api.post<ComparativeView['comments'][number]>(
      `${comparative(id)}/comments${q(companyId)}`,
      input,
    ),
  exportComparative: (companyId: number, id: number, format: 'pdf' | 'xlsx') =>
    api.getFile(`${comparative(id)}/export${toQuery({ companyId, format })}`),
  confirmations: (companyId: number, cycleId: number) =>
    api.get<Confirmation[]>(`${cycle(cycleId)}/confirmations${q(companyId)}`),
  confirm: (companyId: number, cycleId: number, input: ConfirmationInput, file?: File) =>
    api.upload<Confirmation>(
      `${cycle(cycleId)}/confirmation${q(companyId)}`,
      jsonForm('confirmation', input, file),
    ),
  voidConfirmation: (companyId: number, cycleId: number, reason: string) =>
    api.post<Confirmation>(`${cycle(cycleId)}/confirmation/void${q(companyId)}`, { reason }),
  triggerPlacement: (companyId: number, cycleId: number) =>
    api.post<CycleAccount[]>(`${cycle(cycleId)}/trigger-placement${q(companyId)}`),
  submissions: (companyId: number, programmeId: number) =>
    api.get<Submission[]>(`${programme(programmeId)}/submissions${q(companyId)}`),
  checklist: (
    companyId: number,
    scope: { programmeId: number; cycleId?: number; memberChangeId?: number; processType: string },
  ) => api.get<ChecklistItem[]>(`${BASE}/submissions/checklist${toQuery({ companyId, ...scope })}`),
  submit: (companyId: number, input: SubmissionInput) =>
    api.post<Submission>(`${BASE}/submissions${q(companyId)}`, input),
  acknowledge: (companyId: number, id: number, date: string) =>
    api.post<Submission>(`${BASE}/submissions/${String(id)}/acknowledge${q(companyId)}`, { date }),
};
