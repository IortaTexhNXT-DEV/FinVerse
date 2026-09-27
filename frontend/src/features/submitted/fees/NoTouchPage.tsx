import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileOutput, Upload } from 'lucide-react';
import { useState } from 'react';
import { submittedApi } from '@/api/submitted';
import type { NoTouchBatch, NoTouchLine } from '@/api/submitted';
import { InsurerName } from '@/components/broking/LovLabel';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmButton } from '@/components/ui/ConfirmButton';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDateTime } from '@/utils/format';
import { InsurerSelect } from '../common/InsurerSelect';
import { SUBMITTED_SECTION } from '../common/submittedCodes';
import { UploadPanel } from '../common/UploadPanel';

const money = (v: number | null) => (v === null ? '—' : formatAmount(v));

function Lines({ batch }: Readonly<{ batch: NoTouchBatch }>) {
  const lines = useQuery({
    queryKey: ['submitted', 'no-touch-lines', batch.id],
    queryFn: () => submittedApi.noTouchLines(batch.id),
  });
  return (
    <Card title={`Accounts of ${batch.batchNo}`} flush>
      <ErrorAlert error={lines.error} />
      <DataTable<NoTouchLine>
        loading={lines.isLoading}
        rows={lines.data ?? []}
        rowKey={(l) => l.sbmNo}
        emptyMessage="No account in this batch"
        columns={[
          { key: 'sbm', header: 'Masterlist No.', kind: 'code', render: (l) => l.sbmNo },
          { key: 'pn', header: 'PN No.', kind: 'code', render: (l) => l.pnNo ?? '—' },
          { key: 'assured', header: 'Assured', render: (l) => l.assuredName },
          { key: 'policy', header: 'Policy No.', render: (l) => l.policyNo ?? '—' },
          { key: 'plate', header: 'Plate No.', render: (l) => l.plateNo ?? '—' },
          { key: 'si', header: 'Sum Insured', kind: 'amount', render: (l) => money(l.sumInsured) },
          {
            key: 'basic',
            header: 'Basic Premium',
            kind: 'amount',
            render: (l) => money(l.basicPremium),
          },
          { key: 'fee', header: 'Gross Fee', kind: 'amount', render: (l) => money(l.grossFee) },
          { key: 'vat', header: 'VAT', kind: 'amount', render: (l) => money(l.vat) },
          { key: 'wtax', header: 'Withholding Tax', kind: 'amount', render: (l) => money(l.wtax) },
        ]}
      />
    </Card>
  );
}

/**
 * No Touch Billing (FR-SP-074, 075): the monthly No Touch accounts of an insurer exported with
 * blank fee columns, the insurer's return uploaded and the billing to the insurer with a service
 * invoice.
 */
export default function NoTouchPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [insurer, setInsurer] = useState('');
  const [period, setPeriod] = useState('');
  const [upload, setUpload] = useState(false);
  const [open, setOpen] = useState<number>();
  const batches = useQuery({
    queryKey: ['submitted', 'no-touch', companyId],
    queryFn: () => submittedApi.noTouch(companyId),
    enabled: companyId > 0,
  });
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['submitted'] });
  const exportMonth = useMutation({
    mutationFn: () => submittedApi.exportNoTouch(companyId, insurer, period),
    onSuccess: (b) => {
      toast.success(`${b.batchNo}: ${String(b.lineCount)} accounts exported`);
      setOpen(b.id);
      refresh();
    },
  });
  const bill = useMutation({
    mutationFn: (id: number) => submittedApi.billNoTouch(id),
    onSuccess: (b) => {
      toast.success(`${b.batchNo} billed, service invoice ${b.siNo ?? ''}`);
      refresh();
    },
  });
  const selected = batches.data?.find((b) => b.id === open);
  return (
    <div className="stack">
      <PageHeader
        section={SUBMITTED_SECTION}
        title="No Touch Billing"
        description="Monthly service fee of the No Touch accounts, returned and billed to the insurer."
        actions={
          <Button variant="secondary" icon={<Upload size={16} />} onClick={() => setUpload(true)}>
            Upload Insurer Return
          </Button>
        }
      />
      {upload && (
        <UploadPanel
          label="Upload Insurer Return"
          handler="SBM_NO_TOUCH_RETURN"
          onCommitted={refresh}
          onClose={() => setUpload(false)}
        />
      )}
      <Card title="Export a Month">
        <ErrorAlert error={exportMonth.error} />
        <div className="form-grid">
          <Field label="Insurer" required>
            {(id) => <InsurerSelect id={id} value={insurer} onChange={setInsurer} />}
          </Field>
          <Field label="Month" required>
            {(id) => (
              <input
                id={id}
                type="month"
                className="input"
                value={period}
                onChange={(e) => setPeriod(e.target.value)}
              />
            )}
          </Field>
        </div>
        <div className="form-actions">
          <Button
            icon={<FileOutput size={16} />}
            disabled={insurer === '' || !/^\d{4}-\d{2}$/.test(period) || exportMonth.isPending}
            onClick={() => exportMonth.mutate()}
          >
            Export
          </Button>
        </div>
      </Card>
      <ErrorAlert error={batches.error ?? bill.error ?? download.error} />
      <Card title="Batches" flush>
        <DataTable<NoTouchBatch>
          loading={batches.isLoading}
          rows={batches.data ?? []}
          rowKey={(b) => b.id}
          selectedKey={open}
          onRowClick={(b) => setOpen(b.id)}
          emptyMessage="No No Touch batch yet"
          columns={[
            { key: 'no', header: 'Batch', kind: 'code', render: (b) => b.batchNo },
            {
              key: 'insurer',
              header: 'Insurer',
              render: (b) => <InsurerName code={b.insurerCode} />,
            },
            { key: 'period', header: 'Month', render: (b) => b.period },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (b) => <StatusBadge status={b.status} />,
            },
            { key: 'lines', header: 'Accounts', kind: 'amount', render: (b) => b.lineCount },
            { key: 'fee', header: 'Gross Fee', kind: 'amount', render: (b) => money(b.grossFee) },
            { key: 'si', header: 'Service Invoice', kind: 'code', render: (b) => b.siNo ?? '—' },
            {
              key: 'billed',
              header: 'Billed',
              kind: 'datetime',
              render: (b) => formatDateTime(b.billedAt),
            },
            {
              key: 'actions',
              header: 'Actions',
              render: (b) => (
                <span className="form-actions">
                  {b.exportedFileId !== null && (
                    <Button
                      variant="ghost"
                      onClick={() =>
                        download.mutate(() => submittedApi.noTouchFile(b.id, 'export'))
                      }
                    >
                      Export File
                    </Button>
                  )}
                  {b.statementFileId !== null && (
                    <Button
                      variant="ghost"
                      onClick={() =>
                        download.mutate(() => submittedApi.noTouchFile(b.id, 'statement'))
                      }
                    >
                      Statement
                    </Button>
                  )}
                  {b.status === 'RETURNED' && (
                    <ConfirmButton
                      variant="ghost"
                      confirm={{
                        title: `Bill ${b.batchNo}`,
                        record: `${b.insurerCode} · ${b.period}`,
                        effect:
                          'A service invoice is issued to the insurer and the service fee is posted.',
                      }}
                      onConfirm={() => bill.mutateAsync(b.id)}
                    >
                      Bill Insurer
                    </ConfirmButton>
                  )}
                </span>
              ),
            },
          ]}
        />
      </Card>
      {selected && <Lines batch={selected} />}
    </div>
  );
}
