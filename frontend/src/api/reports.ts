import { api, toQuery } from './client';

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
  | 'BUSINESS_LINE';

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

export const reportApi = {
  catalogue: () => api.get<CatalogueEntry[]>('/reports'),
  run: (code: string, params: Record<string, string>) =>
    api.post<ReportResult>(`/reports/${code}/run`, params),
  export: (code: string, params: Record<string, string>, format: ExportFormat) =>
    api.download(`/reports/${code}/export${toQuery({ format })}`, params),
};
