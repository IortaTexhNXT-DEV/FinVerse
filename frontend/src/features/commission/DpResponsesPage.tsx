import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
import { formatDate, formatDateTime } from '@/utils/format';
import { commissionApi } from './commissionApi';
import type { DpBilling } from './commissionApi';

const COLUMNS: Column<DpBilling>[] = [
  { key: 'no', header: 'Billing No.', render: (b) => <strong>{b.billingNo}</strong> },
  { key: 'insurer', header: 'Insurer', render: (b) => b.insurerCode },
  { key: 'items', header: 'Accounts', numeric: true, render: (b) => b.itemCount },
  {
    key: 'net',
    header: 'Net Commission',
    numeric: true,
    render: (b) => <Amount value={b.amounts.net} />,
  },
  { key: 'sent', header: 'Sent', render: (b) => formatDateTime(b.sentAt) },
  { key: 'due', header: 'Answer Due', render: (b) => formatDate(b.slaDue) },
  {
    key: 'status',
    header: 'Status',
    render: (b) => <StatusBadge status={b.overdue ? 'OVERDUE' : b.stage} />,
  },
];

function UploadDialog({
  busy,
  error,
  onClose,
  onUpload,
}: Readonly<{ busy: boolean; error: unknown; onClose: () => void; onUpload: (f: File) => void }>) {
  const [file, setFile] = useState<File>();
  return (
    <Modal
      title="Upload Insurer Answers"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} disabled={file === undefined} onClick={() => file && onUpload(file)}>
            Upload Answers
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field
          label="Answers File"
          required
          hint="xlsx or csv with the columns Billing No., Invoice No., Decision (Approved / Rejected), Reason and Comment"
        >
          {(id) => (
            <input
              id={id}
              type="file"
              className="input"
              accept=".xlsx,.csv"
              onChange={(e) => setFile(e.target.files?.[0])}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * Insurer responses (CMRID.009): billings waiting for the insurer's answer with their due date,
 * and the upload of the answers the insurers return in a file.
 */
export default function DpResponsesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [uploading, setUploading] = useState(false);
  const waiting = useQuery({
    queryKey: ['commission', 'billings', companyId, 'AWAITING_INSURER', '', page],
    queryFn: () => commissionApi.billings(companyId, { stage: 'AWAITING_INSURER' }, page),
    enabled: companyId > 0,
  });
  const upload = useMutation({
    mutationFn: (file: File) => commissionApi.uploadResponses(file),
    onSuccess: async (r) => {
      setUploading(false);
      await queryClient.invalidateQueries({ queryKey: ['commission'] });
      if (r.status === 'SUCCEEDED') {
        toast.success(`${r.runNo}: answers recorded`);
      } else {
        toast.error(`${r.runNo}: ${r.message || r.status}`);
      }
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Commission Receivables"
        title="Insurer Responses"
        description="Billings waiting for the insurer's answer, and answers received from the insurers in a file."
        actions={
          can('COMMREC_PROCESS') ? (
            <Button icon={<Upload size={16} />} onClick={() => setUploading(true)}>
              Upload Answers
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={waiting.error} />
      <Card title="Waiting for the Insurer">
        <div className="stack">
          <DataTable
            caption="Billings waiting for the insurer"
            columns={COLUMNS}
            rows={waiting.data?.content ?? []}
            rowKey={(b) => b.id}
            loading={waiting.isLoading}
            onRowClick={(b) => void navigate(`/commission/dp/billings/${String(b.id)}`)}
            emptyMessage="No items to display"
          />
          <PageFooter data={waiting.data} noun="billings" onPage={setPage} />
        </div>
      </Card>
      {uploading && (
        <UploadDialog
          busy={upload.isPending}
          error={upload.error}
          onClose={() => setUploading(false)}
          onUpload={(file) => upload.mutate(file)}
        />
      )}
    </div>
  );
}
