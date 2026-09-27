import { api, toQuery } from './client';
import type { PageResponse } from './types';
import type {
  ExtractionView,
  HistoryRow,
  LimitCheckView,
  MasterlistFilters,
  MasterlistTab,
  PolicyData,
  PolicyDetail,
  PolicyRow,
  ResultView,
  RuleInput,
  RuleSetCreate,
  RuleSetView,
  RuleView,
  RunView,
  Tracking,
} from './submittedTypes';
import type {
  AmbiguousView,
  Controlled,
  FeeView,
  HomeCounts,
  IaafView,
  InsurerRule,
  IntakeRunView,
  LetterRule,
  LetterView,
  LimitRule,
  MatrixRow,
  NoTouchBatch,
  NoTouchLine,
  PrintBatchView,
  RenewalRow,
  ReviewView,
  ScopeView,
  SourceView,
  StatusMapView,
  TorView,
} from './submittedWorkTypes';

export type * from './submittedTypes';
export type * from './submittedWorkTypes';

/**
 * Submitted Policies API (BRD-12; docs/architecture/SUBMITTED_POLICIES_DESIGN.md sections 7 and
 * 12): masterlist, extraction, processing runs, reviews / IAAF / TOR, renewal work list, letters,
 * handling fees, No Touch billing and setup.
 */
const B = '/submitted';
const P = `${B}/policies`;
const RS = `${B}/setup/rule-sets`;
const co = (companyId: number) => toQuery({ companyId });
const statuses = (companyId: number, status: readonly string[], page = 0, size = 50) =>
  toQuery({ companyId, status: status.join(','), page, size });

export const submittedApi = {
  home: (companyId: number) => api.get<HomeCounts>(`${B}/home${co(companyId)}`),
  counts: (companyId: number, f: MasterlistFilters = {}) =>
    api.get<Record<MasterlistTab, number>>(`${P}/counts${toQuery({ companyId, ...f })}`),
  list: (companyId: number, f: MasterlistFilters, page: number, size = 50) =>
    api.get<PageResponse<PolicyRow>>(`${P}${toQuery({ companyId, ...f, page, size })}`),
  get: (id: number) => api.get<PolicyDetail>(`${P}/${String(id)}`),
  create: (companyId: number, body: PolicyData & { tracking: Tracking | null }) =>
    api.post<PolicyDetail>(`${P}${co(companyId)}`, body),
  update: (id: number, body: PolicyData & { tracking: Tracking | null }) =>
    api.put<PolicyDetail>(`${P}/${String(id)}`, body),
  tag: (id: number, tag: string, reason: string | null) =>
    api.post<PolicyDetail>(`${P}/${String(id)}/renewal-tag`, { tag, reason }),
  track: (id: number, tracking: Tracking) =>
    api.put<PolicyDetail>(`${P}/${String(id)}/tracking`, tracking),
  assign: (companyId: number, ids: number[], handler: string) =>
    api.post<Record<string, number>>(`${P}/assign`, { companyId, ids, handler }),
  act: (id: number, action: string, reasonCode?: string, comment?: string) =>
    api.post<PolicyDetail>(`${P}/${String(id)}/actions/${action}`, { reasonCode, comment }),
  history: (id: number) => api.get<HistoryRow[]>(`${P}/${String(id)}/history`),

  extractions: (companyId: number, decided: boolean) =>
    api.get<PageResponse<ExtractionView>>(
      `${B}/extractions${toQuery({ companyId, decided, size: 50 })}`,
    ),
  extractionsOf: (policyId: number) =>
    api.get<ExtractionView[]>(`${B}/extractions/of-policy/${String(policyId)}`),
  extract: (companyId: number, file: File, segment: string, businessType: string) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<ExtractionView>(
      `${B}/extractions${toQuery({ companyId, segment, businessType })}`,
      form,
    );
  },
  confirm: (id: number, data: PolicyData) =>
    api.post<PolicyDetail>(`${B}/extractions/${String(id)}/confirm`, { ...data, tracking: null }),
  reject: (id: number, reason: string) =>
    api.post<ExtractionView>(`${B}/extractions/${String(id)}/reject`, { reason }),

  runs: (companyId: number) =>
    api.get<PageResponse<RunView>>(`${B}/runs${toQuery({ companyId, size: 50 })}`),
  run: (companyId: number, policyIds: number[]) =>
    api.post<RunView>(`${B}/runs`, { companyId, policyIds }),
  results: (runId: number, outcome?: string) =>
    api.get<PageResponse<ResultView>>(
      `${B}/runs/${String(runId)}/results${toQuery({ outcome, size: 200 })}`,
    ),
  resultsOf: (policyId: number) =>
    api.get<{ results: ResultView[]; limitChecks: LimitCheckView[] }>(
      `${B}/runs/of-policy/${String(policyId)}`,
    ),

  reviews: (policyId: number) => api.get<ReviewView[]>(`${P}/${String(policyId)}/reviews`),
  review: (
    policyId: number,
    body: { reviewDate: string; adequacy: string; findings: string[]; remarks: string },
  ) => api.post<ReviewView>(`${P}/${String(policyId)}/reviews`, body),
  iaafOf: (policyId: number) => api.get<IaafView[]>(`${P}/${String(policyId)}/iaaf`),
  generateIaaf: (policyId: number) =>
    api.post<IaafView>(`${P}/${String(policyId)}/iaaf`, { related: {} }),
  iaafs: (companyId: number, status: readonly string[]) =>
    api.get<PageResponse<IaafView>>(`${B}/iaaf${statuses(companyId, status)}`),
  iaafAction: (id: number, action: 'submit' | 'approve' | 'issue') =>
    api.post<IaafView>(`${B}/iaaf/${String(id)}/${action}`),
  iaafReturn: (id: number, action: 'return' | 'cancel', reasonCode: string, comment?: string) =>
    api.post<IaafView>(`${B}/iaaf/${String(id)}/${action}`, { reasonCode, comment }),

  torsOf: (policyId: number) => api.get<TorView[]>(`${P}/${String(policyId)}/tors`),
  breaches: (policyId: number) =>
    api.get<{ breaches: string }>(`${P}/${String(policyId)}/breaches`),
  generateTor: (policyId: number, proposedTerms: string, aoUsername: string) =>
    api.post<TorView>(`${B}/tors`, { policyId, proposedTerms, aoUsername }),
  tors: (companyId: number, status: readonly string[]) =>
    api.get<PageResponse<TorView>>(`${B}/tors${statuses(companyId, status)}`),
  torAction: (id: number, action: 'submit' | 'approve') =>
    api.post<TorView>(`${B}/tors/${String(id)}/${action}`),
  torReturn: (id: number, action: 'return' | 'cancel', reasonCode: string, comment?: string) =>
    api.post<TorView>(`${B}/tors/${String(id)}/${action}`, { reasonCode, comment }),
  torPdf: (id: number) => api.getFile(`${B}/tors/${String(id)}/pdf`),

  renewals: (companyId: number, status: readonly string[], page = 0) =>
    api.get<PageResponse<RenewalRow>>(`${B}/renewals${statuses(companyId, status, page)}`),
  renewalOf: (policyId: number) => api.get<RenewalRow[]>(`${P}/${String(policyId)}/renewal`),
  renew: (policyId: number) => api.post<RenewalRow>(`${P}/${String(policyId)}/renew`),
  reassign: (policyId: number, insurerCode: string, reasonCode: string) =>
    api.post<RenewalRow>(`${P}/${String(policyId)}/reassign`, { insurerCode, reasonCode }),
  scan: (companyId: number) =>
    api.post<{ handedOff: number; noInsurer: number; replayed: number }>(
      `${B}/renewals/scan${co(companyId)}`,
    ),

  lettersOf: (policyId: number) => api.get<LetterView[]>(`${P}/${String(policyId)}/letters`),
  letters: (companyId: number, status: readonly string[]) =>
    api.get<PageResponse<LetterView>>(`${B}/letters${statuses(companyId, status)}`),
  resend: (id: number) => api.post<LetterView>(`${B}/letters/${String(id)}/resend`),
  letterPdf: (id: number) => api.getFile(`${B}/letters/${String(id)}/pdf`),
  dispatch: (companyId: number) =>
    api.post<{ letters: number; printBatches: number }>(`${B}/letters/dispatch${co(companyId)}`),
  printBatches: (companyId: number) =>
    api.get<PageResponse<PrintBatchView>>(`${B}/print-batches${toQuery({ companyId, size: 50 })}`),
  printBatchFile: (id: number, part: 'merged' | 'control') =>
    api.getFile(`${B}/print-batches/${String(id)}/${part}`),

  fees: (companyId: number, status: readonly string[]) =>
    api.get<PageResponse<FeeView>>(`${B}/handling-fees${statuses(companyId, status)}`),
  ambiguous: (companyId: number) =>
    api.get<AmbiguousView[]>(`${B}/handling-fees/ambiguous${co(companyId)}`),
  tagFees: (companyId: number) =>
    api.post<{ tagged: number; ambiguous: string[] }>(`${B}/handling-fees/tag${co(companyId)}`),
  tagFee: (id: number, unappliedRef: string) =>
    api.post<FeeView>(`${B}/handling-fees/${String(id)}/tag`, { unappliedRef }),
  cancelFee: (id: number, reason: string) =>
    api.post<FeeView>(`${B}/handling-fees/${String(id)}/cancel`, { reason }),

  noTouch: (companyId: number) => api.get<NoTouchBatch[]>(`${B}/no-touch${co(companyId)}`),
  exportNoTouch: (companyId: number, insurerCode: string, period: string) =>
    api.post<NoTouchBatch>(`${B}/no-touch${co(companyId)}`, { insurerCode, period }),
  noTouchLines: (id: number) => api.get<NoTouchLine[]>(`${B}/no-touch/${String(id)}/lines`),
  billNoTouch: (id: number) => api.post<NoTouchBatch>(`${B}/no-touch/${String(id)}/bill`),
  noTouchFile: (id: number, part: 'export' | 'statement') =>
    api.getFile(`${B}/no-touch/${String(id)}/${part}`),

  users: (permission: string) =>
    api.get<{ username: string; displayName: string }[]>(`${B}/users${toQuery({ permission })}`),
  intakeRuns: (companyId: number) =>
    api.get<PageResponse<IntakeRunView>>(`${B}/intake-runs${toQuery({ companyId, size: 50 })}`),

  ruleSets: (companyId: number) => api.get<RuleSetView[]>(`${B}/setup/rule-sets${co(companyId)}`),
  ruleSet: (id: number) => api.get<RuleSetView>(`${RS}/${String(id)}`),
  vocabulary: () => api.get<{ facts: string[]; operators: string[] }>(`${RS}/vocabulary`),
  createRuleSet: (body: RuleSetCreate) => api.post<RuleSetView>(RS, body),
  newVersion: (id: number, effectiveFrom: string) =>
    api.post<RuleSetView>(`${RS}/${String(id)}/versions`, { effectiveFrom }),
  describeRuleSet: (id: number, effectiveFrom: string, description: string) =>
    api.put<RuleSetView>(`${RS}/${String(id)}`, { effectiveFrom, description }),
  addRule: (id: number, rule: RuleInput) => api.post<RuleView>(`${RS}/${String(id)}/rules`, rule),
  changeRule: (ruleId: number, rule: RuleInput) =>
    api.put<RuleView>(`${RS}/rules/${String(ruleId)}`, rule),
  removeRule: (ruleId: number) => api.delete(`${RS}/rules/${String(ruleId)}`),
  ruleSetDecision: (id: number, decision: 'submit' | 'approve' | 'reject', remarks?: string) =>
    api.post<RuleSetView>(`${B}/setup/rule-sets/${String(id)}/${decision}`, { remarks }),
  limits: (companyId: number) =>
    api.get<Controlled<LimitRule>[]>(`${B}/setup/limits${co(companyId)}`),
  insurerRules: (companyId: number) =>
    api.get<Controlled<InsurerRule>[]>(`${B}/setup/insurers${co(companyId)}`),
  letterRules: (companyId: number) =>
    api.get<Controlled<LetterRule>[]>(`${B}/setup/letters${co(companyId)}`),
  matrix: (companyId: number) =>
    api.get<Controlled<MatrixRow>[]>(`${B}/setup/matrix${co(companyId)}`),
  saveSetup: (
    kind: 'limits' | 'insurers' | 'letters' | 'matrix',
    companyId: number,
    body: unknown,
  ) => api.post<{ id: number }>(`${B}/setup/${kind}${co(companyId)}`, body),
  setupDecision: (kind: string, id: number, decision: 'authorize' | 'deactivate') =>
    api.post<{ id: number }>(`${B}/setup/${kind}/${String(id)}/${decision}`),
  sources: () => api.get<SourceView[]>(`${B}/setup/sources`),
  statusMap: () => api.get<StatusMapView[]>(`${B}/setup/status-map`),
  scopes: (companyId: number) => api.get<ScopeView[]>(`${B}/setup/scopes${co(companyId)}`),
  saveScope: (companyId: number, scope: ScopeView) =>
    api.post<ScopeView>(`${B}/setup/scopes${co(companyId)}`, scope),
};
