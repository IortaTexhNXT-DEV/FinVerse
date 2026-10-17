import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productCatalogApi } from '@/api/productCatalog';
import type { VersionSummary } from '@/api/productCatalog';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate, formatDateTime } from '@/utils/format';
import { ageInDays } from './versionForm';
import { UserName } from '@/components/ui/UserName';
import { CellStack } from '@/components/ui/CellStack';

/**
 * Validation Queue (PMADD06): package versions submitted by MBS and waiting for the post-set-up
 * validation, oldest first, with their age. A row opens the version editor with the checklist.
 */
export default function ValidationQueuePage() {
  const navigate = useNavigate();
  const [text, setText] = useState('');
  const queue = useQuery({
    queryKey: ['catalog', 'validation-queue'],
    queryFn: productCatalogApi.validationQueue,
  });
  const needle = text.trim().toLowerCase();
  const rows = (queue.data ?? []).filter(
    (v) =>
      needle === '' ||
      v.productCode.toLowerCase().includes(needle) ||
      v.productName.toLowerCase().includes(needle),
  );
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Validation Queue"
        description="Package versions set up by MBS that wait for validation before they can be sold."
      />
      <Card flush>
        <WorklistToolbar onSearch={setText} placeholder="Search Product" />
        <ErrorAlert error={queue.error} />
        <DataTable<VersionSummary>
          emptyMessage="No package version waits for validation."
          loading={queue.isLoading}
          rows={rows}
          rowKey={(v) => `${v.productCode}-${v.versionNo}`}
          onRowClick={(v) =>
            void navigate(`/catalog/products/${v.productCode}/versions/${v.versionNo}`)
          }
          columns={[
            {
              key: 'p',
              header: 'Product',
              render: (v) => <CellStack main={v.productName} sub={v.productCode} />,
            },
            { key: 'v', header: 'Version', render: (v) => `v${v.versionNo}` },
            { key: 's', header: 'Status', render: (v) => <StatusBadge status={v.status} /> },
            { key: 'e', header: 'Effective From', render: (v) => formatDate(v.effectiveFrom) },
            { key: 'q', header: 'Request', render: (v) => v.sourceRequestNo ?? '' },
            {
              key: 'b',
              header: 'Submitted By',
              render: (v) => <UserName login={v.submittedBy} />,
            },
            { key: 'a', header: 'Submitted', render: (v) => formatDateTime(v.submittedAt) },
            {
              key: 'g',
              header: 'Age (days)',
              numeric: true,
              render: (v) => ageInDays(v.submittedAt),
            },
          ]}
        />
      </Card>
    </div>
  );
}
