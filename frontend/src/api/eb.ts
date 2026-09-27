import { api, toQuery } from './client';
import type { PageResponse } from './types';
import type {
  BusinessType,
  FeedbackChannel,
  ItemAction,
  ProgrammeRow,
  ProgrammeView,
  ProfileInput,
  LineInput,
  ContactInput,
  NewProgrammeInput,
  SendRaResult,
  FeedbackItem,
  DocumentView,
  BorVersion,
  BorChecklist,
  CycleAccount,
  ActivityRow,
  ItemRow,
  ItemInput,
  ItemFilters,
  ProgrammeFilters,
  CycleStep,
} from './ebTypes';

/**
 * Employee Benefits API (BRD-8, waves E1-B and E1-C; docs/architecture/EMPLOYEE_BENEFITS_DESIGN.md
 * section 16): programmes, cycles, renewal advice, feedback, documents, Broker on Record, accounts
 * of the cycles, activity, EB Home counts and Pending Items.
 */

export type {
  ProgrammeTab,
  BusinessType,
  Funding,
  ContactRole,
  FeedbackChannel,
  DocumentSource,
  Responsible,
  ItemAction,
  CycleRef,
  ProgrammeRow,
  LineView,
  ContactView,
  AdviceView,
  CycleView,
  ProgrammeView,
  ProfileInput,
  LineInput,
  ContactInput,
  NewProgrammeInput,
  SendRaResult,
  FeedbackItem,
  DocumentView,
  BorVersion,
  BorChecklist,
  CycleAccount,
  ActivityRow,
  ItemRow,
  ItemInput,
  ItemFilters,
  ProgrammeFilters,
  CycleStep,
} from './ebTypes';

const BASE = '/eb';

function filesForm(fields: Record<string, string | undefined>, files: File[], field = 'files') {
  const form = new FormData();
  Object.entries(fields).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      form.append(key, value);
    }
  });
  files.forEach((f) => form.append(field, f));
  return form;
}

export const ebApi = {
  accountOfficers: () => api.get<string[]>(`${BASE}/account-officers`),
  home: (companyId: number) =>
    api.get<Record<string, number>>(`${BASE}/home${toQuery({ companyId })}`),
  programmes: (companyId: number, filters: ProgrammeFilters, page: number, size = 20) =>
    api.get<PageResponse<ProgrammeRow>>(
      `${BASE}/programmes${toQuery({ companyId, ...filters, page, size })}`,
    ),
  programme: (companyId: number, id: number) =>
    api.get<ProgrammeView>(`${BASE}/programmes/${String(id)}${toQuery({ companyId })}`),
  create: (companyId: number, input: NewProgrammeInput) =>
    api.post<ProgrammeView>(`${BASE}/programmes${toQuery({ companyId })}`, input),
  updateProfile: (companyId: number, id: number, profile: ProfileInput) =>
    api.put<ProgrammeView>(`${BASE}/programmes/${String(id)}${toQuery({ companyId })}`, profile),
  addLine: (companyId: number, id: number, line: LineInput) =>
    api.post<ProgrammeView>(
      `${BASE}/programmes/${String(id)}/lines${toQuery({ companyId })}`,
      line,
    ),
  updateLine: (companyId: number, id: number, lineNo: number, line: LineInput) =>
    api.put<ProgrammeView>(
      `${BASE}/programmes/${String(id)}/lines/${String(lineNo)}${toQuery({ companyId })}`,
      line,
    ),
  removeLine: (companyId: number, id: number, lineNo: number) =>
    api.delete(`${BASE}/programmes/${String(id)}/lines/${String(lineNo)}${toQuery({ companyId })}`),
  addContact: (companyId: number, id: number, contact: ContactInput) =>
    api.post<ProgrammeView>(
      `${BASE}/programmes/${String(id)}/contacts${toQuery({ companyId })}`,
      contact,
    ),
  updateContact: (companyId: number, id: number, contactId: number, contact: ContactInput) =>
    api.put<ProgrammeView>(
      `${BASE}/programmes/${String(id)}/contacts/${String(contactId)}${toQuery({ companyId })}`,
      contact,
    ),
  removeContact: (companyId: number, id: number, contactId: number) =>
    api.delete(
      `${BASE}/programmes/${String(id)}/contacts/${String(contactId)}${toQuery({ companyId })}`,
    ),
  sendRa: (companyId: number, programmeIds: number[]) =>
    api.post<SendRaResult[]>(`${BASE}/programmes/send-ra${toQuery({ companyId })}`, {
      programmeIds,
    }),
  activity: (companyId: number, id: number) =>
    api.get<ActivityRow[]>(`${BASE}/programmes/${String(id)}/activity${toQuery({ companyId })}`),
  openCycle: (
    companyId: number,
    id: number,
    input: { businessType: BusinessType; policyYear?: number; targetInception?: string },
  ) =>
    api.post<ProgrammeView>(
      `${BASE}/programmes/${String(id)}/cycles${toQuery({ companyId })}`,
      input,
    ),
  step: (companyId: number, cycleId: number, step: CycleStep) =>
    api.post<ProgrammeView>(`${BASE}/cycles/${String(cycleId)}/${step}${toQuery({ companyId })}`),
  feedback: (companyId: number, id: number) =>
    api.get<FeedbackItem[]>(`${BASE}/programmes/${String(id)}/feedback${toQuery({ companyId })}`),
  recordFeedback: (
    companyId: number,
    cycleId: number,
    input: { channel: FeedbackChannel | ''; receivedOn: string; text: string },
    files: File[],
  ) =>
    api.upload<FeedbackItem>(
      `${BASE}/cycles/${String(cycleId)}/feedback${toQuery({ companyId })}`,
      filesForm(input, files),
    ),
  documents: (companyId: number, id: number) =>
    api.get<DocumentView[]>(`${BASE}/programmes/${String(id)}/documents${toQuery({ companyId })}`),
  uploadDocuments: (
    companyId: number,
    cycleId: number,
    input: { documentType: string; processType: string; source: string; description?: string },
    files: File[],
  ) =>
    api.upload<DocumentView[]>(
      `${BASE}/cycles/${String(cycleId)}/documents${toQuery({ companyId })}`,
      filesForm(input, files),
    ),
  bors: (companyId: number, id: number) =>
    api.get<BorVersion[]>(`${BASE}/programmes/${String(id)}/bor${toQuery({ companyId })}`),
  uploadBor: (companyId: number, cycleId: number, file: File) =>
    api.upload<BorVersion>(
      `${BASE}/cycles/${String(cycleId)}/bor${toQuery({ companyId })}`,
      filesForm({}, [file], 'file'),
    ),
  validateBor: (companyId: number, borId: number, checklist: BorChecklist) =>
    api.post<BorVersion>(
      `${BASE}/bor/${String(borId)}/validate${toQuery({ companyId })}`,
      checklist,
    ),
  rejectBor: (companyId: number, borId: number, reason: string) =>
    api.post<BorVersion>(`${BASE}/bor/${String(borId)}/reject${toQuery({ companyId })}`, {
      reason,
    }),
  accounts: (companyId: number, id: number) =>
    api.get<CycleAccount[]>(`${BASE}/programmes/${String(id)}/accounts${toQuery({ companyId })}`),
  items: (companyId: number, filters: ItemFilters, page: number, size = 20) =>
    api.get<PageResponse<ItemRow>>(
      `${BASE}/pending-items${toQuery({ companyId, ...filters, page, size })}`,
    ),
  openItem: (companyId: number, input: ItemInput) =>
    api.post<ItemRow>(`${BASE}/pending-items${toQuery({ companyId })}`, input),
  updateItem: (companyId: number, id: number, input: ItemInput) =>
    api.put<ItemRow>(`${BASE}/pending-items/${String(id)}${toQuery({ companyId })}`, input),
  changeItem: (
    companyId: number,
    id: number,
    change: { action: ItemAction; date?: string; receivedOn?: string; remarks?: string },
  ) =>
    api.post<ItemRow>(
      `${BASE}/pending-items/${String(id)}/status${toQuery({ companyId })}`,
      change,
    ),
};
