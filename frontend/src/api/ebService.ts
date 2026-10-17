import { api, toQuery } from './client';
import { filesForm } from './eb';
import type { PageResponse } from './types';
import type {
  BillingInput,
  Member,
  MemberChange,
  MemberChangeFilters,
  MemberChangeInput,
  ProgrammeInvoice,
  RequiredDocument,
  RequiredDocumentInput,
  RosterDifferences,
  RosterVersion,
  Soa,
  SoaFilters,
  SoaInput,
  ThresholdRule,
  ThresholdRuleInput,
} from './ebServiceTypes';

/**
 * Employee Benefits servicing and set-up API (BRD-8; design section 16.8): EB Setup (threshold
 * rules and required documents with maker-checker), the member roster, member changes and the SOA
 * register.
 */

const BASE = '/eb';
const q = (companyId: number) => toQuery({ companyId });

export const ebServiceApi = {
  thresholdRules: (companyId: number) =>
    api.get<ThresholdRule[]>(`${BASE}/setup/threshold-rules${q(companyId)}`),
  saveThresholdRule: (companyId: number, input: ThresholdRuleInput, id?: number) =>
    id === undefined
      ? api.post<ThresholdRule>(`${BASE}/setup/threshold-rules${q(companyId)}`, input)
      : api.put<ThresholdRule>(`${BASE}/setup/threshold-rules/${String(id)}${q(companyId)}`, input),
  thresholdRuleAction: (companyId: number, id: number, action: 'authorize' | 'deactivate') =>
    api.post<ThresholdRule>(`${BASE}/setup/threshold-rules/${String(id)}/${action}${q(companyId)}`),
  requiredDocuments: (companyId: number) =>
    api.get<RequiredDocument[]>(`${BASE}/setup/required-documents${q(companyId)}`),
  saveRequiredDocument: (companyId: number, input: RequiredDocumentInput, id?: number) =>
    id === undefined
      ? api.post<RequiredDocument>(`${BASE}/setup/required-documents${q(companyId)}`, input)
      : api.put<RequiredDocument>(
          `${BASE}/setup/required-documents/${String(id)}${q(companyId)}`,
          input,
        ),
  requiredDocumentAction: (companyId: number, id: number, action: 'authorize' | 'deactivate') =>
    api.post<RequiredDocument>(
      `${BASE}/setup/required-documents/${String(id)}/${action}${q(companyId)}`,
    ),
  roster: (companyId: number, programmeId: number) =>
    api.get<RosterVersion[]>(`${BASE}/programmes/${String(programmeId)}/roster${q(companyId)}`),
  members: (companyId: number, versionId: number, text: string, page: number, size = 50) =>
    api.get<PageResponse<Member>>(
      `${BASE}/roster/${String(versionId)}/members${toQuery({ companyId, q: text, page, size })}`,
    ),
  differences: (companyId: number, versionId: number) =>
    api.get<RosterDifferences>(`${BASE}/roster/${String(versionId)}/differences${q(companyId)}`),
  acceptRoster: (companyId: number, versionId: number) =>
    api.post<RosterVersion>(`${BASE}/roster/${String(versionId)}/accept${q(companyId)}`),
  rejectRoster: (companyId: number, versionId: number, reason: string) =>
    api.post<RosterVersion>(`${BASE}/roster/${String(versionId)}/reject${q(companyId)}`, {
      reason,
    }),
  memberChanges: (companyId: number, filters: MemberChangeFilters, page: number, size = 20) =>
    api.get<PageResponse<MemberChange>>(
      `${BASE}/member-changes${toQuery({ companyId, ...filters, page, size })}`,
    ),
  memberChange: (companyId: number, id: number) =>
    api.get<MemberChange>(`${BASE}/member-changes/${String(id)}${q(companyId)}`),
  captureChange: (
    companyId: number,
    programmeId: number,
    input: MemberChangeInput,
    files: File[],
  ) => {
    const form = filesForm({ change: JSON.stringify(input) }, files, 'files');
    return api.upload<MemberChange>(
      `${BASE}/programmes/${String(programmeId)}/member-changes${q(companyId)}`,
      form,
    );
  },
  changeAction: (companyId: number, id: number, action: 'relay' | 'validate' | 'close') =>
    api.post<MemberChange>(`${BASE}/member-changes/${String(id)}/${action}${q(companyId)}`),
  billChange: (companyId: number, id: number, input: BillingInput, files: File[]) =>
    api.upload<MemberChange>(
      `${BASE}/member-changes/${String(id)}/bill${q(companyId)}`,
      filesForm(
        {
          billedOn: input.billedOn,
          reference: input.reference,
          amount: input.amount,
          direct: String(input.direct),
        },
        files,
        'files',
      ),
    ),
  soas: (companyId: number, filters: SoaFilters, page: number, size = 20) =>
    api.get<PageResponse<Soa>>(`${BASE}/soa${toQuery({ companyId, ...filters, page, size })}`),
  soa: (companyId: number, id: number) => api.get<Soa>(`${BASE}/soa/${String(id)}${q(companyId)}`),
  invoices: (companyId: number, programmeId: number) =>
    api.get<ProgrammeInvoice[]>(
      `${BASE}/programmes/${String(programmeId)}/invoices${q(companyId)}`,
    ),
  receiveSoa: (
    companyId: number,
    programmeId: number,
    input: SoaInput,
    invoiceNos: string[],
    file: File,
  ) => {
    const form = filesForm({ ...input }, [file], 'file');
    invoiceNos.forEach((n) => form.append('invoiceNos', n));
    return api.upload<Soa>(`${BASE}/programmes/${String(programmeId)}/soa${q(companyId)}`, form);
  },
  validateSoa: (companyId: number, id: number, invoiceNos?: string[]) =>
    api.post<Soa>(`${BASE}/soa/${String(id)}/validate${q(companyId)}`, { invoiceNos }),
  rejectSoa: (companyId: number, id: number, reasonCode: string, remarks?: string) =>
    api.post<Soa>(`${BASE}/soa/${String(id)}/reject${q(companyId)}`, { reasonCode, remarks }),
  releaseSoa: (companyId: number, id: number) =>
    api.post<Soa>(`${BASE}/soa/${String(id)}/release${q(companyId)}`),
};
