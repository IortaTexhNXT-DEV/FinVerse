import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
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
import { folderLabel, moduleLabel, referenceText } from '@/utils/businessLabels';
import { CellStack } from '@/components/ui/CellStack';
import { RowActions } from '@/components/ui/RowActions';
import { UserName } from '@/components/ui/UserName';
import { useInsurerName } from '@/components/broking/useLabels';
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
    {
      key: 'src',
      header: 'From',
      render: (h) => (
        <CellStack main={moduleLabel(h.sourceModule)} sub={referenceText(h.sourceRef)} />
      ),
    },
    { key: 'amount', header: 'Amount', numeric: true, render: (h) => <Amount value={h.amount} /> },
    { key: 'at', header: 'Created', render: (h) => formatDateTime(h.createdAt) },
    { key: 'status', header: 'Status', render: (h) => <StatusBadge status={h.status} /> },
    {
      key: 'actions',
      header: '',
      kind: 'center',
      render: (h) => (
        <RowActions
          record={humanize(h.port)}
          actions={[
            {
              label: 'Close Hand-off',
              hidden: h.status !== 'OPEN' || !mayClose,
              onSelect: () => onClose(h),
            },
          ]}
        />
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
  const insurerName = useInsurerName();
  const files = useQuery({
    queryKey: ['ops', 'extracts', companyId],
    queryFn: () => opsApi.extracts(companyId),
    enabled: companyId > 0,
  });
  const columns: Column<ExtractFileInfo>[] = [
    {
      key: 'name',
      header: 'File',
      render: (f) => (
        <CellStack main={<strong>{f.fileName}</strong>} sub={folderLabel(f.folder, insurerName)} />
      ),
    },
    {
      key: 'src',
      header: 'From',
      render: (f) => (
        <CellStack main={moduleLabel(f.sourceModule)} sub={referenceText(f.sourceRef)} />
      ),
    },
    {
      key: 'size',
      header: 'Size',
      numeric: true,
      render: (f) => `${Math.ceil(f.sizeBytes / 1024).toLocaleString('en-PH')} KB`,
    },
    {
      key: 'at',
      header: 'Stored',
      render: (f) => (
        <CellStack main={formatDateTime(f.createdAt)} sub={<UserName login={f.createdBy} />} />
      ),
    },
    {
      key: 'dl',
      header: '',
      kind: 'center',
      render: (f) => (
        <RowActions
          record={f.fileName}
          actions={[
            { label: 'Download', onSelect: () => download.mutate(() => opsApi.extractFile(f.id)) },
          ]}
        />
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
        description="Work to complete by hand while an Operations module is not active."
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
