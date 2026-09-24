import { useMutation, useQuery } from '@tanstack/react-query';
import { FileDown, FilePlus2 } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { reportApi } from '@/api/reports';
import type { ExportFormat } from '@/api/reports';
import { taxApi } from '@/api/tax';
import type { BirList, Period, Worksheet, WorksheetKind } from '@/api/tax';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount } from '@/utils/format';
import { CreateReturnModal } from './CreateReturnModal';
import { PeriodPicker } from './PeriodPicker';
import { formsFor, worksheetKpis } from './taxDisplay';
import type { Granularity, PeriodChoice } from './taxPeriods';
import { periodOf } from './taxPeriods';
import { usePeriodParam } from './usePeriodParam';
import { WorksheetTables } from './WorksheetTables';

const REPORTS: Record<WorksheetKind, string> = {
  VAT: 'TAX-VAT-2550Q',
  EWT: 'TAX-EWT-1601EQ',
  DST: 'TAX-DST-2000',
  PREMIUM_TAX: 'TAX-PREMTAX',
  LGT: 'TAX-PREMTAX',
  FST: 'TAX-PREMTAX',
  NONE: 'TAX-REMIT',
};

const BIR_LISTS: Partial<Record<WorksheetKind, BirList[]>> = { VAT: ['SLS', 'SLP'], EWT: ['QAP'] };
const FORMATS: ExportFormat[] = ['PDF', 'XLSX', 'CSV'];

interface Props {
  kind: WorksheetKind;
  title: string;
  description: string;
  granularity: Granularity;
}

/**
 * A tax worksheet for a period: headline KPIs, the return lines, the ledger reconciliation, the
 * drill-down to the source documents, report and BIR list exports, and "Create return".
 */
export function WorksheetView({ kind, title, description, granularity }: Readonly<Props>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [choice, setChoice] = usePeriodParam(granularity);
  const [creating, setCreating] = useState(false);
  const period = periodOf(choice);
  const worksheet = useQuery({
    queryKey: ['tax-worksheet', kind, companyId, period.from, period.to],
    queryFn: () => taxApi.worksheet(companyId, kind, period),
    enabled: companyId > 0,
  });
  const forms = useQuery({
    queryKey: ['tax-forms', companyId],
    queryFn: () => taxApi.forms(companyId),
    enabled: companyId > 0,
  });

  return (
    <div className="stack">
      <PageHeader
        section="Tax & Statutory"
        title={title}
        description={description}
        actions={
          <>
            <WorksheetExports kind={kind} companyId={companyId} period={period} choice={choice} />
            {can('TAX_MANAGE') && (
              <Button
                variant="accent"
                icon={<FilePlus2 size={16} />}
                onClick={() => setCreating(true)}
              >
                Create Return
              </Button>
            )}
          </>
        }
      />
      <Card>
        <PeriodPicker value={choice} onChange={setChoice} />
      </Card>
      <ErrorAlert error={worksheet.error} />
      {worksheet.data !== undefined && <WorksheetKpis worksheet={worksheet.data} />}
      <WorksheetTables worksheet={worksheet.data} loading={worksheet.isLoading} />
      {creating && (
        <CreateReturnModal
          companyId={companyId}
          forms={formsFor(forms.data ?? [], kind)}
          choice={choice}
          onClose={() => setCreating(false)}
        />
      )}
    </div>
  );
}

function WorksheetKpis({ worksheet }: Readonly<{ worksheet: Worksheet }>) {
  return (
    <div className="grid-4">
      {worksheetKpis(worksheet).map((k) => (
        <Kpi
          key={k.label}
          label={k.label}
          value={k.label === 'Documents' ? k.value : formatAmount(k.value)}
          hint={worksheet.periodLabel}
        />
      ))}
    </div>
  );
}

interface ExportProps {
  kind: WorksheetKind;
  companyId: number;
  period: Period;
  choice: PeriodChoice;
}

/** Report exports (PDF, Excel, CSV) and, per quarter, the BIR relief-style list CSVs. */
function WorksheetExports({ kind, companyId, period, choice }: Readonly<ExportProps>) {
  const exporter = useMutation({
    mutationFn: (format: ExportFormat) =>
      reportApi.export(
        REPORTS[kind],
        { companyId: String(companyId), fromDate: period.from, toDate: period.to, taxType: kind },
        format,
      ),
    onSuccess: ({ blob, fileName }) => saveFile(blob, fileName),
  });
  const listExport = useMutation({
    mutationFn: (list: BirList) => taxApi.exportList(list, companyId, choice.year, choice.index),
    onSuccess: ({ blob, fileName }) => saveFile(blob, fileName),
  });
  const lists = choice.granularity === 'QUARTER' ? (BIR_LISTS[kind] ?? []) : [];
  return (
    <>
      <ErrorAlert error={exporter.error ?? listExport.error} />
      {FORMATS.map((f) => (
        <Button
          key={f}
          variant="secondary"
          size="sm"
          icon={<FileDown size={14} />}
          busy={exporter.isPending && exporter.variables === f}
          onClick={() => exporter.mutate(f)}
        >
          {f}
        </Button>
      ))}
      {lists.map((l) => (
        <Button
          key={l}
          variant="secondary"
          size="sm"
          icon={<FileDown size={14} />}
          busy={listExport.isPending && listExport.variables === l}
          onClick={() => listExport.mutate(l)}
        >
          {l} CSV
        </Button>
      ))}
    </>
  );
}
