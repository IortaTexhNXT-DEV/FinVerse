import { useQuery } from '@tanstack/react-query';
import { Download, Upload } from 'lucide-react';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { ebApi } from '@/api/eb';
import type { DocumentView, ProgrammeView } from '@/api/eb';
import { lovApi } from '@/api/lov';
import { useAuth } from '@/auth/authContext';
import { LovSelect } from '@/components/broking/LovSelect';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import { EB_LOV, EB_SOURCES, EB_UPLOAD_TYPES, ebLabel } from '../common/ebCodes';
import { useEbMutation } from '../common/useEbMutation';

interface UploadValue {
  cycleId: string;
  documentType: string;
  processType: string;
  source: string;
  description: string;
  files: File[];
}

function uploadErrors(v: UploadValue): Record<string, string> {
  const errors: Record<string, string> = {};
  if (v.cycleId === '') {
    errors.cycleId = 'Link the document to its cycle';
  }
  if (v.documentType === '') {
    errors.documentType = 'Select the document type';
  }
  if (v.processType === '') {
    errors.processType = 'Select the process of the document';
  }
  if (v.files.length === 0) {
    errors.files = 'Add at least one file';
  }
  return errors;
}

/** Upload Documents: cycle, type, process, source and files (FR-EB-002, FR-EB-030). */
function UploadDialog({
  programme,
  onClose,
}: Readonly<{ programme: ProgrammeView; onClose: () => void }>) {
  const open = programme.cycles.filter((c) => c.closedAt === null || c.closedAt === undefined);
  const [value, setValue] = useState<UploadValue>({
    cycleId: open[0] ? String(open[0].id) : '',
    documentType: '',
    processType: '',
    source: 'CLIENT',
    description: '',
    files: [],
  });
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? uploadErrors(value) : {};
  const types = useQuery({
    queryKey: ['lov', EB_LOV.documentType],
    queryFn: () => lovApi.options(EB_LOV.documentType),
    staleTime: 5 * 60_000,
  });
  const upload = useEbMutation(
    (companyId, v: UploadValue) =>
      ebApi.uploadDocuments(
        companyId,
        Number(v.cycleId),
        {
          documentType: v.documentType,
          processType: v.processType,
          source: v.source,
          description: v.description.trim() || undefined,
        },
        v.files,
      ),
    'Documents uploaded',
    onClose,
  );
  const save = () => {
    setSubmitted(true);
    if (Object.keys(uploadErrors(value)).length === 0) {
      upload.mutate(value);
    }
  };
  const options = (types.data ?? []).filter((o) => EB_UPLOAD_TYPES.includes(o.code));
  return (
    <Modal
      open
      title="Upload Documents"
      onClose={onClose}
      footer={
        <DialogFooter busy={upload.isPending} label="Upload" onClose={onClose} onSave={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={upload.error} />
        <div className="form-grid">
          <Field label="Cycle" required error={errors.cycleId}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={value.cycleId}
                onChange={(e) => setValue({ ...value, cycleId: e.target.value })}
              >
                <option value="">Select cycle</option>
                {open.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.cycleNo} · {ebLabel(c.businessType)} {c.policyYear}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Document Type" required error={errors.documentType}>
            {(id) => (
              <select
                id={id}
                className="select"
                value={value.documentType}
                onChange={(e) => setValue({ ...value, documentType: e.target.value })}
              >
                <option value="">Select document type</option>
                {options.map((o) => (
                  <option key={o.code} value={o.code}>
                    {o.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Process" required error={errors.processType}>
            {(id) => (
              <LovSelect
                id={id}
                type={EB_LOV.processType}
                value={value.processType}
                onChange={(v) => setValue({ ...value, processType: v })}
                required
              />
            )}
          </Field>
          <Field label="Received From" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={value.source}
                onChange={(e) => setValue({ ...value, source: e.target.value })}
              >
                {EB_SOURCES.map((s) => (
                  <option key={s.code} value={s.code}>
                    {s.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Description">
            {(id) => (
              <input
                id={id}
                className="input"
                maxLength={200}
                value={value.description}
                onChange={(e) => setValue({ ...value, description: e.target.value })}
              />
            )}
          </Field>
        </div>
        <Field label="Files" required error={errors.files}>
          {(id) => (
            <FileDropZone
              id={id}
              multiple
              maxSizeMb={10}
              accept=".pdf,.doc,.docx,.xls,.xlsx,.csv,.png,.jpg,.jpeg,.eml,.msg"
              onChange={(files) => setValue({ ...value, files })}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * Documents tab (FR-EB-002, 003, 030): the registered documents of the programme the user may see,
 * with cycle, type, process, version, source and status, downloadable; Upload Documents.
 */
export function DocumentsTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [uploading, setUploading] = useState(false);
  const download = useFileDownload();
  const documents = useQuery({
    queryKey: ['eb', 'documents', programme.id],
    queryFn: () => ebApi.documents(companyId, programme.id),
  });
  const columns: Column<DocumentView>[] = [
    {
      key: 'type',
      header: 'Document',
      render: (d) => <CellStack main={d.documentTypeLabel} sub={d.fileName} />,
    },
    { key: 'process', header: 'Process', render: (d) => d.processLabel },
    { key: 'cycle', header: 'Cycle', kind: 'code', render: (d) => d.cycleNo ?? '' },
    { key: 'version', header: 'Version', kind: 'center', render: (d) => d.versionNo },
    { key: 'source', header: 'Received From', render: (d) => ebLabel(d.source) },
    {
      key: 'uploaded',
      header: 'Uploaded',
      render: (d) => (
        <CellStack main={formatDateTime(d.uploadedAt)} sub={<UserName login={d.uploadedBy} />} />
      ),
    },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (d) => <StatusBadge status={d.status} />,
    },
    {
      key: 'download',
      header: '',
      width: '56px',
      render: (d) => (
        <Button
          variant="ghost"
          size="sm"
          aria-label={`Download ${d.fileName}`}
          icon={<Download size={14} />}
          onClick={() => download.mutate(() => attachmentsApi.download(d.attachmentId))}
        />
      ),
    },
  ];
  const openCycles = programme.cycles.some((c) => c.closedAt === null || c.closedAt === undefined);
  const mayUpload = (can('EB_MARKET') || can('EB_PROCESS')) && openCycles;
  return (
    <Card
      title="Documents"
      actions={
        mayUpload && (
          <Button
            variant="secondary"
            size="sm"
            icon={<Upload size={14} />}
            onClick={() => setUploading(true)}
          >
            Upload Documents
          </Button>
        )
      }
    >
      <ErrorAlert
        error={documents.error ?? download.error}
        onRetry={() => void documents.refetch()}
      />
      <DataTable<DocumentView>
        loading={documents.isLoading}
        rows={documents.data ?? []}
        rowKey={(d) => d.id}
        columns={columns}
        emptyMessage="No documents to display"
      />
      {uploading && <UploadDialog programme={programme} onClose={() => setUploading(false)} />}
    </Card>
  );
}
