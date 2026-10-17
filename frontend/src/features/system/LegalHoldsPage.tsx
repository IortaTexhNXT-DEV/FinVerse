import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { supportAdminApi } from '@/api/supportAdmin';
import type { HoldAction, HoldFile, LegalHoldRequest } from '@/api/supportAdmin';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { holdActionLabel } from './supportText';

const QUERY_KEY = ['legal-holds'];

/** A decision on a waiting request, or a new request on a file. */
type Pending =
  | { kind: 'approve' | 'reject'; request: LegalHoldRequest }
  | { kind: 'request'; file: HoldFile; action: HoldAction };

/** Toast after each action of the dialog. */
const DONE: Readonly<Record<Pending['kind'], string>> = {
  request: 'Request sent to the records hold approver',
  approve: 'Request approved',
  reject: 'Request rejected',
};

function fileLabel(r: LegalHoldRequest): string {
  return r.fileName ?? `File ${String(r.storedFileId)}`;
}

function DecisionDialog({ pending, onClose }: Readonly<{ pending: Pending; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const act = useMutation({
    mutationFn: (reason: string) => {
      if (pending.kind === 'request') {
        return supportAdminApi.requestHold(pending.file.id, pending.action, reason);
      }
      return pending.kind === 'approve'
        ? supportAdminApi.approveHold(pending.request.id, reason)
        : supportAdminApi.rejectHold(pending.request.id, reason);
    },
    onSuccess: async () => {
      onClose();
      await queryClient.invalidateQueries({ queryKey: QUERY_KEY });
      toast.success(DONE[pending.kind]);
    },
  });
  const common = { busy: act.isPending, error: act.error, onClose, onConfirm: act.mutate };
  if (pending.kind === 'request') {
    return (
      <ConfirmDialog
        {...common}
        title={pending.action === 'PLACE' ? 'Request Legal Hold' : 'Request Release of Legal Hold'}
        record={pending.file.fileName}
        effect={
          pending.action === 'PLACE'
            ? 'Once approved, the file cannot be deleted or purged at the end of its retention until the hold is released.'
            : 'Once approved, the file follows its retention again and is purged when the retention ends.'
        }
        confirmLabel="Send Request"
        reason="required"
      />
    );
  }
  const r = pending.request;
  return (
    <ConfirmDialog
      {...common}
      title={
        pending.kind === 'approve' ? 'Approve Legal Hold Request' : 'Reject Legal Hold Request'
      }
      record={`${fileLabel(r)}: ${holdActionLabel(r.action)}`}
      effect={`Requested on ${formatDateTime(r.requestedAt)}. Reason: ${r.reason}`}
      confirmLabel={pending.kind === 'approve' ? 'Approve' : 'Reject'}
      destructive={pending.kind === 'reject'}
      reason="required"
    />
  );
}

function FileLookup({ onRequest }: Readonly<{ onRequest: (p: Pending) => void }>) {
  const [number, setNumber] = useState('');
  const [fileId, setFileId] = useState<number | null>(null);
  const [tried, setTried] = useState(false);
  const valid = /^\d+$/.test(number.trim());
  const file = useQuery({
    queryKey: [...QUERY_KEY, 'file', fileId],
    queryFn: () => supportAdminApi.holdStatus(fileId ?? 0),
    enabled: fileId !== null,
  });
  const history = useQuery({
    queryKey: [...QUERY_KEY, 'history', fileId],
    queryFn: () => supportAdminApi.holdHistory(fileId ?? 0),
    enabled: fileId !== null,
  });
  const f = file.data;
  return (
    <Card title="Request a Legal Hold">
      <div className="stack">
        <div className="form-grid">
          <Field
            label="File number"
            required
            hint="Shown with the document in the documents list of the record"
            error={tried && !valid ? 'Enter the file number (digits only)' : undefined}
          >
            {(id) => (
              <input
                id={id}
                className="input"
                inputMode="numeric"
                value={number}
                onChange={(e) => setNumber(e.target.value)}
              />
            )}
          </Field>
        </div>
        <div className="form-actions">
          <Button
            variant="secondary"
            onClick={() => {
              setTried(true);
              if (valid) {
                setFileId(Number(number.trim()));
              }
            }}
          >
            Look Up File
          </Button>
        </div>
        <ErrorAlert error={file.error} title="Cannot find the file" />
        {f !== undefined && (
          <>
            <DefinitionGrid
              label="File"
              items={[
                { label: 'File name', value: f.fileName },
                { label: 'Document type', value: f.documentType ? humanize(f.documentType) : '—' },
                { label: 'Record class', value: humanize(f.recordClass) },
                { label: 'Uploaded', value: formatDateTime(f.createdAt) },
                {
                  label: 'Kept until',
                  value: f.retentionUntil ? formatDate(f.retentionUntil) : '—',
                },
                {
                  label: 'Legal hold',
                  value: f.legalHold ? `Yes: ${f.legalHoldReason ?? ''}` : 'No',
                },
              ]}
            />
            <div className="form-actions">
              <Button
                variant="accent"
                onClick={() =>
                  onRequest({ kind: 'request', file: f, action: f.legalHold ? 'RELEASE' : 'PLACE' })
                }
              >
                {f.legalHold ? 'Request Release' : 'Request Legal Hold'}
              </Button>
            </div>
            <DataTable<LegalHoldRequest>
              caption="Legal hold history of the file"
              loading={history.isLoading}
              rows={history.data ?? []}
              rowKey={(r) => r.id}
              emptyMessage="No legal hold request for this file yet."
              columns={[
                { key: 'a', header: 'Request', render: (r) => holdActionLabel(r.action) },
                { key: 'r', header: 'Reason', render: (r) => r.reason },
                {
                  key: 'b',
                  header: 'Requested By',
                  render: (r) => (
                    <CellStack
                      main={<UserName login={r.requestedBy} />}
                      sub={formatDateTime(r.requestedAt)}
                    />
                  ),
                },
                {
                  key: 's',
                  header: 'Status',
                  kind: 'status',
                  render: (r) => <StatusBadge status={r.status} full />,
                },
                {
                  key: 'd',
                  header: 'Decided By',
                  render: (r) =>
                    r.decidedBy ? (
                      <CellStack
                        main={<UserName login={r.decidedBy} />}
                        sub={formatDateTime(r.decidedAt)}
                      />
                    ) : (
                      '—'
                    ),
                },
                { key: 'n', header: 'Decision Note', render: (r) => r.decisionNote ?? '—' },
              ]}
            />
          </>
        )}
      </div>
    </Card>
  );
}

/**
 * Legal Holds (Administration): the records hold officer requests to place or release the legal
 * hold of a file with a reason; the records hold approver approves or rejects each request with a
 * note (never their own). A file under legal hold is neither deleted nor purged.
 */
export default function LegalHoldsPage() {
  const { can } = useAuth();
  const [pending, setPending] = useState<Pending | null>(null);
  const requests = useQuery({
    queryKey: [...QUERY_KEY, 'pending'],
    queryFn: supportAdminApi.pendingHolds,
  });
  const mayApprove = can('FILE_LEGAL_HOLD_APPROVE');
  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Legal Holds"
        description="Requests to place or release the legal hold of a stored file, and their approval."
      />
      <Card title="Requests Waiting for Approval" flush>
        <ErrorAlert error={requests.error} />
        <DataTable<LegalHoldRequest>
          callout="legal-hold-requests"
          loading={requests.isLoading}
          rows={requests.data ?? []}
          rowKey={(r) => r.id}
          emptyMessage="No legal hold request waits for approval."
          columns={[
            {
              key: 'f',
              header: 'File',
              render: (r) => (
                <CellStack
                  main={fileLabel(r)}
                  sub={r.documentType ? humanize(r.documentType) : undefined}
                />
              ),
            },
            { key: 'a', header: 'Request', render: (r) => holdActionLabel(r.action) },
            { key: 'r', header: 'Reason', render: (r) => r.reason },
            { key: 'b', header: 'Requested By', render: (r) => <UserName login={r.requestedBy} /> },
            { key: 't', header: 'Requested On', render: (r) => formatDateTime(r.requestedAt) },
            {
              key: 'x',
              header: <span className="visually-hidden">Actions</span>,
              width: '64px',
              render: (r) => (
                <RowActionMenu
                  label={fileLabel(r)}
                  actions={
                    mayApprove
                      ? [
                          {
                            label: 'Approve',
                            onSelect: () => setPending({ kind: 'approve', request: r }),
                          },
                          {
                            label: 'Reject',
                            danger: true,
                            onSelect: () => setPending({ kind: 'reject', request: r }),
                          },
                        ]
                      : []
                  }
                />
              ),
            },
          ]}
        />
      </Card>
      {can('FILE_LEGAL_HOLD_REQUEST') && <FileLookup onRequest={setPending} />}
      {pending !== null && <DecisionDialog pending={pending} onClose={() => setPending(null)} />}
    </div>
  );
}
