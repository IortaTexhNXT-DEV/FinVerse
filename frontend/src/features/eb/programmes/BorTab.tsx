import { useQuery } from '@tanstack/react-query';
import { CheckCircle2, Download, Upload, XCircle } from 'lucide-react';
import { useState } from 'react';
import { attachmentsApi } from '@/api/attachments';
import { ebApi } from '@/api/eb';
import type { BorChecklist, BorVersion, ProgrammeView } from '@/api/eb';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DateInput } from '@/components/ui/DateInput';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { FileDropZone } from '@/components/ui/FileDropZone';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { DialogFooter } from '../common/DialogFooter';
import { useEbMutation } from '../common/useEbMutation';
import { checklistErrors } from './borChecklist';

/** Validate BOR: the validator's checklist and the validity dates. */
function ValidateDialog({ bor, onClose }: Readonly<{ bor: BorVersion; onClose: () => void }>) {
  const [c, setC] = useState<BorChecklist>({
    signedBySignatory: false,
    notBlank: false,
    clientNameMatches: false,
    validFrom: '',
    validTo: '',
  });
  const [submitted, setSubmitted] = useState(false);
  const errors = submitted ? checklistErrors(c) : {};
  const validate = useEbMutation(
    (companyId, value: BorChecklist) => ebApi.validateBor(companyId, bor.id, value),
    `Broker on Record version ${String(bor.versionNo)} validated`,
    onClose,
  );
  const save = () => {
    setSubmitted(true);
    if (Object.keys(checklistErrors(c)).length === 0) {
      validate.mutate(c);
    }
  };
  const check = (key: 'signedBySignatory' | 'notBlank' | 'clientNameMatches', label: string) => (
    <label className="checkbox">
      <input
        type="checkbox"
        checked={c[key]}
        onChange={(e) => setC({ ...c, [key]: e.target.checked })}
      />
      {label}
    </label>
  );
  return (
    <Modal
      open
      title={`Validate BOR – Version ${String(bor.versionNo)}`}
      onClose={onClose}
      footer={
        <DialogFooter busy={validate.isPending} label="Validate" onClose={onClose} onSave={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={validate.error} />
        {errors.checklist && <div className="alert danger">{errors.checklist}</div>}
        <div className="eb-checklist">
          {check('signedBySignatory', 'Signed by an authorised signatory')}
          {check('notBlank', 'Not blank')}
          {check('clientNameMatches', 'Client name matches')}
        </div>
        <div className="form-grid">
          <Field label="Valid From" required error={errors.validity}>
            {(id) => (
              <DateInput
                id={id}
                value={c.validFrom}
                onChange={(e) => setC({ ...c, validFrom: e.target.value })}
              />
            )}
          </Field>
          <Field label="Valid To" required>
            {(id) => (
              <DateInput
                id={id}
                min={c.validFrom || undefined}
                value={c.validTo}
                onChange={(e) => setC({ ...c, validTo: e.target.value })}
              />
            )}
          </Field>
        </div>
      </div>
    </Modal>
  );
}

/** Upload BOR: the signed Broker on Record of the current cycle (PDF or Word). */
function UploadDialog({ cycleId, onClose }: Readonly<{ cycleId: number; onClose: () => void }>) {
  const [file, setFile] = useState<File>();
  const [submitted, setSubmitted] = useState(false);
  const upload = useEbMutation(
    (companyId, f: File) => ebApi.uploadBor(companyId, cycleId, f),
    (b) => `Broker on Record version ${String(b.versionNo)} uploaded`,
    onClose,
  );
  const save = () => {
    setSubmitted(true);
    if (file) {
      upload.mutate(file);
    }
  };
  return (
    <Modal
      open
      title="Upload Broker on Record"
      onClose={onClose}
      footer={
        <DialogFooter busy={upload.isPending} label="Upload" onClose={onClose} onSave={save} />
      }
    >
      <div className="stack">
        <ErrorAlert error={upload.error} />
        <Field
          label="Signed BOR"
          required
          error={submitted && !file ? 'Add the signed BOR' : undefined}
        >
          {(id) => (
            <FileDropZone
              id={id}
              accept=".pdf,.doc,.docx"
              maxSizeMb={10}
              onChange={(files) => setFile(files[0])}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

type Acting = { mode: 'upload' } | { mode: 'validate' | 'reject'; bor: BorVersion };

/** The columns of the BOR versions with their row actions. */
function borColumns({
  mayValidate,
  onDownload,
  onAct,
}: Readonly<{
  mayValidate: boolean;
  onDownload: (b: BorVersion) => void;
  onAct: (acting: Acting) => void;
}>): Column<BorVersion>[] {
  return [
    { key: 'version', header: 'Version', kind: 'center', render: (b) => b.versionNo },
    {
      key: 'status',
      header: 'Status',
      kind: 'status',
      render: (b) => (
        <StatusBadge status={b.status} tone={b.status === 'VALIDATED' ? 'success' : undefined} />
      ),
    },
    {
      key: 'validity',
      header: 'Valid',
      render: (b) => (b.validFrom ? `${formatDate(b.validFrom)} – ${formatDate(b.validTo)}` : ''),
    },
    {
      key: 'uploaded',
      header: 'Uploaded',
      render: (b) => (
        <CellStack main={formatDateTime(b.uploadedAt)} sub={<UserName login={b.uploadedBy} />} />
      ),
    },
    {
      key: 'decided',
      header: 'Decided',
      render: (b) =>
        b.decidedAt ? (
          <CellStack main={formatDateTime(b.decidedAt)} sub={<UserName login={b.decidedBy} />} />
        ) : (
          ''
        ),
    },
    { key: 'reason', header: 'Reason', render: (b) => b.rejectReason ?? '' },
    {
      key: 'actions',
      header: '',
      width: '132px',
      render: (b) => (
        <span className="eb-actions">
          <Button
            variant="ghost"
            size="sm"
            aria-label={`Download version ${String(b.versionNo)}`}
            icon={<Download size={14} />}
            onClick={() => onDownload(b)}
          />
          {mayValidate && b.status === 'UPLOADED' && (
            <>
              <Button
                variant="ghost"
                size="sm"
                aria-label={`Validate version ${String(b.versionNo)}`}
                icon={<CheckCircle2 size={14} />}
                onClick={() => onAct({ mode: 'validate', bor: b })}
              />
              <Button
                variant="ghost"
                size="sm"
                aria-label={`Reject version ${String(b.versionNo)}`}
                icon={<XCircle size={14} />}
                onClick={() => onAct({ mode: 'reject', bor: b })}
              />
            </>
          )}
        </span>
      ),
    },
  ];
}

/** The open dialog of the BOR tab. */
function BorDialogs({
  acting,
  cycleId,
  reject,
  onClose,
}: Readonly<{
  acting: Acting | undefined;
  cycleId: number | undefined;
  reject: ReturnType<typeof useEbMutation<{ id: number; reason: string }, BorVersion>>;
  onClose: () => void;
}>) {
  if (acting?.mode === 'upload' && cycleId !== undefined) {
    return <UploadDialog cycleId={cycleId} onClose={onClose} />;
  }
  if (acting?.mode === 'validate') {
    return <ValidateDialog bor={acting.bor} onClose={onClose} />;
  }
  if (acting?.mode === 'reject') {
    return (
      <ConfirmDialog
        title="Reject Broker on Record"
        record={`Version ${String(acting.bor.versionNo)}`}
        effect="The AO uploads a corrected BOR as the next version."
        confirmLabel="Reject"
        reason="required"
        destructive
        busy={reject.isPending}
        error={reject.error}
        onConfirm={(reason) => reject.mutate({ id: acting.bor.id, reason })}
        onClose={onClose}
      />
    );
  }
  return null;
}

/**
 * BOR tab (BRID-008; FR-EB-031): the versions of the client's signed Broker on Record with the
 * checklist and validity; upload, validate and reject. Going to market needs a validated BOR.
 */
export function BorTab({ programme }: Readonly<{ programme: ProgrammeView }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const download = useFileDownload();
  const [acting, setActing] = useState<Acting>();
  const close = () => setActing(undefined);
  const bors = useQuery({
    queryKey: ['eb', 'bor', programme.id],
    queryFn: () => ebApi.bors(companyId, programme.id),
  });
  const reject = useEbMutation<{ id: number; reason: string }, BorVersion>(
    (c, v) => ebApi.rejectBor(c, v.id, v.reason),
    'Broker on Record rejected',
    close,
  );
  const mayValidate = can('EB_MARKET') || can('EB_PROCESS');
  const columns = borColumns({
    mayValidate,
    onDownload: (b) => download.mutate(() => attachmentsApi.download(b.attachmentId)),
    onAct: setActing,
  });
  const current = programme.currentCycleId ?? undefined;
  const mayUpload = can('EB_MARKET') && current !== undefined;
  return (
    <Card
      title="Broker on Record"
      actions={
        mayUpload && (
          <Button
            variant="secondary"
            size="sm"
            icon={<Upload size={14} />}
            onClick={() => setActing({ mode: 'upload' })}
          >
            Upload BOR
          </Button>
        )
      }
    >
      <ErrorAlert error={bors.error ?? download.error} onRetry={() => void bors.refetch()} />
      <DataTable<BorVersion>
        loading={bors.isLoading}
        rows={bors.data ?? []}
        rowKey={(b) => b.id}
        columns={columns}
        emptyMessage="No Broker on Record uploaded"
      />
      <BorDialogs acting={acting} cycleId={current} reject={reject} onClose={close} />
    </Card>
  );
}
