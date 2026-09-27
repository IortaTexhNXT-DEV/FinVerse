import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { Modal } from '@/components/ui/Modal';
import { adjustmentApi } from './api';
import type { PostingBatch } from './api';

type Line = PostingBatch['lines'][number];

const TONES: Record<Line['outcome'], string> = {
  POSTED: 'success',
  AWAITING_REAPPLICATION: 'warning',
  FAILED: 'danger',
};

const LABELS: Record<Line['outcome'], string> = {
  POSTED: 'Posted',
  AWAITING_REAPPLICATION: 'Payments to Re-apply',
  FAILED: 'Failed',
};

const COLUMNS: Column<Line>[] = [
  {
    key: 'request',
    header: 'Request No.',
    render: (l) => <Link to={`/adjustment/requests/${String(l.requestId)}`}>{l.requestNo}</Link>,
  },
  { key: 'invoice', header: 'Invoice No.', render: (l) => l.invoiceNo },
  {
    key: 'outcome',
    header: 'Outcome',
    render: (l) => <span className={`badge ${TONES[l.outcome]}`}>{LABELS[l.outcome]}</span>,
  },
  { key: 'message', header: 'Message', render: (l) => l.message ?? '—' },
];

/** The outcome of a posting batch: counts and one line per request (ADJID.006). */
export function BatchResultDialog({
  batch,
  onClose,
}: Readonly<{ batch: PostingBatch | string; onClose: () => void }>) {
  const batchNo = typeof batch === 'string' ? batch : batch.batchNo;
  const loaded = useQuery({
    queryKey: ['adjustment', 'batch', batchNo],
    queryFn: () => adjustmentApi.batch(batchNo),
    enabled: typeof batch === 'string',
  });
  const shown = typeof batch === 'string' ? loaded.data : batch;
  return (
    <Modal
      open
      title={`Batch ${batchNo}`}
      onClose={onClose}
      footer={
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
      }
    >
      <div className="stack">
        <ErrorAlert error={loaded.error} />
        {shown === undefined ? (
          <span className="spinner" aria-label="Loading" />
        ) : (
          <>
            <div className="grid-4">
              <Kpi label="Posted" value={shown.postedCount} />
              <Kpi label="Payments to Re-apply" value={shown.pendingCount} />
              <Kpi label="Failed" value={shown.failedCount} />
            </div>
            <DataTable
              caption="Requests of the batch"
              columns={COLUMNS}
              rows={shown.lines}
              rowKey={(l) => l.requestId}
              emptyMessage="No items to display"
            />
          </>
        )}
      </div>
    </Modal>
  );
}
