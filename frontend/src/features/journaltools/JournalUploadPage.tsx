import { useMutation, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Download, FileUp } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { journalUploadApi } from '@/api/journalAutomation';
import type { RowResult, UploadResult, VoucherResult } from '@/api/journalAutomation';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatBytes } from '@/utils/files';
import { checkUploadFile, invalidRows, previewCsv, summarize } from './uploadHelpers';
import type { CsvPreview } from './uploadHelpers';

type Mode = 'VALIDATE' | 'IMPORT';

function PreviewTable({ preview }: Readonly<{ preview: CsvPreview }>) {
  return (
    <Card title={`Preview – first ${preview.rows.length} of ${preview.totalRows} rows`} flush>
      {preview.missingColumns.length > 0 && (
        <div className="alert warning" role="alert">
          Missing required column(s): {preview.missingColumns.join(', ')}
        </div>
      )}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              {preview.header.map((h) => (
                <th key={h}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {preview.rows.map((row) => (
              <tr key={row.rowNumber}>
                {preview.header.map((h, c) => (
                  <td key={h}>{row.cells[c] ?? ''}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

function ResultTables({ result }: Readonly<{ result: UploadResult }>) {
  const s = summarize(result);
  const tone = s.rejected === 0 ? 'success' : 'warning';
  return (
    <>
      <div className={`alert ${tone}`} role="status">
        <strong>
          {result.committed
            ? `${s.created} of ${s.vouchers} voucher(s) created as drafts (upload ${result.uploadReference ?? ''}).`
            : `${s.valid} of ${s.vouchers} voucher(s) are valid. Nothing has been created yet.`}
        </strong>{' '}
        {s.rejected > 0 && `${s.rejected} voucher(s) rejected; ${s.rowErrors} row(s) with errors.`}
      </div>
      <Card title="Vouchers" flush>
        <DataTable<VoucherResult>
          rows={result.vouchers}
          rowKey={(v) => v.voucherKey}
          columns={[
            { key: 'k', header: 'Voucher Key', render: (v) => <strong>{v.voucherKey}</strong> },
            { key: 'r', header: 'First Row', numeric: true, render: (v) => v.firstRow },
            { key: 'l', header: 'Lines', numeric: true, render: (v) => v.lineCount },
            {
              key: 'd',
              header: 'Total Debit',
              numeric: true,
              render: (v) => <Amount value={v.totalDebit} />,
            },
            { key: 's', header: 'Result', render: (v) => <StatusBadge status={v.status} /> },
            {
              key: 'j',
              header: 'Journal',
              render: (v) =>
                v.batchId === undefined ? (
                  ''
                ) : (
                  <Link to={`/gl/journals/${v.batchId}`}>{v.batchNo}</Link>
                ),
            },
            { key: 'm', header: 'Messages', render: (v) => v.messages.join('; ') },
          ]}
        />
      </Card>
      <Card title="Row errors" flush>
        <DataTable<RowResult>
          rows={invalidRows(result)}
          rowKey={(r) => r.rowNumber}
          emptyMessage="Every row passed the row-level checks."
          columns={[
            { key: 'n', header: 'Row', numeric: true, render: (r) => r.rowNumber },
            { key: 'k', header: 'Voucher Key', render: (r) => r.voucherKey },
            { key: 'm', header: 'Errors', render: (r) => r.messages.join('; ') },
          ]}
        />
      </Card>
    </>
  );
}

/**
 * Bulk journal upload: CSV or Excel file with one row per journal line, grouped into vouchers by
 * voucher key. Validate first (dry run), then import: each valid voucher becomes a draft journal.
 */
export default function JournalUploadPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [problem, setProblem] = useState<string>();
  const [preview, setPreview] = useState<CsvPreview | null>(null);

  const upload = useMutation({
    mutationFn: ({ mode, selected }: { mode: Mode; selected: File }) =>
      journalUploadApi.upload(companyId, selected, mode),
    onSuccess: async (result) => {
      if (result.committed) {
        await queryClient.invalidateQueries({ queryKey: ['journals'] });
        toast.success(`${summarize(result).created} draft journal(s) created`);
      }
    },
  });
  const template = useMutation({
    mutationFn: journalUploadApi.template,
    onSuccess: (f) => saveFile(f.blob, f.fileName),
  });

  const choose = (selected: File | undefined) => {
    upload.reset();
    setPreview(null);
    setFile(selected ?? null);
    setProblem(selected === undefined ? undefined : checkUploadFile(selected));
    if (selected?.name.toLowerCase().endsWith('.csv')) {
      void selected.text().then((text) => setPreview(previewCsv(text)));
    }
  };
  const ready = file !== null && problem === undefined && companyId > 0;

  return (
    <div className="stack">
      <PageHeader
        section="General Ledger"
        title="Journal Upload"
        description="Upload many vouchers at once from CSV or Excel. Each voucher is validated and created as a draft on its own; invalid vouchers are reported and skipped."
        actions={
          <>
            <Button
              variant="secondary"
              icon={<Download size={16} />}
              onClick={() => template.mutate('csv')}
            >
              CSV Template
            </Button>
            <Button
              variant="secondary"
              icon={<Download size={16} />}
              onClick={() => template.mutate('xlsx')}
            >
              Excel Template
            </Button>
          </>
        }
      />
      <Card>
        <div className="stack">
          <Field label="Upload file (.csv or .xlsx, max 5 MB)" required>
            {(id) => (
              <input
                id={id}
                className="input"
                type="file"
                accept=".csv,.xlsx"
                onChange={(e) => choose(e.target.files?.[0])}
              />
            )}
          </Field>
          {file !== null && (
            <span className="muted">
              {file.name} · {formatBytes(file.size)}
            </span>
          )}
          {problem !== undefined && (
            <div className="alert warning" role="alert">
              {problem}
            </div>
          )}
          <div className="row">
            <Button
              variant="secondary"
              icon={<CheckCircle2 size={16} />}
              disabled={!ready}
              busy={upload.isPending && upload.variables.mode === 'VALIDATE'}
              onClick={() => file && upload.mutate({ mode: 'VALIDATE', selected: file })}
            >
              Validate
            </Button>
            <Button
              variant="accent"
              icon={<FileUp size={16} />}
              disabled={!ready}
              busy={upload.isPending && upload.variables.mode === 'IMPORT'}
              onClick={() => file && upload.mutate({ mode: 'IMPORT', selected: file })}
            >
              Import Valid Vouchers
            </Button>
          </div>
        </div>
      </Card>
      <ErrorAlert error={upload.error ?? template.error} />
      {upload.data !== undefined && <ResultTables result={upload.data} />}
      {upload.data === undefined && preview !== null && <PreviewTable preview={preview} />}
    </div>
  );
}
