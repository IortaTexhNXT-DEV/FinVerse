import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { renewalPlacementApi } from '@/api/renewalPlacement';
import type { EpolicyLine, EpolicyReceiptView } from '@/api/renewalPlacement';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';

const STATUS: Record<string, string> = { SUCCESSFUL: 'Successful', FAILED: 'Failed' };
const MATCH: Record<string, string> = { MATCHED: 'Matched', UNMATCHED: 'Unmatched' };

function UploadForm() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [summary, setSummary] = useState<File | null>(null);
  const [zip, setZip] = useState<File | null>(null);
  const upload = useMutation({
    mutationFn: () => {
      if (summary === null || zip === null) {
        throw new Error('Select the E-Policy Summary File and the E-Policy ZIP File');
      }
      return renewalPlacementApi.uploadEpolicies(companyId, summary, zip);
    },
    onSuccess: async (r) => {
      toast.success(
        `${r.receiptNo}: ${STATUS[r.status] ?? r.status}, ${String(r.matched)} of ${String(r.records)} matched`,
      );
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'epolicy'] });
    },
  });
  return (
    <div className="rnw-actions">
      <ErrorAlert error={upload.error} />
      <Field label="E-Policy Summary File (.txt)">
        {(id) => (
          <input
            id={id}
            type="file"
            accept=".txt"
            onChange={(e) => setSummary(e.target.files?.[0] ?? null)}
          />
        )}
      </Field>
      <Field label="E-Policy ZIP File">
        {(id) => (
          <input
            id={id}
            type="file"
            accept=".zip"
            onChange={(e) => setZip(e.target.files?.[0] ?? null)}
          />
        )}
      </Field>
      <Button
        disabled={summary === null || zip === null}
        busy={upload.isPending}
        onClick={() => upload.mutate()}
      >
        Upload E-Policies
      </Button>
    </div>
  );
}

function Lines({ receiptNo }: Readonly<{ receiptNo: string }>) {
  const companyId = useCompanyId();
  const [match, setMatch] = useState('');
  const [search, setSearch] = useState('');
  const lines = useQuery({
    queryKey: ['renewal', 'epolicy', 'lines', companyId, receiptNo, match, search],
    queryFn: () => renewalPlacementApi.receiptLines(companyId, receiptNo, match, search),
  });
  return (
    <Card title={`Records of ${receiptNo}`} flush>
      <span className="rnw-actions">
        <select
          className="select"
          aria-label="Matching Status"
          value={match}
          onChange={(e) => setMatch(e.target.value)}
        >
          <option value="">All</option>
          <option value="MATCHED">Matched</option>
          <option value="UNMATCHED">Unmatched</option>
        </select>
        <input
          className="input"
          aria-label="Search reference or policy number"
          placeholder="Reference or policy number"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </span>
      <ErrorAlert error={lines.error} />
      <DataTable<EpolicyLine>
        loading={lines.isLoading}
        rows={lines.data ?? []}
        rowKey={(l) => `${String(l.seq)}-${l.renewalRef ?? ''}`}
        emptyMessage="No record"
        columns={[
          { key: 'seq', header: 'Sequence', render: (l) => l.seq ?? '' },
          {
            key: 'ref',
            header: 'Renewal Reference Number',
            kind: 'code',
            render: (l) => l.renewalRef ?? '',
          },
          { key: 'policy', header: 'Policy Number', render: (l) => l.policyNo ?? '' },
          { key: 'pdf', header: 'Document', render: (l) => l.pdfFile ?? '' },
          {
            key: 'match',
            header: 'Matching Status',
            kind: 'status',
            render: (l) => <StatusBadge status={MATCH[l.matchStatus] ?? l.matchStatus} />,
          },
          { key: 'remarks', header: 'Remarks', render: (l) => l.remarks ?? '' },
        ]}
      />
    </Card>
  );
}

/**
 * E-policy receipts (FRRN.033.01 to FRRN.033.03): the upload of the summary and ZIP files, each
 * receipt (upload or MFT) with its processing status and reason, latest first, and the records of a
 * receipt with their matching status.
 */
export function EpolicyReceiptsCard() {
  const companyId = useCompanyId();
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState<string | null>(null);
  const receipts = useQuery({
    queryKey: ['renewal', 'epolicy', 'receipts', companyId, status, search],
    queryFn: () => renewalPlacementApi.receipts(companyId, search, status),
  });
  return (
    <div className="stack">
      <Card title="E-Policy Receipts">
        <UploadForm />
        <span className="rnw-actions">
          <select
            className="select"
            aria-label="Processing Status"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            <option value="">All</option>
            <option value="SUCCESSFUL">Successful</option>
            <option value="FAILED">Failed</option>
          </select>
          <input
            className="input"
            aria-label="Search file name"
            placeholder="File name"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </span>
        <ErrorAlert error={receipts.error} />
        <DataTable<EpolicyReceiptView>
          loading={receipts.isLoading}
          rows={receipts.data ?? []}
          rowKey={(r) => r.receiptNo}
          onRowClick={(r) => setOpen(r.receiptNo)}
          emptyMessage="No e-policy file received"
          columns={[
            { key: 'id', header: 'ID', kind: 'code', render: (r) => r.receiptNo },
            {
              key: 'file',
              header: 'File Name',
              render: (r) => [r.summaryFile, r.zipFile].filter(Boolean).join(', '),
            },
            {
              key: 'at',
              header: 'Receipt Date and Time',
              render: (r) => formatDateTime(r.receivedAt),
            },
            {
              key: 'type',
              header: 'Receipt Type',
              render: (r) => (r.receiptType === 'MFT' ? 'MFT' : 'Upload'),
            },
            {
              key: 'status',
              header: 'Processing Status',
              kind: 'status',
              render: (r) => <StatusBadge status={STATUS[r.status] ?? r.status} />,
            },
            {
              key: 'records',
              header: 'Matched',
              render: (r) => `${String(r.matched)} of ${String(r.records)}`,
            },
            { key: 'remarks', header: 'Reason/Remarks', render: (r) => r.remarks ?? '' },
          ]}
        />
      </Card>
      {open !== null && <Lines receiptNo={open} />}
    </div>
  );
}
