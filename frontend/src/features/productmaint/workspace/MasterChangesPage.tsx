import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Send } from 'lucide-react';
import { useState } from 'react';
import { pmRoutingApi } from '@/api/pmRouting';
import type { InboxFile, MasterChange, MasterMonitor } from '@/api/pmRouting';
import { useAuth } from '@/auth/authContext';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime } from '@/utils/format';

const KINDS: Readonly<Record<string, string>> = {
  RELEASED: 'Version released',
  RETIRED: 'Package deactivated',
  EXPIRED: 'Package expired',
};

const STATUS: Readonly<Record<MasterChange['status'], string>> = {
  PENDING: 'Waiting',
  SENT: 'Sent',
  FAILED: 'Failed',
};

const TABS = [
  { id: '', label: 'All' },
  { id: 'PENDING', label: 'Waiting' },
  { id: 'FAILED', label: 'Failed' },
  { id: 'SENT', label: 'Sent' },
  { id: 'INBOX', label: 'Receiving System (SIT/UAT)' },
] as const;

type TabId = (typeof TABS)[number]['id'];

function columns(reprocess: ((id: number) => void) | undefined): Column<MasterChange>[] {
  return [
    { key: 'p', header: 'Package', render: (c) => <ReferenceChip value={c.productCode} /> },
    {
      key: 'v',
      header: 'Version',
      render: (c) => (c.versionNo === null ? '' : String(c.versionNo)),
    },
    { key: 'k', header: 'Change', render: (c) => KINDS[c.changeKind] ?? c.changeKind },
    { key: 'e', header: 'Effective Date', render: (c) => formatDate(c.effectiveDate) },
    { key: 'r', header: 'Request', render: (c) => c.sourceRequestNo ?? '' },
    {
      key: 's',
      header: 'Status',
      render: (c) => <StatusBadge status={c.status} label={STATUS[c.status]} full />,
    },
    { key: 'f', header: 'File', render: (c) => c.fileName ?? '' },
    { key: 't', header: 'Sent', render: (c) => formatDateTime(c.sentAt) },
    { key: 'x', header: 'Error', truncate: true, render: (c) => c.error ?? '' },
    {
      key: 'actions',
      header: 'Actions',
      kind: 'actions',
      render: (c) => (
        <RowActionMenu
          label={c.productCode}
          actions={[
            {
              label: 'Send Again',
              disabled: c.status !== 'FAILED' || reprocess === undefined,
              onSelect: () => reprocess?.(c.id),
            },
          ]}
        />
      ),
    },
  ];
}

const INBOX_COLUMNS: Column<InboxFile>[] = [
  { key: 'f', header: 'File', render: (f) => <strong>{f.fileName}</strong> },
  { key: 'n', header: 'Records', numeric: true, render: (f) => String(f.recordCount) },
  { key: 't', header: 'Received', render: (f) => formatDateTime(f.receivedAt) },
  {
    key: 'c',
    header: 'Content',
    render: (f) => <pre className="muted">{f.content}</pre>,
  },
];

function sentText(fileName: string | null, sent: number): string {
  return fileName === null ? 'Nothing to send' : `${String(sent)} change(s) sent in ${fileName}`;
}

function connectedText(m: MasterMonitor | undefined): string {
  if (m === undefined) {
    return '';
  }
  return m.connected ? 'Yes' : 'No';
}

function MonitorSummary({ monitor: m }: Readonly<{ monitor: MasterMonitor | undefined }>) {
  return (
    <Card>
      <DefinitionGrid
        items={[
          { label: 'Receiving System', value: m?.target ?? '' },
          { label: 'Connected', value: connectedText(m) },
          { label: 'Waiting', value: String(m?.pending ?? 0) },
          { label: 'Failed', value: String(m?.failed ?? 0) },
        ]}
      />
    </Card>
  );
}

/**
 * Product Master Transfers (BDOI FRS FRPM.029.01): the product master changes for the other
 * systems of the bank with their status, transfer file and error; Send Now sends the waiting changes, a failed
 * change is sent again from its row; in SIT and UAT the files of the simulated receiving system.
 */
export default function MasterChangesPage() {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('');
  const [page, setPage] = useState(0);
  const monitor = useQuery({
    queryKey: ['pm-master', tab, page],
    queryFn: () => pmRoutingApi.masterChanges(tab === 'INBOX' ? undefined : tab, page),
  });
  const inbox = useQuery({
    queryKey: ['pm-master', 'inbox'],
    queryFn: () => pmRoutingApi.receivedFiles(),
    enabled: tab === 'INBOX',
  });
  const done = async (fileName: string | null, sent: number) => {
    toast.success(sentText(fileName, sent));
    await queryClient.invalidateQueries({ queryKey: ['pm-master'] });
  };
  const send = useMutation({
    mutationFn: () => pmRoutingApi.transferNow(),
    onSuccess: (r) => done(r.fileName, r.sent),
  });
  const again = useMutation({
    mutationFn: (id: number) => pmRoutingApi.reprocess(id),
    onSuccess: (r) => done(r.fileName, r.sent),
  });
  const maintain = can('PRODUCT_MAINTAIN');
  const m = monitor.data;
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Product Master Transfers"
        description="Product master changes sent to the other systems of the bank."
        actions={
          maintain && (
            <Button icon={<Send size={16} />} busy={send.isPending} onClick={() => send.mutate()}>
              Send Now
            </Button>
          )
        }
      />
      <ErrorAlert error={monitor.error ?? send.error ?? again.error} />
      <MonitorSummary monitor={m} />
      <Card flush>
        <Tabs
          tabs={TABS}
          active={tab}
          onChange={(t) => {
            setTab(t);
            setPage(0);
          }}
        />
        {tab === 'INBOX' ? (
          <DataTable<InboxFile>
            loading={inbox.isLoading}
            rows={inbox.data ?? []}
            rowKey={(f) => f.id}
            columns={INBOX_COLUMNS}
            emptyMessage="No file received yet."
          />
        ) : (
          <>
            <DataTable<MasterChange>
              loading={monitor.isLoading}
              rows={m?.changes.content ?? []}
              rowKey={(c) => c.id}
              columns={columns(maintain ? (id) => again.mutate(id) : undefined)}
              emptyMessage="No product master change."
            />
            <PageFooter data={m?.changes} noun="changes" onPage={setPage} />
          </>
        )}
      </Card>
    </div>
  );
}
