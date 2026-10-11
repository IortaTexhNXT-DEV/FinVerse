import { CellStack } from '@/components/ui/CellStack';
import { useQuery } from '@tanstack/react-query';
import { History, RefreshCw } from 'lucide-react';
import { useState } from 'react';
import { opsApi } from '@/api/operations';
import type { Feed, FeedRun } from '@/api/operations';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatDateTime, humanize } from '@/utils/format';
import { moduleLabel } from '@/utils/businessLabels';
import { scheduleText } from '@/utils/schedule';
import { RowActions } from '@/components/ui/RowActions';
import {
  FeedConfigDialog,
  FeedUploadDialog,
  ReplayDialog,
  RunRecordsDialog,
} from './InterfaceDialogs';
import './operations.css';

const TABS = [
  { id: 'feeds', label: 'Feeds' },
  { id: 'runs', label: 'Run Log' },
] as const;

type TabId = (typeof TABS)[number]['id'];

type Dialog =
  { kind: 'config' | 'upload'; feed: Feed } | { kind: 'replay' } | { kind: 'run'; run: FeedRun };

function RunLog({ onOpen }: Readonly<{ onOpen: (run: FeedRun) => void }>) {
  const [page, setPage] = useState(0);
  const feeds = useQuery({ queryKey: ['ops', 'feeds'], queryFn: opsApi.feeds });
  const feedName = (code: string) =>
    feeds.data?.find((f) => f.code === code)?.name ?? humanize(code);
  const runs = useQuery({
    queryKey: ['ops', 'runs', page],
    queryFn: () => opsApi.runs(undefined, page),
  });
  const columns: Column<FeedRun>[] = [
    {
      key: 'no',
      header: 'Run No. / Trigger',
      render: (r) => <CellStack main={<strong>{r.runNo}</strong>} sub={humanize(r.trigger)} />,
    },
    {
      key: 'feed',
      header: 'Feed / File',
      render: (r) => <CellStack main={feedName(r.feedCode)} sub={r.fileName} />,
    },
    { key: 'start', header: 'Started', render: (r) => formatDateTime(r.startedAt) },
    {
      key: 'counts',
      header: 'Read / OK / Duplicate / Failed',
      render: (r) => `${r.readCount} / ${r.okCount} / ${r.duplicateCount} / ${r.failedCount}`,
    },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
  ];
  return (
    <>
      <ErrorAlert error={runs.error} />
      <DataTable
        caption="Interface runs"
        columns={columns}
        rows={runs.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={runs.isLoading}
        onRowClick={onOpen}
        emptyMessage="No runs yet"
      />
      <PageFooter data={runs.data} noun="runs" onPage={setPage} />
    </>
  );
}

function Feeds({ onDialog }: Readonly<{ onDialog: (d: Dialog) => void }>) {
  const feeds = useQuery({ queryKey: ['ops', 'feeds'], queryFn: opsApi.feeds });
  const columns: Column<Feed>[] = [
    {
      key: 'name',
      header: 'Feed',
      render: (f) => <strong>{f.name}</strong>,
    },
    {
      key: 'partner',
      header: 'Partner',
      render: (f) => <CellStack main={humanize(f.partnerSystem)} sub={humanize(f.direction)} />,
    },
    { key: 'transport', header: 'Transport', render: (f) => humanize(f.transport) },
    { key: 'owner', header: 'Owner', render: (f) => moduleLabel(f.ownerModule) },
    { key: 'cron', header: 'Schedule', render: (f) => scheduleText(f.cron) },
    {
      key: 'status',
      header: 'Status',
      render: (f) => <StatusBadge status={f.active ? 'ACTIVE' : 'INACTIVE'} />,
    },
    {
      key: 'actions',
      header: '',
      kind: 'center',
      render: (f) => (
        <RowActions
          record={f.name}
          actions={[
            { label: 'Configure', onSelect: () => onDialog({ kind: 'config', feed: f }) },
            {
              label: 'Upload File',
              hidden: !f.uploadable || !f.active,
              onSelect: () => onDialog({ kind: 'upload', feed: f }),
            },
          ]}
        />
      ),
    },
  ];
  return (
    <>
      <ErrorAlert error={feeds.error} />
      <DataTable
        caption="Interface feeds"
        columns={columns}
        rows={feeds.data ?? []}
        rowKey={(f) => f.code}
        loading={feeds.isLoading}
      />
    </>
  );
}

/**
 * Interfaces with other systems (BRQID.004/005/006): the feeds with their transport and schedule,
 * the run log with every record's outcome, manual uploads and the replay of the booking feed.
 * Until BDOI gives the interface specifications (OQ01), external data arrives by upload.
 */
export default function InterfacesPage() {
  const [tab, setTab] = useState<TabId>('feeds');
  const [dialog, setDialog] = useState<Dialog>();
  const close = () => setDialog(undefined);
  return (
    <div className="stack">
      <PageHeader
        section="Operations"
        title="Interfaces"
        description="Feeds with Collection, Disbursement, insurers and booking."
        actions={
          <Button
            variant="secondary"
            icon={<RefreshCw size={16} />}
            onClick={() => setDialog({ kind: 'replay' })}
          >
            Replay Booked Invoices
          </Button>
        }
      />
      <Card>
        <div className="stack">
          <Tabs tabs={TABS} active={tab} onChange={setTab} />
          {tab === 'feeds' ? (
            <Feeds onDialog={setDialog} />
          ) : (
            <RunLog onOpen={(run) => setDialog({ kind: 'run', run })} />
          )}
        </div>
      </Card>
      <p className="ops-muted">
        <History size={12} aria-hidden="true" /> A failed or partial run alerts the System
        Administrator (Interface run failed); failed records can be sent again and are taken in
        once.
      </p>
      {dialog?.kind === 'config' && <FeedConfigDialog feed={dialog.feed} onClose={close} />}
      {dialog?.kind === 'upload' && <FeedUploadDialog feed={dialog.feed} onClose={close} />}
      {dialog?.kind === 'replay' && <ReplayDialog onClose={close} />}
      {dialog?.kind === 'run' && <RunRecordsDialog run={dialog.run} onClose={close} />}
    </div>
  );
}
