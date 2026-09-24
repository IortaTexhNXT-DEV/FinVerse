import { CheckCircle2, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { Modal } from '@/components/ui/Modal';

/** Outcome of a bulk action on one record. */
export interface ItemOutcome {
  reference: string;
  ok: boolean;
  message: string;
}

/**
 * Result of a bulk action run record by record (cancel placement, send slips, generate advices,
 * send e-policies): what was done and why a record was refused.
 */
export function ItemResultsDialog({
  title,
  results,
  onClose,
}: Readonly<{ title: string; results: ItemOutcome[]; onClose: () => void }>) {
  const done = results.filter((r) => r.ok).length;
  return (
    <Modal
      open
      title={title}
      onClose={onClose}
      footer={
        <Button variant="primary" onClick={onClose}>
          Close
        </Button>
      }
    >
      <div className="stack">
        <p className="muted">
          {done} of {results.length} done.
        </p>
        <DataTable<ItemOutcome>
          rows={results}
          rowKey={(r) => r.reference}
          columns={[
            { key: 'ref', header: 'Reference', render: (r) => <code>{r.reference}</code> },
            {
              key: 'ok',
              header: 'Result',
              render: (r) =>
                r.ok ? (
                  <CheckCircle2 size={16} aria-label="Done" className="text-success" />
                ) : (
                  <XCircle size={16} aria-label="Refused" className="text-danger" />
                ),
            },
            { key: 'message', header: 'Detail', render: (r) => r.message },
          ]}
        />
      </div>
    </Modal>
  );
}
