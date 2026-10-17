import type { CatalogueEntry, ExportFormat } from '@/api/reports';

/** Button labels of the export formats (Title Case, BDO UX). */
export const FORMAT_LABELS: Record<ExportFormat, string> = {
  XLSX: 'Excel',
  PDF: 'PDF',
  DOCX: 'Word',
  ODS: 'ODS',
  CSV: 'CSV',
  XML: 'XML',
};

/**
 * The label of a format name as stored (batches, archive); unknown names are shown as they are.
 *
 * @param format format name
 * @returns label
 */
export function formatLabel(format: string): string {
  return format in FORMAT_LABELS ? FORMAT_LABELS[format as ExportFormat] : format;
}

/** Menu order: Excel and PDF first on every report (client requirement 16), then Word. */
const MENU_ORDER: ExportFormat[] = ['XLSX', 'PDF', 'DOCX', 'ODS', 'CSV', 'XML'];

/**
 * The formats a report screen offers: the formats of the catalogue entry, Excel and PDF always,
 * Word only for a document or schedule, limited to the formats the screen shows.
 *
 * @param entry catalogue entry (undefined while loading: Excel and PDF)
 * @param shown formats the screen shows (default: all)
 * @returns formats in menu order
 */
export function menuFormats(
  entry: Pick<CatalogueEntry, 'documentStyle' | 'formats'> | undefined,
  shown: readonly ExportFormat[] = MENU_ORDER,
): ExportFormat[] {
  const offered = new Set<ExportFormat>(entry?.formats ?? ['XLSX', 'PDF', 'ODS', 'CSV', 'XML']);
  offered.add('XLSX');
  offered.add('PDF');
  if (entry?.documentStyle === true) {
    offered.add('DOCX');
  } else {
    offered.delete('DOCX');
  }
  return MENU_ORDER.filter((f) => offered.has(f) && shown.includes(f));
}

/**
 * The formats of a document download: PDF and Word (client requirement 16).
 */
export const DOCUMENT_FORMATS: ExportFormat[] = ['PDF', 'DOCX'];

/**
 * The Word file name of a downloaded PDF.
 *
 * @param pdfName PDF file name
 * @returns the same name ending in .docx
 */
export function wordFileName(pdfName: string): string {
  const base = pdfName.replace(/\.pdf$/i, '');
  return `${base === '' ? 'document' : base}.docx`;
}
