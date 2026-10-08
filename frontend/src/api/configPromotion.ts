import { api, toQuery } from './client';
import type { PageResponse } from './types';

/** Configuration Promotion: catalogue, exports, imports, baselines and drift, overrides. */

export type PackageKind = 'EXPORT' | 'UPLOAD' | 'SNAPSHOT';
export type ImportStatus =
  'CHECKED' | 'SUBMITTED' | 'APPLIED' | 'REJECTED' | 'FAILED' | 'CANCELLED';
export type ChangeType = 'ADDED' | 'CHANGED' | 'UNCHANGED' | 'ONLY_IN_TARGET';

export interface CatalogueGroup {
  code: string;
  name: string;
}

export interface CatalogueDataset {
  code: string;
  name: string;
  group: string;
  module: string;
  key: string[];
  dependsOn: string[];
  collection: boolean;
  optional: boolean;
  users: boolean;
  environmentFields: string[];
  environmentRows: boolean;
  items: number;
}

export interface ExcludedReason {
  reason: string;
  text: string;
  tables: number;
}

export interface Catalogue {
  groups: CatalogueGroup[];
  datasets: CatalogueDataset[];
  excluded: ExcludedReason[];
}

export interface EnvironmentFacts {
  environment: string;
  production: boolean;
  changeWindow: string;
  windowOpen: boolean;
  signingConfigured: boolean;
  keyId?: string | null;
  pipelineApply: boolean;
  platformVersion: string;
  schemaVersion: string;
}

export interface OverrideItem {
  key: string;
  values: Record<string, string>;
}

export interface EnvironmentOverride {
  code: string;
  name: string;
  kind: 'ROWS' | 'FIELDS';
  fields: string[];
  items: OverrideItem[];
}

export interface ConfigPackage {
  id: number;
  packageNo: string;
  kind: PackageKind;
  packageUuid: string;
  mode: 'FULL' | 'INCREMENTAL';
  sourceEnvironment: string;
  platformVersion?: string;
  schemaVersion?: string;
  includeUsers: boolean;
  datasetCount: number;
  rowCount: number;
  sha256: string;
  keyId: string;
  fileName: string;
  fileSize: number;
  description?: string | null;
  createdBy: string;
  createdAt: string;
}

export interface ManifestDataset {
  code: string;
  name: string;
  group: string;
  module: string;
  rows: number;
  sha256: string;
  contentSha256: string;
}

export interface PackageDetail {
  pkg: ConfigPackage;
  datasets: ManifestDataset[];
}

export interface IssueView {
  dataset?: string | null;
  datasetName?: string | null;
  key?: string | null;
  message: string;
}

export interface ImportMessages {
  refusals: string[];
  notes: string[];
  blockers: IssueView[];
  warnings: IssueView[];
}

export interface ImportOptions {
  datasets: string[];
  deactivate: string[];
  includeUsers: boolean;
}

export interface ConfigImport {
  id: number;
  importNo: string;
  packageId: number;
  packageNo: string;
  sourceEnvironment: string;
  status: ImportStatus;
  production: boolean;
  pipeline: boolean;
  changeReference?: string | null;
  reason?: string | null;
  compatible: boolean;
  blockerCount: number;
  warningCount: number;
  added: number;
  changed: number;
  unchanged: number;
  onlyInTarget: number;
  preparedBy: string;
  preparedAt: string;
  submittedBy?: string | null;
  submittedAt?: string | null;
  decidedBy?: string | null;
  decidedAt?: string | null;
  decisionNote?: string | null;
  appliedAt?: string | null;
  snapshotPackageId?: number | null;
  rollbackOfId?: number | null;
  errorMessage?: string | null;
  options: ImportOptions;
  messages: ImportMessages;
}

export interface ImportDataset {
  code: string;
  name: string;
  group?: string | null;
  seq: number;
  added: number;
  changed: number;
  unchanged: number;
  onlyInTarget: number;
  blockers: number;
  collection: boolean;
  inserted?: number | null;
  updated?: number | null;
  deactivated?: number | null;
  removed?: number | null;
  packageRows?: number | null;
  targetRows?: number | null;
  targetTotal?: number | null;
  packageSha256?: string | null;
  targetSha256?: string | null;
  reconciled?: boolean | null;
}

export interface FieldView {
  column: string;
  label: string;
  from?: string | null;
  to?: string | null;
}

export interface ItemView {
  type: ChangeType;
  key: string;
  fields: FieldView[];
}

export interface Baseline {
  id: number;
  name: string;
  packageId: number;
  packageNo: string;
  environment: string;
  remarks?: string | null;
  active: boolean;
  createdBy: string;
  createdAt: string;
}

export interface DatasetDrift {
  code: string;
  name: string;
  group: string;
  comparable: boolean;
  added: number;
  changed: number;
  removed: number;
  items: ItemView[];
}

export interface ExportRequest {
  datasets: string[];
  includeUsers: boolean;
  baselineId?: number | null;
  description?: string;
}

export interface UploadRequest {
  file: File;
  datasets: string[];
  deactivate: string[];
  includeUsers: boolean;
  changeReference: string;
  reason: string;
}

const BASE = '/config-promotion';

export const configPromotionApi = {
  environment: () => api.get<EnvironmentFacts>(`${BASE}/environment`),
  overrides: () => api.get<EnvironmentOverride[]>(`${BASE}/overrides`),
  catalogue: () => api.get<Catalogue>(`${BASE}/catalogue`),
  exportPackage: (request: ExportRequest) => api.post<ConfigPackage>(`${BASE}/exports`, request),
  packages: (kind?: PackageKind, page = 0, size = 20) =>
    api.get<PageResponse<ConfigPackage>>(`${BASE}/packages${toQuery({ kind, page, size })}`),
  pkg: (id: number) => api.get<PackageDetail>(`${BASE}/packages/${id}`),
  file: (id: number) => api.getFile(`${BASE}/packages/${id}/file`),
  upload: (r: UploadRequest) => {
    const form = new FormData();
    form.append('file', r.file);
    r.datasets.forEach((d) => form.append('datasets', d));
    r.deactivate.forEach((d) => form.append('deactivate', d));
    form.append('includeUsers', String(r.includeUsers));
    form.append('changeReference', r.changeReference);
    form.append('reason', r.reason);
    return api.upload<ConfigImport>(`${BASE}/imports`, form);
  },
  imports: (page = 0, size = 20) =>
    api.get<PageResponse<ConfigImport>>(`${BASE}/imports${toQuery({ page, size })}`),
  importOf: (id: number) => api.get<ConfigImport>(`${BASE}/imports/${id}`),
  datasets: (id: number) => api.get<ImportDataset[]>(`${BASE}/imports/${id}/datasets`),
  items: (id: number, code: string, type?: ChangeType, search?: string, page = 0, size = 50) =>
    api.get<PageResponse<ItemView>>(
      `${BASE}/imports/${id}/datasets/${code}/items${toQuery({ type, search, page, size })}`,
    ),
  check: (id: number, options: ImportOptions) =>
    api.post<ConfigImport>(`${BASE}/imports/${id}/check`, options),
  submit: (id: number) => api.post<ConfigImport>(`${BASE}/imports/${id}/submit`),
  approve: (id: number, note: string) =>
    api.post<ConfigImport>(`${BASE}/imports/${id}/approve`, { note }),
  reject: (id: number, reason: string) =>
    api.post<ConfigImport>(`${BASE}/imports/${id}/reject`, { reason }),
  cancel: (id: number) => api.post<ConfigImport>(`${BASE}/imports/${id}/cancel`),
  rollback: (id: number) => api.post<ConfigImport>(`${BASE}/imports/${id}/rollback`),
  baselines: () => api.get<Baseline[]>(`${BASE}/baselines`),
  markBaseline: (packageId: number, name: string, remarks: string) =>
    api.post<Baseline>(`${BASE}/baselines`, { packageId, name, remarks }),
  retireBaseline: (id: number) => api.post<Baseline>(`${BASE}/baselines/${id}/retire`),
  drift: (id: number) => api.get<DatasetDrift[]>(`${BASE}/baselines/${id}/drift`),
  driftOf: (id: number, code: string) =>
    api.get<DatasetDrift>(`${BASE}/baselines/${id}/drift/${code}`),
};
