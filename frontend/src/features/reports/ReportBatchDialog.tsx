import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Download, Layers } from 'lucide-react';
import { useState } from 'react';
import type { CatalogueEntry, ExportFormat } from '@/api/reports';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { PrintOptionsFields } from './PrintOptionsFields';
import { DEFAULT_PRINT, downloadBatch, reportOptionsApi } from './reportOptions';
import type { PrintOptions, ReportBatch, ReportBatchItem } from './reportOptions';

const FORMATS: ExportFormat[] = ['XLSX', 'ODS', 'PDF', 'CSV'];
const MAX_REPORTS = 30;

interface Props {
  open: boolean;
  entries: CatalogueEntry[];
  onClose: () => void;
}

/**
 * Report batch (FRBS 2.4.5 / 2.4.7): choose several reports and shared dates, then download one
 * ZIP in the chosen format or print them as one merged PDF. Each report runs with its own
 * permissions; one that cannot run is listed and the others are produced.
 */
export function ReportBatchDialog({ open, entries, onClose }: Readonly<Props>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [codes, setCodes] = useState<string[]>([]);
  const [search, setSearch] = useState('');
  const [dates, setDates] = useState({ fromDate: `${today().slice(0, 8)}01`, toDate: today() });
  const [format, setFormat] = useState<ExportFormat>('XLSX');
  const [merged, setMerged] = useState(false);
  const [print, setPrint] = useState<PrintOptions>(DEFAULT_PRINT);
  const [done, setDone] = useState<ReportBatch | null>(null);

  const run = useMutation({
    mutationFn: () =>
      reportOptionsApi.runBatch({
        codes,
        parameters: {
          companyId: String(companyId),
          fromDate: dates.fromDate,
          toDate: dates.toDate,
          asOfDate: dates.toDate,
        },
        format,
        mergedPdf: merged,
        ...print,
      }),
    onSuccess: async (b) => {
      setDone(b);
      await queryClient.invalidateQueries({ queryKey: ['report-batches'] });
      toast.success(`Batch ${b.batchNo}: ${b.status.toLowerCase()}`);
    },
  });
  const toggle = (code: string) =>
    setCodes((c) => (c.includes(code) ? c.filter((x) => x !== code) : [...c, code]));
  const term = search.trim().toLowerCase();
  const shown = entries.filter(
    (e) =>
      term === '' || e.title.toLowerCase().includes(term) || e.code.toLowerCase().includes(term),
  );
  const close = () => {
    setDone(null);
    onClose();
  };

  return (
    <Modal
      title="Report batch"
      open={open}
      onClose={close}
      footer={
        <>
          <Button variant="secondary" onClick={close}>
            Close
          </Button>
          {done?.fileName !== undefined && (
            <Button
              variant="secondary"
              icon={<Download size={16} />}
              onClick={() => void downloadBatch(done.id)}
            >
              Download {done.fileName}
            </Button>
          )}
          <Button
            variant="accent"
            icon={<Layers size={16} />}
            busy={run.isPending}
            disabled={codes.length === 0 || codes.length > MAX_REPORTS}
            onClick={() => run.mutate()}
          >
            Run {codes.length} Report(s)
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={run.error} />
        <div className="form-grid">
          <Field label="From">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={dates.fromDate}
                onChange={(e) => setDates({ ...dates, fromDate: e.target.value })}
              />
            )}
          </Field>
          <Field label="To / as of">
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={dates.toDate}
                onChange={(e) => setDates({ ...dates, toDate: e.target.value })}
              />
            )}
          </Field>
          <Field label="Output">
            {(id) => (
              <select
                id={id}
                className="select"
                value={merged ? 'MERGED' : format}
                onChange={(e) => {
                  setMerged(e.target.value === 'MERGED');
                  if (e.target.value !== 'MERGED') {
                    setFormat(e.target.value as ExportFormat);
                  }
                }}
              >
                {FORMATS.map((f) => (
                  <option key={f} value={f}>
                    ZIP of {f} files
                  </option>
                ))}
                <option value="MERGED">One merged PDF (print)</option>
              </select>
            )}
          </Field>
          <PrintOptionsFields value={print} onChange={setPrint} />
        </div>
        <input
          className="input"
          aria-label="Filter reports"
          placeholder={`Search reports (${codes.length} selected)`}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <div className="stack" style={{ maxHeight: 280, overflowY: 'auto' }}>
          {shown.map((e) => (
            <label key={e.code} className="checkbox">
              <input
                type="checkbox"
                checked={codes.includes(e.code)}
                onChange={() => toggle(e.code)}
              />
              <span>
                <strong>{e.code}</strong> {e.title}
              </span>
            </label>
          ))}
        </div>
        {done !== null && <BatchItems items={done.items} />}
      </div>
    </Modal>
  );
}

function BatchItems({ items }: Readonly<{ items: ReportBatchItem[] }>) {
  return (
    <DataTable<ReportBatchItem>
      rows={items}
      rowKey={(i) => i.itemNo}
      caption="Reports of the batch"
      columns={[
        { key: 'c', header: 'Report', render: (i) => <strong>{i.reportCode}</strong> },
        { key: 's', header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
        { key: 'r', header: 'Rows', numeric: true, render: (i) => i.rowCount },
        { key: 'e', header: 'Reason', render: (i) => i.error ?? '' },
      ]}
    />
  );
}
