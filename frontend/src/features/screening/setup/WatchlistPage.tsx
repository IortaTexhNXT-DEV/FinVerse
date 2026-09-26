import { useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import { screeningSetupApi } from './api';
import type { ChangeStatus, EntryStatus, ListSource, WatchlistChange, WatchlistEntry } from './api';
import { ChangeDialog } from './ChangeDialog';
import { EntryDetailDialog, EntryFormDialog } from './EntryDialogs';

type TabId = 'ENTRIES' | ChangeStatus;

const TABS: { id: TabId; label: string }[] = [
  { id: 'ENTRIES', label: 'Entries' },
  { id: 'PENDING', label: 'Pending Changes' },
  { id: 'APPROVED', label: 'Approved Changes' },
  { id: 'REJECTED', label: 'Rejected Changes' },
];

function EntriesTab({
  sources,
  onOpen,
}: Readonly<{ sources: ListSource[]; onOpen: (e: WatchlistEntry) => void }>) {
  const [filter, setFilter] = useState({
    search: '',
    source: '',
    status: '' as EntryStatus | '',
    page: 0,
  });
  const entries = useQuery({
    queryKey: ['screening-setup', 'watchlist', 'entries', filter],
    queryFn: () => screeningSetupApi.entries(filter),
  });
  return (
    <Card flush>
      <WorklistToolbar
        placeholder="Search name or reference"
        onSearch={(text) => setFilter({ ...filter, search: text, page: 0 })}
        extra={
          <>
            <select
              className="select"
              aria-label="Source"
              value={filter.source}
              onChange={(e) => setFilter({ ...filter, source: e.target.value, page: 0 })}
            >
              <option value="">All sources</option>
              {sources.map((s) => (
                <option key={s.code} value={s.code}>
                  {s.name}
                </option>
              ))}
            </select>
            <select
              className="select"
              aria-label="Status"
              value={filter.status}
              onChange={(e) =>
                setFilter({ ...filter, status: e.target.value as EntryStatus | '', page: 0 })
              }
            >
              <option value="">All statuses</option>
              {['ACTIVE', 'PENDING', 'DRAFT', 'INACTIVE'].map((s) => (
                <option key={s} value={s}>
                  {humanize(s)}
                </option>
              ))}
            </select>
          </>
        }
      />
      <ErrorAlert error={entries.error} />
      <DataTable<WatchlistEntry>
        caption="Watchlist entries"
        rows={entries.data?.content ?? []}
        rowKey={(e) => e.id}
        loading={entries.isLoading}
        onRowClick={onOpen}
        emptyMessage="No entries match the filters"
        columns={[
          {
            key: 'ref',
            header: 'Reference',
            render: (e) => <span className="mono">{e.externalRef}</span>,
          },
          { key: 'name', header: 'Name', render: (e) => <strong>{e.primaryName}</strong> },
          { key: 'source', header: 'Source', render: (e) => e.sourceCode },
          { key: 'list', header: 'List Type', render: (e) => humanize(e.listType) },
          { key: 'type', header: 'Entity', render: (e) => humanize(e.entityType) },
          { key: 'birth', header: 'Birth Date', render: (e) => formatDate(e.birthDate) },
          { key: 'status', header: 'Status', render: (e) => <StatusBadge status={e.status} /> },
          { key: 'from', header: 'Effective From', render: (e) => formatDate(e.effectiveFrom) },
        ]}
      />
      <PageFooter
        data={entries.data}
        noun="entries"
        onPage={(page) => setFilter({ ...filter, page })}
      />
    </Card>
  );
}

function ChangesTab({
  status,
  onOpen,
}: Readonly<{ status: ChangeStatus; onOpen: (c: WatchlistChange) => void }>) {
  const [page, setPage] = useState(0);
  const changes = useQuery({
    queryKey: ['screening-setup', 'watchlist', 'changes', status, page],
    queryFn: () => screeningSetupApi.changes(status, page),
  });
  return (
    <Card flush>
      <ErrorAlert error={changes.error} />
      <DataTable<WatchlistChange>
        caption="Watchlist changes"
        rows={changes.data?.content ?? []}
        rowKey={(c) => c.id}
        loading={changes.isLoading}
        onRowClick={onOpen}
        emptyMessage={status === 'PENDING' ? 'No change waits for approval' : 'No changes'}
        columns={[
          {
            key: 'ref',
            header: 'Reference',
            render: (c) => <span className="mono">{c.externalRef}</span>,
          },
          { key: 'name', header: 'Name', render: (c) => c.primaryName },
          { key: 'type', header: 'Change', render: (c) => humanize(c.changeType) },
          {
            key: 'maker',
            header: 'Maker',
            render: (c) => `${c.createdBy} ${formatDateTime(c.createdAt)}`,
          },
          { key: 'remarks', header: 'Remarks', render: (c) => c.makerRemarks },
          { key: 'status', header: 'Status', render: (c) => <StatusBadge status={c.status} /> },
        ]}
      />
      <PageFooter data={changes.data} noun="changes" onPage={setPage} />
    </Card>
  );
}

function shownLink(closed: boolean, change?: WatchlistChange): WatchlistChange | undefined {
  return closed ? undefined : change;
}

/**
 * Watchlist (SNSRP-203, 204; FR-SS-022, 023): entries by source and status; Add Entry, Change and
 * Deactivate as changes waiting for a Compliance Checker; the pending, approved and rejected
 * changes with their before and after values.
 */
export default function WatchlistPage() {
  const { can } = useAuth();
  const [search] = useSearchParams();
  const linked = Number(search.get('change') ?? 0);
  const [tab, setTab] = useState<TabId>(linked > 0 ? 'PENDING' : 'ENTRIES');
  const [adding, setAdding] = useState(false);
  const [entryId, setEntryId] = useState<number>();
  const [change, setChange] = useState<WatchlistChange>();
  const sources = useQuery({
    queryKey: ['screening-setup', 'sources'],
    queryFn: screeningSetupApi.sources,
  });
  const linkedChange = useQuery({
    queryKey: ['screening-setup', 'watchlist', 'change', linked],
    queryFn: () => screeningSetupApi.change(linked),
    enabled: linked > 0,
  });
  const [linkClosed, setLinkClosed] = useState(false);
  const shownChange = change ?? shownLink(linkClosed, linkedChange.data);
  return (
    <div className="stack">
      <PageHeader
        section="Setup & Administration · Compliance Setup"
        title="Watchlist"
        description="Sanctioned names, PEPs and internal watchlist entries. Manual additions, changes and deactivations wait for a Compliance Checker; screening uses active entries only."
        actions={
          can('SCR_LIST_MAINTAIN') && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setAdding(true)}>
              Add Entry
            </Button>
          )
        }
      />
      <ErrorAlert error={sources.error} />
      <ErrorAlert error={linkedChange.error} />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'ENTRIES' ? (
        <EntriesTab sources={sources.data ?? []} onOpen={(e) => setEntryId(e.id)} />
      ) : (
        <ChangesTab key={tab} status={tab} onOpen={setChange} />
      )}
      {adding && <EntryFormDialog sources={sources.data ?? []} onClose={() => setAdding(false)} />}
      {entryId !== undefined && (
        <EntryDetailDialog
          entryId={entryId}
          sources={sources.data ?? []}
          onOpenChange={setChange}
          onClose={() => setEntryId(undefined)}
        />
      )}
      {shownChange !== undefined && (
        <ChangeDialog
          change={shownChange}
          onClose={() => {
            setChange(undefined);
            setLinkClosed(true);
          }}
        />
      )}
    </div>
  );
}
