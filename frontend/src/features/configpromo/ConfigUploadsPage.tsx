import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Download, Send, XCircle } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { configUploadsApi } from '@/api/configUploads';
import type { ConfigUploadWithType } from '@/api/configUploads';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime } from '@/utils/format';
import { UploadSummary } from './ConfigUploadButton';
import { UploadRows } from './UploadRows';
import { uploadActions, uploadStatus } from './uploads';
import type { UploadAction } from './uploads';

const JOB_PARAM = 'job';
const SECTION = 'Configuration Promotion';

/**
 * Configuration Uploads: the uploads of the configuration screens waiting for the user's approval
 * and, for one upload, its rows with what each adds or updates; the approver applies or rejects it.
 */
export default function ConfigUploadsPage() {
  const [params] = useSearchParams();
  const selected = params.get(JOB_PARAM);
  if (selected !== null && selected !== '') {
    return <UploadDetail id={Number(selected)} />;
  }
  return <WaitingList />;
}

function WaitingList() {
  const navigate = useNavigate();
  const waiting = useQuery({
    queryKey: ['config-uploads', 'waiting'],
    queryFn: configUploadsApi.waiting,
  });
  const open = (u: ConfigUploadWithType) => void navigate(`?${JOB_PARAM}=${u.job.id}`);
  return (
    <div className="stack">
      <PageHeader
        section={SECTION}
        title="Configuration Uploads"
        description="Uploads of the configuration screens waiting for your approval. Nothing changes until an approver other than the uploader applies them."
      />
      <Card title="Waiting for Your Approval" flush>
        <ErrorAlert error={waiting.error} />
        <DataTable<ConfigUploadWithType>
          callout="config-uploads-waiting"
          loading={waiting.isLoading}
          rows={waiting.data ?? []}
          rowKey={(u) => u.job.id}
          onRowClick={open}
          emptyMessage="No upload waits for your approval."
          columns={[
            {
              key: 'upload',
              header: 'Upload',
              kind: 'code',
              render: (u) => (
                <CellStack main={<strong>{u.job.jobNo}</strong>} sub={u.job.fileName} />
              ),
            },
            {
              key: 'type',
              header: 'Data',
              render: (u) => <CellStack main={u.type.title} sub={u.type.screen} />,
            },
            {
              key: 'rows',
              header: 'Valid Rows',
              numeric: true,
              width: '110px',
              render: (u) => u.job.validRows,
            },
            {
              key: 'by',
              header: 'Submitted By',
              render: (u) => (
                <CellStack
                  main={<UserName login={u.job.submittedBy ?? u.job.createdBy} />}
                  sub={formatDateTime(u.job.submittedAt ?? u.job.createdAt)}
                />
              ),
            },
            {
              key: 'actions',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (u) => (
                <RowActionMenu
                  label={u.job.jobNo}
                  actions={[{ label: 'Open', onSelect: () => open(u) }]}
                />
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
}

function UploadDetail({ id }: Readonly<{ id: number }>) {
  const { user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [action, setAction] = useState<UploadAction | null>(null);
  const upload = useQuery({
    queryKey: ['config-uploads', 'job', id],
    queryFn: () => configUploadsApi.job(id),
  });
  const decide = useMutation({
    mutationFn: ({ what, note }: { what: UploadAction; note: string }) => perform(id, what, note),
    onSuccess: async (u) => {
      setAction(null);
      await queryClient.invalidateQueries({ queryKey: ['config-uploads'] });
      toast.success(`Upload ${u.job.jobNo}: ${uploadStatus(u.job.status).label}`);
    },
  });
  if (upload.data === undefined) {
    return <ErrorAlert error={upload.error} />;
  }
  const { job, type } = upload.data;
  const actions = uploadActions(job, {
    username: user?.username ?? '',
    mayUpload: type.mayUpload,
    mayApprove: type.mayApprove,
  });
  return (
    <div className="stack">
      <PageHeader
        section={SECTION}
        title={`Upload ${job.jobNo}`}
        backTo="/admin/config-uploads"
        description={`${type.title} (${type.screen})`}
        actions={<DetailActions id={id} actions={actions} onAction={setAction} />}
      />
      <ErrorAlert error={decide.error} />
      <Card title="Upload">
        <div className="stack">
          <UploadFacts upload={upload.data} />
          <UploadSummary upload={upload.data} />
        </div>
      </Card>
      <Card title="Rows">
        <UploadRows job={job} />
      </Card>
      {action !== null && (
        <ConfirmDialog
          title={TITLES[action]}
          record={job.jobNo}
          effect={EFFECTS[action](job.validRows)}
          confirmLabel={TITLES[action]}
          reason={REASONS[action]}
          destructive={action === 'reject' || action === 'discard'}
          busy={decide.isPending}
          error={decide.error}
          onConfirm={(note) => decide.mutate({ what: action, note })}
          onClose={() => setAction(null)}
        />
      )}
    </div>
  );
}

const REASONS: Record<UploadAction, 'required' | 'optional' | undefined> = {
  approve: 'optional',
  reject: 'required',
  submit: undefined,
  discard: undefined,
};

const TITLES: Record<UploadAction, string> = {
  approve: 'Approve and Apply',
  reject: 'Reject',
  submit: 'Submit for Approval',
  discard: 'Discard',
};

const EFFECTS: Record<UploadAction, (valid: number) => string> = {
  approve: (n) => `The ${n} valid row(s) are applied, each recorded in the audit trail.`,
  reject: () => 'Nothing is applied; the uploader is told the reason.',
  submit: (n) => `The ${n} valid row(s) wait for the approval of another user.`,
  discard: () => 'The upload is closed; nothing is applied.',
};

function perform(id: number, what: UploadAction, note: string): Promise<ConfigUploadWithType> {
  switch (what) {
    case 'approve':
      return configUploadsApi.approve(id, note);
    case 'reject':
      return configUploadsApi.reject(id, note);
    case 'submit':
      return configUploadsApi.submit(id);
    default:
      return configUploadsApi.cancel(id);
  }
}

function DetailActions({
  id,
  actions,
  onAction,
}: Readonly<{ id: number; actions: UploadAction[]; onAction: (a: UploadAction) => void }>) {
  return (
    <div className="row">
      <Button
        variant="secondary"
        icon={<Download size={16} />}
        onClick={() => void configUploadsApi.report(id).then((f) => saveFile(f.blob, f.fileName))}
      >
        Result Report
      </Button>
      {actions.includes('discard') && (
        <Button variant="ghost" onClick={() => onAction('discard')}>
          Discard
        </Button>
      )}
      {actions.includes('submit') && (
        <Button variant="accent" icon={<Send size={16} />} onClick={() => onAction('submit')}>
          Submit for Approval
        </Button>
      )}
      {actions.includes('reject') && (
        <Button variant="danger" icon={<XCircle size={16} />} onClick={() => onAction('reject')}>
          Reject
        </Button>
      )}
      {actions.includes('approve') && (
        <Button
          variant="accent"
          icon={<CheckCircle2 size={16} />}
          onClick={() => onAction('approve')}
        >
          Approve and Apply
        </Button>
      )}
    </div>
  );
}

function UploadFacts({ upload }: Readonly<{ upload: ConfigUploadWithType }>) {
  const { job, type } = upload;
  const status = uploadStatus(job.status);
  const decided =
    job.decidedBy === null || job.decidedBy === undefined ? (
      '—'
    ) : (
      <CellStack main={<UserName login={job.decidedBy} />} sub={formatDateTime(job.decidedAt)} />
    );
  return (
    <DefinitionGrid
      items={[
        {
          label: 'Status',
          value: <StatusBadge status={job.status} label={status.label} tone={status.tone} />,
        },
        { label: 'Workbook tab', value: type.templateId },
        { label: 'File', value: job.fileName },
        {
          label: 'Uploaded by',
          value: (
            <CellStack
              main={<UserName login={job.createdBy} />}
              sub={formatDateTime(job.createdAt)}
            />
          ),
        },
        { label: 'Decided by', value: decided },
        { label: 'Remarks', value: job.decisionNote ?? '—' },
      ]}
    />
  );
}
