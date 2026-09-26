import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Upload } from 'lucide-react';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime, humanize, today } from '@/utils/format';
import { casesApi } from './api';
import type { CaseDetail, CaseDocument } from './api';
import { uploadErrors } from './caseLogic';
import type { UploadForm } from './caseLogic';
import { LovField, StepDialog } from './StepDialog';

function UploadDialog({
  detail,
  onDone,
  onClose,
}: Readonly<{ detail: CaseDetail; onDone: () => void; onClose: () => void }>) {
  const [form, setForm] = useState<UploadForm>({
    formType: detail.templateType,
    documentType: '',
    dateReceived: today(),
    source: '',
  });
  const [touched, setTouched] = useState(false);
  const errors = touched ? uploadErrors(form, today()) : {};
  const save = useMutation({
    mutationFn: (file: File) =>
      casesApi.upload(detail.row.id, {
        file,
        formType: form.formType,
        documentType: form.documentType,
        dateReceived: form.dateReceived,
        source: form.source.trim(),
      }),
    onSuccess: onDone,
  });
  return (
    <StepDialog
      title="Upload Document"
      confirmLabel="Upload"
      busy={save.isPending}
      error={save.error}
      onClose={onClose}
      onConfirm={() => {
        setTouched(true);
        if (form.file !== undefined && Object.keys(uploadErrors(form, today())).length === 0) {
          save.mutate(form.file);
        }
      }}
    >
      <p className="muted">
        The file is named &lt;Form Type&gt;_&lt;Client Name&gt;_&lt;Date Received&gt;_&lt;Document
        Type&gt;_&lt;n&gt;. KYC documents are also added to the client's KYC documents.
      </p>
      <Field label="File" required error={errors.file}>
        {(id) => (
          <input
            id={id}
            type="file"
            className="input"
            onChange={(e) => setForm({ ...form, file: e.target.files?.[0] })}
          />
        )}
      </Field>
      <div className="form-grid">
        <LovField
          label="Form Type"
          type="SCR_FORM_TYPE"
          value={form.formType}
          onChange={(formType) => setForm({ ...form, formType })}
          error={errors.formType}
        />
        <LovField
          label="Document Type"
          type="SCR_DOCUMENT_TYPE"
          value={form.documentType}
          onChange={(documentType) => setForm({ ...form, documentType })}
          error={errors.documentType}
        />
        <Field label="Date Received" required error={errors.dateReceived}>
          {(id) => (
            <input
              id={id}
              type="date"
              className="input"
              max={today()}
              value={form.dateReceived}
              onChange={(e) => setForm({ ...form, dateReceived: e.target.value })}
            />
          )}
        </Field>
        <Field label="Source" required error={errors.source} hint="Client, branch, Marketing…">
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={100}
              value={form.source}
              onChange={(e) => setForm({ ...form, source: e.target.value })}
            />
          )}
        </Field>
      </div>
    </StepDialog>
  );
}

/**
 * KYC and supporting documents of the case with their metadata and BRD names (SNSRP-601;
 * FR-SS-052). Documents are never removed from a case.
 */
export function DocumentsTab({ detail }: Readonly<{ detail: CaseDetail }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [uploading, setUploading] = useState(false);
  const documents = useQuery({
    queryKey: ['screening', 'case', detail.row.id, 'documents'],
    queryFn: () => casesApi.documents(detail.row.id),
  });
  return (
    <Card
      flush
      title="Documents"
      actions={
        detail.actions.includes('UPLOAD') ? (
          <Button icon={<Upload size={16} />} onClick={() => setUploading(true)}>
            Upload Document
          </Button>
        ) : undefined
      }
    >
      <ErrorAlert error={documents.error ?? download.error} />
      <DataTable<CaseDocument>
        caption="Case documents"
        rows={documents.data ?? []}
        rowKey={(d) => d.id}
        loading={documents.isLoading}
        emptyMessage="No document uploaded yet"
        columns={[
          {
            key: 'name',
            header: 'Document',
            render: (d) => <span className="mono">{d.nominatedName}</span>,
          },
          {
            key: 'type',
            header: 'Form / Document Type',
            render: (d) => (
              <>
                {humanize(d.formType)}
                <span className="cell-sub">{humanize(d.documentType)}</span>
              </>
            ),
          },
          { key: 'received', header: 'Received', render: (d) => formatDate(d.dateReceived) },
          { key: 'source', header: 'Source', render: (d) => d.source },
          { key: 'kyc', header: 'On Client KYC', render: (d) => (d.kycRegistered ? 'Yes' : 'No') },
          {
            key: 'uploaded',
            header: 'Uploaded',
            render: (d) => (
              <>
                {formatDateTime(d.createdAt)}
                <span className="cell-sub">{d.createdBy}</span>
              </>
            ),
          },
          {
            key: 'download',
            header: 'Download',
            render: (d) => (
              <Button
                size="sm"
                variant="ghost"
                aria-label={`Download ${d.nominatedName}`}
                icon={<Download size={14} />}
                onClick={() => download.mutate(() => attachmentsApi.download(d.attachmentId))}
              />
            ),
          },
        ]}
      />
      {uploading && (
        <UploadDialog
          detail={detail}
          onClose={() => setUploading(false)}
          onDone={() => {
            setUploading(false);
            toast.success('Document uploaded');
            void queryClient.invalidateQueries({ queryKey: ['screening', 'case', detail.row.id] });
          }}
        />
      )}
    </Card>
  );
}
