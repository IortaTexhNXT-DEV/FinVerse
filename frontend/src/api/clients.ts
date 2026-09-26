import { api, toQuery } from './client';
import type { ClientStatus, ClientType, KycStatus } from './crm';
import type { PageResponse } from './types';

/** KYC profile per BDO standard (BRNB.030). */
export interface ClientProfile {
  nationality?: string;
  civilStatus?: string;
  occupation?: string;
  sourceOfFunds?: string;
  riskRating?: string;
}

/** Complete client details (BRNB.046). */
export interface ClientDetail {
  id: number;
  companyId: number;
  code: string;
  prospectCode: string;
  clientCode?: string;
  status: ClientStatus;
  onboardingStage?: string;
  clientType: ClientType;
  displayName: string;
  lastName?: string;
  firstName?: string;
  middleName?: string;
  suffix?: string;
  corporateName?: string;
  birthDate?: string;
  tin?: string;
  idType?: string;
  idNumber?: string;
  email?: string;
  mobile?: string;
  phone?: string;
  addressLine?: string;
  city?: string;
  province?: string;
  postalCode?: string;
  marketSegment?: string;
  bankClient: boolean;
  bankCif?: string;
  profile: ClientProfile;
  partyCode?: string;
  kyc: {
    status: KycStatus;
    submittedBy?: string;
    submittedAt?: string;
    verifiedBy?: string;
    verifiedAt?: string;
    reviewDue?: string;
  };
  infoComplete: boolean;
  missingFields: string[];
  lifecycle: {
    createdBy: string;
    createdAt: string;
    confirmedBy?: string;
    confirmedAt?: string;
    deactivationReason?: string;
    deactivationNote?: string;
    deactivatedBy?: string;
    deactivatedAt?: string;
  };
}

/** A client in a list. */
export interface ClientListItem {
  id: number;
  code: string;
  prospectCode: string;
  clientCode?: string;
  displayName: string;
  clientType: ClientType;
  status: ClientStatus;
  onboardingStage?: string;
  kycStatus: KycStatus;
  kycReviewDue?: string;
  kycVerifiedAt?: string;
  riskRating?: string;
  tin?: string;
  email?: string;
  mobile?: string;
  marketSegment?: string;
  bankClient: boolean;
}

/** New or changed client. */
export interface ClientRequest extends ClientProfile {
  companyId?: number;
  clientType: ClientType;
  lastName?: string;
  firstName?: string;
  middleName?: string;
  suffix?: string;
  corporateName?: string;
  birthDate?: string;
  tin?: string;
  idType?: string;
  idNumber?: string;
  email?: string;
  mobile?: string;
  phone?: string;
  addressLine?: string;
  city?: string;
  province?: string;
  postalCode?: string;
  marketSegment?: string;
  bankClient: boolean;
  bankCif?: string;
}

export interface ClientSearch {
  companyId: number;
  code?: string;
  name?: string;
  tin?: string;
  idNumber?: string;
  email?: string;
  mobile?: string;
  status?: ClientStatus;
  kycStatus?: KycStatus;
  marketSegment?: string;
  bankClient?: boolean;
  clientType?: ClientType;
  kycDue?: boolean;
  page?: number;
}

export interface DuplicateQuery {
  companyId: number;
  excludeId?: number;
  clientType?: ClientType;
  tin?: string;
  idType?: string;
  idNumber?: string;
  email?: string;
  mobile?: string;
  lastName?: string;
  firstName?: string;
  birthDate?: string;
  corporateName?: string;
}

/** An existing client matching entered data (BRNB.032). */
export interface DuplicateMatch {
  clientId: number;
  code: string;
  displayName: string;
  status: ClientStatus;
  keys: string[];
  hard: boolean;
}

export interface KycDocument {
  attachmentId: number;
  fileName: string;
  uploadedBy: string;
  uploadedAt: string;
}

export interface KycChecklist {
  complete: boolean;
  missing: string[];
  items: { documentType: string; label: string; required: boolean; documents: KycDocument[] }[];
}

export interface ClientBanner {
  clientId: number;
  clientCode: string;
  tags: { code: string; label: string }[];
  instructions: {
    id: number;
    type: string;
    typeLabel: string;
    text: string;
    effectiveFrom: string;
    effectiveTo?: string;
  }[];
}

export interface ClientInstruction {
  id: number;
  type: string;
  text: string;
  effectiveFrom: string;
  effectiveTo?: string;
  active: boolean;
  createdBy: string;
  createdAt: string;
}

export interface NoteHistory {
  id: number;
  kind: 'TAG' | 'INSTRUCTION';
  ref: string;
  action: 'ADDED' | 'CHANGED' | 'REMOVED';
  fromValue?: string;
  toValue?: string;
  actor: string;
  occurredAt: string;
}

export interface InstructionRequest {
  type: string;
  text: string;
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface ClientRecord {
  kind: string;
  reference: string;
  description?: string;
  status?: string;
  date?: string;
  link?: string;
}

export interface Client360 {
  records: ClientRecord[];
  warnings: { code: string; message: string }[];
}

export interface ClientHistoryEntry {
  id: number;
  occurredAt: string;
  username: string;
  action: string;
  summary: string;
}

export type BankFilter = 'NON_BANK' | 'BANK' | 'ALL';

export interface KycReviewFilters {
  companyId: number;
  dueBy?: string;
  bank?: BankFilter;
  riskRating?: string;
  marketSegment?: string;
  page?: number;
}

export interface ClientAction {
  reasonCode?: string;
  comment?: string;
}

const base = '/crm/clients';

/** Client master, onboarding, tags and instructions, 360 view and KYC reviews (crm module). */
export const clientsApi = {
  search: (s: ClientSearch) => api.get<PageResponse<ClientListItem>>(`${base}${toQuery({ ...s })}`),
  get: (id: number) => api.get<ClientDetail>(`${base}/${String(id)}`),
  create: (body: ClientRequest) => api.post<ClientDetail>(base, body),
  update: (id: number, body: ClientRequest) => api.put<ClientDetail>(`${base}/${String(id)}`, body),
  duplicates: (q: DuplicateQuery) =>
    api.get<DuplicateMatch[]>(`${base}/duplicates${toQuery({ ...q })}`),
  checklist: (id: number) => api.get<KycChecklist>(`${base}/${String(id)}/kyc-checklist`),
  uploadKyc: (id: number, documentType: string, file: File) => {
    const form = new FormData();
    form.append('documentType', documentType);
    form.append('file', file);
    return api.upload<KycChecklist>(`${base}/${String(id)}/kyc-documents`, form);
  },
  submitKyc: (id: number, body: ClientAction) =>
    api.post<ClientDetail>(`${base}/${String(id)}/submit-kyc`, body),
  verifyKyc: (id: number, body: ClientAction) =>
    api.post<ClientDetail>(`${base}/${String(id)}/verify-kyc`, body),
  confirm: (id: number, body: ClientAction) =>
    api.post<ClientDetail>(`${base}/${String(id)}/confirm`, body),
  deactivate: (id: number, body: ClientAction) =>
    api.post<ClientDetail>(`${base}/${String(id)}/deactivate`, body),
  banner: (id: number) => api.get<ClientBanner>(`${base}/${String(id)}/instructions`),
  notes: (id: number) =>
    api.get<{ instructions: ClientInstruction[]; history: NoteHistory[] }>(
      `${base}/${String(id)}/notes`,
    ),
  addTag: (id: number, tagCode: string) =>
    api.post<ClientBanner>(`${base}/${String(id)}/tags`, { tagCode }),
  removeTag: (id: number, tagCode: string) =>
    api.post<ClientBanner>(`${base}/${String(id)}/tags/${tagCode}/remove`),
  addInstruction: (id: number, body: InstructionRequest) =>
    api.post<ClientInstruction>(`${base}/${String(id)}/instructions`, body),
  changeInstruction: (id: number, instructionId: number, body: InstructionRequest) =>
    api.put<ClientInstruction>(`${base}/${String(id)}/instructions/${String(instructionId)}`, body),
  endInstruction: (id: number, instructionId: number) =>
    api.post<ClientInstruction>(`${base}/${String(id)}/instructions/${String(instructionId)}/end`),
  records: (id: number) => api.get<Client360>(`${base}/${String(id)}/records`),
  history: (id: number) => api.get<ClientHistoryEntry[]>(`${base}/${String(id)}/history`),
  kycReviews: (f: KycReviewFilters) =>
    api.get<PageResponse<ClientListItem>>(`/crm/kyc-reviews${toQuery({ ...f })}`),
};
