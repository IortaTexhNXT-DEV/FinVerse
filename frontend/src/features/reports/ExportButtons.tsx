import { Download, FileSpreadsheet, FileText, FileType2 } from 'lucide-react';
import type { ReactNode } from 'react';
import type { ExportFormat } from '@/api/reports';
import { Button } from '@/components/ui/Button';
import { FORMAT_LABELS } from './exportFormats';

const ICONS: Partial<Record<ExportFormat, (size: number) => ReactNode>> = {
  XLSX: (size) => <FileSpreadsheet size={size} />,
  PDF: (size) => <FileText size={size} />,
  DOCX: (size) => <FileType2 size={size} />,
};

interface Props {
  /** Formats in menu order (see menuFormats / DOCUMENT_FORMATS). */
  formats: readonly ExportFormat[];
  /** Format being produced, shown busy. */
  pending?: ExportFormat;
  onExport: (format: ExportFormat) => void;
  size?: 'sm' | 'md';
  variant?: 'secondary' | 'ghost';
  /** Label prefix, e.g. "Export to" gives "Export to Excel". */
  prefix?: string;
  disabled?: boolean;
}

/**
 * The export buttons of a report or document (client requirement 16): Excel and PDF on every
 * report, Word on documents and schedules, one button per format.
 */
export function ExportButtons({
  formats,
  pending,
  onExport,
  size = 'sm',
  variant = 'secondary',
  prefix,
  disabled = false,
}: Readonly<Props>) {
  const iconSize = size === 'sm' ? 14 : 16;
  return (
    <>
      {formats.map((format) => (
        <Button
          key={format}
          size={size}
          variant={variant}
          icon={(ICONS[format] ?? ((s: number) => <Download size={s} />))(iconSize)}
          busy={pending === format}
          disabled={disabled}
          onClick={() => onExport(format)}
        >
          {prefix === undefined ? FORMAT_LABELS[format] : `${prefix} ${FORMAT_LABELS[format]}`}
        </Button>
      ))}
    </>
  );
}
