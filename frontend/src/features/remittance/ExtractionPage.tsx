import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Play } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, humanize } from '@/utils/format';
import { remittanceApi } from './api';
import type { ExtractionRun, InvoiceTag, RemittanceType } from './api';
import { TAG_LABELS, TYPE_LABELS } from './remittanceLabels';
import './remittance.css';

const TAG_COLUMNS: Column<InvoiceTag>[] = [
  { key: 'inv', header: 'Invoice No.', render: (t) => t.invoiceNo },
  { key: 'ins', header: 'Insurer', render: (t) => t.insurerCode },
  { key: 'type', header: 'Type', render: (t) => (t.type ? TYPE_LABELS[t.type] : '') },
  { key: 'tag', header: 'Tag', render: (t) => TAG_LABELS[t.tag] },
  { key: 'why', header: 'Reasons', render: (t) => t.reasons ?? t.remarks ?? '' },
  { key: 'paid', header: 'Paid AR', numeric: true, render: (t) => <Amount value={t.paidAr} /> },
  {
    key: 'rem',
    header: 'Remitted Now',
    numeric: true,
    render: (t) => <Amount value={t.remittable} />,
  },
  { key: 'batch', header: 'Batch', render: (t) => t.batchNo ?? '' },
];

/** The tags a run gave (RMTID.003). */
function RunTagsDialog({ run, onClose }: Readonly<{ run: ExtractionRun; onClose: () => void }>) {
  const [page, setPage] = useState(0);
  const tags = useQuery({
    queryKey: ['remittance', 'tags', run.id, page],
    queryFn: () => remittanceApi.tags(run.id, page),
  });
  return (
    <Modal
      title={`Extraction ${run.runNo}`}
      open
      onClose={onClose}
      footer={<Button onClick={onClose}>Close</Button>}
    >
      <div className="stack">
        <p>{run.message}</p>
        <ErrorAlert error={tags.error} />
        <DataTable
          caption="Extraction tags"
          columns={TAG_COLUMNS}
          rows={tags.data?.content ?? []}
          rowKey={(t) => `${t.invoiceNo}-${t.taggedAt}`}
          loading={tags.isLoading}
          emptyMessage="Only extracted and due invoices are listed; none in this run"
        />
        <PageFooter data={tags.data} noun="invoices" onPage={setPage} />
      </div>
    </Modal>
  );
}

/** Run now for an insurer and type, or for one invoice (RMTID.001/004). */
function RunForm({ companyId }: Readonly<{ companyId: number }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [insurer, setInsurer] = useState('');
  const [type, setType] = useState('');
  const [invoiceNo, setInvoiceNo] = useState('');
  const run = useMutation({
    mutationFn: () =>
      remittanceApi.extract({
        companyId,
        insurerCode: insurer.trim() || undefined,
        type: (type || undefined) as RemittanceType | undefined,
        invoiceNo: invoiceNo.trim() || undefined,
      }),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['remittance'] });
      if (r.status === 'FAILED') {
        toast.error(`${r.runNo} failed: ${r.message ?? ''}`);
      } else {
        toast.success(`${r.runNo}: ${r.message ?? humanize(r.status)}`);
      }
    },
  });
  return (
    <Card title="Run Extraction">
      <div className="stack">
        <ErrorAlert error={run.error} />
        <div className="remit-form">
          <Field label="Insurer Code" hint="Leave empty for every insurer">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={30}
                value={insurer}
                onChange={(e) => setInsurer(e.target.value)}
              />
            )}
          </Field>
          <Field label="Remittance Type">
            {(id) => (
              <select
                id={id}
                className="select"
                value={type}
                onChange={(e) => setType(e.target.value)}
              >
                <option value="">All types</option>
                {Object.entries(TYPE_LABELS)
                  .filter(([code]) => code !== 'SPECIAL')
                  .map(([code, label]) => (
                    <option key={code} value={code}>
                      {label}
                    </option>
                  ))}
              </select>
            )}
          </Field>
          <Field label="Invoice No." hint="Manual extraction of one invoice">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={40}
                value={invoiceNo}
                onChange={(e) => setInvoiceNo(e.target.value)}
              />
            )}
          </Field>
          <div>
            <Button icon={<Play size={16} />} busy={run.isPending} onClick={() => run.mutate()}>
              Run Extraction
            </Button>
          </div>
        </div>
      </div>
    </Card>
  );
}

function scopeOf(r: ExtractionRun): string {
  if (r.invoiceNo !== undefined) {
    return `Invoice ${r.invoiceNo}`;
  }
  const parts = [r.insurerCode ?? 'All insurers', r.type ? TYPE_LABELS[r.type] : 'all types'];
  return parts.join(' · ');
}

const RUN_COLUMNS: Column<ExtractionRun>[] = [
  {
    key: 'no',
    header: 'Run No.',
    render: (r) => (
      <>
        <strong>{r.runNo}</strong>
        <div className="remit-muted">{humanize(r.trigger)}</div>
      </>
    ),
  },
  { key: 'scope', header: 'Scope', render: scopeOf },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
  { key: 'ex', header: 'Examined', numeric: true, render: (r) => r.examined },
  { key: 'ok', header: 'Extracted', numeric: true, render: (r) => r.extracted },
  { key: 'due', header: 'Due, Not Extracted', numeric: true, render: (r) => r.dueNotExtracted },
  { key: 'nd', header: 'Not Yet Due', numeric: true, render: (r) => r.notDue },
  { key: 'b', header: 'Batches', numeric: true, render: (r) => r.batches },
  {
    key: 'at',
    header: 'Started',
    render: (r) => (
      <>
        {formatDateTime(r.startedAt)}
        <div className="remit-muted">{r.createdBy}</div>
      </>
    ),
  },
];

/**
 * Extraction workbench (RMTID.001/003/004/024): runs the extraction now for an insurer and
 * remittance type or for one invoice, and lists the scheduled and manual runs with the tag each
 * invoice received (Extracted, Due - Not Extracted with the reasons, Not Yet Due, Returned).
 */
export default function ExtractionPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState<ExtractionRun>();
  const runs = useQuery({
    queryKey: ['remittance', 'runs', companyId, page],
    queryFn: () => remittanceApi.runs(companyId, page),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Remittance"
        title="Extraction"
        description="Extract the paid and cleared premium due to insurers into remittance batches, per insurer and type or per invoice."
      />
      {can('REMIT_EXTRACT') && <RunForm companyId={companyId} />}
      <ErrorAlert error={runs.error} />
      <Card title="Extraction Runs" flush>
        <div>
          <DataTable
            caption="Extraction runs"
            columns={RUN_COLUMNS}
            rows={runs.data?.content ?? []}
            rowKey={(r) => r.id}
            loading={runs.isLoading}
            onRowClick={setOpen}
          />
          <PageFooter data={runs.data} noun="runs" onPage={setPage} />
        </div>
      </Card>
      {open !== undefined && <RunTagsDialog run={open} onClose={() => setOpen(undefined)} />}
    </div>
  );
}
