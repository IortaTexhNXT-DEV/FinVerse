import { api } from './client';

/** Record the comparative table belongs to. */
export type TermsRecordType = 'quotation' | 'package';

/** Insurer response of a quotation option. */
export type TermsAnswer = 'APPROVED' | 'NOT_COVERED' | 'OTHERS';

/** One quotation option of an insurer (a column of the comparative table). */
export interface OptionColumn {
  insurerCode: string;
  insurerName: string;
  optionNo: number;
  /** Null while the insurer has not responded. */
  answer: TermsAnswer | null;
  otherAnswer: string | null;
  values: Record<string, string | null>;
  /** Keyed in the table (else taken from the insurer's response). */
  saved: boolean;
}

/** The comparative table (BDOI FRS FRPM.006.02, FRPM.009.01, FRPM.012.02). */
export interface TermsTable {
  requestNo: string;
  fields: string[];
  shown: string[];
  clientFields: string[];
  qsValues: Record<string, string | null>;
  columns: OptionColumn[];
  finalTerms: Record<string, string | null>;
  selectedInsurers: string[];
}

export interface TermsView {
  table: TermsTable;
  /** Field names by key. */
  labels: Record<string, string>;
  /** Response names by code. */
  answers: Record<string, string>;
}

/** A change of the Final Terms for Proposal. */
export interface FinalTermChange {
  id: number;
  field: string;
  oldValue: string | null;
  newValue: string | null;
  changedBy: string;
  changedAt: string;
}

/** A proposal slip of one insurer and version (FRPM.009.02, FRPM.013.01). */
export interface ProposalFile {
  id: number;
  insurerCode: string;
  insurerName: string;
  versionNo: number;
  fileName: string;
  attachmentId: number | null;
  generatedBy: string;
  generatedAt: string;
}

export type ClientResponseCode = 'ACCEPTED' | 'REJECTED' | 'RETURNED';

/** A client response to a proposal (FRPM.010.01). */
export interface ClientResponseView {
  id: number;
  response: ClientResponseCode;
  responseName: string;
  responseDate: string;
  remarks: string | null;
  insurers: string | null;
  recordedBy: string;
  recordedAt: string;
}

export interface OptionInput {
  optionNo?: number | null;
  answer: TermsAnswer;
  otherAnswer?: string | null;
  values: Record<string, string>;
}

export interface ClientResponseInput {
  response: ClientResponseCode;
  responseDate?: string | null;
  remarks?: string | null;
  insurers?: string | null;
}

const base = (type: TermsRecordType, id: number) => `/product-maintenance/terms/${type}/${id}`;

/** The comparative table, the insurer selection, the proposal slips and the client response. */
export const pmTermsApi = {
  table: (type: TermsRecordType, id: number) => api.get<TermsView>(base(type, id)),
  fields: (type: TermsRecordType, id: number, shown: string[], client: string[]) =>
    api.put<TermsView>(`${base(type, id)}/fields`, { shown, client }),
  saveOption: (type: TermsRecordType, id: number, insurer: string, input: OptionInput) =>
    api.put<TermsView>(`${base(type, id)}/options/${encodeURIComponent(insurer)}`, input),
  removeOption: (type: TermsRecordType, id: number, insurer: string, optionNo: number) =>
    api.delete(`${base(type, id)}/options/${encodeURIComponent(insurer)}/${optionNo}`),
  saveFinalTerms: (type: TermsRecordType, id: number, values: Record<string, string>) =>
    api.put<TermsView>(`${base(type, id)}/final-terms`, values),
  history: (type: TermsRecordType, id: number) =>
    api.get<FinalTermChange[]>(`${base(type, id)}/final-terms/history`),
  proceed: (type: TermsRecordType, id: number, insurers: string[], comment?: string) =>
    api.post<TermsView>(`${base(type, id)}/proceed`, { insurers, comment }),
  generate: (type: TermsRecordType, id: number, comment?: string) =>
    api.post<ProposalFile[]>(`${base(type, id)}/proposals`, { comment }),
  files: (type: TermsRecordType, id: number) =>
    api.get<ProposalFile[]>(`${base(type, id)}/proposals`),
  exportTable: (type: TermsRecordType, id: number, format: 'xlsx' | 'pdf') =>
    api.getFile(`${base(type, id)}/export?format=${format}`),
  clientResponses: (id: number) =>
    api.get<ClientResponseView[]>(`${base('quotation', id)}/client-responses`),
  recordClientResponse: (id: number, input: ClientResponseInput) =>
    api.post<ClientResponseView>(`${base('quotation', id)}/client-responses`, input),
};
