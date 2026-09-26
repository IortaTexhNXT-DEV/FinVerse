import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { nbadminApi } from '@/api/nbadmin';
import type { AccessBatch } from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { BulkUploadWizard } from '@/components/broking/BulkUploadWizard';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';

/** Handler of the bulk access requests (template UAM_ACCESS_REQUEST). */
export const ACCESS_BULK_HANDLER = 'UAM_ACCESS_REQUEST';

/**
 * Bulk Request (BRD 1.009; FR-UA-019): download the template, upload the file and review the
 * check of every row; the valid rows become the draft lines of a batch, which is then submitted to
 * the approver from the batch page.
 */
export default function BulkRequestPage() {
  const { can } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const batches = useQuery({
    queryKey: ['nbadmin', 'batches', page],
    queryFn: () => nbadminApi.batches(page),
  });
  const mayUpload = can('UAM_ENROLL') && can('BULK_PROCESS');
  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title="Bulk Request"
        description="Enrol, modify, deactivate or reactivate many users with one file. Every row is checked as a single request; the batch goes to one approver."
      />
      {mayUpload && (
        <BulkUploadWizard
          handler={ACCESS_BULK_HANDLER}
          onCommitted={(job) => {
            toast.success(`Batch ${job.jobNo} saved as a draft`);
            void queryClient.invalidateQueries({ queryKey: ['nbadmin', 'batches'] });
          }}
        />
      )}
      <Card title="Batches">
        <ErrorAlert error={batches.error} />
        <DataTable<AccessBatch>
          caption="Bulk request batches"
          loading={batches.isLoading}
          rows={batches.data?.content ?? []}
          rowKey={(b) => b.id}
          onRowClick={(b) => void navigate(`/user-access/bulk/${String(b.id)}`)}
          emptyMessage="No bulk request yet."
          columns={[
            {
              key: 'n',
              header: 'Batch',
              render: (b) => <strong className="mono">{b.batchNo}</strong>,
            },
            { key: 'l', header: 'Lines', numeric: true, render: (b) => b.lines },
            { key: 's', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
            { key: 'r', header: 'Remarks', render: (b) => b.remarks ?? '' },
            { key: 'b', header: 'Requested By', render: (b) => b.createdBy },
            { key: 'a', header: 'Uploaded', render: (b) => formatDateTime(b.createdAt) },
          ]}
        />
        <PageFooter data={batches.data} noun="batches" onPage={setPage} />
      </Card>
    </div>
  );
}
