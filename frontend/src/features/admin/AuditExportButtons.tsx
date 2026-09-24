import { useMutation } from '@tanstack/react-query';
import { FileDown } from 'lucide-react';
import { saveFile } from '@/api/client';
import { reportApi } from '@/api/reports';
import type { ExportFormat } from '@/api/reports';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';

interface AuditFilters {
  from: string;
  to: string;
  username: string;
  entityType: string;
}

/**
 * Download of the audit log report CTL-AUDIT for the filters on screen (BRNB.086/089: view,
 * download and save the audit log report).
 */
export function AuditExportButtons({ filters }: Readonly<{ filters: AuditFilters }>) {
  const { can } = useAuth();
  const toast = useToast();
  const download = useMutation({
    mutationFn: (format: ExportFormat) => {
      const params: Record<string, string> = { fromDate: filters.from, toDate: filters.to };
      if (filters.username.trim() !== '') {
        params.username = filters.username.trim();
      }
      if (filters.entityType.trim() !== '') {
        params.entityType = filters.entityType.trim();
      }
      return reportApi.export('CTL-AUDIT', params, format);
    },
    onSuccess: (file) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${file.fileName} downloaded`);
    },
    onError: (error) => toast.error(error.message),
  });
  if (!can('REPORT_VIEW')) {
    return null;
  }
  return (
    <>
      {(['XLSX', 'PDF', 'CSV'] as ExportFormat[]).map((format) => (
        <Button
          key={format}
          variant="secondary"
          icon={<FileDown size={16} />}
          busy={download.isPending && download.variables === format}
          onClick={() => download.mutate(format)}
        >
          {format === 'XLSX' ? 'Excel' : format}
        </Button>
      ))}
    </>
  );
}
