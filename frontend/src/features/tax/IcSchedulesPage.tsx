import { useMutation, useQuery } from '@tanstack/react-query';
import { FileDown } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { reportApi } from '@/api/reports';
import type { ExportFormat } from '@/api/reports';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useCompanyId } from '@/context/workspaceContext';
import { ReportTable } from '@/features/reports/ReportTable';
import { today } from '@/utils/format';

/** Insurance Commission schedules and the report that renders each one. */
const IC_SCHEDULES = [
  { id: 'IC-PREM-LOB', label: 'Premiums by LOB' },
  { id: 'IC-LOSS-LOB', label: 'Losses by LOB' },
  { id: 'IC-COMM-LOB', label: 'Commissions by LOB' },
  { id: 'IC-NETWORTH', label: 'Net worth' },
  { id: 'IC-RBC', label: 'RBC summary' },
  { id: 'IC-RESERVES', label: 'Reserves' },
  { id: 'IC-INVEST', label: 'Investments' },
] as const;

type ScheduleCode = (typeof IC_SCHEDULES)[number]['id'];
const FORMATS: ExportFormat[] = ['PDF', 'XLSX', 'CSV'];

/**
 * Insurance Commission statutory schedules read from the ledger through the IC mapping: per-LOB
 * movements for premiums, losses and commissions; balances for net worth, RBC, reserves and
 * investments.
 */
export default function IcSchedulesPage() {
  const companyId = useCompanyId();
  const [code, setCode] = useState<ScheduleCode>('IC-PREM-LOB');
  const [from, setFrom] = useState(() => `${today().slice(0, 4)}-01-01`);
  const [to, setTo] = useState(today);
  const params = { companyId: String(companyId), fromDate: from, toDate: to };
  const result = useQuery({
    queryKey: ['ic-schedule', code, companyId, from, to],
    queryFn: () => reportApi.run(code, params),
    enabled: companyId > 0,
  });
  const exporter = useMutation({
    mutationFn: (format: ExportFormat) => reportApi.export(code, params, format),
    onSuccess: ({ blob, fileName }) => saveFile(blob, fileName),
  });

  return (
    <div className="stack">
      <PageHeader
        section="Tax & Statutory"
        title="IC Statutory Schedules"
        description="Annual statement and quarterly report schedules of the Insurance Commission, built from posted ledger balances."
        actions={FORMATS.map((f) => (
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
      />
      <Tabs tabs={IC_SCHEDULES} active={code} onChange={setCode} />
      <Card>
        <div className="row">
          <Field label="From (movements)">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            )}
          </Field>
          <Field label="To / as of">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={result.error ?? exporter.error} />
      <Card title={result.data?.title ?? 'Schedule'} flush>
        {result.isLoading && <span className="spinner" aria-label="Loading" />}
        {result.data !== undefined && <ReportTable result={result.data} />}
        {(result.data?.notes.length ?? 0) > 0 && (
          <div className="card-body">
            {result.data?.notes.map((n) => (
              <p key={n} className="muted">
                {n}
              </p>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
