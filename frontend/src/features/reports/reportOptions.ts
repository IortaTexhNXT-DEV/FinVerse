import { api, saveFile, toQuery } from '@/api/client';
import type { DownloadedFile } from '@/api/client';
import type { ExportFormat, ReportResult } from '@/api/reports';
import type { PageResponse } from '@/api/types';

/** PDF print options (FRBS 2.4.9). */
export interface PrintOptions {
  paper: 'A4' | 'LETTER' | 'LEGAL' | 'A3';
  orientation: 'AUTO' | 'PORTRAIT' | 'LANDSCAPE';
  fitToWidth: boolean;
}

/** The default print layout. */
export const DEFAULT_PRINT: PrintOptions = { paper: 'A4', orientation: 'AUTO', fitToWidth: true };

/** Text filter per column key (FRBS 2.4.4). */
export type ColumnFilters = Record<string, string>;

/** One report of a batch with its outcome (FRBS 2.4.5). */
export interface ReportBatchItem {
  itemNo: number;
  reportCode: string;
  status: 'OK' | 'FAILED';
  rowCount: number;
  error?: string;
}

/** Report batch (FRBS 2.4.5 / 2.4.7). */
export interface ReportBatch {
  id: number;
  batchNo: string;
  format: string;
  mergedPdf: boolean;
  paper: string;
  orientation: string;
  status: 'COMPLETED' | 'PARTIAL' | 'FAILED';
  fileName?: string;
  sizeBytes?: number;
  createdAt: string;
  completedAt?: string;
  items: ReportBatchItem[];
}

/** Batch request. */
export interface ReportBatchRequest extends PrintOptions {
  codes: string[];
  parameters: Record<string, string>;
  format: ExportFormat;
  mergedPdf: boolean;
}

/**
 * The active filters as {@code column:text} pairs.
 *
 * @param filters filters by column
 * @returns pairs, blanks left out
 */
export function filterPairs(filters: ColumnFilters): string[] {
  return Object.entries(filters)
    .filter(([, v]) => v.trim() !== '')
    .map(([k, v]) => `${k}:${v.trim()}`);
}

/**
 * Keeps the detail rows matching every filter ("contains", case-insensitive); with a filter set,
 * group headers and totals are dropped because they no longer add up - as in the export.
 *
 * @param result report result
 * @param filters filters by column
 * @returns rows to show
 */
export function filterRows(result: ReportResult, filters: ColumnFilters): ReportResult['rows'] {
  const active = Object.entries(filters).filter(([, v]) => v.trim() !== '');
  if (active.length === 0) {
    return result.rows;
  }
  return result.rows.filter(
    (r) =>
      r.kind === 'DETAIL' &&
      active.every(([key, text]) =>
        String(r.cells[key] ?? '')
          .toLowerCase()
          .includes(text.trim().toLowerCase()),
      ),
  );
}

/** Export with print options and filters, and report batches. */
export const reportOptionsApi = {
  export: (
    code: string,
    params: Record<string, string>,
    format: ExportFormat,
    print: PrintOptions,
    filters: ColumnFilters,
  ): Promise<DownloadedFile> => {
    const query = toQuery({ format, ...print });
    const filterQuery = filterPairs(filters)
      .map((f) => `&filter=${encodeURIComponent(f)}`)
      .join('');
    return api.download(`/reports/${code}/export${query}${filterQuery}`, params);
  },
  runBatch: (body: ReportBatchRequest) => api.post<ReportBatch>('/reports/batches', body),
  batches: () => api.get<PageResponse<ReportBatch>>('/reports/batches?size=10'),
  batchFile: (id: number) => api.getFile(`/reports/batches/${id}/file`),
};

/**
 * Downloads the file of a report batch.
 *
 * @param id batch
 */
export async function downloadBatch(id: number): Promise<void> {
  const f = await reportOptionsApi.batchFile(id);
  saveFile(f.blob, f.fileName);
}
