import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { useState } from 'react';
import { bulkApi } from '@/api/bulk';
import type { BulkJob } from '@/api/bulk';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { useCompanyId } from '@/context/workspaceContext';
import { UPLOAD_MESSAGES, uploadErrorMessage } from './uploadMessages';
import type { UploadDefinition } from './uploadMessages';

interface Outcome {
  tone: 'success' | 'error' | 'warning';
  text: string;
  job?: BulkJob;
}

/** Uploads a file and processes it when every record is valid. */
async function uploadAndProcess(
  companyId: number,
  upload: UploadDefinition,
  type: UploadDefinition['types'][number] | undefined,
  file: File | null,
): Promise<Outcome> {
  if (type === undefined || file === null) throw new Error('Select the type and the file');
  const job = await bulkApi.upload(companyId, type.handler, file, type.parameters);
  if (job.invalidRows > 0) {
    return { tone: 'error', text: UPLOAD_MESSAGES.missingFields, job };
  }
  const done = await bulkApi.commit(job.id);
  return done.failedRows > 0
    ? { tone: 'warning', text: UPLOAD_MESSAGES.processingError, job: done }
    : { tone: 'success', text: upload.done, job: done };
}

function OutcomeNotice({
  outcome,
  onDownload,
}: Readonly<{ outcome: Outcome; onDownload: (jobId: number) => void }>) {
  const job = outcome.job;
  return (
    <Notice
      tone={outcome.tone}
      title={outcome.tone === 'success' ? 'Upload completed' : 'Upload failed'}
      actions={
        job !== undefined && outcome.tone !== 'success' ? (
          <ErrorsButton job={job} onDownload={() => onDownload(job.id)} />
        ) : undefined
      }
    >
      {outcome.text}
    </Notice>
  );
}

function ErrorsButton({ job, onDownload }: Readonly<{ job: BulkJob; onDownload: () => void }>) {
  return (
    <Button size="sm" variant="secondary" icon={<Download size={14} />} onClick={onDownload}>
      Download Errors {job.jobNo}
    </Button>
  );
}

/**
 * An upload of the Renewal landing page: the type, then the file; the file is validated and, when
 * every record is valid, processed at once, with BDOI's messages.
 */
export function RenewalUploadDialog({
  upload,
  onClose,
}: Readonly<{ upload: UploadDefinition; onClose: () => void }>) {
  const companyId = useCompanyId();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [typeIndex, setTypeIndex] = useState(upload.types.length === 1 ? 0 : -1);
  const [file, setFile] = useState<File | null>(null);
  const [outcome, setOutcome] = useState<Outcome>();
  const type = typeIndex >= 0 ? upload.types[typeIndex] : undefined;
  const run = useMutation({
    mutationFn: () => uploadAndProcess(companyId, upload, type, file),
    onSuccess: async (o) => {
      setOutcome(o);
      await queryClient.invalidateQueries({ queryKey: ['renewal'] });
    },
    onError: (e) => setOutcome({ tone: 'error', text: uploadErrorMessage(e) }),
  });
  return (
    <Modal
      open
      size="md"
      title={upload.button}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          <Button
            busy={run.isPending}
            disabled={type === undefined || file === null || outcome?.tone === 'success'}
            onClick={() => {
              setOutcome(undefined);
              run.mutate();
            }}
          >
            Upload
          </Button>
        </>
      }
    >
      <div className="stack">
        {upload.types.length > 1 && (
          <Field label="Type" required>
            {(id) => (
              <select
                id={id}
                className="input"
                value={typeIndex}
                onChange={(e) => setTypeIndex(Number(e.target.value))}
              >
                <option value={-1}>Select the type</option>
                {upload.types.map((t, i) => (
                  <option key={t.label} value={i}>
                    {t.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
        )}
        <FileDropZone
          label="File (.xlsx or .csv)"
          accept=".xlsx,.csv"
          busy={run.isPending}
          onChange={(files) => setFile(files[0] ?? null)}
        />
        {run.isPending && <Notice tone="info">Upload in progress</Notice>}
        {outcome && (
          <OutcomeNotice
            outcome={outcome}
            onDownload={(id) => download.mutate(() => bulkApi.errorFile(id))}
          />
        )}
      </div>
    </Modal>
  );
}
