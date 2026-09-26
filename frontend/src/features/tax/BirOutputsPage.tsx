import { useMutation } from '@tanstack/react-query';
import { FileSpreadsheet, FileText } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { saveFile } from '@/api/client';
import type { ExportFormat } from '@/api/reports';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { DEFAULT_PRINT, reportOptionsApi } from '@/features/reports/reportOptions';
import { today } from '@/utils/format';
import { BIR_OUTPUT_GROUPS, initialChoice, outputParams, periodLabel } from './birOutputs';
import type { BirOutput, PeriodChoice } from './birOutputs';

const MONTHS = Array.from({ length: 12 }, (_, i) => String(i + 1));

function PeriodFields({
  choice,
  yearError,
  onChange,
}: Readonly<{
  choice: PeriodChoice;
  yearError?: string;
  onChange: (next: Partial<PeriodChoice>) => void;
}>) {
  return (
    <div className="form-grid">
      <Field label="Year" required error={yearError}>
        {(id) => (
          <input
            id={id}
            className="input"
            inputMode="numeric"
            maxLength={4}
            value={choice.year}
            onChange={(e) => onChange({ year: e.target.value })}
          />
        )}
      </Field>
      <Field label="Quarter" hint="1702-Q, 1603, SAWT">
        {(id) => (
          <select
            id={id}
            className="select"
            value={choice.quarter}
            onChange={(e) => onChange({ quarter: e.target.value })}
          >
            {['1', '2', '3', '4'].map((q) => (
              <option key={q} value={q}>
                Q{q}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Month" hint="MAP, 0619-F">
        {(id) => (
          <select
            id={id}
            className="select"
            value={choice.month}
            onChange={(e) => onChange({ month: e.target.value })}
          >
            {MONTHS.map((m) => (
              <option key={m} value={m}>
                {m.padStart(2, '0')}
              </option>
            ))}
          </select>
        )}
      </Field>
      <Field label="Books From">
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            value={choice.from}
            onChange={(e) => onChange({ from: e.target.value })}
          />
        )}
      </Field>
      <Field label="Books To">
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            value={choice.to}
            onChange={(e) => onChange({ to: e.target.value })}
          />
        )}
      </Field>
      <Field label="Ledger Account Class">
        {(id) => (
          <select
            id={id}
            className="select"
            value={choice.accountClass}
            onChange={(e) => onChange({ accountClass: e.target.value })}
          >
            {['ASSET', 'LIABILITY', 'EQUITY', 'INCOME', 'EXPENSE', 'MEMORANDUM'].map((c) => (
              <option key={c} value={c}>
                {c === 'EQUITY' ? 'Capital' : c.charAt(0) + c.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        )}
      </Field>
    </div>
  );
}

/**
 * BIR Forms and Books (FRBS 3.2.0, Appendix A VII; formats AQ07): the new returns and worksheets,
 * alphalists, books of accounts and the IC broker statement, each opened in the report runner or
 * exported to Excel or PDF for the chosen period.
 */
export default function BirOutputsPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const [choice, setChoice] = useState<PeriodChoice>(initialChoice(today()));
  const yearError = /^\d{4}$/.test(choice.year) ? undefined : 'Enter a 4-digit year';
  const exporter = useMutation({
    mutationFn: ({ output, format }: { output: BirOutput; format: ExportFormat }) =>
      reportOptionsApi.export(
        output.code,
        outputParams(output, companyId, choice),
        format,
        DEFAULT_PRINT,
        {},
      ),
    onSuccess: (file, { output }) => {
      saveFile(file.blob, file.fileName);
      toast.success(`${output.title} (${periodLabel(output, choice)}) exported`);
    },
  });
  const columns: Column<BirOutput>[] = [
    {
      key: 'title',
      header: 'Output',
      render: (o) => (
        <>
          <Link to={`/reports/${o.code}`}>{o.title}</Link>
          <span className="cell-sub">
            {o.code} · {o.text}
          </span>
        </>
      ),
    },
    { key: 'period', header: 'Period', render: (o) => periodLabel(o, choice) },
    {
      key: 'export',
      header: 'Export',
      render: (o) => (
        <span className="row">
          {(['XLSX', 'PDF'] as const).map((format) => (
            <Button
              key={format}
              size="sm"
              variant="secondary"
              icon={format === 'XLSX' ? <FileSpreadsheet size={14} /> : <FileText size={14} />}
              disabled={yearError !== undefined}
              busy={
                exporter.isPending &&
                exporter.variables.output.code === o.code &&
                exporter.variables.format === format
              }
              onClick={() => exporter.mutate({ output: o, format })}
            >
              {format === 'XLSX' ? 'Excel' : 'PDF'}
            </Button>
          ))}
        </span>
      ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Tax & Statutory"
        title="BIR Forms and Books"
        description="Returns and worksheets, alphalists, books of accounts and the IC broker statement, exported to Excel or PDF. BIR file formats are to be confirmed (AQ07)."
      />
      <ErrorAlert error={exporter.error} />
      <Card title="Period">
        <PeriodFields
          choice={choice}
          yearError={yearError}
          onChange={(next) => setChoice((c) => ({ ...c, ...next }))}
        />
      </Card>
      {BIR_OUTPUT_GROUPS.map((g) => (
        <Card key={g.title} title={g.title} flush>
          <DataTable
            caption={g.title}
            columns={columns}
            rows={[...g.outputs]}
            rowKey={(o) => o.code}
          />
        </Card>
      ))}
    </div>
  );
}
