import { PeriodCell } from '@/components/ui/PeriodCell';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Plus, Send, Upload } from 'lucide-react';
import { useState } from 'react';
import { renewalApi } from '@/api/renewal';
import type { InsurerBatchView } from '@/api/renewal';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { countOf, formatDate, formatDateTime } from '@/utils/format';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import { UploadPanel } from '../common/UploadPanel';
import '../renewal.css';
import { InsurerName } from '@/components/broking/LovLabel';

function CreateBatchDialog({ onClose }: Readonly<{ onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [insurer, setInsurer] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const insurers = useQuery({
    queryKey: ['renewal', 'code-set', 'renewal.insurer', companyId],
    queryFn: () => renewalApi.codeSet('renewal.insurer', companyId),
  });
  const create = useMutation({
    mutationFn: () =>
      renewalApi.createBatch(companyId, { insurerCode: insurer, expiryFrom: from, expiryTo: to }),
    onSuccess: async (b) => {
      onClose();
      toast.success(`Batch ${b.batchNo} created with ${countOf(b.lineCount, 'renewal')}`);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  return (
    <Modal
      open
      title="New insurer batch"
      onClose={onClose}
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={create.isPending}
            disabled={insurer === '' || from === '' || to === '' || to < from}
            onClick={() => create.mutate()}
          >
            Create Batch
          </Button>
        </>
      }
    >
      <p className="muted">
        The batch takes the renewals of the insurer in Processing for the expiry range, with the 28
        columns of the insurer extract.
      </p>
      <ErrorAlert error={create.error ?? insurers.error} />
      <Field label="Insurance Company" required>
        {(id) => (
          <select
            id={id}
            className="select"
            value={insurer}
            onChange={(e) => setInsurer(e.target.value)}
          >
            <option value="">Select the insurer</option>
            {(insurers.data ?? []).map((i) => (
              <option key={i.code} value={i.code}>
                {i.label}
              </option>
            ))}
          </select>
        )}
      </Field>
      <div className="form-grid">
        <Field label="Expiry From" required>
          {(id) => <DateInput id={id} value={from} onChange={(e) => setFrom(e.target.value)} />}
        </Field>
        <Field label="Expiry To" required>
          {(id) => <DateInput id={id} value={to} onChange={(e) => setTo(e.target.value)} />}
        </Field>
      </div>
    </Modal>
  );
}

function BatchDetail({ batchNo, onClose }: Readonly<{ batchNo: string; onClose: () => void }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [confirm, setConfirm] = useState(false);
  const detail = useQuery({
    queryKey: ['renewal', 'batch', companyId, batchNo],
    queryFn: () => renewalApi.batch(companyId, batchNo),
  });
  const send = useMutation({
    mutationFn: () => renewalApi.sendBatch(companyId, batchNo),
    onSuccess: async () => {
      setConfirm(false);
      toast.success(`Batch ${batchNo} sent to the insurer`);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
  });
  const d = detail.data;
  return (
    <Card
      title={`Batch ${batchNo}`}
      actions={
        <span className="rnw-actions">
          <Button
            variant="secondary"
            icon={<Download size={16} />}
            busy={download.isPending}
            onClick={() => download.mutate(() => renewalApi.batchFile(companyId, batchNo))}
          >
            Download
          </Button>
          {can('RNW_INSURER') && d?.batch.status === 'DRAFT' && (
            <Button icon={<Send size={16} />} onClick={() => setConfirm(true)}>
              Send to Insurer
            </Button>
          )}
          <Button variant="ghost" onClick={onClose}>
            Close
          </Button>
        </span>
      }
    >
      <ErrorAlert error={detail.error} />
      {d && (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Responded</th>
                {d.headers.map((h) => (
                  <th key={h}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {d.lines.map((l, i) => (
                <tr key={l.renewalRef ?? String(i)}>
                  <td>{l.responded ? 'Yes' : 'No'}</td>
                  {l.columns.map((c, j) => (
                    <td key={d.headers[j] ?? String(j)}>
                      <span className="nowrap">{c ?? ''}</span>
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {confirm && (
        <ConfirmDialog
          title="Send to Insurer"
          record={batchNo}
          effect="The extract is e-mailed protected to the insurer's renewal contacts; the renewals move to With Insurer."
          confirmLabel="Send"
          busy={send.isPending}
          error={send.error}
          onClose={() => setConfirm(false)}
          onConfirm={() => send.mutate()}
        />
      )}
    </Card>
  );
}

/**
 * Insurer Batches (FR-RN-070, 071): the renewals sent to each insurer for its renewal terms, with
 * the 28 columns of the extract, sent protected; the replies are uploaded here or recorded on the
 * renewal.
 */
export default function InsurerBatchesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [creating, setCreating] = useState(false);
  const [upload, setUpload] = useState(false);
  const [open, setOpen] = useState<string>();
  const batches = useQuery({
    queryKey: ['renewal', 'batches', companyId],
    queryFn: () => renewalApi.batches(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Insurer Batches"
        description="Renewals sent to insurers and their replies."
        actions={
          can('RNW_INSURER') && (
            <>
              <Button
                variant="secondary"
                icon={<Upload size={16} />}
                onClick={() => setUpload(true)}
              >
                Upload Insurer Responses
              </Button>
              <Button icon={<Plus size={16} />} onClick={() => setCreating(true)}>
                New Batch
              </Button>
            </>
          )
        }
      />
      {upload && (
        <UploadPanel
          label="Upload Insurer Responses"
          handler="RNW_INSURER_RESPONSE"
          onClose={() => setUpload(false)}
        />
      )}
      <ErrorAlert error={batches.error} onRetry={() => void batches.refetch()} />
      <Card flush>
        <DataTable<InsurerBatchView>
          loading={batches.isLoading}
          rows={batches.data ?? []}
          rowKey={(b) => b.batchNo}
          selectedKey={open}
          onRowClick={(b) => setOpen(b.batchNo)}
          emptyMessage="No insurer batches"
          columns={[
            { key: 'no', header: 'Batch', kind: 'code', render: (b) => b.batchNo },
            {
              key: 'insurer',
              header: 'Insurance Company',
              render: (b) => <InsurerName code={b.insurerCode} />,
            },
            {
              key: 'range',
              header: 'Expiry range',
              kind: 'period',
              render: (b) => <PeriodCell from={b.expiryFrom} to={b.expiryTo} />,
            },
            { key: 'lines', header: 'Renewals', kind: 'amount', render: (b) => b.lineCount },
            {
              key: 'sent',
              header: 'Sent',
              render: (b) =>
                b.sentAt ? (
                  <>
                    {formatDateTime(b.sentAt)} <UserName login={b.sentBy ?? ''} />
                  </>
                ) : (
                  ''
                ),
            },
            {
              key: 'due',
              header: 'Reply due',
              kind: 'date',
              render: (b) => formatDate(b.replyDue),
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (b) => <StatusBadge status={b.status} />,
            },
          ]}
        />
      </Card>
      {open !== undefined && <BatchDetail batchNo={open} onClose={() => setOpen(undefined)} />}
      {creating && <CreateBatchDialog onClose={() => setCreating(false)} />}
    </div>
  );
}
