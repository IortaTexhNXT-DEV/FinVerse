import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileDown, Paperclip } from 'lucide-react';
import { migrationApi } from '@/api/migration';
import type { BatchLog, Signoff } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Tag } from '@/components/ui/Tag';
import { formatDateTime } from '@/utils/format';
import { MigStatus } from '../common/MigStatus';
import { migLabel } from '../common/migrationCodes';
import { useDownload } from '../common/useDownload';

function EvidenceCell({
  signoff,
  mine,
  onDownload,
  onAttach,
}: Readonly<{
  signoff: Signoff;
  mine: boolean;
  onDownload: () => void;
  onAttach: (file: File) => void;
}>) {
  if (signoff.evidenceFileId !== undefined) {
    return (
      <Button variant="ghost" size="sm" icon={<FileDown size={14} />} onClick={onDownload}>
        {signoff.evidenceName}
      </Button>
    );
  }
  if (!mine) {
    return null;
  }
  return (
    <label className="btn btn-ghost btn-sm">
      <Paperclip size={14} /> Attach
      <input
        type="file"
        hidden
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) {
            onAttach(file);
          }
        }}
      />
    </label>
  );
}

/** The gates signed on a batch with their evidence. */
export function BatchSignoffs({ batchNo }: Readonly<{ batchNo: string }>) {
  const client = useQueryClient();
  const download = useDownload();
  const { user } = useAuth();
  const signoffs = useQuery({
    queryKey: ['migration', 'batch', batchNo, 'signoffs'],
    queryFn: () => migrationApi.batchSignoffs(batchNo),
  });
  const attach = useMutation({
    mutationFn: (input: { id: number; file: File }) =>
      migrationApi.attachEvidence(input.id, input.file),
    onSuccess: () => client.invalidateQueries({ queryKey: ['migration'] }),
  });
  return (
    <Card flush>
      <ErrorAlert error={signoffs.error ?? attach.error ?? download.error} />
      <DataTable<Signoff>
        loading={signoffs.isLoading}
        rows={signoffs.data ?? []}
        rowKey={(s) => s.id}
        emptyMessage="No gate signed on this batch"
        columns={[
          {
            key: 'gate',
            header: 'Gate',
            render: (s) => <CellStack main={s.gate} sub={s.gateLabel} />,
          },
          { key: 'role', header: 'Role', render: (s) => migLabel(s.roleCode) },
          { key: 'user', header: 'Signed by', render: (s) => s.username },
          {
            key: 'at',
            header: 'Signed',
            kind: 'datetime',
            render: (s) => formatDateTime(s.signedAt),
          },
          { key: 'comment', header: 'Comment', render: (s) => s.comment ?? '' },
          {
            key: 'decision',
            header: 'Decision',
            kind: 'status',
            render: (s) => <MigStatus status={s.decision} />,
          },
          {
            key: 'evidence',
            header: 'Evidence',
            render: (s) => (
              <EvidenceCell
                signoff={s}
                mine={s.username === user?.username}
                onDownload={() => download.run(() => migrationApi.evidence(s.id))}
                onAttach={(file) => attach.mutate({ id: s.id, file })}
              />
            ),
          },
        ]}
      />
    </Card>
  );
}

/** The run log of a batch. */
export function BatchLogTable({ batchNo }: Readonly<{ batchNo: string }>) {
  const log = useQuery({
    queryKey: ['migration', 'batch', batchNo, 'log'],
    queryFn: () => migrationApi.batchLog(batchNo),
  });
  return (
    <Card flush>
      <ErrorAlert error={log.error} />
      <DataTable<BatchLog>
        loading={log.isLoading}
        rows={log.data ?? []}
        rowKey={(l) => `${l.loggedAt}-${l.step}-${l.message}`}
        emptyMessage="No log line"
        columns={[
          {
            key: 'at',
            header: 'Time',
            kind: 'datetime',
            render: (l) => formatDateTime(l.loggedAt),
          },
          { key: 'step', header: 'Step', render: (l) => migLabel(l.step) },
          {
            key: 'level',
            header: 'Level',
            render: (l) => (
              <Tag tone={l.level === 'ERROR' ? 'danger' : 'neutral'}>{migLabel(l.level)}</Tag>
            ),
          },
          { key: 'message', header: 'Message', render: (l) => l.message },
          { key: 'counts', header: 'Counts', render: (l) => l.counts ?? '' },
          { key: 'by', header: 'By', render: (l) => l.loggedBy },
        ]}
      />
    </Card>
  );
}
