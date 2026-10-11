import { useMutation } from '@tanstack/react-query';
import { FileDown } from 'lucide-react';
import { saveFile } from '@/api/client';
import { reportApi } from '@/api/reports';
import type { ExportFormat } from '@/api/reports';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { useToast } from '@/components/ui/toastContext';
import { exportParams } from './auditTrail';
import type { AuditFilters } from './auditTrail';

/**
 * Download of the audit log report CTL-AUDIT for the filters on screen (BRNB.086/089: view,
 * download and save the audit log report; BDOI FRS FRUM.008.01: CSV, PDF and Excel, named
 * "Audit Logs_MMDDYYYY", with a confirmation once saved).
 */
export function AuditExportButtons({
  filters,
  formats = ['XLSX', 'PDF', 'CSV'],
}: Readonly<{ filters: AuditFilters; formats?: ExportFormat[] }>) {
  const { can } = useAuth();
  const toast = useToast();
  const download = useMutation({
    mutationFn: (format: ExportFormat) =>
      reportApi.export('CTL-AUDIT', exportParams(filters), format),
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
      {formats.map((format) => (
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
