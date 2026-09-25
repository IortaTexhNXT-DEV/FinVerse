import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
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
import { formatDateTime } from '@/utils/format';
import { prodreconApi } from './prodreconApi';
import type { ReconUpload } from './prodreconApi';
import { monthLabel } from './prodreconLogic';

const COLUMNS: Column<ReconUpload>[] = [
  {
    key: 'file',
    header: 'File',
    render: (u) => (
      <>
        <strong>{u.fileName}</strong>
        <div className="muted">
          Attempt {u.attemptNo} · {u.runNo ?? ''}
        </div>
      </>
    ),
  },
  { key: 'insurer', header: 'Insurer', render: (u) => u.insurerCode ?? '' },
  { key: 'month', header: 'Production Month', render: (u) => monthLabel(u.productionMonth) },
  { key: 'read', header: 'Rows Read', numeric: true, render: (u) => u.rowsRead },
  { key: 'ok', header: 'Accepted', numeric: true, render: (u) => u.rowsAccepted },
  { key: 'failed', header: 'Failed', numeric: true, render: (u) => u.rowsFailed },
  { key: 'message', header: 'Message', render: (u) => u.message ?? '' },
  {
    key: 'at',
    header: 'Uploaded',
    render: (u) => `${formatDateTime(u.createdAt)} · ${u.createdBy}`,
  },
  { key: 'status', header: 'Status', render: (u) => <StatusBadge status={u.status} /> },
];

function UploadDialog({
  busy,
  error,
  onClose,
  onUpload,
}: Readonly<{
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onUpload: (file: File) => void;
}>) {
  const [file, setFile] = useState<File>();
  return (
    <Modal
      title="Upload Insurer Feedback"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} disabled={file === undefined} onClick={() => file && onUpload(file)}>
            Upload and Match
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field
          label="Insurer Feedback File"
          required
          hint="The register returned by the insurer (xlsx or csv), named <INSURER>_PRODREG_<yyyyMM>_<seq>"
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
 * Insurer feedback uploads (PRCID.009-012): each file returned by an insurer, matched on upload;
 * a file already taken in is refused, and every attempt is kept.
 */
export default function ReconUploadsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [insurer, setInsurer] = useState('');
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const uploads = useQuery({
    queryKey: ['prodrecon', 'uploads', companyId, insurer, page],
    queryFn: () => prodreconApi.uploads(companyId, insurer || undefined, page),
    enabled: companyId > 0,
  });
  const upload = useMutation({
    mutationFn: (file: File) => prodreconApi.upload(companyId, file),
    onSuccess: async (r) => {
      setOpen(false);
      await queryClient.invalidateQueries({ queryKey: ['prodrecon'] });
      if (r.runStatus === 'SUCCEEDED') {
        toast.success(`${r.runNo}: feedback matched`);
      } else {
        toast.error(`${r.runNo}: ${r.runMessage ?? r.runStatus}`);
      }
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Product Reconciliation"
        title="Insurer Feedback"
        description="Registers returned by the insurers, uploaded and matched against BDOI production."
        actions={
          can('RECON_PROCESS') ? (
            <Button icon={<Upload size={16} />} onClick={() => setOpen(true)}>
              Upload Feedback
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={uploads.error} />
      <Card>
        <div className="stack">
          <WorklistToolbar
            placeholder="Search Insurer Code"
            onSearch={(text) => {
              setInsurer(text.trim().toUpperCase());
              setPage(0);
            }}
          />
          <DataTable
            caption="Insurer feedback uploads"
            columns={COLUMNS}
            rows={uploads.data?.content ?? []}
            rowKey={(u) => u.id}
            loading={uploads.isLoading}
            emptyMessage="No items to display"
          />
          <PageFooter data={uploads.data} noun="uploads" onPage={setPage} />
        </div>
      </Card>
      {open && (
        <UploadDialog
          busy={upload.isPending}
          error={upload.error}
          onClose={() => setOpen(false)}
          onUpload={(file) => upload.mutate(file)}
        />
      )}
    </div>
  );
}
