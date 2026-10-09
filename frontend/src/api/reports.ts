import { api, toQuery } from './client';
import type { PageResponse } from './types';

export type ParameterType =
  | 'DATE'
  | 'TEXT'
  | 'NUMBER'
  | 'BOOLEAN'
  | 'SELECT'
  | 'COMPANY'
  | 'BRANCH'
  | 'ACCOUNT'
  | 'CURRENCY'
  | 'BUSINESS_LINE'
  | 'INSURER'
  /** Several codes of a list ("only" or "all except"); options[0] names the list. */
  | 'CODE_SET'
  /** One value of a platform list chosen by name; options[0] names the list. */
  | 'LOOKUP';

export interface ParameterSpec {
  name: string;
  label: string;
  type: ParameterType;
  required: boolean;
  options: string[];
  defaultValue?: string;
}

export interface CatalogueEntry {
  code: string;
  title: string;
  category: string;
  categoryLabel: string;
  description: string;
  parameters: ParameterSpec[];
  /** Whether the user may download or print it. */
  exportable?: boolean;
  /** A document or schedule: Word is offered next to Excel and PDF (client requirement 16). */
  documentStyle?: boolean;
  /** Export formats offered, in menu order. */
  formats?: ExportFormat[];
}

export type ColumnType = 'TEXT' | 'DATE' | 'NUMBER' | 'AMOUNT' | 'PERCENT';
export type RowKind = 'DETAIL' | 'GROUP_HEADER' | 'SUBTOTAL' | 'TOTAL' | 'SECTION';

export interface ReportColumn {
  key: string;
  label: string;
  type: ColumnType;
  summed: boolean;
}

export interface ReportRow {
  kind: RowKind;
  level: number;
  label?: string;
  cells: Record<string, string | number | null>;
}

export interface ReportResult {
  code: string;
  title: string;
  parameterEcho: string[];
  columns: ReportColumn[];
  rows: ReportRow[];
  notes: string[];
}

export type ExportFormat = 'PDF' | 'XLSX' | 'CSV' | 'ODS' | 'XML' | 'DOCX';

/** One archived run or export of a report. */
export interface ReportRunEntry {
  id: number;
  reportCode: string;
  title: string;
  parameters: string;
  action: string;
  format?: string;
  rowCount: number;
  fileName?: string;
  createdBy: string;
  createdAt: string;
}

/** One value of a report parameter list. */
export interface CodeOption {
  code: string;
  label: string;
}

export const reportApi = {
  runs: (code: string, page = 0, size = 5) =>
    api.get<PageResponse<ReportRunEntry>>(`/reports/runs${toQuery({ code, page, size })}`),
  codeSet: (source: string, companyId: number) =>
    api.get<CodeOption[]>(`/reports/code-sets/${source}${toQuery({ companyId })}`),
  catalogue: () => api.get<CatalogueEntry[]>('/reports'),
  run: (code: string, params: Record<string, string>) =>
    api.post<ReportResult>(`/reports/${code}/run`, params),
  export: (code: string, params: Record<string, string>, format: ExportFormat) =>
    api.download(`/reports/${code}/export${toQuery({ format })}`, params),
};
