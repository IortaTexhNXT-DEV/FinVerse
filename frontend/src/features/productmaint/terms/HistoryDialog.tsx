import { useQuery } from '@tanstack/react-query';
import { pmTermsApi } from '@/api/pmTerms';
import type { FinalTermChange, TermsRecordType } from '@/api/pmTerms';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { formatDateTime } from '@/utils/format';

const COLUMNS: Column<FinalTermChange>[] = [
  { key: 'field', header: 'Field', render: (c) => c.field },
  { key: 'old', header: 'Previous Value', render: (c) => c.oldValue ?? '—' },
  { key: 'new', header: 'New Value', render: (c) => c.newValue ?? '—' },
  { key: 'by', header: 'Changed By', render: (c) => c.changedBy },
  { key: 'at', header: 'Changed On', render: (c) => formatDateTime(c.changedAt) },
];

/** Every change of the Final Terms for Proposal, newest first (BDOI FRS FRPM.009.01). */
export function HistoryDialog({
  type,
  id,
  onClose,
}: Readonly<{ type: TermsRecordType; id: number; onClose: () => void }>) {
  const history = useQuery({
    queryKey: ['pm-terms', type, id, 'history'],
    queryFn: () => pmTermsApi.history(type, id),
  });
  return (
    <Modal
      title="Final Terms History"
      open
      size="lg"
      onClose={onClose}
      footer={<Button onClick={onClose}>Close</Button>}
    >
      <ErrorAlert error={history.error} />
      <DataTable
        columns={COLUMNS}
        rows={history.data ?? []}
        rowKey={(c) => c.id}
        loading={history.isLoading}
        emptyMessage="The Final Terms have not been changed"
      />
    </Modal>
  );
}
