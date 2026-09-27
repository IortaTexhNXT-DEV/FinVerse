import { api, toQuery } from './client';
import type { PageResponse } from './types';
import type {
  AcceptanceView,
  AccountHistory,
  BatchOutcome,
  BucketRuleData,
  CandidateDetail,
  CandidateRow,
  CheckSettingView,
  ChecksView,
  CorrectionView,
  Computations,
  DecisionRuleData,
  Disposition,
  ExtractionRunView,
  FollowupView,
  HistoryView,
  InsurerBatchDetail,
  InsurerBatchView,
  InsurerResponseView,
  LamdLineView,
  LamdReportView,
  LetterView,
  Officer,
  PackageChoiceView,
  PackageMapData,
  PackageMapView,
  RemarkView,
  RenewalFilters,
  RenewalHome,
  RiskCodeView,
  TransferView,
  VersionView,
} from './renewalTypes';

export type * from './renewalTypes';

/**
 * Renewal API (BRD-6; docs/architecture/RENEWAL_DESIGN.md sections 11 and 12): the renewal lists
 * and record, extraction, marketing, processing, insurer, LAMD, letters, acceptance, Setup and the
 * package choices of migrated policies.
 */
const BASE = '/renewal';
const C = '/renewal/candidates';

const ref = (renewalRef: string) => `${C}/${encodeURIComponent(renewalRef)}`;
const co = (companyId: number) => toQuery({ companyId });

export const renewalApi = {
  codeSet: (source: string, companyId: number) =>
    api.get<{ code: string; label: string }[]>(`/reports/code-sets/${source}${co(companyId)}`),
  home: (companyId: number) => api.get<RenewalHome>(`${BASE}/home${co(companyId)}`),
  list: (companyId: number, filters: RenewalFilters, page: number, size = 200) =>
    api.get<PageResponse<CandidateRow>>(`${C}${toQuery({ companyId, ...filters, page, size })}`),
  count: (companyId: number, filters: RenewalFilters) =>
    api.get<number>(`${C}/count${toQuery({ companyId, ...filters })}`),
  exportList: (companyId: number, filters: RenewalFilters) =>
    api.getFile(`${C}/export.xlsx${toQuery({ companyId, ...filters })}`),
  get: (companyId: number, renewalRef: string) =>
    api.get<CandidateDetail>(`${ref(renewalRef)}${co(companyId)}`),
  checks: (companyId: number, renewalRef: string) =>
    api.get<ChecksView>(`${ref(renewalRef)}/checks${co(companyId)}`),
  history: (companyId: number, renewalRef: string) =>
    api.get<HistoryView>(`${ref(renewalRef)}/history${co(companyId)}`),
  accountHistory: (companyId: number, renewalRef: string) =>
    api.post<AccountHistory>(`${ref(renewalRef)}/account-history${co(companyId)}`),
  details: (companyId: number, renewalRef: string) =>
    api.getFile(`${ref(renewalRef)}/details.pdf${co(companyId)}`),
  initiate: (companyId: number, renewalRefs: string[]) =>
    api.post<{ initiated: string[]; refused: Record<string, string> }>(`${C}/initiate`, {
      companyId,
      renewalRefs,
    }),
  officers: (companyId: number) => api.get<Officer[]>(`${BASE}/officers${co(companyId)}`),
  assign: (companyId: number, renewalRefs: string[], ao: string, reasonCode?: string) =>
    api.post<BatchOutcome>(`${C}/assign`, { companyId, renewalRefs, ao, reasonCode }),
  dispose: (
    companyId: number,
    renewalRef: string,
    input: {
      code: Disposition;
      reasonCode?: string;
      newInvoiceNo?: string;
      receivingUnit?: string;
      remarks?: string;
    },
  ) => api.post<{ stage: string }>(`${ref(renewalRef)}/disposition${co(companyId)}`, input),
  push: (companyId: number, renewalRefs: string[]) =>
    api.post<BatchOutcome>(`${C}/push`, { companyId, renewalRefs }),
  remarks: (companyId: number, renewalRef: string) =>
    api.get<RemarkView[]>(`${ref(renewalRef)}/remarks${co(companyId)}`),
  addRemark: (companyId: number, renewalRef: string, text: string) =>
    api.post<RemarkView>(`${ref(renewalRef)}/remarks${co(companyId)}`, { text }),
  reopen: (companyId: number, renewalRef: string, text: string) =>
    api.post<{ stage: string }>(`${ref(renewalRef)}/reopen${co(companyId)}`, { text }),
  returnToAo: (companyId: number, renewalRefs: string[], reasonCode: string, remarks: string) =>
    api.post<BatchOutcome>(`${C}/return`, { companyId, renewalRefs, reasonCode, remarks }),
  post: (companyId: number, renewalRefs: string[]) =>
    api.post<BatchOutcome>(`${C}/post`, { companyId, renewalRefs }),
  override: (
    companyId: number,
    renewalRefs: string[],
    input: { kind: string; target?: string; reasonCode: string; remarks: string },
  ) => api.post<BatchOutcome>(`${C}/override`, { companyId, renewalRefs, ...input }),
  startNbPath: (companyId: number, renewalRef: string, kind?: 'QUOTATION' | 'PROPOSAL') =>
    api.post<{ reference: string }>(`${ref(renewalRef)}/nb-path${toQuery({ companyId, kind })}`),
  transfersIn: (companyId: number) =>
    api.get<TransferView[]>(`${BASE}/transfers/incoming${co(companyId)}`),
  transfersOut: (companyId: number) =>
    api.get<TransferView[]>(`${BASE}/transfers/outgoing${co(companyId)}`),
  requestTransfer: (
    companyId: number,
    input: { renewalRef: string; toUnit: string; reasonCode?: string; remarks: string },
  ) => api.post<TransferView>(`${BASE}/transfers${co(companyId)}`, input),
  acceptTransfer: (id: number, remarks?: string) =>
    api.post<TransferView>(`${BASE}/transfers/${String(id)}/accept`, { remarks }),
  declineTransfer: (id: number, remarks: string) =>
    api.post<TransferView>(`${BASE}/transfers/${String(id)}/decline`, { remarks }),
  cancelTransfer: (id: number) => api.post<TransferView>(`${BASE}/transfers/${String(id)}/cancel`),
  processingOfficers: () => api.get<Officer[]>(`${BASE}/processing/officers`),
  assignPo: (companyId: number, renewalRefs: string[], po?: string) =>
    api.post<BatchOutcome>(`${BASE}/processing/assign`, { companyId, renewalRefs, po }),
  returnToMarketing: (
    companyId: number,
    renewalRefs: string[],
    input: { toLeader: boolean; reasonCode: string; remarks: string },
  ) => api.post<BatchOutcome>(`${C}/return-to-marketing`, { companyId, renewalRefs, ...input }),
  computations: (companyId: number, renewalRef: string) =>
    api.get<Computations>(`${ref(renewalRef)}/computations${co(companyId)}`),
  createAccount: (companyId: number, renewalRef: string) =>
    api.post<{ arn: string; id: number }>(`${ref(renewalRef)}/renewal-account${co(companyId)}`),
  completeFile: (
    jobId: number,
    input: { companyId: number; expiryFrom: string; expiryTo: string; unit: string },
  ) =>
    api.post<{ jobNo: string; tagged: number }>(
      `${BASE}/uploads/${String(jobId)}/complete-file`,
      input,
    ),
  batches: (companyId: number) =>
    api.get<InsurerBatchView[]>(`${BASE}/insurer-batches${co(companyId)}`),
  batch: (companyId: number, batchNo: string) =>
    api.get<InsurerBatchDetail>(
      `${BASE}/insurer-batches/${encodeURIComponent(batchNo)}${co(companyId)}`,
    ),
  createBatch: (
    companyId: number,
    input: { insurerCode: string; expiryFrom: string; expiryTo: string },
  ) => api.post<InsurerBatchView>(`${BASE}/insurer-batches`, { companyId, ...input }),
  batchFile: (companyId: number, batchNo: string) =>
    api.getFile(`${BASE}/insurer-batches/${encodeURIComponent(batchNo)}/file.xlsx${co(companyId)}`),
  sendBatch: (companyId: number, batchNo: string) =>
    api.post<InsurerBatchView>(
      `${BASE}/insurer-batches/${encodeURIComponent(batchNo)}/send${co(companyId)}`,
    ),
  responses: (companyId: number, renewalRef: string) =>
    api.get<InsurerResponseView[]>(`${ref(renewalRef)}/insurer-responses${co(companyId)}`),
  recordResponse: (
    companyId: number,
    renewalRef: string,
    input: {
      response: string;
      insurerRef?: string;
      revisedPremium?: number;
      revisedSumInsured?: number;
      revisedRate?: number;
      receivedOn?: string;
      remarks?: string;
      remarket: boolean;
    },
  ) => api.post<InsurerResponseView>(`${ref(renewalRef)}/insurer-responses${co(companyId)}`, input),
  lamdReports: (companyId: number) =>
    api.get<LamdReportView[]>(`${BASE}/lamd-reports${co(companyId)}`),
  lamdLines: (companyId: number, reportNo: string) =>
    api.get<LamdLineView[]>(
      `${BASE}/lamd-reports/${encodeURIComponent(reportNo)}/lines${co(companyId)}`,
    ),
  generateRa: (
    companyId: number,
    renewalRefs: string[],
    notice: 'FIRST' | 'SECOND',
    confirmLate: boolean,
  ) =>
    api.post<BatchOutcome>(`${BASE}/letters/ra`, { companyId, renewalRefs, notice, confirmLate }),
  sendRa: (companyId: number, renewalRefs: string[]) =>
    api.post<BatchOutcome>(`${BASE}/letters/send`, { companyId, renewalRefs }),
  closingLetters: (companyId: number, renewalRefs: string[]) =>
    api.post<BatchOutcome>(`${BASE}/letters/closing`, { companyId, renewalRefs }),
  letters: (companyId: number, renewalRef: string) =>
    api.get<LetterView[]>(`${ref(renewalRef)}/letters${co(companyId)}`),
  letterFile: (companyId: number, letterNo: string) =>
    api.getFile(`${BASE}/letters/${encodeURIComponent(letterNo)}/file.pdf${co(companyId)}`),
  acceptances: (companyId: number, renewalRef: string) =>
    api.get<AcceptanceView[]>(`${ref(renewalRef)}/acceptance${co(companyId)}`),
  accept: (
    companyId: number,
    renewalRef: string,
    input: {
      method: string;
      attachmentId?: number;
      reference?: string;
      acceptedOn?: string;
      remarks?: string;
      financialImpactAck: boolean;
    },
  ) => api.post<AcceptanceView>(`${ref(renewalRef)}/acceptance${co(companyId)}`, input),
  followups: (companyId: number, renewalRef: string) =>
    api.get<FollowupView[]>(`${ref(renewalRef)}/followups${co(companyId)}`),
  addFollowup: (
    companyId: number,
    renewalRef: string,
    input: { channel: string; outcome: string; remarks: string; nextActionDate?: string },
  ) => api.post<FollowupView>(`${ref(renewalRef)}/followups${co(companyId)}`, input),
  runs: (companyId: number) =>
    api.get<PageResponse<ExtractionRunView>>(`${BASE}/extraction/runs${co(companyId)}`),
  extract: (companyId: number, expiryFrom: string, expiryTo: string) =>
    api.post<ExtractionRunView>(`${BASE}/extraction/runs`, { companyId, expiryFrom, expiryTo }),
  source: () => api.get<{ connected: boolean }>(`${BASE}/extraction/source`),
  goLive: (companyId: number, goLive: string) =>
    api.post<ExtractionRunView>(`${BASE}/extraction/go-live`, { companyId, goLive }),
  corrections: (companyId: number, status = 'PENDING') =>
    api.get<CorrectionView[]>(
      `${BASE}/extraction/ra-sent-corrections${toQuery({ companyId, status })}`,
    ),
  decideCorrection: (id: number, approve: boolean, remarks?: string) =>
    api.post<CorrectionView>(`${BASE}/extraction/ra-sent-corrections/${String(id)}/decision`, {
      approve,
      remarks,
    }),
  riskCodes: (companyId: number) =>
    api.get<RiskCodeView[]>(`${BASE}/setup/risk-codes${co(companyId)}`),
  saveRiskCode: (
    companyId: number,
    id: number | null,
    data: {
      riskCode: string;
      lineCode?: string;
      reason: string;
      effectiveFrom: string;
      effectiveTo?: string;
    },
  ) =>
    id === null
      ? api.post<RiskCodeView>(`${BASE}/setup/risk-codes${co(companyId)}`, data)
      : api.put<RiskCodeView>(`${BASE}/setup/risk-codes/${String(id)}${co(companyId)}`, data),
  riskCodeAction: (companyId: number, id: number, action: 'AUTHORIZE' | 'DEACTIVATE') =>
    api.post<RiskCodeView>(`${BASE}/setup/risk-codes/${String(id)}/${action}${co(companyId)}`),
  checkSettings: () => api.get<CheckSettingView[]>(`${BASE}/setup/checks`),
  updateCheck: (code: string, input: { active: boolean; severity: string; parameters?: string }) =>
    api.put<CheckSettingView>(`${BASE}/setup/checks/${code}`, input),
  authorizeCheck: (code: string) =>
    api.post<CheckSettingView>(`${BASE}/setup/checks/${code}/authorize`),
  bucketRules: (companyId: number) =>
    api.get<VersionView<BucketRuleData>[]>(`${BASE}/setup/bucket-rules${co(companyId)}`),
  saveBucketRules: (
    companyId: number,
    input: { id?: number; effectiveFrom?: string; description?: string; rules: BucketRuleData[] },
  ) => api.post<VersionView<BucketRuleData>>(`${BASE}/setup/bucket-rules${co(companyId)}`, input),
  decideBucketRules: (companyId: number, id: number, step: string, remarks?: string) =>
    api.post<VersionView<BucketRuleData>>(
      `${BASE}/setup/bucket-rules/${String(id)}/${step}${co(companyId)}`,
      { remarks },
    ),
  matrices: (companyId: number) =>
    api.get<VersionView<DecisionRuleData>[]>(`${BASE}/setup/matrix${co(companyId)}`),
  saveMatrix: (
    companyId: number,
    input: { id?: number; effectiveFrom?: string; description?: string; rules: DecisionRuleData[] },
  ) => api.post<VersionView<DecisionRuleData>>(`${BASE}/setup/matrix${co(companyId)}`, input),
  decideMatrix: (companyId: number, id: number, step: string, remarks?: string) =>
    api.post<VersionView<DecisionRuleData>>(
      `${BASE}/setup/matrix/${String(id)}/${step}${co(companyId)}`,
      { remarks },
    ),
  packageMap: (companyId: number) =>
    api.get<PackageMapView[]>(`${BASE}/setup/package-map${co(companyId)}`),
  saveMapEntry: (companyId: number, id: number | null, data: PackageMapData) =>
    id === null
      ? api.post<PackageMapView>(`${BASE}/setup/package-map${co(companyId)}`, data)
      : api.put<PackageMapView>(`${BASE}/setup/package-map/${String(id)}${co(companyId)}`, data),
  mapEntryAction: (companyId: number, id: number, action: 'AUTHORIZE' | 'DEACTIVATE') =>
    api.post<PackageMapView>(`${BASE}/setup/package-map/${String(id)}/${action}${co(companyId)}`),
  packageChoices: (companyId: number, renewalRef: string) =>
    api.get<PackageChoiceView[]>(`${ref(renewalRef)}/package-choices${co(companyId)}`),
  proposePackage: (
    companyId: number,
    renewalRef: string,
    input: { productCode: string; productVersionNo: number; reason: string },
  ) => api.post<PackageChoiceView>(`${ref(renewalRef)}/package-choices${co(companyId)}`, input),
  pendingChoices: () =>
    api.get<{ choice: PackageChoiceView; renewalRef: string; clientName: string }[]>(
      `${BASE}/package-choices/pending`,
    ),
  decideChoice: (id: number, approve: boolean, remarks?: string) =>
    api.post<PackageChoiceView>(`${BASE}/package-choices/${String(id)}/decision`, {
      approve,
      remarks,
    }),
};
