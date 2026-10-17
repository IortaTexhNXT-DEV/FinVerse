import { saveFile } from '@/api/client';

/** A cell as written to the file: quoted when needed; a leading formula character is neutralised. */
export function csvCell(value: string | number | boolean | null | undefined): string {
  const text = value === null || value === undefined ? '' : String(value);
  const safe = /^[=+\-@]/.test(text) ? `'${text}` : text;
  return /[",\r\n]/.test(safe) ? `"${safe.replaceAll('"', '""')}"` : safe;
}

/** Rows as CSV text with a header line (CRLF line ends, as spreadsheets expect). */
export function toCsv(header: readonly string[], rows: readonly (readonly unknown[])[]): string {
  return [header, ...rows]
    .map((row) =>
      row.map((v) => csvCell(v as string | number | boolean | null | undefined)).join(','),
    )
    .join('\r\n');
}

/** Saves rows as a CSV file in the browser. */
export function saveCsv(
  fileName: string,
  header: readonly string[],
  rows: readonly (readonly unknown[])[],
): void {
  saveFile(new Blob([toCsv(header, rows)], { type: 'text/csv;charset=utf-8' }), fileName);
}
