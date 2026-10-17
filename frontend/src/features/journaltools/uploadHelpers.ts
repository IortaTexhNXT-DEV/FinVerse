import type { RowResult, UploadResult } from '@/api/journalAutomation';
import { checkFile } from '@/utils/files';

/** Largest journal upload accepted by the server. */
export const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;

/** Accepted upload formats. */
export const UPLOAD_EXTENSIONS = ['csv', 'xlsx'] as const;

/** Columns the server requires (see docs/samples/journal-upload.md). */
export const REQUIRED_COLUMNS = [
  'voucher_key',
  'branch_code',
  'value_date',
  'currency',
  'narration',
  'account_code',
  'debit',
  'credit',
];

/** Client-side pre-check of an upload file; undefined when it may be sent. */
export function checkUploadFile(file: { name: string; size: number }): string | undefined {
  return checkFile(file, UPLOAD_EXTENSIONS, MAX_UPLOAD_BYTES);
}

/**
 * Minimal RFC 4180 CSV reader for the preview (quoted fields, doubled quotes, CRLF, BOM).
 * The server parses the file again; this only shows the user what will be sent.
 */
export function parseCsv(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [];
  let cell = '';
  let quoted = false;
  const source = text.startsWith('\uFEFF') ? text.slice(1) : text;
  const endCell = () => {
    row.push(cell.trim());
    cell = '';
  };
  const endRow = () => {
    endCell();
    if (row.some((c) => c !== '')) {
      rows.push(row);
    }
    row = [];
  };
  let i = 0;
  while (i < source.length) {
    const c = source.charAt(i);
    i += 1;
    if (quoted) {
      if (c !== '"') {
        cell += c;
      } else if (source.charAt(i) === '"') {
        cell += '"';
        i += 1;
      } else {
        quoted = false;
      }
    } else if (c === '"') {
      quoted = true;
    } else if (c === ',') {
      endCell();
    } else if (c === '\n') {
      endRow();
    } else if (c !== '\r') {
      cell += c;
    }
  }
  endRow();
  return rows;
}

export interface PreviewRow {
  /** Row number in the file (the header is row 1). */
  rowNumber: number;
  cells: string[];
}

export interface CsvPreview {
  header: string[];
  rows: PreviewRow[];
  totalRows: number;
  missingColumns: string[];
}

/** Header, first data rows and missing required columns of a CSV text. */
export function previewCsv(text: string, maxRows = 10): CsvPreview {
  const all = parseCsv(text);
  const header = (all[0] ?? []).map((h) => h.toLowerCase().replace(/[\s-]/g, '_'));
  return {
    header,
    rows: all.slice(1, maxRows + 1).map((cells, i) => ({ rowNumber: i + 2, cells })),
    totalRows: Math.max(0, all.length - 1),
    missingColumns: REQUIRED_COLUMNS.filter((c) => !header.includes(c)),
  };
}

export interface UploadSummary {
  vouchers: number;
  valid: number;
  created: number;
  rejected: number;
  rowErrors: number;
}

/** Counts for the result banner. */
export function summarize(result: UploadResult): UploadSummary {
  const rejected = result.vouchers.filter((v) => v.status === 'ERROR').length;
  return {
    vouchers: result.vouchers.length,
    valid: result.vouchers.length - rejected,
    created: result.vouchers.filter((v) => v.status === 'CREATED').length,
    rejected,
    rowErrors: result.rows.filter((r) => !r.valid).length,
  };
}

/** Rows with errors, in file order. */
export function invalidRows(result: UploadResult): RowResult[] {
  return result.rows.filter((r) => !r.valid).sort((a, b) => a.rowNumber - b.rowNumber);
}
