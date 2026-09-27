import { api, toQuery } from './client';
import type { PageResponse } from './types';
import type {
  Batch,
  BatchLog,
  DataObject,
  Decision,
  Extract,
  GateRow,
  HomeTiles,
  Issue,
  Layout,
  LayoutColumn,
  MapDiff,
  MapEntry,
  MapEntryInput,
  MapSet,
  MapVersion,
  MaskingRule,
  MatchPair,
  MatchSides,
  ReconLine,
  ReconRun,
  Resubmission,
  Rule,
  Signoff,
  StageRow,
  UnmappedCode,
  Xref,
} from './migrationTypes';

export type * from './migrationTypes';

/**
 * Data Migration console API (BRD-13; docs/architecture/DATA_MIGRATION_DESIGN.md sections 5 to 13
 * and 22): data objects and decisions, code maps, layouts and load templates, extracts, batches
 * with their gates, reconciliation, client matching and sign-off.
 */
const BASE = '/migration';

export interface SignInput {
  approve: boolean;
  role?: string;
  comment?: string;
}

export const migrationApi = {
  home: (companyId: number) => api.get<HomeTiles>(`${BASE}/home${toQuery({ companyId })}`),

  objects: () => api.get<DataObject[]>(`${BASE}/objects`),
  object: (code: string) => api.get<DataObject>(`${BASE}/objects/${code}`),
  decisions: (code: string) => api.get<Decision[]>(`${BASE}/objects/${code}/decisions`),
  pendingDecisions: () => api.get<Decision[]>(`${BASE}/decisions/pending`),
  submitDecision: (companyId: number, code: string, conditionMet: boolean) =>
    api.post<Decision>(`${BASE}/objects/${code}/decision${toQuery({ companyId })}`, {
      conditionMet,
    }),
  approveDecision: (no: string, comment?: string) =>
    api.post<Decision>(`${BASE}/decisions/${no}/approve`, { comment }),
  returnDecision: (no: string, reason: string) =>
    api.post<Decision>(`${BASE}/decisions/${no}/return`, { reason }),
  objectTemplate: (code: string) => api.getFile(`${BASE}/objects/${code}/template`),

  mapSets: () => api.get<MapSet[]>(`${BASE}/maps`),
  mapVersions: (set: string) =>
    api.get<MapVersion[]>(`${BASE}/maps/${encodeURIComponent(set)}/versions`),
  mapEntries: (versionId: number) =>
    api.get<MapEntry[]>(`${BASE}/maps/versions/${String(versionId)}/entries`),
  mapDiff: (versionId: number) =>
    api.get<MapDiff>(`${BASE}/maps/versions/${String(versionId)}/diff`),
  createDraft: (companyId: number, set: string, copyApproved: boolean, comment?: string) =>
    api.post<MapVersion>(
      `${BASE}/maps/${encodeURIComponent(set)}/versions${toQuery({ companyId })}`,
      { copyApproved, comment },
    ),
  importDraft: (companyId: number, set: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<MapVersion>(
      `${BASE}/maps/${encodeURIComponent(set)}/import${toQuery({ companyId })}`,
      form,
    );
  },
  exportVersion: (versionId: number) =>
    api.getFile(`${BASE}/maps/versions/${String(versionId)}/export`),
  addEntry: (versionId: number, entry: MapEntryInput) =>
    api.post<MapEntry>(`${BASE}/maps/versions/${String(versionId)}/entries`, entry),
  updateEntry: (versionId: number, entryId: number, entry: MapEntryInput) =>
    api.put<MapEntry>(
      `${BASE}/maps/versions/${String(versionId)}/entries/${String(entryId)}`,
      entry,
    ),
  deleteEntry: (versionId: number, entryId: number) =>
    api.delete(`${BASE}/maps/versions/${String(versionId)}/entries/${String(entryId)}`),
  submitVersion: (versionId: number) =>
    api.post<MapVersion>(`${BASE}/maps/versions/${String(versionId)}/submit`),
  approveVersion: (versionId: number, comment?: string) =>
    api.post<{ version: MapVersion; toCreate: string[] }>(
      `${BASE}/maps/versions/${String(versionId)}/approve`,
      { comment },
    ),
  returnVersion: (versionId: number, reason: string) =>
    api.post<MapVersion>(`${BASE}/maps/versions/${String(versionId)}/return`, { reason }),
  unmapped: (companyId: number) =>
    api.get<UnmappedCode[]>(`${BASE}/maps/unmapped${toQuery({ companyId })}`),

  layouts: () => api.get<Layout[]>(`${BASE}/layouts`),
  layoutColumns: (id: number) => api.get<LayoutColumn[]>(`${BASE}/layouts/${String(id)}/columns`),
  freezeLayout: (id: number) => api.post<Layout>(`${BASE}/layouts/${String(id)}/freeze`),
  layoutTemplate: (code: string) => api.getFile(`${BASE}/templates/layouts/${code}`),
  controlTemplate: () => api.getFile(`${BASE}/templates/control`),
  workbook: () => api.getFile(`${BASE}/templates/workbook`),
  rules: () => api.get<Rule[]>(`${BASE}/rules`),
  updateRule: (code: string, severity: string, active: boolean) =>
    api.put<Rule>(`${BASE}/rules/${code}`, { severity, active }),
  maskingRules: () => api.get<MaskingRule[]>(`${BASE}/masking-rules`),

  extracts: (companyId: number, objectCode: string | undefined, page: number, size = 25) =>
    api.get<PageResponse<Extract>>(
      `${BASE}/extracts${toQuery({ companyId, objectCode, page, size })}`,
    ),
  uploadExtract: (
    companyId: number,
    input: { objectCode?: string; mode: string; file: File; control?: File },
  ) => {
    const form = new FormData();
    form.append('file', input.file);
    if (input.control) {
      form.append('control', input.control);
    }
    return api.upload<Extract>(
      `${BASE}/extracts${toQuery({ companyId, objectCode: input.objectCode, mode: input.mode })}`,
      form,
    );
  },
  extractFile: (extractNo: string, kind: 'data' | 'control') =>
    api.getFile(`${BASE}/extracts/${extractNo}/files/${kind}`),

  batches: (companyId: number, page: number, size = 25) =>
    api.get<PageResponse<Batch>>(`${BASE}/batches${toQuery({ companyId, page, size })}`),
  batch: (batchNo: string) => api.get<Batch>(`${BASE}/batches/${batchNo}`),
  plan: (companyId: number, objectCode: string, extractNos: string[]) =>
    api.post<Batch>(`${BASE}/batches${toQuery({ companyId })}`, { objectCode, extractNos }),
  batchLog: (batchNo: string) => api.get<BatchLog[]>(`${BASE}/batches/${batchNo}/log`),
  batchRows: (batchNo: string, status: string | undefined, page: number, size = 50) =>
    api.get<PageResponse<StageRow>>(
      `${BASE}/batches/${batchNo}/rows${toQuery({ status, page, size })}`,
    ),
  batchIssues: (batchNo: string, page: number, size = 50) =>
    api.get<PageResponse<Issue>>(`${BASE}/batches/${batchNo}/issues${toQuery({ page, size })}`),
  validate: (batchNo: string) => api.post<Batch>(`${BASE}/batches/${batchNo}/validate`),
  waive: (batchNo: string, rowIds: number[], reason: string, note: string) =>
    api.post<Batch>(`${BASE}/batches/${batchNo}/waive`, { rowIds, reason, note }),
  exclude: (batchNo: string, rowIds: number[], reason: string, note: string) =>
    api.post<Batch>(`${BASE}/batches/${batchNo}/exclude`, { rowIds, reason, note }),
  resolveIssue: (issueId: number, resolution: string, note?: string) =>
    api.post<Issue>(`${BASE}/issues/${String(issueId)}/resolve`, { resolution, note }),
  signValidation: (batchNo: string, input: SignInput) =>
    api.post<Signoff>(`${BASE}/batches/${batchNo}/signoff/validation`, input),
  approveLoad: (batchNo: string, comment?: string) =>
    api.post<Batch>(`${BASE}/batches/${batchNo}/approve-load`, { approve: true, comment }),
  load: (batchNo: string) => api.post<Batch>(`${BASE}/batches/${batchNo}/load`),
  rerun: (batchNo: string) => api.post<Batch>(`${BASE}/batches/${batchNo}/rerun`),
  requestRollback: (batchNo: string, reason: string) =>
    api.post<Batch>(`${BASE}/batches/${batchNo}/rollback`, { reason }),
  approveRollback: (batchNo: string, comment?: string) =>
    api.post<Batch>(`${BASE}/batches/${batchNo}/rollback/approve`, { approve: true, comment }),
  rejectRollback: (batchNo: string, reason: string) =>
    api.post<Batch>(`${BASE}/batches/${batchNo}/rollback/reject`, { reason }),
  signReconciliation: (batchNo: string, input: SignInput) =>
    api.post<Signoff>(`${BASE}/batches/${batchNo}/signoff/reconciliation`, input),
  signAcceptance: (batchNo: string, input: SignInput) =>
    api.post<Signoff>(`${BASE}/batches/${batchNo}/signoff/acceptance`, input),
  batchSignoffs: (batchNo: string) => api.get<Signoff[]>(`${BASE}/batches/${batchNo}/signoffs`),
  rejects: (batchNo: string) => api.getFile(`${BASE}/batches/${batchNo}/rejects`),
  resubmit: (batchNo: string, extractNo: string) =>
    api.post<Resubmission>(`${BASE}/batches/${batchNo}/resubmissions`, { extractNo }),
  resubmissions: (companyId: number) =>
    api.get<Resubmission[]>(`${BASE}/resubmissions${toQuery({ companyId })}`),
  decideResubmission: (no: string, approve: boolean, note?: string) =>
    api.post<Resubmission>(`${BASE}/resubmissions/${no}/decide`, { approve, note }),

  reconciliation: (batchNo: string) =>
    api.get<ReconRun | undefined>(`${BASE}/batches/${batchNo}/reconciliation`),
  reconcile: (batchNo: string) => api.post<ReconRun>(`${BASE}/batches/${batchNo}/reconcile`),
  explainBreak: (lineId: number, reason: string, text: string) =>
    api.post<ReconLine>(`${BASE}/recon-lines/${String(lineId)}/explain`, { reason, text }),
  approveBreak: (lineId: number) =>
    api.post<ReconLine>(`${BASE}/recon-lines/${String(lineId)}/approve`),

  matches: (batchNo?: string) => api.get<MatchPair[]>(`${BASE}/matches${toQuery({ batchNo })}`),
  matchSides: (pairId: number) => api.get<MatchSides>(`${BASE}/matches/${String(pairId)}/sides`),
  decideMatch: (pairId: number, merge: boolean) =>
    api.post<MatchPair>(`${BASE}/matches/${String(pairId)}/decide${toQuery({ merge })}`),

  gateMatrix: (companyId: number) =>
    api.get<GateRow[]>(`${BASE}/signoffs/matrix${toQuery({ companyId })}`),
  signoffs: (companyId: number) => api.get<Signoff[]>(`${BASE}/signoffs${toQuery({ companyId })}`),
  signMapping: (companyId: number, objectCode: string, input: SignInput) =>
    api.post<Signoff>(`${BASE}/signoffs/mapping/${objectCode}${toQuery({ companyId })}`, input),
  attachEvidence: (signoffId: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<Signoff>(`${BASE}/signoffs/${String(signoffId)}/evidence`, form);
  },
  evidence: (signoffId: number) => api.getFile(`${BASE}/signoffs/${String(signoffId)}/evidence`),

  xref: (q: string, page = 0) => api.get<PageResponse<Xref>>(`${BASE}/xref${toQuery({ q, page })}`),
  xrefOf: (entity: string, code: string) =>
    api.get<Xref[]>(`${BASE}/xref/of${toQuery({ entity, code })}`),
};
