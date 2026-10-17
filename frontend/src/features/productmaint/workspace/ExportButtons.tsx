import { useMutation } from '@tanstack/react-query';
import { FileDown } from 'lucide-react';
import { saveFile } from '@/api/client';
import { reportApi } from '@/api/reports';
import type { ExportFormat } from '@/api/reports';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';

interface ExportButtonsProps {
  /** Report that renders the file. */
  report: string;
  /** Report parameters: the filters on screen. */
  params: Record<string, string>;
  formats?: readonly ExportFormat[];
  /** Permission of the export (the report's). */
  permission?: string;
}

const LABELS: Partial<Record<ExportFormat, string>> = { XLSX: 'Excel', PDF: 'PDF', CSV: 'CSV' };

/**
 * Excel, PDF and CSV downloads of a Product Maintenance list for the filters on screen (BDOI FRS
 * FRPM.001.01, FRPM.003.01, FRPM.021.01), with a confirmation once saved.
 */
export function ExportButtons({
  report,
  params,
  formats = ['XLSX', 'PDF', 'CSV'],
  permission = 'PKG_REPORT_VIEW',
}: Readonly<ExportButtonsProps>) {
  const { can } = useAuth();
  const toast = useToast();
  const download = useMutation({
    mutationFn: (format: ExportFormat) => reportApi.export(report, params, format),
    onSuccess: (file) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${file.fileName} downloaded`);
    },
    onError: (error) => toast.error(error.message),
  });
  if (!can(permission)) {
    return null;
  }
  return (
    <>
      {formats.map((format) => (
        <Button
          key={format}
          variant="secondary"
          icon={<FileDown size={16} />}
          busy={download.isPending && download.variables === format}
          onClick={() => download.mutate(format)}
        >
          {LABELS[format] ?? format}
        </Button>
      ))}
    </>
  );
}
