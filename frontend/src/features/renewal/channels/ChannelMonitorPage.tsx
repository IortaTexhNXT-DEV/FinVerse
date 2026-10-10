import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { useState } from 'react';
import { CHANNEL_STATUS_LABELS, renewalChannelsApi } from '@/api/renewalChannels';
import type { ChannelRow } from '@/api/renewalChannels';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { RowActionMenu } from '@/components/ui/RowActionMenu';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime } from '@/utils/format';
import { RENEWAL_SECTION } from '../common/renewalCodes';
import '../renewal.css';
import { ConnectionCard } from './ConnectionCard';

const STATUSES = Object.keys(CHANNEL_STATUS_LABELS);

function History({ row, onClose }: Readonly<{ row: ChannelRow; onClose: () => void }>) {
  const companyId = useCompanyId();
  const events = useQuery({
    queryKey: ['renewal', 'channel-history', companyId, row.messageNo],
    queryFn: () => renewalChannelsApi.history(companyId, row.messageNo),
  });
  return (
    <Modal
      open
      size="lg"
      title={`Delivery History ${row.messageNo}`}
      onClose={onClose}
      facts={[
        { label: 'Document', value: row.fileName ?? row.docKind },
        { label: 'Recipients', value: row.recipients ?? '' },
        { label: 'CCM transaction reference', value: row.externalRef ?? '' },
      ]}
    >
      <ErrorAlert error={events.error} />
      <DataTable
        loading={events.isLoading}
        rows={events.data ?? []}
        rowKey={(e) => `${e.at}-${e.status}`}
        emptyMessage="No status yet"
        columns={[
          { key: 'at', header: 'When', kind: 'datetime', render: (e) => formatDateTime(e.at) },
          { key: 'status', header: 'Status', render: (e) => e.label },
          { key: 'detail', header: 'Detail', render: (e) => e.detail ?? '' },
          { key: 'by', header: 'By', render: (e) => e.by },
        ]}
      />
    </Modal>
  );
}

/**
 * Channel Monitor: the documents handed to CCM (clients) and MFT (insurers) with their delivery
 * status, the delivery history, resend and cancel, the error report and the connection of each
 * channel.
 */
export default function ChannelMonitorPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const download = useFileDownload();
  const [channel, setChannel] = useState('');
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [open, setOpen] = useState<ChannelRow>();
  const rows = useQuery({
    queryKey: ['renewal', 'channels', companyId, channel, status, search],
    queryFn: () =>
      renewalChannelsApi.list(companyId, {
        channel: channel || undefined,
        status: status || undefined,
        search: search || undefined,
      }),
  });
  const act = useMutation({
    mutationFn: (call: () => Promise<{ messageNo: string; error: string | null }>) => call(),
    onSuccess: async (r) => {
      if (r.error) toast.error(r.error);
      else toast.success(`${r.messageNo}: done`);
      await queryClient.invalidateQueries({ queryKey: ['renewal', 'channels'] });
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section={RENEWAL_SECTION}
        title="Channel Monitor"
        description="Documents sent to clients through CCM and to insurers through MFT, with their delivery status."
        actions={
          <Button
            variant="secondary"
            icon={<Download size={16} />}
            onClick={() =>
              download.mutate(() => renewalChannelsApi.errorReport(companyId, channel || undefined))
            }
          >
            Error Report
          </Button>
        }
      />
      <div className="rnw-grid">
        <ConnectionCard channel="CCM" />
        <ConnectionCard channel="MFT" />
      </div>
      <div className="rnw-drill-tools">
        <select
          className="input"
          aria-label="Channel"
          value={channel}
          onChange={(e) => setChannel(e.target.value)}
        >
          <option value="">All channels</option>
          <option value="CCM">CCM</option>
          <option value="MFT">MFT</option>
        </select>
        <select
          className="input"
          aria-label="Delivery status"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {CHANNEL_STATUS_LABELS[s]}
            </option>
          ))}
        </select>
        <input
          className="input"
          aria-label="Search reference"
          placeholder="Reference number or document"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>
      <ErrorAlert error={rows.error ?? act.error} />
      <Card flush>
        <DataTable<ChannelRow>
          loading={rows.isLoading}
          rows={rows.data ?? []}
          rowKey={(r) => r.messageNo}
          emptyMessage="No message"
          columns={[
            { key: 'no', header: 'Message', kind: 'code', render: (r) => r.messageNo },
            { key: 'channel', header: 'Channel', render: (r) => r.channel },
            {
              key: 'ref',
              header: 'Reference Number',
              kind: 'code',
              render: (r) => r.renewalRef ?? '',
            },
            { key: 'file', header: 'File Name', render: (r) => r.fileName ?? r.docKind },
            { key: 'to', header: 'Recipients', render: (r) => r.recipients ?? '' },
            {
              key: 'status',
              header: 'Delivery Status',
              kind: 'status',
              render: (r) => (
                <StatusBadge
                  status={r.status}
                  label={CHANNEL_STATUS_LABELS[r.status] ?? r.status}
                />
              ),
            },
            { key: 'ext', header: 'CCM Transaction Reference', render: (r) => r.externalRef ?? '' },
            { key: 'error', header: 'Error', render: (r) => r.lastError ?? '' },
            {
              key: 'at',
              header: 'Queued',
              kind: 'datetime',
              render: (r) => formatDateTime(r.createdAt),
            },
            {
              key: 'actions',
              header: 'Actions',
              render: (r) => (
                <RowActionMenu
                  label={r.messageNo}
                  actions={[
                    { label: 'Delivery History', onSelect: () => setOpen(r) },
                    {
                      label: 'Resend',
                      disabled: !['FAILED', 'SENT', 'DELIVERED'].includes(r.status),
                      onSelect: () =>
                        act.mutate(() => renewalChannelsApi.resend(companyId, r.messageNo)),
                    },
                    {
                      label: 'Cancel',
                      danger: true,
                      disabled: !['PENDING_TRANSMISSION', 'FAILED'].includes(r.status),
                      onSelect: () =>
                        act.mutate(() => renewalChannelsApi.cancel(companyId, r.messageNo)),
                    },
                  ]}
                />
              ),
            },
          ]}
        />
      </Card>
      {open && <History row={open} onClose={() => setOpen(undefined)} />}
    </div>
  );
}
