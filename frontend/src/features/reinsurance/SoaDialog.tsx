import { useMutation, useQuery } from '@tanstack/react-query';
import { Printer } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { payablesApi } from '@/api/payables';
import { reinsuranceApi } from '@/api/reinsurance';
import type { Soa } from '@/api/reinsurance';
import { reportApi } from '@/api/reports';
import type { ExportFormat } from '@/api/reports';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { ExportButtons } from '@/features/reports/ExportButtons';
import { DateField, SelectField } from '@/features/underwriting/FormFields';
import { today } from '@/utils/format';
import { soaActions, soaReportParams } from './soa';
import { SoaStatement } from './SoaStatement';

/** A statement is a report and a document: Excel, PDF and Word. */
const STATEMENT_FORMATS: ExportFormat[] = ['XLSX', 'PDF', 'DOCX'];

interface Props {
  soa: Soa;
  companyId: number;
  onClose: () => void;
  onChanged: (soa: Soa, message: string) => void;
}

/**
 * A statement of account: view, print, download as Excel, PDF or Word (client requirement 16),
 * approve (checker) and settle.
 */
export function SoaDialog({ soa, companyId, onClose, onChanged }: Readonly<Props>) {
  const { can, user } = useAuth();
  const [settle, setSettle] = useState({ date: today(), bank: '' });
  const actions = soaActions(soa, user?.username, can);
  const banks = useQuery({
    queryKey: ['ri-banks', companyId],
    queryFn: () => payablesApi.bankAccounts(companyId),
    enabled: actions.settle,
    retry: false,
  });
  const approve = useMutation({
    mutationFn: () => reinsuranceApi.approveStatement(soa.id),
    onSuccess: (s) => onChanged(s, `Statement ${s.soaNo} approved`),
  });
  const pay = useMutation({
    mutationFn: () => reinsuranceApi.settleStatement(soa.id, settle.date, settle.bank),
    onSuccess: (s) => onChanged(s, `Statement ${s.soaNo} settled`),
  });
  const pdf = useMutation({
    mutationFn: (format: ExportFormat) =>
      reportApi.export('RI-SOA', soaReportParams(soa, companyId), format),
    onSuccess: (file) => saveFile(file.blob, file.fileName),
  });

  return (
    <Modal
      title={`Statement ${soa.soaNo}`}
      open
      onClose={onClose}
      footer={
        <div className="row">
          <Button variant="secondary" icon={<Printer size={16} />} onClick={() => window.print()}>
            Print
          </Button>
          <ExportButtons
            formats={STATEMENT_FORMATS}
            size="md"
            pending={pdf.isPending ? pdf.variables : undefined}
            onExport={(format) => pdf.mutate(format)}
          />
          {actions.approve && (
            <Button variant="accent" busy={approve.isPending} onClick={() => approve.mutate()}>
              Approve
            </Button>
          )}
          {actions.settle && (
            <Button
              variant="accent"
              busy={pay.isPending}
              disabled={settle.bank === ''}
              onClick={() => pay.mutate()}
            >
              Settle
            </Button>
          )}
        </div>
      }
    >
      <ErrorAlert error={approve.error ?? pay.error ?? pdf.error} />
      <SoaStatement soa={soa} />
      {actions.settle && (
        <div className="form-grid">
          <DateField
            label="Settlement date"
            required
            value={settle.date}
            onChange={(v) => setSettle({ ...settle, date: v })}
          />
          <SelectField
            label="Bank account"
            required
            value={settle.bank}
            emptyLabel="Select bank account"
            options={(banks.data ?? []).map((b) => ({
              value: b.glAccountCode,
              label: `${b.code} ${b.name} (${b.glAccountCode})`,
            }))}
            onChange={(v) => setSettle({ ...settle, bank: v })}
          />
        </div>
      )}
    </Modal>
  );
}
