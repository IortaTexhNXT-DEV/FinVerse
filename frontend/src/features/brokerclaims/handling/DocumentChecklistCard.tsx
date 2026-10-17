import { useQuery } from '@tanstack/react-query';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Notice } from '@/components/ui/Notice';
import { StatusBadge } from '@/components/ui/StatusBadge';
import type { ChecklistItem } from './api';
import { handlingApi, missingDocuments } from './api';

const COLUMNS: Column<ChecklistItem>[] = [
  { key: 'doc', header: 'Document', render: (i) => i.label },
  { key: 'req', header: 'Required', render: (i) => (i.required ? 'Yes' : 'Optional') },
  {
    key: 'up',
    header: 'Uploaded',
    render: (i) => <StatusBadge status={i.uploaded ? 'UPLOADED' : 'MISSING'} />,
  },
];

/**
 * The standard document checklist of the claim type (FRS BRD-07 FRCLM.006.01, 006.02): the claim
 * is filed with complete documents only when every required document is uploaded.
 */
export function DocumentChecklistCard({
  claimId,
  companyId,
}: Readonly<{ claimId: number; companyId: number }>) {
  const items = useQuery({
    queryKey: ['broker-claims', 'checklist', claimId, 'attachments'],
    queryFn: () => handlingApi.checklist(companyId, claimId),
  });
  const list = items.data ?? [];
  const missing = missingDocuments(list);
  return (
    <div className="stack">
      <ErrorAlert error={items.error} />
      {list.length > 0 && missing.length > 0 && (
        <Notice tone="warning" title="Documents still to upload">
          {missing.join(', ')}
        </Notice>
      )}
      <DataTable
        caption="Document checklist"
        columns={COLUMNS}
        rows={list}
        rowKey={(i) => i.documentType}
        emptyMessage="No checklist for the claim type."
      />
    </div>
  );
}
