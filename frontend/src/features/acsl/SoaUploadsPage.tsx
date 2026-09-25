import { useQuery } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { acslApi } from './api';
import type { SoaUpload } from './api';
import { SoaUploadDialog } from './SoaUploadDialog';

const COLUMNS: Column<SoaUpload>[] = [
  {
    key: 'no',
    header: 'Upload No.',
    render: (u) => (
      <>
        <strong>{u.uploadNo}</strong>
        <span className="cell-sub">{formatDateTime(u.createdAt)}</span>
      </>
    ),
  },
  { key: 'insurer', header: 'Insurer', render: (u) => u.insurerCode },
  {
    key: 'period',
    header: 'Period',
    render: (u) => `${formatDate(u.periodFrom)} – ${formatDate(u.periodTo)}`,
  },
  { key: 'file', header: 'File', render: (u) => u.fileName },
  {
    key: 'rows',
    header: 'Loaded / Failed',
    numeric: true,
    render: (u) => `${String(u.rowsLoaded)} / ${String(u.rowsFailed)}`,
  },
  {
    key: 'variance',
    header: 'With Variance',
    numeric: true,
    render: (u) => (u.run ? String(u.run.withVariance) : '—'),
  },
  {
    key: 'notFound',
    header: 'Not Found',
    numeric: true,
    render: (u) => (u.run ? String(u.run.notFound) : '—'),
  },
  { key: 'by', header: 'Uploaded By', render: (u) => u.createdBy },
];

/**
 * Insurer statements of account (ACSL 2.14.0-2.14.1): every upload with its load and
 * reconciliation counts, searchable by upload or insurer, and the upload of a new statement.
 */
export default function SoaUploadsPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const { can } = useAuth();
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [uploading, setUploading] = useState(false);
  const list = useQuery({
    queryKey: ['acsl', 'uploads', companyId, q, page],
    queryFn: () => acslApi.uploads(companyId, q, page),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · ACSL"
        title="Insurer SOA Reconciliation"
        description="Upload an insurer's statement of account and reconcile every line with the books: outstanding, for remittance, remitted, cancelled, direct billed or not found."
        actions={
          can('ACSL_UPLOAD') && (
            <Button
              variant="primary"
              icon={<Upload size={16} />}
              onClick={() => setUploading(true)}
            >
              Upload SOA
            </Button>
          )
        }
      />
      <ErrorAlert error={list.error} />
      <Card flush>
        <WorklistToolbar
          placeholder="Search Upload No. or Insurer"
          onSearch={(text) => {
            setQ(text);
            setPage(0);
          }}
        />
        <DataTable
          caption="Insurer SOA uploads"
          columns={COLUMNS}
          rows={list.data?.content ?? []}
          rowKey={(u) => u.id}
          loading={list.isLoading}
          emptyMessage="No items to display"
          onRowClick={(u) => void navigate(`/acsl/soa/${String(u.id)}`)}
        />
        <PageFooter data={list.data} noun="uploads" onPage={setPage} />
      </Card>
      {uploading && <SoaUploadDialog onClose={() => setUploading(false)} />}
    </div>
  );
}
