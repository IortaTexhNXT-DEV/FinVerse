import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { placementApi } from '@/api/placement';
import type { BillingBatch, PaymentReport } from '@/api/placement';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';

/**
 * Uploads a payment report (BRNB.067/068): a CLPC report answering a billing batch (matched by PN
 * or loan application number) or a report of the other segments (matched by ARN); .xlsx, .csv or
 * .ods. The report opens in the match review.
 */
export function UploadReportDialog({
  companyId,
  batches,
  initialBatchId,
  onClose,
  onUploaded,
}: Readonly<{
  companyId: number;
  batches: BillingBatch[];
  initialBatchId?: number;
  onClose: () => void;
  onUploaded: (report: PaymentReport) => void;
}>) {
  const [kind, setKind] = useState<'CLPC' | 'REFERENCE'>('CLPC');
  const [batchId, setBatchId] = useState(
    initialBatchId === undefined ? '' : String(initialBatchId),
  );
  const [file, setFile] = useState<File | null>(null);
  const upload = useMutation({
    mutationFn: (chosen: File) =>
      placementApi.uploadReport(
        companyId,
        kind,
        chosen,
        kind === 'CLPC' && batchId !== '' ? Number(batchId) : undefined,
      ),
    onSuccess: onUploaded,
  });
  const open = batches.filter((b) => b.status !== 'CLOSED');
  const missingBatch = kind === 'CLPC' && batchId === '';
  return (
    <Modal
      open
      title="Upload Payment Report"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="primary"
            busy={upload.isPending}
            disabled={file === null || missingBatch}
            onClick={() => file && upload.mutate(file)}
          >
            Upload and Match
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={upload.error} />
        <Field label="Report" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={kind}
              onChange={(e) => setKind(e.target.value as 'CLPC' | 'REFERENCE')}
            >
              <option value="CLPC">
                CLPC payment report (CBG Fire, PN / loan application no.)
              </option>
              <option value="REFERENCE">Payment report of other segments (ARN)</option>
            </select>
          )}
        </Field>
        {kind === 'CLPC' && (
          <Field label="Billing batch" required hint="The batch whose billing file CLPC answers.">
            {(id) => (
              <select
                id={id}
                className="select"
                value={batchId}
                onChange={(e) => setBatchId(e.target.value)}
              >
                <option value="">Select…</option>
                {open.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.batchNo} · {b.itemCount} account(s)
                  </option>
                ))}
              </select>
            )}
          </Field>
        )}
        <Field
          label="File"
          required
          hint="Columns: PN No. / Loan Application No. (CLPC) or ARN, Status (PAID / UNPAID), Amount, Payment Date."
        >
          {(id) => (
            <input
              id={id}
              type="file"
              className="input"
              accept=".xlsx,.csv,.ods"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
