import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { useState } from 'react';
import { opsApi } from '@/api/operations';
import type { ExtractFileInfo, Handoff } from '@/api/operations';
import { useAuth } from '@/auth/authContext';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, humanize } from '@/utils/format';
import './operations.css';

const TABS = [
  { id: 'OPEN', label: 'Open Hand-offs' },
  { id: 'CLOSED', label: 'Closed Hand-offs' },
  { id: 'EXTRACTS', label: 'Extract Repository' },
] as const;

type TabId = (typeof TABS)[number]['id'];

const CLOSERS = ['CASH_RECEIPT', 'CASH_DISPOSITION', 'FLOWIN_MANAGE'];

function CloseDialog({ handoff, onClose }: Readonly<{ handoff: Handoff; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [note, setNote] = useState('');
  const [error, setError] = useState<string>();
  const close = useMutation({
    mutationFn: () => opsApi.closeHandoff(handoff.id, note.trim()),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['ops'] });
      toast.success('Hand-off closed');
      onClose();
    },
  });
  return (
    <Modal
      title="Close Hand-off"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={close.isPending}
            onClick={() => (note.trim() === '' ? setError('Say what was done') : close.mutate())}
          >
            Close Hand-off
          </Button>
        </>
      }
    >
      <div className="stack">
        <p>{handoff.summary}</p>
        <ErrorAlert error={close.error} />
        <Field label="What Was Done" required error={error}>
          {(id) => (
            <input
              id={id}
              className="input"
              maxLength={200}
              value={note}
              onChange={(e) => {
                setNote(e.target.value);
                setError(undefined);
              }}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function HandoffTable({
  status,
  onClose,
}: Readonly<{ status: 'OPEN' | 'CLOSED'; onClose: (h: Handoff) => void }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [page, setPage] = useState(0);
  const rows = useQuery({
    queryKey: ['ops', 'handoffs', companyId, status, page],
    queryFn: () => opsApi.handoffs(companyId, status, page),
    enabled: companyId > 0,
  });
  const mayClose = CLOSERS.some(can);
  const columns: Column<Handoff>[] = [
    { key: 'port', header: 'Work', render: (h) => humanize(h.port) },
    { key: 'summary', header: 'What To Do', render: (h) => h.summary },
    { key: 'src', header: 'From', render: (h) => `${humanize(h.sourceModule)} · ${h.sourceRef}` },
    { key: 'amount', header: 'Amount', numeric: true, render: (h) => <Amount value={h.amount} /> },
    { key: 'at', header: 'Created', render: (h) => formatDateTime(h.createdAt) },
    {
      key: 'status',
      header: 'Status',
      render: (h) =>
        h.status === 'OPEN' && mayClose ? (
          <Button size="sm" variant="secondary" onClick={() => onClose(h)}>
            Close
          </Button>
        ) : (
          <StatusBadge status={h.status} />
        ),
    },
  ];
  return (
    <>
      <ErrorAlert error={rows.error} />
      <DataTable
        caption="Hand-offs"
        columns={columns}
        rows={rows.data?.content ?? []}
        rowKey={(h) => h.id}
        loading={rows.isLoading}
        emptyMessage="No items to display"
      />
      <PageFooter data={rows.data} noun="hand-offs" onPage={setPage} />
    </>
  );
}

function Extracts() {
  const companyId = useCompanyId();
  const download = useFileDownload();
  const files = useQuery({
    queryKey: ['ops', 'extracts', companyId],
    queryFn: () => opsApi.extracts(companyId),
    enabled: companyId > 0,
  });
  const columns: Column<ExtractFileInfo>[] = [
    { key: 'folder', header: 'Folder', render: (f) => f.folder },
    { key: 'name', header: 'File', render: (f) => <strong>{f.fileName}</strong> },
    {
      key: 'src',
      header: 'From',
      render: (f) => `${humanize(f.sourceModule)} ${f.sourceRef ?? ''}`,
    },
    {
      key: 'size',
      header: 'Size',
      numeric: true,
      render: (f) => `${Math.ceil(f.sizeBytes / 1024)} KB`,
    },
    {
      key: 'at',
      header: 'Stored',
      render: (f) => `${formatDateTime(f.createdAt)} · ${f.createdBy}`,
    },
    {
      key: 'dl',
      header: 'Download',
      render: (f) => (
        <Button
          size="sm"
          variant="secondary"
          icon={<Download size={14} />}
          onClick={() => download.mutate(() => opsApi.extractFile(f.id))}
        >
          Download
        </Button>
      ),
    },
  ];
  return (
    <>
      <ErrorAlert error={files.error ?? download.error} />
      <DataTable
        caption="Extract repository"
        columns={columns}
        rows={files.data ?? []}
        rowKey={(f) => f.id}
        loading={files.isLoading}
        emptyMessage="No extracts yet"
      />
    </>
  );
}

/**
 * Hand-offs and extracts: work handed over by a port whose module is not active yet (an OR to
 * issue, an unapplied item to set up) to do by hand, and the in-system extract repository that
 * stands in for the shared-drive folders (OQ17).
 */
export default function HandoffsPage() {
  const [tab, setTab] = useState<TabId>('OPEN');
  const [closing, setClosing] = useState<Handoff>();
  return (
    <div className="stack">
      <PageHeader
        section="Operations"
        title="Hand-offs and Extracts"
        description="Work to complete by hand while an Operations module is not active, and the files Operations produced for other teams."
      />
      <Card>
        <div className="stack">
          <Tabs tabs={TABS} active={tab} onChange={setTab} />
          {tab === 'EXTRACTS' ? <Extracts /> : <HandoffTable status={tab} onClose={setClosing} />}
        </div>
      </Card>
      {closing !== undefined && (
        <CloseDialog handoff={closing} onClose={() => setClosing(undefined)} />
      )}
    </div>
  );
}
