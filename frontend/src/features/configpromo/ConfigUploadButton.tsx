import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, FileSpreadsheet, Send, Upload } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { configUploadsApi } from '@/api/configUploads';
import type { ConfigUploadType, ConfigUploadWithType } from '@/api/configUploads';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Kpi } from '@/components/ui/Kpi';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { UploadRows } from './UploadRows';
import { uploadLink, uploadStatus } from './uploads';

interface ConfigUploadButtonProps {
  /** The upload types of the screen (one, or several on a screen that holds several tabs). */
  types: readonly string[];
}

/**
 * The Upload action of a configuration screen: download the template (the tab of the master data
 * and configuration workbook) or the current data in the same layout, upload the filled file, see
 * every row checked with what it adds or updates, and submit it to a second user who approves it
 * before anything changes. Shown only to the users who may upload or approve the screen's data.
 */
export function ConfigUploadButton({ types }: Readonly<ConfigUploadButtonProps>) {
  const [open, setOpen] = useState(false);
  const available = useQuery({
    queryKey: ['config-uploads', 'types'],
    queryFn: configUploadsApi.types,
    staleTime: 5 * 60_000,
  });
  const mine = (available.data ?? []).filter((t) => types.includes(t.code) && t.mayUpload);
  const first = mine[0];
  if (first === undefined) {
    return null;
  }
  return (
    <>
      <Button variant="secondary" icon={<Upload size={16} />} onClick={() => setOpen(true)}>
        Upload
      </Button>
      {open && <UploadDialog types={mine} first={first} onClose={() => setOpen(false)} />}
    </>
  );
}

function UploadDialog({
  types,
  first,
  onClose,
}: Readonly<{ types: ConfigUploadType[]; first: ConfigUploadType; onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [code, setCode] = useState(first.code);
  const [file, setFile] = useState<File | null>(null);
  const [current, setCurrent] = useState<ConfigUploadWithType | null>(null);
  const type = types.find((t) => t.code === code) ?? first;
  const download = (what: 'template' | 'data') =>
    void (
      what === 'template'
        ? configUploadsApi.template(type.code, companyId)
        : configUploadsApi.exportData(type.code, companyId)
    ).then((f) => saveFile(f.blob, f.fileName));
  const upload = useMutation({
    mutationFn: () =>
      file === null
        ? Promise.reject(new Error('Choose the file to upload'))
        : configUploadsApi.upload(companyId, type.code, file),
    onSuccess: (u) => setCurrent(u),
  });
  const submit = useMutation({
    mutationFn: () => configUploadsApi.submit(current?.job.id ?? 0),
    onSuccess: async (u) => {
      setCurrent(u);
      await queryClient.invalidateQueries({ queryKey: ['config-uploads'] });
      toast.success(`Upload ${u.job.jobNo} sent for approval`);
    },
  });
  const discard = useMutation({
    mutationFn: () => configUploadsApi.cancel(current?.job.id ?? 0),
    onSuccess: () => {
      setCurrent(null);
      setFile(null);
    },
  });
  const job = current?.job;
  const footer =
    job === undefined ? (
      <>
        <Button variant="ghost" onClick={onClose}>
          Close
        </Button>
        <Button
          variant="accent"
          icon={<Upload size={16} />}
          busy={upload.isPending}
          disabled={file === null}
          onClick={() => upload.mutate()}
        >
          Upload and Check
        </Button>
      </>
    ) : (
      <>
        {job.invalidRows > 0 && (
          <Button
            variant="secondary"
            icon={<FileSpreadsheet size={16} />}
            onClick={() =>
              void configUploadsApi.errorFile(job.id).then((f) => saveFile(f.blob, f.fileName))
            }
          >
            Rows to Correct
          </Button>
        )}
        {job.status === 'VALIDATED' && (
          <>
            <Button variant="ghost" busy={discard.isPending} onClick={() => discard.mutate()}>
              Discard
            </Button>
            <Button
              variant="accent"
              icon={<Send size={16} />}
              busy={submit.isPending}
              disabled={job.validRows === 0}
              onClick={() => submit.mutate()}
            >
              Submit for Approval
            </Button>
          </>
        )}
        {job.status !== 'VALIDATED' && (
          <Button variant="primary" onClick={onClose}>
            Close
          </Button>
        )}
      </>
    );
  return (
    <Modal title={`Upload: ${type.title}`} open onClose={onClose} footer={footer}>
      <div className="stack">
        <ErrorAlert error={upload.error ?? submit.error ?? discard.error} />
        {current === null ? (
          <>
            {types.length > 1 && (
              <Field label="What to upload">
                {(id) => (
                  <select
                    id={id}
                    className="select"
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                  >
                    {types.map((t) => (
                      <option key={t.code} value={t.code}>
                        {t.title}
                      </option>
                    ))}
                  </select>
                )}
              </Field>
            )}
            <p className="muted">
              {`The file follows the tab ${type.templateId} of the master data and configuration workbook. A row whose code exists updates that record; nothing changes until a second user approves the upload.`}
            </p>
            <div className="row">
              <Button
                variant="secondary"
                size="sm"
                icon={<Download size={16} />}
                onClick={() => download('template')}
              >
                Template
              </Button>
              <Button
                variant="secondary"
                size="sm"
                icon={<Download size={16} />}
                onClick={() => download('data')}
              >
                Current Data
              </Button>
            </div>
            <Field
              label="File"
              required
              hint="Excel, OpenDocument or CSV in the layout of the template."
            >
              {(id) => (
                <FileDropZone
                  id={id}
                  accept=".xlsx,.ods,.csv"
                  busy={upload.isPending}
                  onChange={(files) => setFile(files[0] ?? null)}
                />
              )}
            </Field>
          </>
        ) : (
          <>
            <UploadSummary upload={current} />
            <UploadRows job={current.job} />
          </>
        )}
      </div>
    </Modal>
  );
}

/** The counts of an upload and where it stands. */
export function UploadSummary({ upload }: Readonly<{ upload: ConfigUploadWithType }>) {
  const { job } = upload;
  const status = uploadStatus(job.status);
  return (
    <>
      {job.status === 'SUBMITTED' && (
        <Notice tone="info" title={status.label}>
          {'A second user approves the upload on '}
          <Link to={uploadLink(job.id)}>{`upload ${job.jobNo}`}</Link>
          {'; the valid rows are then applied.'}
        </Notice>
      )}
      {job.status === 'VALIDATED' && job.invalidRows > 0 && (
        <Notice tone="warning" title={`${job.invalidRows} row(s) not valid`}>
          Correct them in the file of rows to correct and upload it again, or submit the valid rows
          only.
        </Notice>
      )}
      <div className="grid-4">
        <Kpi label="Rows" value={job.totalRows} />
        <Kpi label="Valid" value={job.validRows} />
        <Kpi label="Not Valid" value={job.invalidRows} />
        <Kpi
          label={job.status === 'COMPLETED' ? 'Applied' : 'Status'}
          value={job.status === 'COMPLETED' ? job.committedRows : status.label}
        />
      </div>
    </>
  );
}
